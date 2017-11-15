import junit.framework.TestCase
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

      var topicMetadataSeq = KafkaUtils.getTopicMetadata(topics, brokers, clientId, soTimeout, soBufferSize)
      println(topicMetadataSeq)

      var brokerZkUtils = KafkaUtils.getKafkaBrokerZkUtils(conf)
      println(brokerZkUtils)

      var topicPartitionOffset = KafkaUtils.getTopicAndPartitionOffsetInfo(soTimeout,soBufferSize,clientId,topicMetadataSeq,brokerZkUtils)
      println(topicPartitionOffset)
   }




}
