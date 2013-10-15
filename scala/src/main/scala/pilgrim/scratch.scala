//package pilgrim
//
//import skunkworks.OpenCVMTCUtil._
//import spark.SparkContext._
//import billy.DetectorJsonProtocol._
//import billy._
//import billy.detectors._
//import billy.summary._
//import billy.wideBaseline._
//import java.io.File
//import nebula._
//import org.opencv._
//import org.opencv.contrib._
//import reflect.runtime.universe._
//import shapeless._
//import skunkworks._
//import spark._
//import spray.json._
//
////////////////////////////////////////////////////
//
//object Foo {
//  loadOpenCV
//
//  implicit val runtimeConfig: RuntimeConfig = ???
//  val sparkContext: SparkContext = ???
//
//  val imageClasses = "graffiti" :: "trees" :: HNil
//
//  //    Seq(
//  //    "graffiti",
//  //    "trees",
//  //    "jpeg",
//  //    "boat",
//  //    "bark",
//  //    "bikes",
//  //    "light",
//  //    "wall").sorted
//
//  val otherImages = 2 :: 3 :: HNil
//
//  //    Seq(2)
//
//  val fastDetector = BoundedPairDetector(
//    BoundedDetector(OpenCVDetector.FAST, 5000),
//    200)
//  val detectors = fastDetector :: HNil
//
//  val extractor = new contrib.NCCLogPolarExtractor(
//    2,
//    32,
//    16,
//    32,
//    3)
//  val extractors = extractor :: HNil
//
//  val matcher = new contrib.NCCLogPolarMatcher(8)
//  val matchers = matcher :: HNil
//
//  //////////////////////////////
//
//  object verifyPairDetector extends Poly1 {
//    implicit def default[D <% PairDetector] = at[D] { identity }
//  }
//
//  detectors map verifyPairDetector
//
//  /////////////////////////////////
//
//  object verifyExtractor extends Poly1 {
//    implicit def default[E <% Extractor[F], F] = at[E] { identity }
//  }
//
//  extractors map verifyExtractor
//
//  ///////////////////////////////////
//
//  object verifyMatcher extends Poly1 {
//    implicit def default[M <% Matcher[F], F] = at[M] { identity }
//  }
//
//  matchers map verifyMatcher
//
//  /////////////////////////////////////
//
//  object verifyJson extends Poly1 {
//    implicit def default[A: JsonFormat] = at[A] { identity }
//  }
//
//  detectors map verifyJson
//  extractors map verifyJson
//  matchers map verifyJson
//
//  /////////////////////////////////////////
//
//  //  val experiments = {
//  //    object constructExperiment extends Poly1 {
//  //      implicit def default[D <% PairDetector, E <% Extractor[F], M <% Matcher[F], F] = at[(D, E, M)] {
//  //        case (detector, extractor, matcher) => {
//  //          WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
//  //        }
//  //      }
//  //    }
//  //    
//  //    
//  //  }
//
//  implicit val imports: Imports = ???
//
//  val experimentTable = {
//    val rowLabels = HListUtil.mkTuple2(imageClasses, otherImages)
//    val columnLabels = HListUtil.mkTuple3(detectors, extractors, matchers)
//
//    object makeTable extends Poly1 {
//      implicit def default = at[(String, Int)] {
//        case (imageClass, otherImage) =>
//          object constructExperiment extends Poly1 {
//            implicit def default[D <% PairDetector, E <% Extractor[F], M <% Matcher[F], F] = at[(D, E, M)] {
//              case (detector, extractor, matcher) => {
//                WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
//              }
//            }
//          }
//
//          // This lifting, combined with flatMap, filters out types that can't be used                                                                                                                                                                                                             
//          // to construct experiments.                                                                                                                                                                                                                                                             
//          object constructExperimentLifted extends Lift1(constructExperiment)
//
//          columnLabels flatMap constructExperimentLifted
//      }
//    }
//
//    rowLabels map makeTable
//  }
//
//  val experimentMessageTable: Seq[Seq[JSONAndTypeName]] = {
//    val rowLabels = HListUtil.mkTuple2(imageClasses, otherImages)
//    val columnLabels = HListUtil.mkTuple3(detectors, extractors, matchers)
//
//    object makeTable extends Poly1 {
//      implicit def default = at[(String, Int)] {
//        case (imageClass, otherImage) =>
//          object constructExperiment extends Poly1 {
//            implicit def default[D <% PairDetector, E <% Extractor[F], M <% Matcher[F], F] = at[(D, E, M)] {
//              case (detector, extractor, matcher) => {
//                WideBaselineExperiment(
//                  imageClass,
//                  otherImage,
//                  detector,
//                  extractor,
//                  matcher)
//              }
//            }
//          }
//
//          // This lifting, combined with flatMap, filters out types that can't be used                                                                                                                                                                                                             
//          // to construct experiments.                                                                                                                                                                                                                                                             
//          object constructExperimentLifted extends Lift1(constructExperiment)
//
//          val experiments = columnLabels flatMap constructExperimentLifted
//
//          object constructJSONAndTypeName extends Poly1 {
//            implicit def default[E <% RuntimeConfig => ExperimentRunner[R]: JsonFormat: TypeTag, R] =
//              at[E] { experiment =>
//                JSONAndTypeName(
//                  experiment.toJson,
//                  instanceToTypeName(experiment))
//              }
//          }
//
//          (experiments map constructJSONAndTypeName) toList
//      }
//    }
//
//    (rowLabels map makeTable) toList
//  }
//
//  //  val experimentMessages: Seq[JSONAndTypeName] = {
//  //    // This is the same as "flatten".
//  //    val experiments = experimentTable flatMap identity
//  //
//  //    object constructJSONAndTypeName extends Poly1 {
//  //      implicit def default[E <% RuntimeConfig => ExperimentRunner[R]: JsonFormat: TypeTag, R] =
//  //        at[E] { experiment =>
//  //          JSONAndTypeName(
//  //            experiment.toJson,
//  //            instanceToTypeName(experiment))
//  //        }
//  //    }
//  //
//  //    (experiments map constructJSONAndTypeName) toList
//  //  }
//
//  //  val shuffled = new scala.util.Random(0).shuffle(experimentMessages)
//  //  shuffled.foreach(typecheckExperiment)
//  //  sparkContext.parallelize(shuffled).foreach(runExperiment)
//}