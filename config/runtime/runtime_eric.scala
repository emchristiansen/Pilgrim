import java.io.File

val homeDirectory = new File(System.getProperty("user.home"))

val sparkContext = new SparkContext(
//    "spark://10.0.3.30:7077", 
    "spark://10.0.4.214:7077", 
    "TestLUCID", 
    "/u/echristiansen/github/spark", 
    Seq())
//    Seq("/u/echristiansen/Dropbox/t/2013_q1/LUCID/src/scala/target/LucidTest-assembly-0.1-SNAPSHOT.jar"))

val runtimeConfig = RuntimeConfig(
  homeDirectory + "data",
  homeDirectory + "Dropbox/t/2013_q1/LUCID",
  None,
  false,
  true)

(runtimeConfig, sparkContext)
