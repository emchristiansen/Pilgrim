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
import scala.pickling.binary._
import st.sparse.sundry._
import breeze.linalg.DenseMatrix
import scala.reflect.ClassTag
import com.sksamuel.scrimage._
import org.opencv.core.KeyPoint
import java.io.File
import scala.util._
import org.apache.commons.io.FileUtils

class SimpleMiddlebury extends Task with Logging {
  override def run(unparsedArgs: Seq[String])(
    implicit runtimeConfig: RuntimeConfig) {
    require(unparsedArgs.isEmpty)

    implicit val matlabLibraryRoot = runtimeConfig.matlabLibraryRoot.get
    implicit val logRoot = {
      val file = new File(runtimeConfig.outputRoot, "log")
      file.mkdir
      LogRoot(ExistingDirectory(file))
    }

    println(matlabLibraryRoot)
    println(logRoot)

    //    val image = Image(new File("/home/eric/Dropbox/goldfish_girl.png"))
    //    println(image.width)
    //    val pickled = image.toPOD.pickle
    //    val image2 = RichImage.fromPOD(pickled.unpickle[ImagePOD])
    //    assert(image == image2)
    //    println("here")

    //    val image = Image(new File("/home/eric/Dropbox/goldfish_girl.png"))
    //    println(image.width)
    //    val pickled = image.pickle
    //    val image2 = pickled.unpickle[Image]
    //    assert(image == image2)
    //    println("here")    

    //    val exampleTime = new DateTime
    //    val exampleResults = Results(DenseMatrix.zeros[Double](4, 4))
    //    val exampleRecording = Set((exampleTime, exampleResults))
    //    val unpickled = exampleRecording.pickle.unpickle[Set[(DateTime, Results)]]
    //    println(exampleRecording)
    //    println(unpickled)
    //    assert(exampleRecording == unpickled)

    val similarityThreshold = 2.002
    //    val numSmoothingIterationsSeq = Seq(
    //      0, 
    //      1)
    val numSmoothingIterationsSeq = Seq(
      0,
      1,
      2,
      4,
      8,
      16,
      32)
      val scaleFactor = 0.5

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

    val detectors = Seq(
      s"${cn[DoublyBoundedPairDetector[_]]}(2, 800, 2000, ${on[OpenCVDetector.FAST.type]})",
      s"${cn[DoublyBoundedPairDetector[_]]}(2, 800, 2000, ${on[OpenCVDetector.SIFT.type]})")

    //    val detectors = DoublyBoundedPairDetector(2, 200, 500, OpenCVDetector.FAST) ::
    //      //      DoublyBoundedPairDetector(2, 200, 500, OpenCVDetector.SIFT) ::
    //      HNil

    //    val pixelSExtractors =
    //      AndExtractor(
    //        PatchExtractor(Gray, 24, 1),
    //        ForegroundMaskExtractor(24)) ::
    //        HNil

    //    val extractors = Seq(on[OpenCVExtractor.BRIEF.type])

        val extractors = Seq(
          on[OpenCVExtractor.BRIEF.type],
          on[OpenCVExtractor.BRISK.type],
          on[OpenCVExtractor.SIFT.type])

//    val extractors = Seq(
//      s"""${cn[AndExtractor[_, _, _, _]]}(
//        ${cn[PatchExtractor]}(${on[Gray.type]}, 24, 1),
//        ${cn[ForegroundMaskExtractor]}(24))""")

    //    val extractors =
    //      OpenCVExtractor.BRIEF ::
    //        OpenCVExtractor.BRISK ::
    //        OpenCVExtractor.SIFT ::
    //        HNil

    //    val extractors = pixelSExtractors
    ////        ++
    ////          (OpenCVExtractor.BRIEF ::
    ////            OpenCVExtractor.BRISK ::
    ////            OpenCVExtractor.SIFT ::
    ////            HNil)

    //    val pixelSMatchers =
    //      //      PixelSMatcher(1, 1, 1, 1) ::
    //      //        PixelSMatcher(1, 0, 0, 0) ::
    //      //        PixelSMatcher(0, 1, 0, 0) ::
    //      PixelSMatcher(0, 0, 1, 0) ::
    //        PixelSMatcher(0, 0, 0, 1) ::
    //        HNil
    //
    //    val matchers =
    //      VectorMatcher.L0 ::
    //        VectorMatcher.L1 ::
    //        VectorMatcher.L2 ::
    //        HNil

        val matchers = Seq(
          on[VectorMatcher.L0.type],
          on[VectorMatcher.L1.type],
          on[VectorMatcher.L2.type])

//    val matchers = Seq(
//      s"${cn[PixelSMatcher]}(1, 0, 0, 0)",
//      s"${cn[PixelSMatcher]}(0, 1, 0, 0)",
//      s"${cn[PixelSMatcher]}(0, 0, 1, 0)",
//      s"${cn[PixelSMatcher]}(0, 0, 0, 1)")

    //    object constructExperiment extends Poly1 {
    //      implicit def default[D <% PairDetector, E <% Extractor[F], M <% Matcher[F], F](
    //        implicit ftt: FastTypeTag[Middlebury[D, E, M, F]],
    //        sp: SPickler[Middlebury[D, E, M, F]],
    //        u: Unpickler[Middlebury[D, E, M, F]],
    //        ftt2e: FastTypeTag[FastTypeTag[Middlebury[D, E, M, F]]]) =
    //        at[(D, E, M)] {
    //          case (detector, extractor, matcher) => {
    //            for (imageClass <- imageClasses) yield {
    //              val experiment =
    //                Middlebury(
    //                  databaseYear,
    //                  imageClass,
    //                  maxDescriptorPairs,
    //                  detector,
    //                  extractor,
    //                  matcher)
    //              experiment.pickle.unpickle[Middlebury[D, E, M, F]]
    //              //                            Experiment.cached(oxford)
    //              experiment: Experiment
    //            }
    //          }
    //        }
    //    }

    //    // This lifting, combined with flatMap, filters out types that can't be used
    //    // to construct experiments.   
    //    object constructExperimentLifted extends LiftU(constructExperiment)
    //
    //    val tuples = HListUtil.cartesian3(
    //      detectors,
    //      extractors,
    //      matchers)

    val matlabLibraryRootSource = s"""
${cn[MatlabLibraryRoot]}(${cn[ExistingDirectory]}(
  "${matlabLibraryRoot.data.data.getPath}"))
"""

    val logRootSource = s"""
${cn[LogRoot]}(${cn[ExistingDirectory]}(
  "${logRoot.data.data.getPath}"))
"""

    val experimentOptions = for (
      numSmoothingIterations <- numSmoothingIterationsSeq;
      imageClass <- imageClasses;
      detector <- detectors;
      extractor <- extractors;
      matcher <- matchers
    ) yield {
      val source = s"""
//import st.sparse.billy.detectors._
//import st.sparse.billy.extractors._
//import st.sparse.billy.matchers._
//import st.sparse.billy.experiments.wideBaseline._
      
// Such a hack.
implicit val matlabLibraryRoot = $matlabLibraryRootSource
implicit val logRoot = $logRootSource      
     
val middlebury = ${cn[Middlebury[_, _, _, _]]}(
  $databaseYear, 
  "$imageClass", 
  $maxDescriptorPairs, 
  $detector,
  $extractor,
  $matcher)
val blurred = ${cn[BlurredMiddlebury[_, _, _, _]]}(
  $similarityThreshold, 
  $numSmoothingIterations, 
  $scaleFactor,
  middlebury)
  
//${on[Experiment]}.jsonCached(blurred)
  blurred
"""
      val experimentTry = Try(eval[Experiment](source))
      experimentTry match {
        case Success(e) => println(s"Compiled $e")
        case Failure(e) =>
          println(s"Unable to compile:\n$source")
          println(s"Exception: $e")
      }

      //      if (!experimentOption.isDefined) {
      //        println(s"Unable to compile:\n$source")
      //      } else {
      //        println(s"Compiled ${experimentOption.get}")
      //      }
      //      val experimentOption = Try(eval[Experiment](source)).toOption
      //      if (!experimentOption.isDefined) {
      //        println(s"Unable to compile:\n$source")
      //      } else {
      //        println(s"Compiled ${experimentOption.get}")
      //      }
      //      experimentOption

      experimentTry.toOption
    }

    //    val experiments = {
    //      val hList = tuples flatMap constructExperimentLifted
    //      hList.toList.flatten.toIndexedSeq
    //    }

    val experiments: Seq[Experiment] = {
      val uncached: Seq[Experiment] = experimentOptions.flatten
//      uncached.map(e => Experiment.cached(e))
      uncached
//      Experiment.cached(uncached.head)
//      ???
    }
    println("Experiments:")
    experiments foreach println

    val results = experiments.map { _.run }

            //val results = experiments.par.map(_.run).toIndexedSeq

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
