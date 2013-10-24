package tasks

import pilgrim._
import st.sparse.billy._
import st.sparse.billy.experiments.RuntimeConfig
import st.sparse.billy.experiments.wideBaseline._
import st.sparse.billy.extractors.OpenCVExtractor
import st.sparse.billy.detectors.OpenCVDetector
import st.sparse.billy.matchers.VectorMatcher
import scala.pickling._
import scala.pickling.binary._
import st.sparse.billy.detectors.BoundedDetector
import shapeless._
import shapeless.ops.hlist._
import shapeless.poly._
import scala.reflect.runtime._
import st.sparse.sundry._
import org.apache.commons.io.FileUtils
import st.sparse.billy.experiments.wideBaseline.Table
import st.sparse.billy.experiments.wideBaseline.Results
import java.io.File
import scala.util.Try
import com.typesafe.scalalogging.slf4j.Logging

class SimpleOxford extends Task with Logging {
  override def run(unparsedArgs: Seq[String])(
    implicit runtimeConfig: RuntimeConfig) {
    require(unparsedArgs.isEmpty)

    //    val imageClasses = "bikes" :: "boat" :: HNil
    //    val otherImages = 2 :: 3 :: 4 :: 5 :: 6 :: HNil
    val imageClasses = Seq("bikes", "boat")
    val otherImages = 2 to 6
    val detectors = BoundedDetector(OpenCVDetector.FAST, 100) ::
      BoundedDetector(OpenCVDetector.SIFT, 100) ::
      HNil
    val extractors = OpenCVExtractor.BRISK :: OpenCVExtractor.SIFT :: HNil
    val matchers = VectorMatcher.L0 :: VectorMatcher.L1 :: HNil

    object constructExperiment extends Poly1 {
      implicit def default[D <% Detector, E <% Extractor[F], M <% Matcher[F], F](
        implicit ftt: FastTypeTag[Oxford[D, E, M, F]],
        sp: SPickler[Oxford[D, E, M, F]],
        u: Unpickler[Oxford[D, E, M, F]],
        ftt2e: FastTypeTag[FastTypeTag[Oxford[D, E, M, F]]]) =
        at[(D, E, M)] {
          case (detector, extractor, matcher) => {
            for (imageClass <- imageClasses; otherImage <- otherImages) yield {
              val oxford =
                Oxford(imageClass, otherImage, detector, extractor, matcher)
              oxford.pickle.unpickle[Oxford[D, E, M, F]]
              Experiment.cached(oxford)
            }
          }
        }
    }

    // This lifting, combined with flatMap, filters out types that can't be used
    // to construct experiments.   
    object constructExperimentLifted extends LiftU(constructExperiment)

    val tuples = HListUtil.cartesian3(
      detectors,
      extractors,
      matchers)

    val experiments = {
      val hList = tuples flatMap constructExperimentLifted
      hList.toList.flatten.toIndexedSeq
    }

    val results = experiments.map(_.run).toIndexedSeq

    val table = Table(
      experiments zip results,
      (e: Experiment) => e.modelParametersString,
      (e: Experiment) => e.experimentParametersString,
      (r: Results) => r.recognitionRate.toString)

    FileUtils.writeStringToFile(
      new File("/home/eric/Downloads/results1.csv"),
      table.tsv)

    println("In Oxford")
  }
}