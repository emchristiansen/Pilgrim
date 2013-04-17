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

object PrecisionRecall {
  def run(
    sparkContext: SparkContext)(
      implicit runtimeConfig: RuntimeConfig,
      imports: Imports) = {
    loadOpenCV

    val briskExperiment = (imageClass: String, otherImage: Int) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.BRISK, 5000),
        100)
      val extractor = OpenCVExtractor.BRISK
      val matcher = VectorMatcher.L0
      WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
    }

    val siftExperiment = (imageClass: String, otherImage: Int) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 5000),
        100)
      val extractor = OpenCVExtractor.SIFT
      val matcher = VectorMatcher.L2
      WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
    }

    val asiftExperiment = (imageClass: String, otherImage: Int) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 5000),
        100)
      val extractor = OpenCVASIFT
      val matcher = OpenCVASIFT
      WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
    }

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

    implicit val typeNameTODO_pilgrim_oxford_0 =
      StaticTypeName.typeNameFromConcreteInstance(siftExperiment("", 0))
    implicit val typeNameTODO_pilgrim_oxford_0_0 =
      StaticTypeName.typeNameFromConcreteInstance(asiftExperiment("", 0))
    implicit val typeNameTODO_pilgrim_oxford_2 =
      StaticTypeName.typeNameFromConcreteInstance(briskExperiment("", 0))
    implicit val typeNameTODO_pilgrim_oxford_3 =
      StaticTypeName.typeNameFromConcreteInstance(nccLogPolarExperiment("", 0))

    def experimentToSource[E <% RuntimeConfig => ExperimentRunner[R]: JsonFormat: TypeName, R <% RuntimeConfig => ExperimentSummary](
      experiment: E)(implicit runtimeConfig: RuntimeConfig): ScalaSource[Seq[(Double, Double)]] = {
      val source = s"""
    loadOpenCV  

    implicit val runtimeConfig = ${getSource(runtimeConfig)} 
    
    val experiment = ${getSource(experiment)}
    val results = experiment.run
    val summary: ExperimentSummary = results
    
    asserty(summary.summaryCurves.size == 1)
    summary.summaryCurves.values.head
    """.addImports

      ScalaSource[Seq[(Double, Double)]](source)
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

    val otherImages = Seq(2, 3, 4, 5, 6)

    val sources = for (
      imageClass <- imageClasses;
      otherImage <- otherImages
    ) yield {
      Seq(
        (experimentToSource(briskExperiment(imageClass, otherImage)), imageClass, otherImage, "brisk"),
        (experimentToSource(siftExperiment(imageClass, otherImage)), imageClass, otherImage, "sift"),
        (experimentToSource(asiftExperiment(imageClass, otherImage)), imageClass, otherImage, "asift"),
        (experimentToSource(nccLogPolarExperiment(imageClass, otherImage)), imageClass, otherImage, "nccLogPolar"))
    }

    val sourcesFlat = sources.transpose.flatten

    val plots = Util.sparkShuffleMap(sparkContext, sourcesFlat.toIndexedSeq) { source =>
      (source._1.eval, source._2, source._3, source._4)
    }

    println(plots.size)

    def getCurves(imageClass: String, method: String): Seq[Seq[(Double, Double)]] = {
      val hits = plots.filter(_._2 == imageClass).filter(_._4 == method)
      asserty(hits.size == 5)
      hits.sortBy(_._3).map(_._1)
    }

    def getPlots(
      imageClass: String,
      method: String,
      color: String,
      label: String): String = {
      val curves = getCurves(imageClass, method)

      def getPlot(curve: Seq[(Double, Double)], doLabel: Boolean): String = {
        val (xs, ys) = curve.unzip
        s"""
ax.plot(
  ${xs.mkString("[", ", ", "]")},
  ${ys.mkString("[", ", ", "]")},
  ${if (doLabel) "label = '" + label + "'," else ""}
  antialiased=True,
  linewidth=4.0,
  c = '${color}',
  alpha=0.5)
        """
      }

      val head +: tail = curves

      val plots = getPlot(head, true) +: (tail map (t => getPlot(t, false)))

      plots mkString ("\n\n")
    }

    def plotSeveral(
      imageClass: String,
      printRecall: Boolean,
      printPrecision: Boolean): String = {
      s"""
${getPlots(imageClass, "brisk", "c", "BRISK")}      
${getPlots(imageClass, "sift", "r", "SIFT")}
${getPlots(imageClass, "asift", "b", "ASIFT")}
${getPlots(imageClass, "nccLogPolar", "g", "NLPOLAR")}
          
ax.set_xlim(0, 1)
ax.set_ylim(0, 1)
  
${if (printRecall) "ax.set_xlabel('Recall')" else ""}
${if (printPrecision) "ax.set_ylabel('Precision')" else ""}
ax.set_title('${imageClass}')
  
#ax.legend(loc='lower left', prop{'size':1})

ax.grid(True)      
      """
    }

    val plot = new SPyPlot {
      override def source = s"""     
fig = figure(figsize=(10, 10))
        
########################      
     
ax = fig.add_subplot(4,2,1)      
${plotSeveral("bikes", false, true)}
ax.legend(loc='lower left')
      
ax = fig.add_subplot(4,2,2)      
${plotSeveral("graffiti", false, false)}

ax = fig.add_subplot(4,2,3)      
${plotSeveral("jpeg", false, true)}

ax = fig.add_subplot(4,2,4)      
${plotSeveral("wall", false, false)}

ax = fig.add_subplot(4,2,5)      
${plotSeveral("light", false, true)}

ax = fig.add_subplot(4,2,6)      
${plotSeveral("boat", false, false)}

ax = fig.add_subplot(4,2,7)      
${plotSeveral("bark", true, true)}

ax = fig.add_subplot(4,2,8)      
${plotSeveral("trees", true, false)}

tight_layout()   

savefig("/u/echristiansen/Dropbox/transfer/prPlot.pdf", bbox_inches='tight')
savefig("${plotFile}", bbox_inches='tight')
"""
    } toImage

    ImageIO.write(plot, "png", Util.summaryDirectory + "parameterSweep.png")
  }
}