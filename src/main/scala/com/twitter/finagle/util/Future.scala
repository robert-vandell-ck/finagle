package com.twitter.finagle.util

import java.util.concurrent.atomic.{AtomicBoolean, AtomicReference}

import org.jboss.netty.channel._

/**
 * A ChannelFuture that doesn't need to have a channel on creation.
 */
class LatentChannelFuture extends DefaultChannelFuture(null, false) {
  @volatile private var channel: Channel = _

  def setChannel(c: Channel) { channel = c }
  override def getChannel() = channel
}

sealed abstract class State
case object Cancelled extends State
case class Ok(channel: Channel) extends State
case class Error(cause: Throwable) extends State

// TODO: decide what to do about cancellation here.
class RichChannelFuture(val self: ChannelFuture) {
  def apply(f: State => Unit) {
    self.addListener(new ChannelFutureListener {
      def operationComplete(future: ChannelFuture) {
        f(new RichChannelFuture(future).state)
      }
    })
  }

  // def apply(f: ChannelFuture => Unit) {
  //   if (self.isDone) {
  //     f(self)
  //   } else {
  //     self.addListener(new ChannelFutureListener {
  //       def operationComplete(future: ChannelFuture) { f(future) }
  //     })
  //   }
  // }

  def state: State =
    if (self.isSuccess)
      Ok(self.getChannel)
    else if (self.isCancelled)
      Cancelled
    else
      Error(self.getCause)

  def proxyTo(other: ChannelFuture) {
    this {
      case Ok(channel)  => other.setSuccess()
      case Error(cause) => other.setFailure(cause)
      case Cancelled    => other.cancel()
    }
  }

  /**
   * the ChannelFuture forms a Monad.
   */
  def flatMap(f: Channel => ChannelFuture): ChannelFuture = {
    val future = new LatentChannelFuture

    this {
      case Ok(channel) =>
        val nextFuture = f(channel)
        nextFuture.addListener(new ChannelFutureListener {
          def operationComplete(nextFuture: ChannelFuture) {
            future.setChannel(nextFuture.getChannel)
            if (nextFuture.isSuccess)
              future.setSuccess()
            else
              future.setFailure(nextFuture.getCause)
          }
        })
      case Error(throwable) =>
        future.setFailure(throwable)
      case Cancelled =>
        future.cancel()
    }

    future
  }

  def map[T](f: Channel => Channel) = {
    val future = new LatentChannelFuture
    future.setChannel(self.getChannel)

    this {
      case Ok(channel) =>
        future.setChannel(f(channel))
        future.setSuccess()
      case Error(cause) =>
        future.setFailure(cause)
      case Cancelled =>
        future.cancel()
    }

    future
  }

  def foreach[T](f: Channel => T) {
    this {
      case Ok(channel) => f(channel)
      case _ => ()
    }
  }

  def onSuccess(f: => Unit) = {
    foreach { _ => f }
    self
  }

  def onError(f: Throwable => Unit) = {
    this {
      case Error(cause) => f(cause)
      case _ => ()
    }
    self
  }

  def onSuccessOrFailure(f: => Unit) {
    this {
      case Cancelled => ()
      case _ => f
    }
  }

  // tbd
  // def always(f: Channel => Unit) =
  //   this { case state: State => f(self.getChannel) }

  def orElse(other: RichChannelFuture): ChannelFuture = {
    val combined = new LatentChannelFuture

    this.proxyTo(combined)
    other.proxyTo(combined)

    combined
  }

  def andThen(next: ChannelFuture): ChannelFuture = flatMap { _ => next }

  def joinWith(other: RichChannelFuture): ChannelFuture = {
    val joined = Channels.future(self.getChannel)
    val latch = new AtomicBoolean(false)
    @volatile var cause: Throwable = null

    def maybeSatisfy() =
      if (!latch.compareAndSet(false, true)) {
        if (cause ne null)
          joined.setFailure(cause)
        else
          joined.setSuccess()
      }

    for (f <- List(this, other)) {
      f {
        case Ok(_) =>
          maybeSatisfy()
        case Error(theCause) =>
          cause = theCause
          maybeSatisfy()
      }
    }

    joined
  }

  def close() {
    self.addListener(ChannelFutureListener.CLOSE)
 }
}

object Conversions {
  implicit def channelFutureToRichChannelFuture(f: ChannelFuture) =
    new RichChannelFuture(f)
}