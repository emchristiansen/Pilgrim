package tasks

import pilgrim._
import st.sparse.billy.experiments.RuntimeConfig
import st.sparse.billy.experiments.wideBaseline.Experiment
import st.sparse.billy.experiments.wideBaseline.Oxford
import st.sparse.billy.extractors.OpenCVExtractor
import st.sparse.billy.detectors.OpenCVDetector
import st.sparse.billy.matchers.VectorMatcher
import scala.pickling._
import scala.pickling.binary._
import st.sparse.billy.detectors.BoundedDetector
import shapeless._
import shapeless.poly._

class SimpleOxford extends Task {
  override def run(unparsedArgs: Seq[String])(
    implicit runtimeConfig: RuntimeConfig) {
    require(unparsedArgs.isEmpty)
    
    val matchers = VectorMatcher.L1 :: VectorMatcher.L2 :: HNil
    
    matchers map identity
    
    val experiments = (2 to 6) map { otherImage =>
      Oxford(
        "boat",
        otherImage,
        BoundedDetector(OpenCVDetector.FAST, 100),
        OpenCVExtractor.SIFT,
        VectorMatcher.L2): Experiment
    }
    
    val results = experiments.par.map(_.run).toIndexedSeq
    results foreach (println)

    println("In Oxford")
  }
}