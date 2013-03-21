package pilgrim

import nebula._
import nebula.testing._
import nebula.imageProcessing._
import nebula.util._
import billy._
import billy.brown._
import billy.mpie._
import billy.smallBaseline._
import billy.wideBaseline._
import billy.summary._
import billy.detectors._
import billy.extractors._
import billy.matchers._
import skunkworks.extractors._
import skunkworks.matchers._
import nebula._
import org.scalatest._
import javax.imageio.ImageIO
import java.io.File
import org.junit.runner.RunWith
import nebula.util._
import spray.json._
import breeze.linalg._
import breeze.math._
import org.scalacheck._
import org.scalatest.prop._
import org.scalatest._
import nebula.util.DenseMatrixUtil._
import org.opencv.core._
import org.opencv.core
import org.opencv.features2d._
import org.opencv.imgproc.Imgproc._
import org.opencv.imgproc.Imgproc
import org.opencv.contrib.Contrib
import org.opencv.highgui.Highgui
import nebula.imageProcessing.RichImage._
import skunkworks.LogPolar
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import spire.algebra._
import spire.math._
import spire.implicits._

import org.rogach.scallop._
import spark._
import org.apache.commons.io.FileUtils
import scala.util.Random
import shapeless._
import org.opencv._
import reflect.runtime.universe._
import skunkworks._
import OpenCVMTCUtil._
import billy.wideBaseline._
import billy.DetectorJsonProtocol._

/////////////////////////////////////////////////////////////////

class Conf(args: Seq[String]) extends ScallopConf(args) {
  banner("Pilgrim: A command line tool for running local feature experiments.")

  val runtimeConfigFile = opt[String](
    descr = "File with runtime environment information.",
    required = true,
    default = None) map ExistingFile.apply

  val sparkContextFile = opt[String](
    descr = "Optional file with Spark cluster computing information. If not defined, computation is local.",
    required = false,
    default = None) map ExistingFile.apply

  val tableConfigFiles = opt[List[String]](
    descr = "Files where each file specifies a table where each entry is an experiment.",
    required = true,
    default = Some(Nil)) map (_ map ExistingFile.apply)

  val parameterSweep = opt[Boolean](
    descr = "Whether to perform a parameter sweep for the NCCLogPolar descriptor.",
    required = false,
    default = Some(false))

  val writeImages = opt[Boolean](
    descr = "If true, will generate summary images.",
    required = false,
    default = Some(false))
}

object Util {
  // TODO: Get experiments from results.
  // TODO: Get summaries remotely.
  def mkTable(
    writeImages: Boolean,
    experimentsJson: Seq[Seq[JSONAndTypeName]],
    summariesJson: Seq[Seq[JSONAndTypeName]])(
      implicit imports: Imports,
      runtime: RuntimeConfig) {
    val tableTitles = {
      val jsons = experimentsJson.map(_.map(_.json))
      TableTitles(jsons)
    }

    val rowSize = experimentsJson.head.size

    val experimentsTable = {
      val summaries = summariesJson.par.map(_.map(json =>
        eval[ExperimentSummary](json.toSource.addImports))).toIndexedSeq

      Table(
        tableTitles.title,
        tableTitles.rowLabels,
        tableTitles.columnLabels,
        summaries.toMatrix)
    }

    // Make a table for each numeric summary option.
    val summaryNames =
      experimentsTable.entries(0, 0).summaryNumbers.keys
    for (summaryName <- summaryNames) {
      println("Making table for %s".format(summaryName))
      val entriesSeqSeq = experimentsTable.entries.toSeqSeq.flatten.par
      val mappedSeqSeq = entriesSeqSeq.map(_.summaryNumbers(summaryName))
      val entries = mappedSeqSeq.toIndexedSeq.grouped(rowSize).toIndexedSeq.toMatrix

      def writeTable(table: Table[Double]) {
        println("Table title is %s".format(table.title))
        println("Writing table to %s".format(table.path))
        val tsv = table.toTSV(_.formatted("%.2f"))
        FileUtils.writeStringToFile(table.path, tsv)
      }

      val absoluteTable = experimentsTable.copy(
        title = experimentsTable.title + "_" + summaryName,
        entries = entries)

      val relativeTable = experimentsTable.copy(
        title = experimentsTable.title + "_" + summaryName + "_normalized",
        entries = entries).normalizeColumns

      writeTable(absoluteTable)
      writeTable(relativeTable)
    }
  }

  val defineTable = """
  val experimentTable = {
    val rowLabels = HListUtil.mkTuple2(imageClasses, otherImages)
    val columnLabels = HListUtil.mkTuple3(detectors, extractors, matchers)

    object makeTable extends Poly1 {
      implicit def default = at[(String, Int)] {
        case (imageClass, otherImage) =>
          object constructExperiment extends Poly1 {
            implicit def default[D <% PairDetector, E <% Extractor[F], M <% Matcher[F], F] = at[(D, E, M)] {
              case (detector, extractor, matcher) => {
                WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
              }
            }
          }

          // This lifting, combined with flatMap, filters out types that can't be used                                                                                                                                                                                                             
          // to construct experiments.                                                                                                                                                                                                                                                             
          object constructExperimentLifted extends Lift1(constructExperiment)

          columnLabels flatMap constructExperimentLifted
      }
    }

    rowLabels map makeTable
  }   
"""

