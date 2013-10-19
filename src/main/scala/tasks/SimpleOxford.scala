package tasks

import pilgrim._
import st.sparse.billy.experiments.RuntimeConfig
import st.sparse.billy.experiments.wideBaseline.Experiment
import st.sparse.billy.experiments.wideBaseline.Oxford
import st.sparse.billy.extractors.OpenCVExtractor
import st.sparse.billy.detectors.OpenCVDetector
import st.sparse.billy.matchers.VectorMatcher
import scala.pickling._
import scala.pickling.binary._
import st.sparse.billy.detectors.BoundedDetector
import shapeless._
import shapeless.poly._
import scala.reflect.runtime._

class SimpleOxford extends Task {
  override def run(unparsedArgs: Seq[String])(
    implicit runtimeConfig: RuntimeConfig) {
    require(unparsedArgs.isEmpty)

    val cm = universe.runtimeMirror(getClass.getClassLoader)
    import scala.tools.reflect.ToolBox
    val tb = cm.mkToolBox()

    def typeName[A: FastTypeTag]: String = 
      implicitly[FastTypeTag[A]].tpe.toString
      
    def tn[A: FastTypeTag] = typeName[A]
    
    def constructorName[A: FastTypeTag]: String =
      typeName[A].takeWhile(_ != '[')
      
    def cn[A: FastTypeTag] = constructorName
    
    def objectName[A: FastTypeTag]: String =
      typeName[A].replace(".type", "")
      
    def on[A: FastTypeTag]: String = objectName[A]
    
    def evalExperiments(
      imageClasses: Seq[String],
      otherImages: Seq[Int],
      detectorStrings: Seq[String],
      extractorStrings: Seq[String],
      matcherStrings: Seq[String]): Seq[Experiment] =
      for (
        imageClass <- imageClasses;
        otherImage <- otherImages;
        detectorString <- detectorStrings;
        extractorString <- extractorStrings;
        matcherString <- matcherStrings
      ) yield {
        val code = s"""        
${cn[Oxford[_, _, _, _]]}(
  "$imageClass", 
  $otherImage, 
  $detectorString, 
  $extractorString, 
  $matcherString): ${cn[Experiment]}    	
"""
        println(code)

        tb.eval(tb.parse(code)).asInstanceOf[Experiment]
      }

    val experiments = evalExperiments(
      Seq("boat"),
      2 to 6,
      Seq(s"${cn[BoundedDetector[_]]}(${on[OpenCVDetector.FAST.type]}, 100)"),
      Seq(on[OpenCVExtractor.SIFT.type], on[OpenCVExtractor.SURF.type]),
      Seq(on[VectorMatcher.L1.type], on[VectorMatcher.L2.type]))

    val results = experiments.par.map(_.run).toIndexedSeq
    results foreach (println)

    println("In Oxford")
  }
}