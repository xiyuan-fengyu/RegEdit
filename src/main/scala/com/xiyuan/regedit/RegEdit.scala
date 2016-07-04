package com.xiyuan.regedit

import java.io.{File, FileOutputStream}
import java.util.concurrent.{Executors, ExecutorService}

import com.registry._

/**
  * Created by xiyuan_fengyu on 2016/7/4.
  */
object RegEdit {
  //参考  http://blog.csdn.net/chenyuyao89/article/details/8665568

  private val dllName = "reg_x64.dll"

  try {
    val dir = new File(".").getAbsoluteFile.getParentFile.getAbsolutePath
    val filePath = dir + s"/$dllName"
    val file = new File(filePath)
    if (!file.exists()) {
      val input = this.getClass.getClassLoader.getResourceAsStream(s"jniLib/$dllName")
      val output = new FileOutputStream(file)
      val buffer = new Array[Byte](2048)
      while (input.read(buffer) != -1) {
        output.write(buffer)
        output.flush()
      }
      input.close()
      output.close()
    }
    System.load(filePath)
  }
  catch {
    case e: Exception =>
      e.printStackTrace()
  }

  def replaceValues(root: RegistryKey, replaceStr: String, withStr: String, replaceRegex: String = null, withRegex: String = null): Unit = {
    println(root)

    val tempReplaceRegex = if (replaceRegex == null) {
      replaceStr.replaceAll("\\\\","\\\\\\\\") + "|" + replaceStr.toLowerCase.replaceAll("\\\\","\\\\\\\\")
    }
    else replaceRegex

    val tempWithRegex = if (withRegex == null) {
      withStr.replaceAll("\\\\","\\\\\\\\")
    }
    else withRegex

    //更新当前key下的所有键值
    try {
      root.getValues.toArray.foreach(v => {
        v.asInstanceOf[RegistryValue].getValueType match {
          case ValueType.REG_SZ =>
            val value = v.asInstanceOf[RegStringValue]
            if (value.getValue.toLowerCase.contains(replaceStr.toLowerCase)) {
              value.setValue(value.getValue.replaceAll(tempReplaceRegex, tempWithRegex))
            }
          case ValueType.REG_EXPAND_SZ =>
            val value = v.asInstanceOf[RegStringValue]
            if (value.getValue.toLowerCase.contains(replaceStr.toLowerCase)) {
              value.setValue(value.getValue.replaceAll(tempReplaceRegex, tempWithRegex))
            }
          case ValueType.REG_MULTI_SZ =>
            val value = v.asInstanceOf[RegMultiStringValue]
            val valueArr = value.getValue
            for (i <- valueArr.indices) {
              val str = valueArr(i)
              if (str.toLowerCase.contains(replaceStr.toLowerCase)) {
                valueArr(i) = str.replaceAll(tempReplaceRegex, tempWithRegex)
              }
            }
            value.setValue(valueArr)
          case _ => ()
        }
      })
    }
    catch {
      case e: Exception =>
        e.printStackTrace()
    }

    try {
      root.getSubKeys.toArray.foreach(k => {
        replaceValues(k.asInstanceOf[RegistryKey], replaceStr, withStr, replaceRegex = tempReplaceRegex, withRegex = tempWithRegex)
      })
    }
    catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }

  def main(args: Array[String]) {
//    //创建新key
//    val newKey = new RegistryKey(RegistryKey.getRootKeyForIndex(RegistryKey.HKEY_CURRENT_USER_INDEX), "RegEditTest")
//    newKey.create()
//    println(newKey)
//
//    //创建子key
//    val newSubKey = newKey.createSubKey("SubKeyTest")
//    newSubKey.create()
//    println(newSubKey)
//
//    //删除key
//    try {
//      new RegistryKey(RegistryKey.getRootKeyForIndex(RegistryKey.HKEY_CURRENT_USER_INDEX), "RegEditTest\\SubKeyTest").deleteKey()
//    }
//    catch {
//      case e: Exception =>
//        e.printStackTrace()
//    }

    //获取一个key中的某个键值对
//    val key = new RegistryKey(RegistryKey.getRootKeyForIndex(RegistryKey.HKEY_CURRENT_USER_INDEX), "RegEditTest")
//    val value = key.getValue("test")
//    println(value)
//    //更新value
//    value.asInstanceOf[RegStringValue].setValue("new value")


//    replaceValues(new RegistryKey(RegistryKey.getRootKeyForIndex(RegistryKey.HKEY_CURRENT_USER_INDEX), "RegEditTest"), "C:\\Users\\YT", "C:\\Users\\xiyuan_fengyu")

    val threadPool:ExecutorService = Executors.newFixedThreadPool(10)
    try {
      RegistryKey.listRoots().foreach(key => {
        threadPool.submit(new Runnable {
          override def run(): Unit = {
            replaceValues(key, "C:\\Users\\YT", "C:\\Users\\xiyuan_fengyu")
          }
        })
      })
    }finally {
      threadPool.shutdown()
    }

  }

}
