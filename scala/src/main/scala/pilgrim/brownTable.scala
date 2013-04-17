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
import skunkworks._
import OpenCVMTCUtil._
import billy.wideBaseline._
import billy.DetectorJsonProtocol._

import spyplot._

/////////////////////////////////////////////////////////////////

object BrownTable {
  def run(
    implicit runtimeConfig: RuntimeConfig) = {
    loadOpenCV

    //    val extractor = new contrib.NCCLogPolarExtractor(
    //      4,
    //      30,
    //      8,
    //      16,
    //      1.2)
    //    val matcher = new contrib.NCCLogPolarMatcher(4)

    val extractor = {
      val logPolar = LogPolarExtractor(
        false,
        2,
        30,
        8,
        16,
        2,
        "Gray")
      NCCLogPolarExtractor(logPolar)
    }
    
    val matcher = NCCLogPolarMatcher(true, 4)

    val brownExperiment = (dataset: String, numMatches: Int) => {
      val experiment = BrownExperiment(dataset, numMatches, extractor, matcher)

      val results = experiment.run
      val summary: ExperimentSummary = results

      asserty(summary.summaryNumbers.size == 1)
      val rate = summary.summaryNumbers("errorRateAtRecall95")

      println(s"$dataset, $numMatches, $rate")
    }

    brownExperiment("liberty", 1000)
    brownExperiment("notredame", 1000)
    //    brownExperiment("yosemite", 1000)
  }
}