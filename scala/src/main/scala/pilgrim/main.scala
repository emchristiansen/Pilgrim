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

  val oxfordRadarPlot = opt[Boolean](
    descr = "Whether to create a radar plot for the Oxford dataset.",
    required = false,
    default = Some(false))

  val benchmark = opt[Boolean](
    descr = "Whether to benchmark the various methods.",
    required = false,
    default = Some(false))

  val precisionRecall = opt[Boolean](
    descr = "Whether to make precision-recall curves.",
    required = false,
    default = Some(false))

  val brownTable = opt[Boolean](
    descr = "Whether to run Brown experiments.",
    required = false,
    default = Some(false))

  val rotationAndScalePlot = opt[Boolean](
    descr = "Whether to create a plot of performance change with rotation and scale.",
    required = false,
    default = Some(false))

  val similarityMTCTable = opt[Boolean](
    descr = "Whether to create a table of recognition rate for several Similarity-MTC methods.",
    required = false,
    default = Some(false))

  val offsetHomography = opt[Boolean](
    descr = "Tests for estimating homographies from two regions.",
    required = false,
    default = Some(false))

  val writeImages = opt[Boolean](
    descr = "If true, will generate summary images.",
    required = false,
    default = Some(false))
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
      "billy.matchers._",
      "billy.wideBaseline._")

    implicit val runtimeConfig =
      eval[RuntimeConfig](
        FileUtils.readFileToString(args.runtimeConfigFile()).addImports)

    val sparkContext = if (args.sparkContextFile.isDefined) {
      val sparkContextSource =
        FileUtils.readFileToString(args.sparkContextFile())

      val sparkContext = eval[SparkContext](sparkContextSource.addImports)

      Some(sparkContext)
    } else None

    if (args.sparkContextFile.isDefined) {
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
//
//      println("\n\n" + results.collect.toList + "\n\n")
    }

    for (file <- args.tableConfigFiles()) {
      val experimentParametersSource = FileUtils.readFileToString(file)

      val experimentMessageTable = Util.getExperimentMessageTable(
        runtimeConfig,
        experimentParametersSource).transpose
      val experimentMessages = experimentMessageTable.flatten.toIndexedSeq

      println(s"Running ${experimentMessages.size} experiments")

      val summaryMessages = sparkContext match {
        case Some(sparkContext) => Util.sparkShuffleMap(
          sparkContext,
          experimentMessages) { experiment =>
            val results = runExperiment(experiment)
            getSummary(results)
          }
        case None =>
          experimentMessages.par.map(runExperiment).map(getSummary).toIndexedSeq
      }

      //      val summaryMessages = sparkContext match {
      //        case Some(sparkContext) => Util.sparkShuffleMap(
      //          sparkContext,
      //          resultMessages) {
      //            getSummary
      //          }
      //        case None =>
      //          resultMessages.par.map(getSummary).toIndexedSeq
      //      }      

      //      val resultMessages = sparkContext match {
      //        case Some(sparkContext) => Util.sparkShuffleMap(
      //          sparkContext,
      //          experimentMessages) {
      //            runExperiment
      //          }
      //        case None =>
      //          experimentMessages.par.map(runExperiment).toIndexedSeq
      //      }
      //
      //      val summaryMessages = sparkContext match {
      //        case Some(sparkContext) => Util.sparkShuffleMap(
      //          sparkContext,
      //          resultMessages) {
      //            getSummary
      //          }
      //        case None =>
      //          resultMessages.par.map(getSummary).toIndexedSeq
      //      }

      val summaryMessageTable =
        summaryMessages.grouped(experimentMessageTable.head.size).toSeq

      Util.mkTable(
        args.writeImages(),
        experimentMessageTable,
        summaryMessageTable)
    }

    if (args.parameterSweep()) {
      ParameterSweep.run(sparkContext.get)
    }

    if (args.oxfordRadarPlot()) {
      OxfordRadarPlot.run(sparkContext.get)
    }

    if (args.benchmark()) {
      Timings.run
    }

    if (args.precisionRecall()) {
      PrecisionRecall.run(sparkContext.get)
    }

    if (args.brownTable()) {
      BrownTable.run
    }

    if (args.rotationAndScalePlot()) {
      RotationAndScalePlot.run(sparkContext.get)
    }

    if (args.similarityMTCTable()) {
      SimilarityMTCTable.run(sparkContext.get)
    }
    
    if (args.offsetHomography()) {
      OffsetHomography.run(sparkContext.get)
    }

    sparkContext foreach (_.stop)
  }
}
