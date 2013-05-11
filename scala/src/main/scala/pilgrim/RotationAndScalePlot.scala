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
    sparkContext: SparkContext)(
      implicit runtimeConfig: RuntimeConfig,
      imports: Imports) = {
    loadOpenCV

    val numKeyPoints = 100

    //    val imageClass = "boat"

    val siftExperiment = (imageClass: String, scaleFactor: Double, angle: Double) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 5000),
        numKeyPoints)
      val extractor = OpenCVExtractor.SIFT
      val matcher = VectorMatcher.L2
      RotationAndScaleExperiment(
        imageClass,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
    }

    val nccLogPolarExperiment = (imageClass: String, scaleFactor: Double, angle: Double) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 5000),
        numKeyPoints)
      val numScales = 16
      val numAngles = 32
      val extractor = new contrib.NCCLogPolarExtractor(
        1,
        32,
        numScales,
        numAngles,
        1.2)
      val matcher = new contrib.NCCLogPolarMatcher(numScales / 2)
      RotationAndScaleExperiment(
        imageClass,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
    }

    val lucidExperiment = (imageClass: String, scaleFactor: Double, angle: Double) => {
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
      RotationAndScaleExperiment(
        imageClass,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
    }

    val briskExperiment = (imageClass: String, scaleFactor: Double, angle: Double) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.BRISK, 5000),
        numKeyPoints)
      val extractor = OpenCVExtractor.BRISK
      val matcher = VectorMatcher.L0
      RotationAndScaleExperiment(
        imageClass,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
    }

    val orbExperiment = (imageClass: String, scaleFactor: Double, angle: Double) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.FAST, 5000),
        numKeyPoints)
      val extractor = OpenCVExtractor.ORB
      val matcher = VectorMatcher.L0
      RotationAndScaleExperiment(
        imageClass,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
    }

    val sidExperiment = (imageClass: String, scaleFactor: Double, angle: Double) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.FAST, 5000),
        numKeyPoints)
      val extractor = SIDExtractor
      val matcher = VectorMatcher.L2
      RotationAndScaleExperiment(
        imageClass,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
    }

    val siftSExperimentScale = (imageClass: String, scaleFactor: Double, angle: Double) => {
      //      val scaleWidth = 3.0
      val logPolarGridScale = LogPolarGrid(
        minRadius = 0.5,
        maxRadius = 2.0,
        numScales = 32,
        numAngles = 2)

      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 5000),
        100)
      val extractor = SimilarityMTCExtractor(
        OpenCVExtractor.SIFT,
        logPolarGridScale)

      val matcher = SimilarityMTCMatcher[VectorMatcher.L2.type, IndexedSeq[Double]](
        VectorMatcher.L2,
        logPolarGridScale.numScales / 2)
      RotationAndScaleExperiment(
        imageClass,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
    }

    val siftSExperimentRotation = (imageClass: String, scaleFactor: Double, angle: Double) => {
      val logPolarGridRotation = LogPolarGrid(
        minRadius = 0.99,
        maxRadius = 1.0,
        numScales = 2,
        numAngles = 16)

      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 5000),
        100)
      val extractor = SimilarityMTCExtractor(
        OpenCVExtractor.SIFT,
        logPolarGridRotation)

      val matcher = SimilarityMTCMatcher[VectorMatcher.L2.type, IndexedSeq[Double]](
        VectorMatcher.L2,
        logPolarGridRotation.numScales / 2)
      RotationAndScaleExperiment(
        imageClass,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
    }

    val lucidSExperimentScale = (imageClass: String, scaleFactor: Double, angle: Double) => {
      //      val scaleWidth = 2.51
      val logPolarGridScale = LogPolarGrid(
        minRadius = 0.5,
        maxRadius = 2.0,
        numScales = 16,
        numAngles = 2)

      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 5000),
        100)
      val extractor = SimilarityMTCExtractor(
        LUCIDExtractor(
          false,
          false,
          24,
          5,
          "Gray"),
        logPolarGridScale)

      val matcher = SimilarityMTCMatcher[VectorMatcher.L1.type, IndexedSeq[Int]](
        VectorMatcher.L1,
        logPolarGridScale.numScales / 2)
      RotationAndScaleExperiment(
        imageClass,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
    }

    val lucidSExperimentRotation = (imageClass: String, scaleFactor: Double, angle: Double) => {
      val logPolarGridRotation = LogPolarGrid(
        minRadius = 0.99,
        maxRadius = 1.0,
        numScales = 2,
        numAngles = 16)

      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 5000),
        100)
      val extractor = SimilarityMTCExtractor(
        LUCIDExtractor(
          false,
          false,
          24,
          5,
          "Gray"),
        logPolarGridRotation)

      val matcher = SimilarityMTCMatcher[VectorMatcher.L1.type, IndexedSeq[Int]](
        VectorMatcher.L1,
        logPolarGridRotation.numScales / 2)
      RotationAndScaleExperiment(
        imageClass,
        detector,
        extractor,
        matcher,
        scaleFactor,
        angle)
    }

    implicit val typeNameTODO_pilgrim_rotationAndScale_0 =
      StaticTypeName.typeNameFromConcreteInstance(siftExperiment("", 0, 0))
    //    implicit val typeNameTODO_pilgrim_rotationAndScale_0_0 =
    //      StaticTypeName.typeNameFromConcreteInstance(asiftExperiment("", 0, 0))
    implicit val typeNameTODO_pilgrim_rotationAndScale_2 =
      StaticTypeName.typeNameFromConcreteInstance(briskExperiment("", 0, 0))
    implicit val typeNameTODO_pilgrim_rotationAndScale_3 =
      StaticTypeName.typeNameFromConcreteInstance(nccLogPolarExperiment("", 0, 0))
    implicit val typeNameTODO_pilgrim_rotationAndScale_4 =
      StaticTypeName.typeNameFromConcreteInstance(sidExperiment("", 0, 0))
    implicit val typeNameTODO_pilgrim_rotationAndScale_5 =
      StaticTypeName.typeNameFromConcreteInstance(lucidExperiment("", 0, 0))

    implicit val typeNameTODO_pilgrim_rotationAndScale_6 =
      StaticTypeName.typeNameFromConcreteInstance(siftSExperimentScale("", 0, 0))
    //    implicit val typeNameTODO_pilgrim_rotationAndScale_7 =
    //      StaticTypeName.typeNameFromConcreteInstance(lucidSExperimentScale("", 0, 0))
    implicit val typeNameTODO_pilgrim_rotationAndScale_7_1 =
      StaticTypeName.typeNameFromConcreteInstance(lucidSExperimentRotation("", 0, 0))

    implicit val typeNameTODO_pilgrim_rotationAndScale_8 =
      StaticTypeName.typeNameFromConcreteInstance(orbExperiment("", 0, 0))

    def experimentToSource[E <% RuntimeConfig => ExperimentRunner[R]: JsonFormat: TypeName, R <% RuntimeConfig => ExperimentSummary](
      experiment: E)(implicit runtimeConfig: RuntimeConfig): ScalaSource[Double] = {
      val source = s"""
        loadOpenCV  
    
        implicit val runtimeConfig = ${getSource(runtimeConfig)} 
        
        val experiment = ${getSource(experiment)}
        val results = experiment.run
        val summary: ExperimentSummary = results
        
        asserty(summary.summaryNumbers.size == 1)
        summary.summaryNumbers("recognitionRate")
        """.addImports

      ScalaSource[Double](source)
    }

    val imageClasses = Seq(
      //      "graffiti",
      //      "trees",
      //      "jpeg",
            "boat",
            "bark"
      //      "bikes",
      //            "light",
//      "wall"
        )

    //    val imageClasses = Seq(
    //      "boat")

    //        val scales = 0.4 to 3.0 by 0.1
    // Another Scala bug workaround
    val scales = (0.3 to 3.0 by 0.2).toList.toIndexedSeq
    val scaleSearch = scales map { scale => (scale, 0.0) }
    val angles = (0.0 to 2 * math.Pi by (2.001 * math.Pi / 16)).toList.toIndexedSeq
    val angleSearch = angles map { angle => (1.0, angle) }

    val sources = for (
      imageClass <- imageClasses;
      (scaleFactor, angle) <- scaleSearch ++ angleSearch
    ) yield {
      Seq(
        (experimentToSource(briskExperiment(imageClass, scaleFactor, angle)),
          imageClass,
          scaleFactor,
          angle,
          "BRISK"),
        (experimentToSource(siftExperiment(imageClass, scaleFactor, angle)),
          imageClass,
          scaleFactor,
          angle,
          "SIFT"),
        (experimentToSource(nccLogPolarExperiment(imageClass, scaleFactor, angle)),
          imageClass, scaleFactor, angle, "NCCLP"),
        (experimentToSource(lucidSExperimentScale(imageClass, scaleFactor, angle)),
          imageClass, scaleFactor, angle, "LUCID-S-Scale"),
        (experimentToSource(lucidSExperimentRotation(imageClass, scaleFactor, angle)),
          imageClass, scaleFactor, angle, "LUCID-S-Rotation"),
//        (experimentToSource(siftSExperimentScale(imageClass, scaleFactor, angle)),
//          imageClass, scaleFactor, angle, "SIFT-S-Scale"),
//        (experimentToSource(siftSExperimentRotation(imageClass, scaleFactor, angle)),
//          imageClass, scaleFactor, angle, "SIFT-S-Rotation"),
        (experimentToSource(lucidExperiment(imageClass, scaleFactor, angle)),
          imageClass, scaleFactor, angle, "LUCID"),
        (experimentToSource(sidExperiment(imageClass, scaleFactor, angle)),
          imageClass, scaleFactor, angle, "SID")
          )
    }

    val sourcesFlat = sources.transpose.flatten

    val sparkResults = Util.sparkShuffleMap(sparkContext, sourcesFlat.toIndexedSeq) { source =>
      (source._1.eval, source._2, source._3, source._4, source._5)
    }

    def distinctBy[A, B](seq: Seq[A], f: A => B): Seq[A] = {
      seq.groupBy(f).map(_._2.head).toSeq
    }

    def getCurves(method: String, isScale: Boolean): Seq[Seq[(Double, Double)]] = {
      for (imageClass <- imageClasses) yield {
        val hitsUnclean = sparkResults.filter(_._2 == imageClass).filter(_._5 == method)
        // There's a nondeterminism bug in OpenCV's SIFT that this addresses.
        val hits = hitsUnclean map {
          case hit @ (score, imageClass, scaleFactor, angle, label) =>
            if ((scaleFactor - 1).abs < 0.001 && angle.abs < 0.001) {
              (1.0, imageClass, 1.0, 0.0, label)
            } else hit
        }
        if (isScale) {
          val curve = {
            val dups = hits.filter(_._4 == 0).filter(h => scales.contains(h._3)).sortBy(_._3).map { x =>
              (x._3, x._1)
            }

            distinctBy(dups, { a: (Double, Double) => a._1 }).sortBy(_._1)
          }
          asserty(curve.size == scales.size)
          asserty(curve.map(_._1) == scales)
          curve
        } else {
          val curve = {
            val dups = hits.filter(_._3 == 1).filter(h => angles.contains(h._4)).sortBy(_._4) map { x =>
              (x._4, x._1)
            }

            distinctBy(dups, { a: (Double, Double) => a._1 }).sortBy(_._1)
          }
          asserty(curve.size == angles.size)
          asserty(curve.map(_._1) == angles)
          curve
        }
      }
    }

    def averageCurves(
      curves: Seq[Seq[(Double, Double)]]): Seq[(Double, Double)] = {
      val grouped: Map[Double, Seq[Double]] = curves.flatten.groupBy(_._1) mapValues { seq =>
        seq map {
          case (x, y) => y
        }
      }
      val averagedUnsorted = grouped.toSeq map {
        case (x, ys) => (x, MathUtil.mean(ys))
      }

      val averaged = averagedUnsorted.sortBy(_._1)

      asserty(averaged.size == curves.head.size)
      asserty(averaged.map(_._1) == curves.head.map(_._1))
      averaged
      //      grouped map { 
      //        case (x, )
      //      }
    }

    def getPlot(
      method: String,
      isScale: Boolean): String = {
      val curve = {
        val method2 = if (method.contains("-S")) {
          if (isScale) method + "-Scale"
          else method + "-Rotation"
        } else method

        val curves = getCurves(method2, isScale)

        averageCurves(curves)
      }
      val color = MTCUtil.methodToColor(method)

      def helper(curve: Seq[(Double, Double)], doLabel: Boolean): String = {
        val (xs, ys) = curve.unzip
        s"""
ax.plot(
  ${xs.mkString("[", ", ", "]")},
  ${ys.mkString("[", ", ", "]")},
  ${if (doLabel) "label = '" + method + "'," else ""}
  antialiased=True,
  linewidth=8.0,
  c = '${color}',
  alpha=0.5)
        """
      }

      helper(curve, true)

      //      val head +: tail = curves
      //      
      //      val plots = getPlot(head, true) +: (tail map (t => getPlot(t, false)))
      //
      //      plots mkString ("\n\n")
    }

    //    def plotSeveral(
    //      imageClass: String,
    //      isScale: Boolean): String = {
    //      s"""
    //${getPlots(imageClass, "lucid", "LUCID")}        
    //${getPlots(imageClass, "brisk", "BRISK")}      
    //${getPlots(imageClass, "sift", "SIFT")}
    //${getPlots(imageClass, "asift", "ASIFT")}
    //${getPlots(imageClass, "sid", "SID")}
    //${getPlots(imageClass, "nccLogPolar", "NCCLP")}
    //          
    //ax.set_xlim(0, 1)
    //ax.set_ylim(0, 1)
    //  
    //${if (printRecall) "ax.set_xlabel('Recall')" else ""}
    //${if (printPrecision) "ax.set_ylabel('Precision')" else ""}
    //ax.set_title('${imageClass}')
    //  
    //#ax.legend(loc='lower left', prop{'size':1})
    //
    //ax.grid(True)      
    //      """
    //    }    

        def scalePlots = s"""
    ${getPlot("LUCID", true)}
    ${getPlot("BRISK", true)}
    ${getPlot("SIFT", true)}
    ${getPlot("SID", true)}
    ${getPlot("LUCID-S", true)}
    ${getPlot("NCCLP", true)}
    """
    
        def rotationPlots = s"""
    ${getPlot("LUCID", false)}
    ${getPlot("BRISK", false)}
    ${getPlot("SIFT", false)}
    ${getPlot("SID", false)}
    ${getPlot("LUCID-S", false)}
    ${getPlot("NCCLP", false)}
    """      

