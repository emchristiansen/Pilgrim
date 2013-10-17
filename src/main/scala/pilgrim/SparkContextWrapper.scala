package pilgrim

import org.apache.spark.SparkContext

trait SparkContextWrapper {
  def sparkContext(unparsedArgs: Seq[String]): SparkContext
}