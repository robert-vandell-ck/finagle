scala_library(
  provides = scala_artifact(
    org = 'com.twitter',
    name = 'finagle-memcached',
    repo = artifactory,
  ),
  dependencies=[
    '3rdparty/jvm/com/fasterxml/jackson/core:jackson-databind',
    '3rdparty/jvm/com/fasterxml/jackson/module:jackson-module-scala',
    '3rdparty/jvm/com/github/ben-manes/caffeine:caffeine',
    '3rdparty/jvm/com/google/inject:guice',
    '3rdparty/jvm/com/twitter/bijection:core',
    '3rdparty/jvm/io/netty:netty4',
    '3rdparty/jvm/io/netty:netty4-handler-proxy',
    '3rdparty/jvm/io/netty:netty4-http',
    '3rdparty/jvm/org/apache/thrift:libthrift-0.5.0',
    '3rdparty/jvm/org/apache/zookeeper:zookeeper-client',
    'finagle/finagle-core/src/main/scala:scala',
    'finagle/finagle-memcached/src/main/resources',
    'finagle/finagle-netty4',
    'finagle/finagle-serversets',
    'finagle/finagle-serversets/src/main/java:java',
    'finagle/finagle-serversets/src/main/thrift:thrift-java',
    'finagle/finagle-toggle/src/main/scala:scala',
    'util/util-app/src/main/scala:scala',
    'util/util-collection/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
    'util/util-function/src/main/java:java',
    'util/util-hashing',
    'util/util-logging/src/main/scala:scala',
    'util/util-registry/src/main/scala:scala',
    'util/util-stats/src/main/scala:scala',
  ],
  fatal_warnings=True,
  sources=rglobs('*.scala'),
  # Exposes a service-loaded class.
  scope='forced',
  # NB: This is not an actual dependency, but is necessary to have Pants behave
  #     like SBT, throwing an error for "import java.*" statements.
  java_sources=['finagle/finagle-memcached/src/main/java'],
  exports=[
    '3rdparty/jvm/com/twitter/bijection:core',
    'finagle/finagle-core/src/main/scala:scala',
    'util/util-core/src/main/scala:scala',
  ]
)
