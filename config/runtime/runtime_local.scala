val sparkContext = new SparkContext(
//    "local[1]", 
    "local[1]", 
    "TestLUCID")

val runtimeConfig = RuntimeConfig(
  homeDirectory + "Bitcasa/data",
  homeDirectory + "Dropbox/t/2013_q1/pilgrim",
  None,
  false,
  false)

(runtimeConfig, sparkContext)
