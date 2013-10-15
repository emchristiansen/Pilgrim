package pilgrim

import spray.json._
import reflect.runtime.universe._
import nebula._
import spark._
import SparkContext._
import billy._

////////////////////////////

//case class JsonAndTypeTag[A](val json: JsValue, val typeTag: TypeTag[A])
//
//case class ExperimentJsonAndTypeName(val json: JsValue, val typeName: String)
//
//object MyTest {
//  def runExperiment(
//      experiment: ExperimentJsonAndTypeName)(
//          implicit imports: Imports,
//          runtimeConfig: RuntimeConfig): JsValue = {     
//    val source = s"""
//    loadOpenCV  
//    
//    val experiment = \"\"\"${experiment.json}\"\"\".asJson.convertTo[${experiment.typeName}]
//    
//    def addRuntime(runtimeConfig: RuntimeConfig): JsValue = {
//      implicit val rC = runtimeConfig
//      val results = experiment.run
//      results.toJson
//    }
//    
//    addRuntime _
//    """
//    
//    val needsRuntime = eval[RuntimeConfig => JsValue](source.addImports(imports))
//    needsRuntime(runtimeConfig)
//  }
//}