  val defineExperimentMessages = """
  val experimentMessages: Seq[JSONAndTypeName] = {
    // This is the same as "flatten".
    val experiments = experimentTable flatMap identity

    object constructJSONAndTypeName extends Poly1 {
      implicit def default[E <% RuntimeConfig => ExperimentRunner[R]: JsonFormat: TypeTag, R] =
        at[E] { experiment =>
          JSONAndTypeName(
            experiment.toJson,
            instanceToTypeName(experiment))
        }
    }

    (experiments map constructJSONAndTypeName) toList
  }
"""

  val defineExperimentMessageTable = """
  val experimentMessageTable: Seq[Seq[JSONAndTypeName]] = {
    val rowLabels = HListUtil.mkTuple2(imageClasses, otherImages)
    val columnLabels = HListUtil.mkTuple3(detectors, extractors, matchers)

    object makeTable extends Poly1 {
      implicit def default = at[(String, Int)] {
        case (imageClass, otherImage) =>
          object constructExperiment extends Poly1 {
            implicit def default[D <% PairDetector, E <% Extractor[F], M <% Matcher[F], F] = at[(D, E, M)] {
              case (detector, extractor, matcher) => {
                WideBaselineExperiment(
                  imageClass,
                  otherImage,
                  detector,
                  extractor,
                  matcher)
              }
            }
          }

          // This lifting, combined with flatMap, filters out types that can't be used                                                                                                                                                                                                             
          // to construct experiments.                                                                                                                                                                                                                                                             
          object constructExperimentLifted extends Lift1(constructExperiment)

          val experiments = columnLabels flatMap constructExperimentLifted

          object constructJSONAndTypeName extends Poly1 {
            implicit def default[E <% RuntimeConfig => ExperimentRunner[R]: JsonFormat: TypeTag, R] =
              at[E] { experiment =>
                JSONAndTypeName(
                  experiment.toJson,
                  instanceToTypeName(experiment))
              }
          }

          (experiments map constructJSONAndTypeName) toList
      }
    }

    (rowLabels map makeTable) toList
  }    
"""

  def getExperimentMessageTable(
    runtimeConfig: RuntimeConfig,
    experimentParametersSource: String)(
      implicit imports: Imports): Seq[Seq[JSONAndTypeName]] = {
    val source = s"""
loadOpenCV

implicit val runtimeConfig = ${getSource(runtimeConfig)}
      
// Defines:
// imageClasses: HList[String]
// otherImages: HList[Int]
// detectors: HList[_ <% PairDetector]
// extractors: HList[_ <% Extractor[_]]
// matchers: HList[_ <% Matcher[_]]
${experimentParametersSource}
   
// Defines:
// experimentMessageTable: Seq[Seq[JSONAndTypeName]]
${defineExperimentMessageTable}

experimentMessageTable
""".addImports

    eval[Seq[Seq[JSONAndTypeName]]](source)
  }

  def getExperimentMessages(
    runtimeConfig: RuntimeConfig,
    experimentParametersSource: String)(
      implicit imports: Imports): Seq[JSONAndTypeName] = {
    val source = s"""
  loadOpenCV
  
  implicit val runtimeConfig = ${getSource(runtimeConfig)}
        
  // Defines:
  // imageClasses: HList[String]
  // otherImages: HList[Int]
  // detectors: HList[_ <% PairDetector]
  // extractors: HList[_ <% Extractor[_]]
  // matchers: HList[_ <% Matcher[_]]
  ${experimentParametersSource}
  
  // Defines:
  // experimentTable: HList[HList[_ <% WideBaselineExperiment[...]]]
  ${defineTable}
      
  // Defines:
  // experimentMessages: Seq[JSONAndTypeName]
  ${defineExperimentMessages}
  
  experimentMessages
  """.addImports

    eval[Seq[JSONAndTypeName]](source)
  }
}

