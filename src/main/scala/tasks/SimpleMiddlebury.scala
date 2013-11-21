package tasks

import pilgrim._
import st.sparse.billy._
import st.sparse.billy.experiments.RuntimeConfig
import st.sparse.billy.experiments.wideBaseline._
import st.sparse.billy.extractors._
import st.sparse.billy.detectors._
import st.sparse.billy.matchers._
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
import breeze.linalg._
import org.joda.time._
import st.sparse.persistentmap.CustomPicklers._

class SimpleMiddlebury extends Task with Logging {
  override def run(unparsedArgs: Seq[String])(
    implicit runtimeConfig: RuntimeConfig) {
    require(unparsedArgs.isEmpty)

    implicit val matlabLibraryRoot = runtimeConfig.matlabLibraryRoot.get

    val exampleTime = new DateTime
    val exampleResults = Results(DenseMatrix.zeros[Double](4, 4))
    val exampleRecording = Set((exampleTime, exampleResults))
    val unpickled = exampleRecording.pickle.unpickle[Set[(DateTime, Results)]]
    println(exampleRecording)
    println(unpickled)
    assert(exampleRecording == unpickled)

    val databaseYear = 2005
    //    val imageClasses = Seq(
    //      "Art",
    //      "Books",
    //      "Dolls",
    //      "Laundry",
    //      "Moebius",
    //      "Reindeer")
    val imageClasses = Seq(
      "Moebius")
    val maxDescriptorPairs = 100
    val detectors = DoublyBoundedPairDetector(2, 200, 500, OpenCVDetector.FAST) ::
//      DoublyBoundedPairDetector(2, 200, 500, OpenCVDetector.SIFT) ::
      HNil

    val pixelSExtractors =
      AndExtractor(
        PatchExtractor(Gray, 24, 1),
        ForegroundMaskExtractor(24)) ::
        HNil

    val extractors = pixelSExtractors 
//    ++
//      (OpenCVExtractor.BRIEF ::
//        OpenCVExtractor.BRISK ::
//        OpenCVExtractor.SIFT ::
//        HNil)

    val pixelSMatchers =
//      PixelSMatcher(1, 1, 1, 1) ::
//        PixelSMatcher(1, 0, 0, 0) ::
//        PixelSMatcher(0, 1, 0, 0) ::
        PixelSMatcher(0, 0, 1, 0) ::
        PixelSMatcher(0, 0, 0, 1) ::
        HNil

    val matchers = pixelSMatchers 
//    ++
//      (VectorMatcher.L0 :: VectorMatcher.L1 :: HNil)

    object constructExperiment extends Poly1 {
      implicit def default[D <% PairDetector, E <% Extractor[F], M <% Matcher[F], F](
        implicit ftt: FastTypeTag[Middlebury[D, E, M, F]],
        sp: SPickler[Middlebury[D, E, M, F]],
        u: Unpickler[Middlebury[D, E, M, F]],
        ftt2e: FastTypeTag[FastTypeTag[Middlebury[D, E, M, F]]]) =
        at[(D, E, M)] {
          case (detector, extractor, matcher) => {
            for (imageClass <- imageClasses) yield {
              val experiment =
                Middlebury(
                  databaseYear,
                  imageClass,
                  maxDescriptorPairs,
                  detector,
                  extractor,
                  matcher)
              experiment.pickle.unpickle[Middlebury[D, E, M, F]]
              //                            Experiment.cached(oxford)
              experiment: Experiment
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

    println("Experiments:")
    experiments foreach println
    
    //    val results = experiments.map { _.run }

    val results = experiments.par.map(_.run).toIndexedSeq

    val table = Table(
      experiments zip results,
      (e: Experiment) => e.modelParametersString,
      (e: Experiment) => e.experimentParametersString,
      (r: Results) => r.recognitionRate.toString)

    FileUtils.writeStringToFile(
      new File("/home/eric/Downloads/results2.csv"),
      table.tsv)

    println("In Middlebury")
  }
}