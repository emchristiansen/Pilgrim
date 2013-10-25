package tasks

import pilgrim._
import st.sparse.billy._
import st.sparse.billy.experiments.RuntimeConfig
import st.sparse.billy.experiments.wideBaseline._
import st.sparse.billy.extractors.OpenCVExtractor
import st.sparse.billy.detectors._
import st.sparse.billy.matchers.VectorMatcher
import scala.pickling._
import scala.pickling.binary._
import st.sparse.billy.detectors.BoundedDetector
import shapeless._
import shapeless.ops.hlist._
import shapeless.poly._
import scala.reflect.runtime._
import st.sparse.sundry._
import org.apache.commons.io.FileUtils
import st.sparse.billy.experiments.wideBaseline.Table
import st.sparse.billy.experiments.wideBaseline.Results
import java.io.File
import scala.util.Try
import com.typesafe.scalalogging.slf4j.Logging
import breeze.linalg._
import org.joda.time._
import st.sparse.persistentmap.CustomPicklers._

//import scala.concurrent.Await
//import scala.concurrent.Future
//import scala.concurrent.duration._
//import scala.concurrent._
//import java.lang.management.ManagementFactory
//import akka.actor._
//import akka._
//import akka.dispatch._

class SimpleOxford extends Task with Logging {
  override def run(unparsedArgs: Seq[String])(
    implicit runtimeConfig: RuntimeConfig) {
    require(unparsedArgs.isEmpty)

    //    val actorSystem = ActorSystem.create()
    //    println(actorSystem.toString)
    //    //    println(actorSystem.settings)
    //
    //    implicit val executionContext = actorSystem.dispatchers.lookup("my-dispatcher")
    //    println(executionContext)
    //
    //    //    val future = Future {
    //    //      "Hello" + "World" + " " + ManagementFactory.getRuntimeMXBean().getName()
    //    //    }
    //    //
    //    //    val futures = 20 times future
    //    //
    //    //    val results = futures.map(Await.result(_, 0 nanos))
    //
    //    //    results foreach (println)

    val exampleTime = new DateTime
    val exampleResults = Results(DenseMatrix.zeros[Double](4, 4))
    val exampleRecording = Set((exampleTime, exampleResults))
    val unpickled = exampleRecording.pickle.unpickle[Set[(DateTime, Results)]]
    println(exampleRecording)
    println(unpickled)
    assert(exampleRecording == unpickled)
    
    val imageClasses = Seq("bikes", "boat")
    val otherImages = 2 to 6
    val detectors = DoublyBoundedPairDetector(2, 100, 500, OpenCVDetector.FAST) ::
      DoublyBoundedPairDetector(2, 100, 500, OpenCVDetector.SIFT) ::
      HNil
    val extractors = OpenCVExtractor.BRISK :: OpenCVExtractor.SIFT :: HNil
    val matchers = VectorMatcher.L0 :: VectorMatcher.L1 :: HNil

    object constructExperiment extends Poly1 {
      implicit def default[D <% PairDetector, E <% Extractor[F], M <% Matcher[F], F](
        implicit ftt: FastTypeTag[Oxford[D, E, M, F]],
        sp: SPickler[Oxford[D, E, M, F]],
        u: Unpickler[Oxford[D, E, M, F]],
        ftt2e: FastTypeTag[FastTypeTag[Oxford[D, E, M, F]]]) =
        at[(D, E, M)] {
          case (detector, extractor, matcher) => {
            for (imageClass <- imageClasses; otherImage <- otherImages) yield {
              val oxford =
                Oxford(imageClass, otherImage, detector, extractor, matcher)
              oxford.pickle.unpickle[Oxford[D, E, M, F]]
//              Experiment.cached(oxford)
              oxford: Experiment
            }
          }
        }
    }

    // This lifting, combined with flatMap, filters out types that can't be used
    // to construct experiments.   
    object constructExperimentLifted extends LiftU(constructExperiment)

    val tuples = HListUtil.cartesian3(
      detectors,
      extractors,
      matchers)

    val experiments = {
      val hList = tuples flatMap constructExperimentLifted
      hList.toList.flatten.toIndexedSeq
    }

    val results = experiments.map { _.run }
    //    val resultsFutures = experiments.map { experiment =>
    //      Future { experiment.run }
    //    }.toIndexedSeq
    //    val results = resultsFutures.map(Await.result(_, 1000 seconds))

    val table = Table(
      experiments zip results,
      (e: Experiment) => e.modelParametersString,
      (e: Experiment) => e.experimentParametersString,
      (r: Results) => r.recognitionRate.toString)

    FileUtils.writeStringToFile(
      new File("/home/eric/Downloads/results1.csv"),
      table.tsv)

    println("In Oxford")

    //    actorSystem.shutdown()
  }
}