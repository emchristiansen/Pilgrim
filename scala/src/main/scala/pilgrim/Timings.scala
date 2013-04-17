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

object Timings {
  def run(implicit runtimeConfig: RuntimeConfig) {
    loadOpenCV

    val image = {
//      val file = ExistingFile(
//        runtimeConfig.dataRoot + "oxfordImages/graffiti/images/img1.bmp")
      val file = nebula.IO.getResource("/graffiti.bmp")
      Highgui.imread(file.getPath)
    }

    val keyPoints = {
      val keyPoints = new MatOfKeyPoint
      FeatureDetector.create(FeatureDetector.SIFT).detect(image, keyPoints)
      keyPoints
    }

    val extractOpenCV = (extractor: Int) => (numPoints: Int) => () => {
      val keyPointsCulled = keyPoints.toArray.take(numPoints)
      asserty(keyPointsCulled.size == numPoints)
      val descriptors = new Mat
      DescriptorExtractor.create(extractor).compute(
        image,
        new MatOfKeyPoint(keyPointsCulled: _*),
        descriptors)
      descriptors
    }

    def benchmarkMarginal[A](f: Seq[() => A], sizes: Seq[Int]): Double = {
      asserty(f.size == sizes.size)

      println(sizes)

      val timings = f map { f => Benchmark.measure(f, 1) }

      println(timings)

      // Solve AX = B.
      val a = new Array2DRowRealMatrix(timings.size, 2)
      val b = new Array2DRowRealMatrix(timings.size, 1)
      for (((size, timing), index) <- sizes.zip(timings).zipWithIndex) {
        a.setEntry(index, 0, size)
        a.setEntry(index, 1, 1) // Offset
        b.setEntry(index, 0, timing)
      }

      val solver = new SingularValueDecomposition(a).getSolver
      val x = solver.solve(b)

      //      println(a)
      //      println(b)
      //      println(x)

      // We want the linear term.
      x.getEntry(0, 0)
    }

    def benchmarkExtractor[A](f: Int => () => A, name: String): Unit = {
      val extractorSizes = Seq(10, 20, 100, 200, 300, 1000, 2000)
      val extractors = extractorSizes map f

      val marginal = benchmarkMarginal(extractors, extractorSizes)

      println(s"Benchmarked ${name}, and marginal cost was: ${marginal}")
    }

    val extractSIFT = extractOpenCV(DescriptorExtractor.SIFT)
    val extractBRIEF = extractOpenCV(DescriptorExtractor.BRIEF)
    val extractNLPolar = (numPoints: Int) => () => {
      val extractor = new contrib.NCCLogPolarExtractor(
        4,
        32,
        8,
        16,
        1.2)
      val keyPointsCulled = keyPoints.toArray.take(numPoints)
      asserty(keyPointsCulled.size == numPoints)
      Contrib.extract(extractor, image, new MatOfKeyPoint(keyPointsCulled: _*))
    }

        benchmarkExtractor(extractOpenCV(DescriptorExtractor.SIFT), "sift")
    //    benchmarkExtractor(extractOpenCV(DescriptorExtractor.BRIEF), "brief")
        benchmarkExtractor(extractOpenCV(DescriptorExtractor.BRISK), "brisk")
    //    benchmarkExtractor(extractOpenCV(DescriptorExtractor.ORB), "orb")
    //    benchmarkExtractor(extractOpenCV(DescriptorExtractor.FREAK), "freak")
        benchmarkExtractor(extractNLPolar, "nlPolar")

    //////////////////////////////

    def siftMatcher(numPoints: Int) = {
      val descriptors = extractOpenCV(DescriptorExtractor.SIFT).apply(numPoints).apply

      val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE)
      val dmatches = new MatOfDMatch

      () => matcher.`match`(descriptors, descriptors, dmatches)
    }

    val sizes = Seq(10, 20, 50, 75, 100)
    val marginal = benchmarkMarginal(
      sizes map siftMatcher,
      sizes map (x => math.pow(x, 2).round.toInt))

    println(s"Benchmarked sift matching, and marginal cost was: ${marginal}")
    
    def briskMatcher(numPoints: Int) = {
      val descriptors = extractOpenCV(DescriptorExtractor.BRISK).apply(numPoints).apply

      val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING)
      val dmatches = new MatOfDMatch

      () => matcher.`match`(descriptors, descriptors, dmatches)
    }

    val briskMarginal = benchmarkMarginal(
      sizes map briskMatcher,
      sizes map (x => math.pow(x, 2).round.toInt))

    println(s"Benchmarked brisk matching, and marginal cost was: ${briskMarginal}")    

    def nlPolarMatcher(numPoints: Int) = {
      val extractor = new contrib.NCCLogPolarExtractor(
        4,
        32,
        8,
        16,
        1.2)
      val vectorOptionNCCBlock = Contrib.extract(
        extractor,
        image,
        new MatOfKeyPoint(keyPoints.toArray.take(numPoints): _*))

      val vectorNCCBlock = Contrib.flatten(vectorOptionNCCBlock)

      val matcher = new contrib.NCCLogPolarMatcher(4)
      () => Contrib.matchAllPairs(matcher, vectorNCCBlock, vectorNCCBlock)
    }

//    val smallSizes = Seq(50, 75, 100, 125, 150)
    val marginalNLP = benchmarkMarginal(
      sizes map nlPolarMatcher,
      sizes map (x => math.pow(x, 2).round.toInt))

    println(s"Benchmarked nlPolar matching, and marginal cost was: ${marginalNLP}")

    //    def benchmarkMatcher[A](extractor: Int => () => Mat, name: String) {
    //      val extractorSizes = Seq(10, 20, 100, 200, 300, 1000, 2000)
    //      val extractors = extractorSizes map f
    //    }
  }
}