package pilgrim

import nebula._
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
import org.scalatest.junit.JUnitRunner
import nebula.util._
import spray.json._
import com.sun.xml.internal.bind.v2.model.runtime.RuntimeClassInfo
import breeze.linalg._
import breeze.math._
import grizzled.math.stats
import org.scalacheck._
import org.scalatest.prop._
import org.scalatest._
import DenseMatrixUtil._
import org.opencv.features2d.{ DMatch, KeyPoint }
import java.util.Date
import org.opencv.contrib

import reflect.runtime.universe

import skunkworks.OpenCVMTCUtil._
import spark.SparkContext._
import billy.DetectorJsonProtocol._
import billy._
import billy.detectors._
import billy.summary._
import billy.wideBaseline._
import java.io.File
import nebula._
import org.opencv._
import org.opencv.contrib._
import shapeless._
import skunkworks._
import spark._
import spray.json._

import com.google.caliper.Benchmark
import com.google.caliper.SimpleBenchmark
import com.google.caliper.Param

////////////////////////////////////////////////////////////////////////////////

@RunWith(classOf[JUnitRunner])
@WrapWith(classOf[ConfigMapWrapperSuite])
class Test(
  val configMap: Map[String, Any]) extends ConfigMapFunSuite with GeneratorDrivenPropertyChecks with ShouldMatchers {

  class Benchmark extends SimpleBenchmark {
    @Param(Array("10", "100", "1000", "10000"))
    val length: Int = 0
    var array: Array[Int] = _

    override def setUp() {
      array = new Array(length)
    }

    def timeForeach(reps: Int) = {
      var result = 0
      array.foreach {
        result += _
      }
      result
    }
  }
  
  val benchmark = new Benchmark
  
  benchmark.setUp
  benchmark.timeForeach(100)
  
//  Runner.main(classOf[Benchmark], Array[String]())
}