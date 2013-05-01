// This is in MB.
//System.setProperty("spark.akka.frameSize", "1000")

val environment = Map(
//  "SPARK_LIBRARY_PATH" -> "/usr/local/lib",
//  "SPARK_MASTER_IP" -> "10.0.4.214",
//  "SPARK_MEM" -> "48g",
//  "SPARK_WORKER_MEMORY" -> "48g",
//  "SPARK_CLASSPATH" -> "")
//  "SPARK_WORKER_CORES" -> "8",
  "SPARK_CLASSPATH" -> System.getProperty("java.class.path"))

new SparkContext(
  master = "spark://10.0.3.31:7077", 
  jobName = "Pilgrim", 
  sparkHome = "/u/echristiansen/github/spark", 
  jars = Seq(),
  environment = environment)
