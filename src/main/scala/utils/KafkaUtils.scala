package utils

import kafka.api._
import kafka.common.{ErrorMapping, TopicAndPartition}
import kafka.consumer.SimpleConsumer
import kafka.utils.{Json, ZkUtils}
import org.apache.kafka.common.security.JaasUtils

import scala.collection.mutable
import scala.util.control.NonFatal


object KafkaUtils {
  //定义操作日志
  //var  logger :Logger = new Logger("kafkaUtils object");

  /**
    * 获取操作kafka的ZKUtils
    */
  def getZkUtils(conf:Configuration): ZkUtils ={
    // 从配置获取连接地址等信息
    val zkUrl = conf.get(Constants.ZK_CONNECT)
    val sessionTimeout = conf.getOrElse(Constants.ZK_SESSION_TIMEOUT, Constants.ZK_SESSION_TIMEOUT_DEFAULT).toInt
    val connectionTimeout = conf.getOrElse(Constants.ZK_CONNECTION_TIMEOUT, Constants.ZK_CONNECTION_TIMEOUT_DEFAULT).toInt
    // 创建zkClient
    val zkClient = ZkUtils.createZkClient(zkUrl, sessionTimeout, connectionTimeout)
    val zkUtils = ZkUtils(zkClient, JaasUtils.isZkSecurityEnabled)
    zkUtils
  }



  /**
    * 获取操作kafka的ZKUtils
    */
  def getKafkaBrokerZkUtils(conf:Configuration): ZkUtils ={
    var zkUrl = conf.get(Constants.KAFKA_ZK_CONNECT)
    var sessionTimeout = conf.getOrElse(Constants.KAFKA_ZK_SESSION_TIMEOUT,Constants.KAFKA_ZK_SESSION_TIMEOUT_DEFAULT).toInt
    var connectionTimeout = conf.getOrElse(Constants.KAFKA_ZK_CONNECTION_TIMEOUT,Constants.KAFKA_ZK_CONNECTION_TIMEOUT_DEFAULT).toInt
    var zkClient = ZkUtils.createZkClient(zkUrl, sessionTimeout, connectionTimeout)
    val zkUtils = ZkUtils(zkClient, JaasUtils.isZkSecurityEnabled)
    zkUtils
  }



  /**
    * 获取topic 对应分区的偏移量
    * @return
    */
  def getTopicAndPartitionOffsetInfo(soTimeOut:Int,soBufferSize:Int,clientId:String,topicMetadataSeq:Seq[TopicMetadata],brokerZkUtils:ZkUtils) : Map[TopicAndPartition, (Long, Long)] ={
    //定义返回
    val tpsAndPrts = mutable.Map[TopicAndPartition, (Long, Long)]()

    if(! topicMetadataSeq.isEmpty){
      topicMetadataSeq.foreach(tm =>{
        var topic = tm.topic
        tm.partitionsMetadata.foreach(pm =>{
          var tp = new TopicAndPartition(topic,pm.partitionId)
          // 从zk找到该分区的leader broker的信息
          val brokerId = brokerZkUtils.getLeaderForPartition(topic, pm.partitionId).get
          val brokerInfoStr = brokerZkUtils.readDataMaybeNull(ZkUtils.BrokerIdsPath + "/" + brokerId)._1.get
          val brokerInfo = Json.parseFull(brokerInfoStr).get.asInstanceOf[Map[String, Any]]
          //通过消费者的方式获取初始偏移和最新偏移
          var offset = getOffsetsByConsumer(brokerInfo.get("host").get.asInstanceOf[String],brokerInfo.get("port").get.asInstanceOf[Int],soTimeout,soBufferSize,clientId,tp);
          tpsAndPrts += ((tp,offset))
        })
      })
    }
    tpsAndPrts.toMap
  }


