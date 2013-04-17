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
import org.apache.commons.math3.linear._
import org.opencv.highgui._
import org.opencv.features2d._
import org.opencv.contrib._
import org.opencv._

/////////////////////////////////////////////////////////////////

object RotationAndScalePlot {
  def run(
    implicit runtimeConfig: RuntimeConfig) = {
    loadOpenCV

    val numKeyPoints = 200

    val imagePath = ExistingFile(
      runtimeConfig.dataRoot + "oxfordImages/boat/images/img1.bmp")

    val siftExperiment = (scaleFactor: Double, angle: Double) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 5000),
        numKeyPoints)
      val extractor = OpenCVExtractor.SIFT
      val matcher = VectorMatcher.L2
      val experiment = RotationAndScaleExperiment(
        imagePath,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
      val summary: ExperimentSummary = experiment.run
      asserty(summary.summaryNumbers.size == 1)
      summary.summaryNumbers("recognitionRate")
    }

    val nccLogPolarExperiment = (scaleFactor: Double, angle: Double) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 5000),
        numKeyPoints)
      val extractor = new contrib.NCCLogPolarExtractor(
        1,
        32,
        32,
        32,
        1.2)
      val matcher = new contrib.NCCLogPolarMatcher(16)
      val experiment = RotationAndScaleExperiment(
        imagePath,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
      val summary: ExperimentSummary = experiment.run
      asserty(summary.summaryNumbers.size == 1)
      summary.summaryNumbers("recognitionRate")
    }

    val lucidExperiment = (scaleFactor: Double, angle: Double) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.FAST, 5000),
        numKeyPoints)
      val extractor = LUCIDExtractor(
        false,
        false,
        24,
        5,
        "Gray")
      val matcher = VectorMatcher.L1
      val experiment = RotationAndScaleExperiment(
        imagePath,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
      val summary: ExperimentSummary = experiment.run
      asserty(summary.summaryNumbers.size == 1)
      summary.summaryNumbers("recognitionRate")
    }

    val briskExperiment = (scaleFactor: Double, angle: Double) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.BRISK, 5000),
        numKeyPoints)
      val extractor = OpenCVExtractor.BRISK
      val matcher = VectorMatcher.L0
      val experiment = RotationAndScaleExperiment(
        imagePath,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
      val summary: ExperimentSummary = experiment.run
      asserty(summary.summaryNumbers.size == 1)
      summary.summaryNumbers("recognitionRate")
    }

    def getPlot(curve: Seq[(Double, Double)], label: String, color: String): String = {
      val (xs, ys) = curve.unzip
      s"""
ax.plot(
  ${xs.mkString("[", ", ", "]")},
  ${ys.mkString("[", ", ", "]")},
  label = '${label}',
  antialiased=True,
  linewidth=4.0,
  c = '${color}',
  alpha=0.5)
        """
    }

//    val scalePlot = {
//      def plotFromMethod(
//        experiment: (Double, Double) => Double,
//        label: String,
//        color: String): String = {
//        val curve = for (scaleFactor <- (0.4 to 3.0 by 0.05).par) yield {
//          val score: Double = experiment(scaleFactor, 0)
//          val cleanedScore = if (!(score > 0)) 0 else score
//          (scaleFactor, cleanedScore)
//        }
//        getPlot(curve.toIndexedSeq, label, color)
//      }
//
//      val plot = new SPyPlot {
//        override def source = s"""     
//fig = figure(figsize=(10, 10))
//ax = fig.add_subplot(1, 1, 1)
//        
//########################      
//     
//${plotFromMethod(siftExperiment, "SIFT", "r")}
//${plotFromMethod(nccLogPolarExperiment, "NLPolar", "g")}
//${plotFromMethod(lucidExperiment, "LUCID", "m")}
//${plotFromMethod(briskExperiment, "BRISK", "c")}
//
//ax.legend(loc='lower right')
//ax.grid(True)
//ax.set_xlabel("Scale factor")
//ax.set_ylabel("Recognition rate")
//
//
//tight_layout()   
//
//savefig("/u/echristiansen/Dropbox/transfer/scaleChange.pdf", bbox_inches='tight')
//savefig("${plotFile}", bbox_inches='tight')
//"""
//      } toImage
//
//      println(Util.summaryDirectory + "scaleChange.png")
//      ImageIO.write(plot, "png", Util.summaryDirectory + "scaleChange.png")
//    }

    val rotationPlot = {
      def plotFromMethod(
        experiment: (Double, Double) => Double,
        label: String,
        color: String): String = {
        val curve = for (angle <- (0.0 to 2 * math.Pi by 0.1).par) yield {
          val score: Double = experiment(1, angle)
          val cleanedScore = if (score == Double.NaN) 0 else score
          (angle, cleanedScore)
        }
        getPlot(curve.toIndexedSeq, label, color)
      }

      val plot = new SPyPlot {
        override def source = s"""     
fig = figure(figsize=(10, 10))
ax = fig.add_subplot(1, 1, 1)
        
########################      
     
${plotFromMethod(siftExperiment, "SIFT", "r")}
${plotFromMethod(nccLogPolarExperiment, "NLPolar", "g")}
${plotFromMethod(lucidExperiment, "LUCID", "m")}
${plotFromMethod(briskExperiment, "BRISK", "c")}

ax.legend(loc='lower right')
ax.grid(True)
ax.set_xlabel("Rotation angle")
ax.set_ylabel("Recognition rate")


tight_layout()   

savefig("/u/echristiansen/Dropbox/transfer/angleChange.pdf", bbox_inches='tight')
savefig("${plotFile}", bbox_inches='tight')
"""
      } toImage

      println(Util.summaryDirectory + "angleChange.png")
      ImageIO.write(plot, "png", Util.summaryDirectory + "angleChange.png")
    }
  }
}