object Main {
  def main(unparsedArgs: Array[String]) {
    val args = new Conf(unparsedArgs)
    println(args.summary)

    implicit val imports = Imports(
      "java.io.File",
      "nebula._",
      "billy._",
      "skunkworks._",
      "spark._",
      "spark.SparkContext._",
      "org.opencv._",
      "org.opencv.contrib._",
      "shapeless._",
      "skunkworks.OpenCVMTCUtil._",
      "billy.wideBaseline._",
      "billy.DetectorJsonProtocol._",
      "reflect.runtime.universe._",
      "spray.json._",
      "spray.json.DefaultJsonProtocol._",
      "billy.summary._",
      "billy.detectors._",
      "billy.extractors._",
      "billy.matchers._")

    implicit val runtimeConfig =
      eval[RuntimeConfig](
        FileUtils.readFileToString(args.runtimeConfigFile()).addImports)

    val sparkContext = if (args.sparkContextFile.isDefined) {     
      val sparkContextSource = 
        FileUtils.readFileToString(args.sparkContextFile())
      
      val sparkContext = eval[SparkContext](sparkContextSource.addImports)
      
      Some(sparkContext)
    } else None

//    if (args.sparkContextFile.isDefined) {
//      trait MyTrait {
//        def bar: String
//      }
//      
//      implicit class IntToMyTrait(self: Int) extends MyTrait {
//        override def bar = s"int_${self}"
//      }
//      
//      implicit val int: Int = 5
//      
//      def foo(x: Int)(implicit int: Int) = x + int
//      
//      val results = sparkContext.get.parallelize(1 to 10).map(x => foo(x).bar)
//    }
    
    for (file <- args.tableConfigFiles()) {
      val experimentParametersSource = FileUtils.readFileToString(file)

      val experimentMessageTable = Util.getExperimentMessageTable(
        runtimeConfig,
        experimentParametersSource).transpose
      val experimentMessages = experimentMessageTable.flatten

      println(s"Running ${experimentMessages.size} experiments")

      val resultMessages: Seq[JSONAndTypeName] = sparkContext match {
        case Some(sparkContext) =>
          // Shuffle the experiments to provide better expected load
          // distribution.
          val shuffled = new Random(0).shuffle(experimentMessages)
          sparkContext.parallelize(shuffled).map(runExperiment).collect
        case None =>
          experimentMessages.par.map(runExperiment).toIndexedSeq
      }

      val summaryMessages: Seq[JSONAndTypeName] = sparkContext match {
        case Some(sparkContext) =>
          // Shuffle the experiments to provide better expected load
          // distribution.
          val shuffled = new Random().shuffle(resultMessages)
          sparkContext.parallelize(shuffled).map(getSummary).collect
        case None =>
          resultMessages.par.map(getSummary).toIndexedSeq
      }

      // TODO: Bug: The order has been messed up by the previous shuffles.
      val summaryMessageTable =
        summaryMessages.grouped(experimentMessageTable.head.size).toSeq

      Util.mkTable(
        args.writeImages(),
        experimentMessageTable,
        summaryMessageTable)
    }

    if (args.parameterSweep()) {
      loadOpenCV

      case class ParameterSetting(
        minRadius: Double,
        maxRadius: Double,
        numScales: Int,
        numAngles: Int,
        blurWidth: Double,
        scaleSearchRadiusFactor: Double)

      val parameterSettings = for (
        minRadius <- Seq(1, 2, 3, 4);
        maxRadius <- Seq(8, 16, 24, 32);
        numScales <- Seq(2, 4, 8, 16, 32);
        numAngles <- Seq(2, 4, 8, 16, 32);
        blurWidth <- Seq(0.2, 0.4, 0.8, 1.6, 3.2);
        scaleSearchRadiusFactor <- Seq(0, 0.2, 0.4, 0.6, 0.8)
      ) yield ParameterSetting(
        minRadius,
        maxRadius,
        numScales,
        numAngles,
        blurWidth,
        scaleSearchRadiusFactor)

      
      val shuffled = new scala.util.Random().shuffle(parameterSettings)
      val results = for (ParameterSetting(
        minRadius,
        maxRadius,
        numScales,
        numAngles,
        blurWidth,
        scaleSearchRadiusFactor) <- shuffled) yield {
        val fastDetector = BoundedPairDetector(
          BoundedDetector(OpenCVDetector.FAST, 5000),
          200)
        val siftDetector = BoundedPairDetector(
          BoundedDetector(OpenCVDetector.SIFT, 5000),
          200)

        val extractor = new contrib.NCCLogPolarExtractor(
          minRadius,
          maxRadius,
          numScales,
          numAngles,
          blurWidth)

        val scaleSearchRadius = (numScales * scaleSearchRadiusFactor).floor.toInt
        val matcher = new contrib.NCCLogPolarMatcher(scaleSearchRadius)

        val fast = (imageClass: String, otherImage: Int) =>
          WideBaselineExperiment(
            imageClass,
            otherImage,
            fastDetector,
            extractor,
            matcher)
        val fastAccuracy: Double = CompareMethods.relativeBenchmark(fast)

        val sift = (imageClass: String, otherImage: Int) =>
          WideBaselineExperiment(
            imageClass,
            otherImage,
            siftDetector,
            extractor,
            matcher)
        val siftAccuracy: Double = CompareMethods.relativeBenchmark(sift)

        val commonParams = List[String](
          minRadius.toString,
          maxRadius.toString,
          numScales.toString,
          numAngles.toString,
          blurWidth.toString,
          scaleSearchRadiusFactor.toString)

        val fastName = "fast" :: commonParams
        val siftName = "sift" :: commonParams

        Seq((fastAccuracy, fastName), (siftAccuracy, siftName))
      }

      val sorted = results.flatten.sortBy(_._1).reverse
      sorted foreach println
    }

    sparkContext foreach (_.stop)
  }
}
