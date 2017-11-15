package utils

import java.io.{FileInputStream, InputStream}
import java.util.Properties


trait Configuration extends Serializable{

  /**
    * properties配置文件
    */
  val properties: Properties = new Properties()


  /**
    * 获取文件输入流
    * @param filePath
    * @return
    */
  def getInputStream(filePath: String): InputStream ={
    new FileInputStream(filePath)
  }


  /**
    * 初始化
    * @param conFile
    * @return
    */
  def init(conFile:String): this.type ={
    try {
      properties.load(getInputStream(conFile));
    }catch {
      case e: Throwable => {
        println("Can't find the conf file.")
        throw e
      }
    }
    this
  }



  /**
    * 获取指定键的值
    *
    * @param key 指定键
    * @return 不存在返回null
    */
  def get(key: String): String = {
    getOrElse(key, null)
  }

  /**
    * 获取指定键的值
    *
    * @param key 指定键
    * @param default 默认值
    * @return
    */
  def getOrElse(key: String, default: String): String = {
    val value = properties.getProperty(key)
    if(value == null || value.trim.length == 0){
      default
    } else {
      value
    }
  }

}



