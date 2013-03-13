import java.io.File
import spark._
import SparkContext._

val homeDirectory = new File(System.getProperty("user.home"))

val sparkContext = new SparkContext(
//    "local[1]", 
    "local[1]", 
    "TestLUCID")

val runtimeConfig = RuntimeConfig(
  homeDirectory + "Bitcasa/data",
  homeDirectory + "Dropbox/t/2013_q1/LUCID",
  None,
  false,
  false)

(runtimeConfig, sparkContext)
