package tasks

import pilgrim._

import st.sparse.billy._
import st.sparse.billy.experiments._
import st.sparse.billy.experiments.wideBaseline._
import st.sparse.billy.detectors._
import st.sparse.billy.extractors._
import st.sparse.billy.matchers._
import org.scalatest.fixture
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalacheck.Gen
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import scala.pickling._
//import scala.pickling.binary._
import st.sparse.sundry._
import breeze.linalg.DenseMatrix
import scala.reflect.ClassTag
import com.sksamuel.scrimage._
import org.opencv.core.KeyPoint
import java.io.File
import scala.util._
import org.apache.commons.io.FileUtils
import shapeless._
import shapeless.poly._
import spray.json._
import spray.json.DefaultJsonProtocol._

class HListMiddlebury extends Task with Logging {
  override def run(unparsedArgs: Seq[String])(
    implicit runtimeConfig: RuntimeConfig) {
    require(unparsedArgs.isEmpty)

    implicit val matlabLibraryRoot = runtimeConfig.matlabLibraryRoot.get
    implicit val logRoot = {
      val file = new File(runtimeConfig.outputRoot, "log")
      file.mkdir
      LogRoot(ExistingDirectory(file))
    }
    implicit val database = runtimeConfig.database

    println(matlabLibraryRoot)
    println(logRoot)

    val similarityThreshold = 2.002
    val numSmoothingIterationsSeq = Seq(
      0,
      1,
      2,
      4,
      8,
      16,
      32)
//        val numSmoothingIterationsSeq = Seq(
//          0,
//          1,
//          2)
    val scaleFactor = 0.00001

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

    val detectors =
      DoublyBoundedPairDetector(2, 400, 5000, OpenCVDetector.FAST) ::
        DoublyBoundedPairDetector(2, 400, 5000, OpenCVDetector.SIFT) ::
        HNil

    val extractors =
//      AndExtractor(
//        PatchExtractor(Gray, 24, 1),
//        ForegroundMaskExtractor(24)) ::
        OpenCVExtractor.BRIEF ::
        OpenCVExtractor.BRISK ::
        OpenCVExtractor.SIFT ::
        HNil

    //    val extractors =
    //      OpenCVExtractor.BRIEF ::
    //        OpenCVExtractor.BRISK ::
    //        OpenCVExtractor.SIFT ::
    //        HNil

    val matchers =
      PixelSMatcher(1, 0, 0, 0) ::
        PixelSMatcher(0, 1, 0, 0) ::
        PixelSMatcher(0, 0, 1, 0) ::
        PixelSMatcher(0, 0, 0, 1) ::
        VectorMatcher.L0 ::
        VectorMatcher.L1 ::
        VectorMatcher.L2 ::
        HNil

    //    val matchers =
    //      VectorMatcher.L0 ::
    //        VectorMatcher.L1 ::
    //        VectorMatcher.L2 ::
    //        HNil

    //    object constructExperiment extends Poly1 {
    //      implicit def default[D <% PairDetector, E <% Extractor[F], M <% Matcher[F], F](
    //        implicit ftt: FastTypeTag[Middlebury[D, E, M, F]],
    //        sp: SPickler[Middlebury[D, E, M, F]],
    //        u: Unpickler[Middlebury[D, E, M, F]],
    //        ftt2e: FastTypeTag[FastTypeTag[Middlebury[D, E, M, F]]],
    //        bfft: FastTypeTag[BlurredMiddlebury[D, E, M, F]],
    //        bsp: SPickler[BlurredMiddlebury[D, E, M, F]],
    //        bu: Unpickler[BlurredMiddlebury[D, E, M, F]],
    //        bftt2e: FastTypeTag[FastTypeTag[BlurredMiddlebury[D, E, M, F]]]) =
    //        at[(D, E, M)] {
    //          case (detector, extractor, matcher) => {
    //            for (
    //              imageClass <- imageClasses;
    //              numSmoothingIterations <- numSmoothingIterationsSeq
    //            ) yield {
    //              val middlebury =
    //                Middlebury(
    //                  databaseYear,
    //                  imageClass,
    //                  maxDescriptorPairs,
    //                  detector,
    //                  extractor,
    //                  matcher)
    //
    //              val blurred = BlurredMiddlebury(
    //                similarityThreshold,
    //                numSmoothingIterations,
    //                scaleFactor,
    //                middlebury)
    //              //              experiment.pickle.unpickle[Middlebury[D, E, M, F]]
    //              Experiment.cached(blurred)
    ////              Experiment.jsonCached(blurred)
    //              //              experiment: Experiment
    //            }
    //          }
    //        }
    object constructExperiment extends Poly1 {
      implicit def default[D <% PairDetector: JsonFormat, E <% Extractor[F]: JsonFormat, M <% Matcher[F]: JsonFormat, F](
        implicit ftt: FastTypeTag[BlurredMiddlebury[D, E, M, F]]) =
        at[(D, E, M)] {
          case (detector, extractor, matcher) => {
            for (
              imageClass <- imageClasses;
              numSmoothingIterations <- numSmoothingIterationsSeq
            ) yield {
              val middlebury =
                Middlebury(
                  databaseYear,
                  imageClass,
                  maxDescriptorPairs,
                  detector,
                  extractor,
                  matcher)

              val blurred = BlurredMiddlebury(
                similarityThreshold,
                numSmoothingIterations,
                scaleFactor,
                middlebury)
              //              experiment.pickle.unpickle[Middlebury[D, E, M, F]]
              Experiment.cached(blurred)
              //              Experiment.jsonCached(blurred)
              //              experiment: Experiment
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

    println(s"Experiments (${experiments.size}):")
    experiments foreach println

    val results = experiments.map { _.run }

    //    val results = experiments.par.map(_.run).toIndexedSeq

    val table = Table(
      experiments zip results,
      (e: Experiment) => e.modelParametersString,
      (e: Experiment) => e.experimentParametersString,
      (r: Results) => r.recognitionRate.toString)

    FileUtils.writeStringToFile(
      new File(runtimeConfig.outputRoot, "middleburyResults.csv"),
      table.tsv)

    println("Finished Middlebury")
  }
}
