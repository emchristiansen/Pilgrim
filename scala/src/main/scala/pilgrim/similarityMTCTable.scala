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

object SimilarityMTCTable {
  def run(
    sparkContext: SparkContext)(
      implicit runtimeConfig: RuntimeConfig,
      imports: Imports) = {
    loadOpenCV

    val nccLogPolarExperiment = (imageClass: String, otherImage: Int) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 5000),
        100)
      val extractor = new contrib.NCCLogPolarExtractor(
        4,
        32,
        8,
        16,
        1.2)
      val matcher = new contrib.NCCLogPolarMatcher(4)
      WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
    }

    val logPolarGrid = LogPolarGrid(
      minRadius = 0.5,
      maxRadius = 2,
      numScales = 8,
      numAngles = 16)

    //val logPolarGrid = LogPolarGrid(
    //      minRadius = 0.25,
    //      maxRadius = 4,
    //      numScales = 8,
    //      numAngles = 16)      

    val siftSExperiment = (imageClass: String, otherImage: Int) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 5000),
        100)
      val extractor = SimilarityMTCExtractor(
        OpenCVExtractor.SIFT,
        logPolarGrid)

      val matcher = SimilarityMTCMatcher[VectorMatcher.L2.type, IndexedSeq[Double]](
        VectorMatcher.L2,
        logPolarGrid.numScales / 2)
      WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
    }

    val lucidSExperiment = (imageClass: String, otherImage: Int) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.FAST, 5000),
        100)
      val extractor = SimilarityMTCExtractor(
        LUCIDExtractor(
          false,
          false,
          8,
          3,
          "Gray"),
        logPolarGrid)

      val matcher = SimilarityMTCMatcher[VectorMatcher.L1.type, IndexedSeq[Int]](
        VectorMatcher.L1,
        logPolarGrid.numScales / 2)
      WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
    }

    val lucidExperiment = (imageClass: String, otherImage: Int) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.FAST, 5000),
        100)

      val extractor = LUCIDExtractor(
        false,
        false,
        8,
        3,
        "Gray")

      val matcher = VectorMatcher.L1
      //      matcher: Matcher[SortDescriptor]
      WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
    }

    implicit val typeNameTODO_pilgrim_sim_0 =
      StaticTypeName.typeNameFromConcreteInstance(nccLogPolarExperiment("", 0))
    implicit val typeNameTODO_pilgrim_sim_1 =
      StaticTypeName.typeNameFromConcreteInstance(siftSExperiment("", 0))
    implicit val typeNameTODO_pilgrim_sim_2 =
      StaticTypeName.typeNameFromConcreteInstance(lucidSExperiment("", 0))
    implicit val typeNameTODO_pilgrim_sim_3 =
      StaticTypeName.typeNameFromConcreteInstance(lucidExperiment("", 0))

    def experimentToSource[E <% RuntimeConfig => ExperimentRunner[R]: JsonFormat: TypeName, R <% RuntimeConfig => ExperimentSummary](
      experiment: E)(implicit runtimeConfig: RuntimeConfig): ScalaSource[Double] = {
      val source = s"""
    loadOpenCV  

    implicit val runtimeConfig = ${getSource(runtimeConfig)} 
    
    val experiment = ${getSource(experiment)}
    val results = experiment.run
    val summary: ExperimentSummary = results
    
    asserty(summary.summaryNumbers.size == 1)
    summary.summaryNumbers.values.head
    """.addImports

      ScalaSource[Double](source)
    }

    val imageClasses = Seq(
      "graffiti",
      "wall",
      "bark")

    val otherImages = Seq(2, 3, 4, 5, 6)

    val sources = for (
      imageClass <- imageClasses;
      otherImage <- otherImages
    ) yield {
      Seq(
        (experimentToSource(nccLogPolarExperiment(imageClass, otherImage)),
          imageClass,
          otherImage,
          "nccLogPolar"),
//        (experimentToSource(siftSExperiment(imageClass, otherImage)),
//          imageClass,
//          otherImage,
//          "siftS"),
        (experimentToSource(lucidSExperiment(imageClass, otherImage)),
          imageClass,
          otherImage,
          "lucidS"),
        (experimentToSource(lucidExperiment(imageClass, otherImage)),
          imageClass,
          otherImage,
          "lucid"))
    }

    val sourcesFlat = sources.transpose.flatten

    val scores = Util.sparkShuffleMap(sparkContext, sourcesFlat.toIndexedSeq) { source =>
      (source._1.eval, source._2, source._3, source._4)
    }

    println(scores)
    println(scores.size)

    def getCurve(imageClass: String, method: String): Seq[Double] = {
      val hits = scores.filter(_._2 == imageClass).filter(_._4 == method)
      asserty(hits.size == 5)
      hits.sortBy(_._3).map(_._1)
    }
    
    val methods = Seq(
      "nccLogPolar",
//      "siftS",
      "lucidS",
      "lucid")
    
    for (method <- methods; imageClass <- imageClasses) {
      println(s"$method, $imageClass")
      println(getCurve(imageClass, method))
    }
  }
}