import junit.framework.TestCase
import kafka.api.TopicMetadata
import kafka.utils.ZkUtils
import utils.{BaseConfiguration, Configuration, Constants, KafkaUtils}


class KafkaUtilsTest extends TestCase{

    def testKafkaUtils(): Unit ={
      var filePath = Thread.currentThread().getContextClassLoader.getResource("conf.properties").getPath
      var conf = new BaseConfiguration().init(filePath)

      var clientId = conf.get(Constants.CLIENT_ID)
      var brokers = conf.get(Constants.KAFKA_METADATA_BROKER_LIST)
      var topics = conf.get(Constants.TOPICS).toString.split(",").toSet
      var soTimeout = 3000
      var soBufferSize = 6500
      // 默认读取最新的消息
      val autoOffsetReset = conf.getOrElse("auto.offset.reset", "largest").toLowerCase

      var groupId = conf.get(Constants.GROUP_ID)

      var topicMetadataSeq = KafkaUtils.getTopicMetadata(topics, brokers, clientId, soTimeout, soBufferSize)
      //println(topicMetadataSeq)

      var brokerZkUtils = KafkaUtils.getKafkaBrokerZkUtils(conf)
      //println(brokerZkUtils)

      var topicPartitionFirstLasttimeOffset = KafkaUtils.getTopicAndPartitionFirstAndLasttimeOffsetInfo(soTimeout,soBufferSize,clientId,topicMetadataSeq,brokerZkUtils)
      //println(topicPartitionOffset)

      var zkUtils = KafkaUtils.getZkUtils(conf)

      var offsetFromZK = KafkaUtils.getOffsetFromZKByGroup(groupId,topicMetadataSeq,zkUtils)

      //println(offsetFromZK)

      var topicPartitionLastTimeOffset = KafkaUtils.getTopicAndPartitionLastTimeOffsetInfo(soTimeout,soBufferSize,clientId,topicMetadataSeq,brokerZkUtils)

      var topicPartitionOffset = KafkaUtils.getConsumerOffsetAndCommitOffset(topicMetadataSeq, brokerZkUtils, zkUtils,soTimeout, soBufferSize, clientId, groupId,autoOffsetReset)
      println(topicPartitionOffset);

      var topicAndPartitionMessageMap = KafkaUtils.getMessage(topicPartitionOffset,clientId,brokerZkUtils)

      println(topicAndPartitionMessageMap);
   }




}
