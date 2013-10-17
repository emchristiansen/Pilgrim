package sparkContexts

import org.apache.spark.SparkContext
import pilgrim.SparkContextWrapper

class LocalSingleThread extends SparkContextWrapper {
  override def sparkContext(unparsedArgs: Seq[String]) = {
    require(unparsedArgs.isEmpty)
    
    new SparkContext(
      "local[1]",
      "Pilgrim-LocalSingleThread")
  }
}