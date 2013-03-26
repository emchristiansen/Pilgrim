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

import org.json4s._
import org.json4s.native.JsonMethods._
import spyplot._

/////////////////////////////////////////////////////////////////

object ParameterSweep {
  def run(
    sparkContext: SparkContext)(
      implicit runtimeConfig: RuntimeConfig,
      imports: Imports) = {
    loadOpenCV

    case class ParameterSetting(
      minRadius: Double,
      maxRadius: Double,
      numScales: Int,
      numAngles: Int,
      blurWidth: Double,
      scaleSearchRadiusFactor: Double)      

//    val parameterSettings = for (
//      //      minRadius <- Seq(1, 2, 4);
//      minRadius <- Seq(1, 2, 3, 4, 5);
//      maxRadius <- Seq(32);
//      numScales <- Seq(4, 8, 16, 32);
//      numAngles <- Seq(8, 16, 32);
//      blurWidth <- Seq(0.8, 1.0);
//      //              scaleSearchRadiusFactor <- Seq(0.3, 0.6)
//      scaleSearchRadiusFactor <- Seq(0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7)
//    ) yield ParameterSetting(
//      minRadius,
//      maxRadius,
//      numScales,
//      numAngles,
//      blurWidth,
//      scaleSearchRadiusFactor)
      
    val parameterSettings = for (
      //      minRadius <- Seq(1, 2, 4);
      minRadius <- Seq(2, 3, 4, 5);
      maxRadius <- Seq(32);
      numScales <- Seq(4, 8, 16);
      numAngles <- Seq(16, 32, 64);
      blurWidth <- Seq(0.6, 0.8, 1.0, 1.2);
//      scaleSearchRadiusFactor <- Seq(0.3, 0.4, 0.5, 0.6, 0.7)
      scaleSearchRadiusFactor <- Seq(0.3, 0.4, 0.5, 0.6, 0.7)      
    ) yield ParameterSetting(
      minRadius,
      maxRadius,
      numScales,
      numAngles,
      blurWidth,
      scaleSearchRadiusFactor)      
      
//    val parameterSettings = for (
//      //      minRadius <- Seq(1, 2, 4);
//      minRadius <- Seq(4);
//      maxRadius <- Seq(32);
//      numScales <- Seq(8);
//      numAngles <- Seq(16);
//      blurWidth <- Seq(0.8);
////      scaleSearchRadiusFactor <- Seq(0.3, 0.4, 0.5, 0.6, 0.7)
//      scaleSearchRadiusFactor <- Seq(0.5)      
//    ) yield ParameterSetting(
//      minRadius,
//      maxRadius,
//      numScales,
//      numAngles,
//      blurWidth,
//      scaleSearchRadiusFactor)       

    //      val shuffled = new scala.util.Random().shuffle(parameterSettings)
    val incompleteExperimentsUnflat = for (
      parameterSetting @ ParameterSetting(
        minRadius,
        maxRadius,
        numScales,
        numAngles,
        blurWidth,
        scaleSearchRadiusFactor) <- parameterSettings
    ) yield {
      val maxKeyPoints = 100
      
      val fastDetector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.FAST, 20 * maxKeyPoints),
        maxKeyPoints)
      val siftDetector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 20 * maxKeyPoints),
        maxKeyPoints)
      val briskDetector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.BRISK, 20 * maxKeyPoints),
        maxKeyPoints)
      val orbDetector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.ORB, 20 * maxKeyPoints),
        maxKeyPoints)
      val surfDetector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SURF, 20 * maxKeyPoints),
        maxKeyPoints)

      val extractor = new contrib.NCCLogPolarExtractor(
        minRadius,
        maxRadius,
        numScales,
        numAngles,
        blurWidth)

      val scaleSearchRadius = (numScales * scaleSearchRadiusFactor).floor.toInt
      val matcher = new contrib.NCCLogPolarMatcher(scaleSearchRadius)

      ////////////////////////

      val fast = IncompleteWideBaselineExperiment(
        fastDetector,
        extractor,
        matcher)

      val sift = IncompleteWideBaselineExperiment(
        siftDetector,
        extractor,
        matcher)

      val brisk = IncompleteWideBaselineExperiment(
        briskDetector,
        extractor,
        matcher)

      val orb = IncompleteWideBaselineExperiment(
        orbDetector,
        extractor,
        matcher)

      val surf = IncompleteWideBaselineExperiment(
        surfDetector,
        extractor,
        matcher)

      val fastClosure = (imageClass: String, otherImage: Int) =>
        WideBaselineExperiment(
          imageClass,
          otherImage,
          fastDetector,
          extractor,
          matcher)

      val siftClosure = (imageClass: String, otherImage: Int) =>
        WideBaselineExperiment(
          imageClass,
          otherImage,
          siftDetector,
          extractor,
          matcher)

      val briskClosure = (imageClass: String, otherImage: Int) =>
        WideBaselineExperiment(
          imageClass,
          otherImage,
          briskDetector,
          extractor,
          matcher)

      val orbClosure = (imageClass: String, otherImage: Int) =>
        WideBaselineExperiment(
          imageClass,
          otherImage,
          orbDetector,
          extractor,
          matcher)

      val surfClosure = (imageClass: String, otherImage: Int) =>
        WideBaselineExperiment(
          imageClass,
          otherImage,
          surfDetector,
          extractor,
          matcher)

      Seq(
        (CompareMethods.relativeBenchmarkSources(fastClosure), fast.toJson, parameterSetting),
        (CompareMethods.relativeBenchmarkSources(siftClosure), sift.toJson, parameterSetting),
        (CompareMethods.relativeBenchmarkSources(briskClosure), brisk.toJson, parameterSetting),
        (CompareMethods.relativeBenchmarkSources(orbClosure), orb.toJson, parameterSetting),
        (CompareMethods.relativeBenchmarkSources(surfClosure), surf.toJson, parameterSetting))
    }

    val incompleteExperiments: Seq[(Seq[ScalaSource[Double]], JsValue, ParameterSetting)] =
      incompleteExperimentsUnflat.flatten

    println(s"There are ${incompleteExperiments.size} incomplete experiments.")

    val experimentsPerIncomplete = incompleteExperiments.head._1.size
    for (incompleteExperiment <- incompleteExperiments) {
      asserty(incompleteExperiment._1.size == experimentsPerIncomplete)
    }

    val ungrouped = incompleteExperiments.map(_._1).flatten
    println(s"There are ${ungrouped.size} total tasks")
    val ungroupedScores = Util.sparkShuffleMap(
      sparkContext,
      ungrouped.toIndexedSeq) {
        _.eval
      }

    val groupedScores = ungroupedScores.grouped(experimentsPerIncomplete).toSeq
    val scores = for (group <- groupedScores) yield {
      (group sum) / (group size)
    }

    val reversed = (scores zip incompleteExperiments.map(_._2)) sortBy (_._1)
    val sortedResults = reversed reverse

    //    sortedResults foreach println

    //////////////////////////////////

    val sizes = incompleteExperiments.map(_._3).map { parameterSetting =>
      parameterSetting.numScales * parameterSetting.numAngles
    }

    val jValues = incompleteExperiments.map(_._2).map { jsValue =>
      parse(jsValue.compactPrint)
    }

    val detectors = jValues map { jValue =>
      val detectorName = jValue \\ "detector" \\ "pairDetector" \\ "detector"
      detectorName.values.toString
    }

    printBestParametersForEachSize(
      scores,
      sizes,
      incompleteExperiments.map(_._2))
    genAccuracyVsSpeedPlot(scores, sizes, detectors)
  }

  def printBestParametersForEachSize(
    scores: Seq[Double],
    sizes: Seq[Int],
    jsValues: Seq[JsValue]) {
    asserty(scores.size == sizes.size)
    asserty(scores.size == jsValues.size)

    val numToPrintForEachSize = 8

    val ungrouped = (sizes, scores, jsValues) zipped

    val grouped = ungrouped groupBy (_._1)

    for ((size, group) <- grouped) {
      println(s"\n\nSize is ${size}.")

      val scores = group map (_._2) toList
      val jsValues = group map (_._3) toList

      val reversed = (scores zip jsValues) sortBy (_._1)
      reversed.reverse.take(numToPrintForEachSize) foreach (println)
    }
  }

  def genAccuracyVsSpeedPlot(
    scores: Seq[Double],
    sizes: Seq[Int],
    detectors: Seq[String]) {
    asserty(scores.size == sizes.size)
    asserty(scores.size == detectors.size)

    val sizesWithScores = sizes zip scores

    val grouped = (detectors zip sizesWithScores) groupBy (_._1)

    val (siftSizes, siftScores) = grouped("SIFT").map(_._2).unzip
    val (fastSizes, fastScores) = grouped("FAST").map(_._2).unzip
    val (briskSizes, briskScores) = grouped("BRISK").map(_._2).unzip
    val (orbSizes, orbScores) = grouped("ORB").map(_._2).unzip
    val (surfSizes, surfScores) = grouped("SURF").map(_._2).unzip

    val plot = new SPyPlot {
      override def source = s"""     
fig = figure()
ax = fig.add_subplot(1,1,1)      
      
ax.scatter(
  ${siftSizes.toPyList}, 
  ${siftScores.toPyList}, 
  label='SIFT detector',
  c='b',
  marker='o',
  antialiased=True)
ax.scatter(
  ${fastSizes.toPyList}, 
  ${fastScores.toPyList}, 
  label='FAST detector',
  c='r',
  marker='D',
  antialiased=True)
ax.scatter(
  ${briskSizes.toPyList}, 
  ${briskScores.toPyList}, 
  label='BRISK detector',
  c='g',
  marker='*',
  antialiased=True)
ax.scatter(
  ${orbSizes.toPyList}, 
  ${orbScores.toPyList}, 
  label='ORB detector',
  c='k',
  marker='^',
  antialiased=True)
ax.scatter(
  ${surfSizes.toPyList}, 
  ${surfScores.toPyList}, 
  label='SURF detector',
  c='m',
  marker='<',
  antialiased=True)  

ax.set_xscale('log', basex=2)  
  
ax.set_xlabel("Descriptor size")
ax.set_ylabel("Relative recognition rate")  
  
ax.legend(loc='lower right')

ax.grid(True)
  
savefig("${plotFile}", bbox_inches='tight')
"""
    } toImage

    ImageIO.write(plot, "png", Util.summaryDirectory + "parameterSweep.png")
  }
}