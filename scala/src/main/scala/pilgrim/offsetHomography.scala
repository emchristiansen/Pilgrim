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
import java.awt.image.BufferedImage
import org.opencv._
import nebula.util._
import MathUtil._
import scala.util.{ Try, Success, Failure }

/////////////////////////////////////////////////////////////////

object OffsetHomography {
  def run(
    sparkContext: SparkContext)(
      implicit runtimeConfig: RuntimeConfig,
      imports: Imports) = {
    loadOpenCV

    def homographyGoodnessOfFit(
      correspondences: Seq[(KeyPoint, KeyPoint)],
      homography: Homography): Double = {
      val threshold = 2

      val hits = correspondences map {
        case (left, right) =>
          val attempt = Try {
            val estimate = homography.transformXYOnly(left)

            val xError = (estimate.pt.x - right.pt.x).abs
            val yError = (estimate.pt.y - right.pt.y).abs

            if (Seq(xError, yError).max > threshold) 0
            else 1
          }

          attempt.getOrElse(0)
      }

      hits.sum.toDouble / hits.size
    }

    val numKeyPointsToDrop = 0
    val numKeyPointsToKeep = 500
    val detector = BoundedPairDetector(
      BoundedDetector(OpenCVDetector.SIFT, 20000),
      numKeyPointsToDrop + numKeyPointsToKeep)
    val minRadius = 4
    val maxRadius = 32
    val numScales = 32
    val numAngles = 32
    val extractor = new contrib.NCCLogPolarExtractor(
      minRadius,
      maxRadius,
      numScales,
      numAngles,
      1.2)
    val scaleSearchRadius = numScales / 2
    val matcher = new contrib.NCCLogPolarMatcher(scaleSearchRadius)
    val numIters = 100

    def twoPointHomography(
      correspondences: Seq[((KeyPoint, contrib.NCCBlock), (KeyPoint, contrib.NCCBlock))]): Homography = {
      asserty(correspondences.size == 2)

      val ((leftKeyPointA, leftDescriptorA), (rightKeyPointA, rightDescriptorA)) = correspondences(0)
      val ((leftKeyPointB, leftDescriptorB), (rightKeyPointB, rightDescriptorB)) = correspondences(1)

      def verify(keyPoint: KeyPoint) {
        asserty(keyPoint.size == 1)
        asserty(keyPoint.angle == 0)
      }
      Seq(leftKeyPointA, rightKeyPointA, leftKeyPointB, rightKeyPointB) map verify

      //      val rightKeyPointAGolden = groundTruth.transform(leftKeyPointA)
      //      val rightKeyPointBGolden = groundTruth.transform(leftKeyPointB)

      def getDistanceMatrix(
        leftDescriptor: contrib.NCCBlock,
        rightDescriptor: contrib.NCCBlock): DenseMatrix[Double] = {
        val distanceMapMat =
          Contrib.getDistanceMap(matcher, leftDescriptor, rightDescriptor)

        DenseMatrixUtil.matToMatrixDoubleSingleChannel(
          distanceMapMat).get
      }

      def argminToScaleAndRotation(argmin: (Int, Int)): (Double, Double) = {
        val (scaleIndex, rotationIndex) = argmin

        val scaleRange = maxRadius / minRadius
        val scaleFactor =
          math.exp(math.log(scaleRange) / (numScales - 1))
        assertNear(math.pow(scaleFactor, numScales - 1), scaleRange)

        val scaleOffset = scaleSearchRadius - scaleIndex
        val scale = math.pow(scaleFactor, scaleOffset)

        val angle = {
          val unnormalized = -2 * math.Pi * rotationIndex.toDouble / numAngles
          unnormalized mod (2 * math.Pi)
        }

        (scale, angle)
      }

      def relativeScaleAndRotation(
        leftDescriptor: contrib.NCCBlock,
        rightDescriptor: contrib.NCCBlock): (Double, Double) = {
        val distances = getDistanceMatrix(leftDescriptor, rightDescriptor)
        argminToScaleAndRotation(distances.argmin)
      }

      def adjustKeyPoint(
        rightKeyPoint: KeyPoint,
        leftDescriptor: contrib.NCCBlock,
        rightDescriptor: contrib.NCCBlock): KeyPoint = {
        val (scale, rotation) = relativeScaleAndRotation(
          leftDescriptor, rightDescriptor)
        new KeyPoint(
          rightKeyPoint.pt.x.toFloat,
          rightKeyPoint.pt.y.toFloat,
          scale.toFloat,
          rotation.toFloat)
      }

      val adjustedA = adjustKeyPoint(
        rightKeyPointA,
        leftDescriptorA,
        rightDescriptorA)

      val adjustedB = adjustKeyPoint(
        rightKeyPointB,
        leftDescriptorB,
        rightDescriptorB)

      //      println("In 2 point")
      //      println("A")
      //      println(leftKeyPointA)
      //      println(rightKeyPointAGolden)
      //      println(adjustedA)
      //      println("B")
      //      println(leftKeyPointB)
      //      println(rightKeyPointBGolden)
      //      println(adjustedB)

      Geometry.fitHomographyFromTwoSimilarityCorrespondences(
        Seq(leftKeyPointA, leftKeyPointB),
        Seq(adjustedA, adjustedB))

      //      groundTruth
    }

    val offsetExperiment = (leftImage: BufferedImage, rightImage: BufferedImage) => {
      //      val experiment = WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)

      //      val groundTruth = experiment.groundTruth

      def keyPointsAndDescriptors(image: BufferedImage) = {
        val keyPoints = detector.detect(image).drop(numKeyPointsToDrop)
        val descriptors = extractor.extract(image, keyPoints)

        val seq =
          for ((keyPoint, Some(descriptor)) <- (keyPoints zip descriptors)) yield (keyPoint, descriptor)
        seq.toIndexedSeq
      }

      val leftRegions = keyPointsAndDescriptors(leftImage)
      val rightRegions = keyPointsAndDescriptors(rightImage)

      val dmatches = matcher.doMatch(true, leftRegions.map(_._2), rightRegions.map(_._2))

      val groupedByLeft = dmatches.groupBy(_.queryIdx)
      val groups: IndexedSeq[DMatch] = groupedByLeft.values.map(_.sortBy(_.distance).head).toIndexedSeq

      // Repeat a test |numIters| times: Select a subset of points at random,
      // fit a homography, and see how good it is.
      val scores = for (index <- 0 until numIters) yield {
        val correspondences: Seq[DMatch] = new Random(index).shuffle(groups).toIndexedSeq

        val regionCorrespondences = for (dmatch <- correspondences) yield (
          leftRegions(dmatch.queryIdx), rightRegions(dmatch.trainIdx))

        val keyPointCorrespondences = regionCorrespondences map {
          case ((k0, d0), (k1, d1)) => (k0, k1)
        }

        val (left4, right4) = keyPointCorrespondences.take(4).unzip

        val homography4: Homography = Geometry.fitHomographyFromFourTranslations(
          left4, right4)

        val goodness4 = homographyGoodnessOfFit(keyPointCorrespondences, homography4)

        def wipeKeyPoint(keyPointWithCrap: KeyPoint) = new KeyPoint(
          keyPointWithCrap.pt.x.toFloat,
          keyPointWithCrap.pt.y.toFloat,
          1,
          0)

        def wipeCorrespondence(correspondence: (KeyPoint, contrib.NCCBlock)) =
          (wipeKeyPoint(correspondence._1), correspondence._2)

        def wipeCorrespondencePair(pair: ((KeyPoint, contrib.NCCBlock), (KeyPoint, contrib.NCCBlock))) =
          (wipeCorrespondence(pair._1), wipeCorrespondence(pair._2))

        val homography2: Homography =
          twoPointHomography(
            regionCorrespondences.take(2) map wipeCorrespondencePair)
        val goodness2 = homographyGoodnessOfFit(keyPointCorrespondences, homography2)
        //        println(homography4)
        //        println(goodness2)

        //        println(groundTruth)

        (goodness4, goodness2)
      }

      val (scores4, scores2) = (scores.map(_._1), scores.map(_._2))

      def isInlierSet(score: Double): Int =
        if (score > 0.8) 1 else 0

      val hits4 = scores4 map isInlierSet
      val hits2 = scores2 map isInlierSet

//      (MathUtil.mean(hits4), MathUtil.mean(hits2))
      (MathUtil.mean(scores4), MathUtil.mean(scores2))
      //      (MathUtil.mean(scores.map(_._1)), MathUtil.mean(scores.map(_._2)))
    }

    val imageClasses = Seq(
      "graffiti",
      "trees",
      "jpeg",
      "boat",
      "bark",
      "bikes",
      "light",
      "wall")

    for (imageClass <- imageClasses) {
      val scorePairs = 2 to 6 map { otherImage =>
        val root = runtimeConfig.dataRoot + "oxfordImages" + imageClass + "images"
        val leftImage = ImageIO.read(root + "img1.bmp")
        asserty(leftImage.getWidth > 0)
        val rightImage = ImageIO.read(root + s"img$otherImage.bmp")
        asserty(rightImage.getWidth > 0)
        offsetExperiment(leftImage, rightImage)
      }

      val (scores4, scores2) = scorePairs unzip

      println(imageClass)
      println(s"4-point: $scores4")
      println(s"2-point: $scores2")
    }

    //    val leftImageIndex = 4
    //    val goodnessPairs = 0 to 9 map { rightImageIndex =>
    //      val houseRoot = runtimeConfig.dataRoot + "oxfordModelHouse"
    //      val leftImage = ImageIO.read(houseRoot + s"house.00$leftImageIndex.png")
    //      val rightImage = ImageIO.read(houseRoot + s"house.00$rightImageIndex.png")
    //
    //      offsetExperiment(leftImage, rightImage)
    //    }
    //
    //    println(s"$leftImageIndex")
    //    println(s"4-point: ${goodnessPairs.map(_._1)}")
    //    println(s"2-point: ${goodnessPairs.map(_._2)}")

    //    val timesSquareRoot = runtimeConfig.dataRoot + "timesSquare"
    //    val timesSquare0 = ImageIO.read(timesSquareRoot + "TimesSquare0_gray.png")
    //    val timesSquare1 = ImageIO.read(timesSquareRoot + "TimesSquare1_gray.png")
    //    println(timesSquare0.getWidth)
    //    println(timesSquare0.getHeight)
    //    println(timesSquare1.getWidth)
    //    println(timesSquare1.getHeight)
    //    println(offsetExperiment(timesSquare0, timesSquare1))
  }
}