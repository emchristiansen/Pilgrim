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
import reflect._

/////////////////////////////

object Util {
  def summaryDirectory = 
    homeDirectory + "Dropbox/t/2013_q1/mtciccv2013_2/src/main/resources/figures"
  
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
      val summaries = summariesJson.map(_.map { json =>
        eval[ExperimentSummary](json.toSource.addImports)
      }).toIndexedSeq

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
            typeTag[E].tpe.toString)
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
                  typeTag[E].tpe.toString)
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

  def sparkShuffleMap[A: ClassTag, B: ClassTag](
    sparkContext: SparkContext,
    input: IndexedSeq[A])(
    function: A => B): IndexedSeq[B] = {
    val (shuffle, unshuffle) =
      nebula.util.Util.makeShufflers[A, B](input)
    val shuffled = sparkContext.parallelize(shuffle(input)).map(
      function).collect.toIndexedSeq
    unshuffle(shuffled)
  }
}