  /**
    * 以消费者的方式获取Kafka端指定主题及分区的最早和最新偏移量
    *
    * @param host leader broker的地址
    * @param port leader broker的端口
    * @param soTimeout socket超时
    * @param bufferSize 缓存大小
    * @param clientId 客户端id
    * @param tp 主题及分区
    * @return 返回两个偏移量的元祖，_1 -> 最早， _2 -> 最新
    */
  def getOffsetsByConsumer(host: String, port: Int, soTimeout: Int, bufferSize: Int, clientId: String, tp: TopicAndPartition): (Long, Long) ={
    consume(host, port, soTimeout, bufferSize, clientId)({ consumer =>
      // 构造最早偏移量的请求
      val earliestInfo = new PartitionOffsetRequestInfo(OffsetRequest.EarliestTime, 1)
      val earliestReq = new OffsetRequest(Map((tp, earliestInfo)), OffsetRequest.CurrentVersion)
      // 获取最早偏移量的响应
      val earliestResp = consumer.getOffsetsBefore(earliestReq)
      // 获取最早偏移
      val earliestOffset = earliestResp.partitionErrorAndOffsets(tp).offsets(0)

      // 构造最新偏移量请求
      val lastInfo = new PartitionOffsetRequestInfo(OffsetRequest.LatestTime, 1)
      val lastReq = new OffsetRequest(Map((tp, lastInfo)), OffsetRequest.CurrentVersion)
      // 获取最新偏移量的响应
      val lastResp = consumer.getOffsetsBefore(lastReq)
      // 获取最新偏移
      val lastOffset = lastResp.partitionErrorAndOffsets(tp).offsets(0)

      return (earliestOffset, lastOffset)
    })
    throw new Exception(s"Error while request the earliest and last offsets for topicAndPartition: $tp")
  }






  /**
    * 获取指定主题的元数据
    * @param topics 主题列表
    * @param brokers kafka broker字符串
    * @param clientId 客户端ID
    * @param soTimeout 连接超时
    * @param soBufferSize socket缓冲区
    * @return 返回主题的元数据集合
    */
  def getTopicMetadata(topics: Set[String], brokers: String, clientId: String, soTimeout: Int, soBufferSize: Int): Seq[TopicMetadata] ={
    val req = new TopicMetadataRequest(TopicMetadataRequest.CurrentVersion, 0, clientId, topics.toSeq)
    withBrokers(getBrokers(brokers), soTimeout, soBufferSize, clientId){ consumer =>
      val resp = consumer.send(req)
      val respErrs = resp.topicsMetadata.filter(m => {
        m.errorCode != ErrorMapping.NoError
      })
      if(respErrs.isEmpty){
        return resp.topicsMetadata
      }
    }
    throw new Exception(s"Can not get partition metadata of the given topics from brokers : $brokers")
  }

  /**
    * 对每一个broker进行连接并执行消费函数
    *
    * @param brokers
    * @param soTimeout
    * @param bufferSize
    * @param clientId
    * @param fn
    */
  private def withBrokers(brokers: Iterable[(String, Int)], soTimeout: Int, bufferSize: Int, clientId: String)(fn: SimpleConsumer => Any): Unit ={
    brokers.foreach{ broker =>
      consume(broker._1, broker._2, soTimeout, bufferSize, clientId)(fn)
    }
  }

  /**
    * 消费指定的broker，执行引入的消费方法
    *
    * @param host
    * @param port
    * @param soTimeout
    * @param bufferSize
    * @param clientId
    * @param fn
    */
  private def consume(host: String, port: Int, soTimeout: Int, bufferSize: Int, clientId: String)(fn: SimpleConsumer => Any): Unit ={
    var consumer: SimpleConsumer = null
    try {
      consumer = new SimpleConsumer(host, port, soTimeout, bufferSize, clientId)
      fn(consumer)
    } catch {
      case NonFatal(e) =>
    } finally {
      if(consumer != null){
        consumer.close()
      }
    }
  }

  /**
    * 将Broker的字符串形式转化成集合的形式
    * xxx.xxx.xxx.xx:2181,xxx.xxx.xxx.xx:2181
    * => ((xxx.xxx.xxx.xx,2181), (xxx.xxx.xxx.xx,2181))
    *
    *
    * @param brokers
    */
  private def getBrokers(brokers: String) ={
    brokers.split(",").map{ broker =>
      val parts = broker.split(":")
      if(parts.length == 1){
        throw new IllegalArgumentException(s"Broker not in correct format of <host>:<port>")
      }
      (parts(0), parts(1).toInt)
    }
  }

}