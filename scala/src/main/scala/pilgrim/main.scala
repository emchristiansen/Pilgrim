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

  val tableConfigFiles = opt[List[String]](
    descr = "Files where each file specifies a table where each entry is an experiment.",
    required = false,
    default = Some(Nil)) map (_ map ExistingFile.apply)

  val writeImages = opt[Boolean](
    descr = "If true, will generate summary images.",
    required = false,
    default = Some(false))
}

object Util {
  def mkTable(
    writeImages: Boolean,
    capstonesAndJson: Seq[Seq[(Capstone, JsValue)]])(
      implicit runtime: RuntimeConfig,
      sparkContext: SparkContext) {
    val capstones = capstonesAndJson.map(_.map(_._1))
    val tableTitles = {
      val jsons = capstonesAndJson.map(_.map(_._2))
      TableTitles(jsons)
    }

    val rowSize = capstones.head.size

    val experimentsTable = {
      // Run the experiments and write the results to disk.
      runCapstones(writeImages, capstones.flatten)
      // Rerun the experiments to load from disk.
      val summaries = capstones.flatten.toIndexedSeq.map(_.apply(writeImages, runtime)).grouped(rowSize).toIndexedSeq
      Table(tableTitles.title, tableTitles.rowLabels, tableTitles.columnLabels, summaries.toMatrix)
    }

    sparkContext.stop

    // Make a table for each numeric summary option.
    val summaryNames =
      experimentsTable.entries(0, 0).summaryNumbers.keys
    for (summaryName <- summaryNames) {
      println("Making table for %s".format(summaryName))
      val entriesSeqSeq = experimentsTable.entries.toSeqSeq.flatten.par
      val mappedSeqSeq = entriesSeqSeq.map(_.summaryNumbers(summaryName).apply)
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

  def runCapstones(
    writeImages: Boolean,
    capstones: Seq[Capstone])(
      implicit runtimeConfig: RuntimeConfig,
      sparkContext: SparkContext) {
    // Evenly spread the work across the workers (in expectation).
    val shuffled = new Random(0).shuffle(capstones)
    sparkContext.parallelize(shuffled).foreach(_(writeImages, runtimeConfig))
  }
}

object Main {
  def main(unparsedArgs: Array[String]) {
    //        println(Foo.answer)

    val args = new Conf(unparsedArgs)
    println(args.summary)

    val source = s"""
    loadOpenCV
      
    ${experimentParameters}
    
    ${defineTable}
    
    ${defineExperiments}
    
    ${runExperiments}
    """
    
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
      "billy.summary._",
      "billy.detectors._")

    implicit val (runtimeConfig, sparkContext) =
      eval[Tuple2[RuntimeConfig, SparkContext]](
        FileUtils.readFileToString(args.runtimeConfigFile()).addImports(imports))

    if (args.tableConfigFiles.isDefined) {
      for (file <- args.tableConfigFiles()) {
        val expression = FileUtils.readFileToString(file).addImports(imports)
        val capstonesAndJsons = eval[Seq[Seq[(Capstone, JsValue)]]](expression)
        
        Util.runCapstones(false, Seq(capstonesAndJsons.head.head._1))
        
//        Util.mkTable(args.writeImages(), capstonesAndJsons)
      }
    }
  }
}
