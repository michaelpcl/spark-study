package basic

import org.apache.log4j.Logger
import org.apache.spark.{SparkConf, SparkContext, SparkException}

object ExceptionHandingTest {
  var log = Logger.getLogger(ExceptionHandingTest.getClass)

  def ThrowException(sc : SparkContext): Unit  ={
    var temp = sc.defaultParallelism
    println("default parallelism************" + temp);

    //创建rdd
    var rdd = sc.parallelize(0 until sc.defaultParallelism )
    var tempValue = 0.50
    //遍历rdd循环输出
    try{
      rdd.foreach{ i =>
        println("*********" + i)
        if(i==0) {
           tempValue = 0.99
        }
        if(i==2){
          tempValue = 0.25
        }
        if(tempValue < 0.30){
          throw new NullPointerException ("小于0.30的异常")
        }
        else if (tempValue > 0.90){
          throw new NoSuchElementException ("大于0.90的异常")
        }
        /*else{
          throw new NoSuchMethodException() ("其他数值的异常")
        }*/
      }

      /*var tempValue = 0.91
      if(tempValue < 0.30){
        throw new NullPointerException ("小于0.30的异常")
      }
      else if (tempValue > 0.90){
        throw new NoSuchElementException ("大于0.90的异常")
      }*/

      //返回的异常被包装为SparkException
    }catch {
      case nu:SparkException => throw new NullPointerException("小于0.30的异常")
      case no:SparkException => throw new NoSuchElementException ("大于0.90的异常")
      case se:SparkException => throw new NoSuchMethodException ("其他数值的异常")
      case e => println("dsafdsdsafdsafdsafdsafdsfdsafdsfsagfdsgdsf" + e)
    }
  }


  def main(args:Array[String]){
    //System.setProperty("hadoop.home.dir", "D:/hadoop-2.6.0/")
    //System.setProperty("spark.testing.memory", "1048576")//后面的值大于512m即可
    var sparkConf = new SparkConf()
    sparkConf.setAppName("ExceptionHandingTest")
    sparkConf.setMaster("local[*]")

    var sc = new SparkContext(sparkConf)
    try{
      ThrowException(sc)
    }catch {
      case e :NullPointerException => {
        log.error("****exception" + e.getMessage)
        println("小于0.30的异常")
      }
      case e :NoSuchElementException => {
        log.error("******exception" + e.getMessage)
        println("大于0.90的异常")
      }
      case e :NoSuchMethodException => {
        log.error("******exception" + e.getMessage)
        println("其他数值的异常")
      }
    }
  }
}
