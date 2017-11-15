package utils

object Constants {
  //消费的主题
  var TOPICS = "topics"

  //消费分组
  val GROUP_ID = "group.id"

  //客户端ID
  val CLIENT_ID = "client.id"

  //kafka连接的zk地址
  var KAFKA_ZK_CONNECT = "kafka.zookeeper.connect"

  //kafka的broker列表
  val KAFKA_METADATA_BROKER_LIST = "kafka.metadata.broker.list"

  //kafka zk会话超时
  val KAFKA_ZK_SESSION_TIMEOUT = "kafka.zookeeper.session.timeout.ms"

  //kafka zk默认超时
  val KAFKA_ZK_SESSION_TIMEOUT_DEFAULT = "1000"

  //kafka zk连接超时
  val KAFKA_ZK_CONNECTION_TIMEOUT = "kafka.zookeeper.connection.timeout.ms"

  //kafka zk连接默认超时
  val KAFKA_ZK_CONNECTION_TIMEOUT_DEFAULT = "3000"





  //zk的连接地址,存储相关信息到zk
  var ZK_CONNECT = "zookeeper.connect"
  //zk会话超时
  val ZK_SESSION_TIMEOUT = "zookeeper.session.timeout.ms"

  //zk默认超时
  val ZK_SESSION_TIMEOUT_DEFAULT = "1000"

  //zk连接超时
  val ZK_CONNECTION_TIMEOUT = "zookeeper.connection.timeout.ms"

  //zk连接默认超时
  val ZK_CONNECTION_TIMEOUT_DEFAULT = "3000"

}