//    def scalePlots = s"""
//${getPlot("LUCID-S", true)}    
//"""
//
//    def rotationPlots = s"""
//
//"""

    val plot = new SPyPlot {
      override def source = s"""     
fig = figure(figsize=(16, 4))
        
########################      
     
ax = fig.add_subplot(1,2,1)
$scalePlots

ax.set_xlim(${scales.min}, ${scales.max})
ax.set_ylim(0.5, 1)

ax.set_xlabel("Scale factor")
ax.set_ylabel("Recognition rate")
ax.grid(True)

#############

ax = fig.add_subplot(1,2,2)  
$rotationPlots

ax.set_xlim(${angles.min}, ${angles.max})
ax.set_ylim(0.5, 1)

ax.set_xlabel("Rotation angle in radians")
ax.grid(True)

ax.legend(loc='lower right')

tight_layout()   

savefig("/u/echristiansen/Dropbox/transfer/scaleAndRotationPlot.pdf", bbox_inches='tight')
"""
    } toImage

    //    sparkResults foreach println
    //    println(sparkResults.size)

    //    def getPlot(curve: Seq[(Double, Double)], label: String): String = {
    //      val color = MTCUtil.methodToColor(label)
    //
    //      val (xs, ys) = curve.unzip
    //      s"""
    //    ax.plot(
    //      ${xs.mkString("[", ", ", "]")},
    //      ${ys.mkString("[", ", ", "]")},
    //      label = '${label}',
    //      antialiased=True,
    //      linewidth=4.0,
    //      c = '${color}',
    //      alpha=0.5)
    //            """
    //    }
    //
    //    val scalePlot = {
    //      def plotFromMethod(
    //        experiment: (Double, Double) => Double,
    //        label: String): String = {
    //        val color = MTCUtil.methodToColor(label)
    //
    //        val curve = for (scaleFactor <- (0.4 to 3.0 by 0.1).par) yield {
    //          val score: Double = experiment(scaleFactor, 0)
    //          val cleanedScore = if (!(score > 0)) 0 else score
    //          (scaleFactor, cleanedScore)
    //        }
    //        getPlot(curve.toIndexedSeq, label)
    //      }
    //
    //      val plot = new SPyPlot {
    //        override def source = s"""     
    //    fig = figure(figsize=(10, 10))
    //    ax = fig.add_subplot(1, 1, 1)
    //                
    //    ########################      
    //             
    //    ${plotFromMethod(siftExperiment, "SIFT")}
    //    ${plotFromMethod(nccLogPolarExperiment, "NCCLP")}
    //    ${plotFromMethod(lucidExperiment, "LUCID")}
    //    ${plotFromMethod(briskExperiment, "BRISK")}
    //        
    //    ax.legend(loc='lower right')
    //    ax.grid(True)
    //    ax.set_xlabel("Scale factor")
    //    ax.set_ylabel("Recognition rate")
    //        
    //        
    //    tight_layout()   
    //        
    //    savefig("/u/echristiansen/Dropbox/transfer/scaleChange.pdf", bbox_inches='tight')
    //    savefig("${plotFile}", bbox_inches='tight')
    //        """
    //      } toImage
    //
    //      println(Util.summaryDirectory + "scaleChange.png")
    //      ImageIO.write(plot, "png", Util.summaryDirectory + "scaleChange.png")
    //    }

    //    val rotationPlot = {
    //      def plotFromMethod(
    //        experiment: (Double, Double) => Double,
    //        label: String): String = {
    //        val color = MTCUtil.methodToColor(label)
    //        
    //        val curve = for (angle <- (0.0 to 2 * math.Pi by 0.1).par) yield {
    //          val score: Double = experiment(1, angle)
    //          val cleanedScore = if (score == Double.NaN) 0 else score
    //          (angle, cleanedScore)
    //        }
    //        getPlot(curve.toIndexedSeq, label)
    //      }
    //
    //      val plot = new SPyPlot {
    //        override def source = s"""     
    //fig = figure(figsize=(10, 10))
    //ax = fig.add_subplot(1, 1, 1)
    //        
    //########################      
    //     
    //${plotFromMethod(siftExperiment, "SIFT")}
    //${plotFromMethod(nccLogPolarExperiment, "NCCLP")}
    //${plotFromMethod(lucidExperiment, "LUCID")}
    //${plotFromMethod(briskExperiment, "BRISK")}
    //${plotFromMethod(orbExperiment, "ORB")}
    //
    //ax.legend(loc='lower right')
    //ax.grid(True)
    //ax.set_xlabel("Rotation angle")
    //ax.set_ylabel("Recognition rate")
    //
    //
    //tight_layout()   
    //
    //savefig("/u/echristiansen/Dropbox/transfer/angleChange.pdf", bbox_inches='tight')
    //savefig("${plotFile}", bbox_inches='tight')
    //"""
    //      } toImage
    //
    //      println(Util.summaryDirectory + "angleChange.png")
    //      ImageIO.write(plot, "png", Util.summaryDirectory + "angleChange.png")
    //    }
  }
}