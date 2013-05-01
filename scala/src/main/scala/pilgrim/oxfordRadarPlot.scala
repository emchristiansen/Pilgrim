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

object OxfordRadarPlot {
  def run(
    sparkContext: SparkContext)(
      implicit runtimeConfig: RuntimeConfig,
      imports: Imports) = {
    loadOpenCV

    //    val briefExperiment = (imageClass: String, otherImage: Int) => {
    //      val detector = BoundedPairDetector(
    //        BoundedDetector(OpenCVDetector.FAST, 5000),
    //        100)
    //      val extractor = OpenCVExtractor.BRIEF
    //      val matcher = VectorMatcher.L0
    //      WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
    //    }

    val lucidExperiment = (imageClass: String, otherImage: Int) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.FAST, 5000),
        100)
      //      val patchExtractor = PatchExtractor(
      //        false,
      //        false,
      //        24,
      //        5,
      //        "Gray")
      //
      //      patchExtractor: Extractor[IndexedSeq[Int]]
      //      //      patchExtractor.toJson
      //
      //      val extractor = NormalizedExtractor(
      //        patchExtractor,
      //        PatchNormalizer.Rank)
      //
      //      extractor: Extractor[SortDescriptor]
      //      //      extractor.toJson

      val extractor = LUCIDExtractor(
        false,
        false,
        24,
        5,
        "Gray")

      val matcher = VectorMatcher.L1
      //      matcher: Matcher[SortDescriptor]
      WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
    }

    val briskExperiment = (imageClass: String, otherImage: Int) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.BRISK, 5000),
        100)
      val extractor = OpenCVExtractor.BRISK
      val matcher = VectorMatcher.L0
      WideBaselineExperiment(imageClass, otherImage, detector, extractor, matcher)
    }

    val freakExperiment = (imageClass: String, otherImage: Int) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.BRISK, 5000),
        100)
      val extractor = OpenCVExtractor.FREAK
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

    val sidExperiment = (imageClass: String, otherImage: Int) => {
      val detector = BoundedPairDetector(
        BoundedDetector(OpenCVDetector.SIFT, 5000),
        100)
      val extractor = SIDExtractor
      val matcher = VectorMatcher.L2
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
    implicit val typeNameTODO_pilgrim_oxford_1 =
      StaticTypeName.typeNameFromConcreteInstance(lucidExperiment("", 0))
    implicit val typeNameTODO_pilgrim_oxford_2 =
      StaticTypeName.typeNameFromConcreteInstance(briskExperiment("", 0))
    implicit val typeNameTODO_pilgrim_oxford_3 =
      StaticTypeName.typeNameFromConcreteInstance(nccLogPolarExperiment("", 0))
    implicit val typeNameTODO_pilgrim_oxford_4 =
      StaticTypeName.typeNameFromConcreteInstance(freakExperiment("", 0))
    implicit val typeNameTODO_pilgrim_oxford_5 =
      StaticTypeName.typeNameFromConcreteInstance(sidExperiment("", 0))

    //    // TODO: Delete
    //    val lE = lucidExperiment("", 0)
    //    lE.detector.toJson
    //    lE.extractor.extractor.toJson
    //    lE.extractor.normalizer.toJson
    //    val foo = normalizedExtractorJsonProtocol[]
    //    normalizedExtractorJsonProtocol(lE.extractor)
    //    
    //    lE.extractor.toJson
    //    lE.matcher.toJson

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
        (experimentToSource(lucidExperiment(imageClass, otherImage)), imageClass, otherImage, "lucid"),
        (experimentToSource(briskExperiment(imageClass, otherImage)), imageClass, otherImage, "brisk"),
        //        (experimentToSource(freakExperiment(imageClass, otherImage)), imageClass, otherImage, "freak"),
        (experimentToSource(siftExperiment(imageClass, otherImage)), imageClass, otherImage, "sift"),
        (experimentToSource(asiftExperiment(imageClass, otherImage)), imageClass, otherImage, "asift"),
        (experimentToSource(sidExperiment(imageClass, otherImage)), imageClass, otherImage, "sid"),
        (experimentToSource(nccLogPolarExperiment(imageClass, otherImage)), imageClass, otherImage, "nccLogPolar"))
    }

    val sourcesFlat = sources.transpose.flatten

    val scores = Util.sparkShuffleMap(sparkContext, sourcesFlat.toIndexedSeq) { source =>
      (source._1.eval, source._2, source._3, source._4)
    }

    println(scores)
    println(scores.size)

    def getCurve(imageClass: String, method: String) = {
      val hits = scores.filter(_._2 == imageClass).filter(_._4 == method)
      asserty(hits.size == 5)
      hits.sortBy(_._3).map(_._1)
    }

    val plot = new SPyPlot {
      override def source = s"""     
import numpy as np

import matplotlib.pyplot as plt
from matplotlib.path import Path
from matplotlib.spines import Spine
from matplotlib.projections.polar import PolarAxes
from matplotlib.projections import register_projection


def radar_factory(num_vars, frame='circle'):

    # calculate evenly-spaced axis angles
    theta = 2*np.pi * np.linspace(0, 1-1./num_vars, num_vars)
    # rotate theta such that the first axis is at the top
    theta += np.pi/2

    def draw_poly_patch(self):
        verts = unit_poly_verts(theta)
        return plt.Polygon(verts, closed=True, edgecolor='k')

    def draw_circle_patch(self):
        # unit circle centered on (0.5, 0.5)
        return plt.Circle((0.5, 0.5), 0.5)

    patch_dict = {'polygon': draw_poly_patch, 'circle': draw_circle_patch}
    if frame not in patch_dict:
        raise ValueError('unknown value for `frame`: %s' % frame)

    class RadarAxes(PolarAxes):

        name = 'radar'
        # use 1 line segment to connect specified points
        RESOLUTION = 1
        # define draw_frame method
        draw_patch = patch_dict[frame]

        def fill(self, *args, **kwargs):
            closed = kwargs.pop('closed', True)
            return super(RadarAxes, self).fill(closed=closed, *args, **kwargs)

        def plot(self, *args, **kwargs):
            lines = super(RadarAxes, self).plot(*args, **kwargs)
            for line in lines:
                self._close_line(line)

        def _close_line(self, line):
            x, y = line.get_data()
            # FIXME: markers at x[0], y[0] get doubled-up
            if x[0] != x[-1]:
                x = np.concatenate((x, [x[0]]))
                y = np.concatenate((y, [y[0]]))
                line.set_data(x, y)

        def set_varlabels(self, labels):
            self.set_thetagrids(theta * 180/np.pi, labels)

        def _gen_axes_patch(self):
            return self.draw_patch()

        def _gen_axes_spines(self):
            if frame == 'circle':
                return PolarAxes._gen_axes_spines(self)
            # The following is a hack to get the spines (i.e. the axes frame)
            # to draw correctly for a polygon frame.

            # spine_type must be 'left', 'right', 'top', 'bottom', or `circle`.
            spine_type = 'circle'
            verts = unit_poly_verts(theta)
            # close off polygon by repeating first vertex
            verts.append(verts[0])
            path = Path(verts)

            spine = Spine(self, spine_type, path)
            spine.set_transform(self.transAxes)
            return {'polar': spine}

    register_projection(RadarAxes)
    return theta


def unit_poly_verts(theta):
    x0, y0, r = [0.5] * 3
    verts = [(r*np.cos(t) + x0, r*np.sin(t) + y0) for t in theta]
    return verts


def example_data():
    #The following data is from the Denver Aerosol Sources and Health study.
    #See  doi:10.1016/j.atmosenv.2008.12.017
    #
    #The data are pollution source profile estimates for five modeled pollution
    #sources (e.g., cars, wood-burning, etc) that emit 7-9 chemical species.
    #The radar charts are experimented with here to see if we can nicely
    #visualize how the modeled source profiles change across four scenarios:
    #  1) No gas-phase species present, just seven particulate counts on
    #     Sulfate
    #     Nitrate
    #     Elemental Carbon (EC)
    #     Organic Carbon fraction 1 (OC)
    #     Organic Carbon fraction 2 (OC2)
    #     Organic Carbon fraction 3 (OC3)
    #     Pyrolized Organic Carbon (OP)
    #  2)Inclusion of gas-phase specie carbon monoxide (CO)
    #  3)Inclusion of gas-phase specie ozone (O3).
    #  4)Inclusion of both gas-phase speciesis present...
    data = {
        'column names':
            ['1:2', '1:3', '1:4', '1:5', '1:6'],
        'graffiti':
            [[${getCurve("graffiti", "lucid").mkString(", ")}],
             [${getCurve("graffiti", "brisk").mkString(", ")}],
             [${getCurve("graffiti", "sift").mkString(", ")}],
             [${getCurve("graffiti", "asift").mkString(", ")}],
             [${getCurve("graffiti", "sid").mkString(", ")}],
             [${getCurve("graffiti", "nccLogPolar").mkString(", ")}]],
        'trees':
            [[${getCurve("trees", "lucid").mkString(", ")}],
             [${getCurve("trees", "brisk").mkString(", ")}],
             [${getCurve("trees", "sift").mkString(", ")}],
             [${getCurve("trees", "asift").mkString(", ")}],
             [${getCurve("trees", "sid").mkString(", ")}],
             [${getCurve("trees", "nccLogPolar").mkString(", ")}]],
        'jpeg':
            [[${getCurve("jpeg", "lucid").mkString(", ")}],
             [${getCurve("jpeg", "brisk").mkString(", ")}],
             [${getCurve("jpeg", "sift").mkString(", ")}],
             [${getCurve("jpeg", "asift").mkString(", ")}],
             [${getCurve("jpeg", "sid").mkString(", ")}],
             [${getCurve("jpeg", "nccLogPolar").mkString(", ")}]],
        'boat':
            [[${getCurve("boat", "lucid").mkString(", ")}],
             [${getCurve("boat", "brisk").mkString(", ")}],
             [${getCurve("boat", "sift").mkString(", ")}],
             [${getCurve("boat", "asift").mkString(", ")}],
             [${getCurve("boat", "sid").mkString(", ")}],
             [${getCurve("boat", "nccLogPolar").mkString(", ")}]],
        'bark':
            [[${getCurve("bark", "lucid").mkString(", ")}],
             [${getCurve("bark", "brisk").mkString(", ")}],
             [${getCurve("bark", "sift").mkString(", ")}],
             [${getCurve("bark", "asift").mkString(", ")}],
             [${getCurve("bark", "sid").mkString(", ")}],
             [${getCurve("bark", "nccLogPolar").mkString(", ")}]],
        'bikes':
            [[${getCurve("bikes", "lucid").mkString(", ")}],
             [${getCurve("bikes", "brisk").mkString(", ")}],
             [${getCurve("bikes", "sift").mkString(", ")}],
             [${getCurve("bikes", "asift").mkString(", ")}],
             [${getCurve("bikes", "sid").mkString(", ")}],
             [${getCurve("bikes", "nccLogPolar").mkString(", ")}]],
        'light':
            [[${getCurve("light", "lucid").mkString(", ")}],
             [${getCurve("light", "brisk").mkString(", ")}],
             [${getCurve("light", "sift").mkString(", ")}],
             [${getCurve("light", "asift").mkString(", ")}],
             [${getCurve("light", "sid").mkString(", ")}],
             [${getCurve("light", "nccLogPolar").mkString(", ")}]],
        'wall':
            [[${getCurve("wall", "lucid").mkString(", ")}],
             [${getCurve("wall", "brisk").mkString(", ")}],
             [${getCurve("wall", "sift").mkString(", ")}],
             [${getCurve("wall", "asift").mkString(", ")}],
             [${getCurve("wall", "sid").mkString(", ")}],
             [${getCurve("wall", "nccLogPolar").mkString(", ")}]]}
    return data


N = 5
theta = radar_factory(N, frame='polygon')

data = example_data()
spoke_labels = data.pop('column names')

fig = plt.figure(figsize=(18, 9))
#fig.subplots_adjust(wspace=0.25, hspace=0.20, top=0.85, bottom=0.05)

colors = [
  '${MTCUtil.methodToColor("LUCID")}', 
  '${MTCUtil.methodToColor("BRISK")}', 
  '${MTCUtil.methodToColor("SIFT")}', 
  '${MTCUtil.methodToColor("ASIFT")}',
  '${MTCUtil.methodToColor("SID")}',
  '${MTCUtil.methodToColor("NCCLP")}']
# Plot the four cases from the example data on separate axes
for n, title in enumerate(data.keys()):
    ax = fig.add_subplot(2, 4, n+1, projection='radar')
    plt.rgrids([0.2, 0.4, 0.6, 0.8])
    plt.ylim(0, 1)
    ax.set_title(title, weight='bold', size='medium', position=(0.5, 1.1),
                 horizontalalignment='center', verticalalignment='center')
    for d, color in zip(data[title], colors):
        ax.plot(theta, d, color=color)
        ax.fill(theta, d, facecolor=color, alpha=0.10)
    ax.set_varlabels(spoke_labels)

    # add legend relative to top-left plot
plt.subplot(2, 4, 8)
labels = ('LUCID', 'BRISK', 'SIFT', 'ASIFT', 'SID', 'NCCLP')
legend = plt.legend(labels, loc=(-.22, 1 - .95), labelspacing=0.1)
plt.setp(legend.get_texts(), fontsize='small')

#plt.figtext(0.5, 0.965, '5-Factor Solution Profiles Across Four Scenarios',
#                ha='center', color='black', weight='bold', size='large')

tight_layout()    
savefig("/u/echristiansen/Dropbox/transfer/OxfordRadar.pdf", bbox_inches='tight')
  
savefig("${plotFile}", bbox_inches='tight')
"""
    } toImage

    println(s"Saving to ${Util.summaryDirectory + "oxfordRadar.png"}")
    ImageIO.write(plot, "png", Util.summaryDirectory + "oxfordRadar.png")
  }
}