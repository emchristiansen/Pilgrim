package tasks

import pilgrim._
import st.sparse.billy.experiments.RuntimeConfig
import org.apache.spark.SparkContext
import st.sparse.billy.experiments.wideBaseline.Experiment
import st.sparse.billy.experiments.wideBaseline.Oxford
import st.sparse.billy.extractors.OpenCVExtractor
import st.sparse.billy.detectors.OpenCVDetector
import st.sparse.billy.matchers.VectorMatcher

import scala.pickling._
import scala.pickling.binary._

class SimpleOxford extends Task {
  override def run(unparsedArgs: Seq[String])(
    implicit runtimeConfig: RuntimeConfig,
    sparkContext: SparkContext) {
    require(unparsedArgs.isEmpty)

    val experiments = (2 to 6) map { otherImage =>
      Oxford(
        "boat",
        otherImage,
        OpenCVDetector.FAST,
        OpenCVExtractor.SIFT,
        VectorMatcher.L2): Experiment
    }
    
    val runtimeConfigPickle = runtimeConfig.pickle
    val results = sparkContext.parallelize(experiments).map { experiment =>
      implicit val runtimeConfig: RuntimeConfig = 
        runtimeConfigPickle.unpickle[RuntimeConfig]
      println(runtimeConfig)
      experiment.run
    }.collect

    println("In Oxford")
  }
}