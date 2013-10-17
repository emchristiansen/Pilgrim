package pilgrim

import org.rogach.scallop.ScallopConf
import st.sparse.sundry.ExistingFile
import org.rogach.scallop.Subcommand

class Conf(args: Seq[String]) extends ScallopConf(args) {
  banner("Pilgrim: A command line tool for running local feature experiments.")

  val runtimeConfig = opt[String](
    descr = "Fully-qualified class name of RuntimeConfigWrapper, with optional arguments",
    required = true)

  val sparkContext = opt[String](
    descr = "Fully-qualified class name of SparkContextWrapper, with optional arguments",
    required = false,
    default = Some("sparkContexts.LocalSingleThread"))

  //  val runtimeConfigFile = opt[String](
  //    descr = "File with runtime environment information.",
  //    required = true,
  //    default = None) map ExistingFile.apply
  //
  //  val sparkContextFile = opt[String](
  //    descr = "Optional file with Spark cluster computing information. If not defined, computation is local.",
  //    required = false,
  //    default = None) map ExistingFile.apply

  val task = opt[String](
    descr = "Fully-qualified class name of Task to run, with optional arguments.",
    required = true)
}

object Pilgrim {
  def main(unparsedArgs: Array[String]) {
    val args = new Conf(unparsedArgs)
    println(args.summary)

    def splitString(string: String) =
      string.split(" ").map(_.trim).filterNot(_.isEmpty).toList

    val runtimeConfigClassName :: runtimeConfigOptions =
      splitString(args.runtimeConfig())
    val runtimeConfigWrapper =
      Class.forName(runtimeConfigClassName).newInstance.
        asInstanceOf[RuntimeConfigWrapper]
    implicit val runtimeConfig =
      runtimeConfigWrapper.runtimeConfig(runtimeConfigOptions)

    val sparkContextClassName :: sparkContextOptions =
      splitString(args.sparkContext())
    val sparkContextWrapper =
      Class.forName(sparkContextClassName).newInstance.
        asInstanceOf[SparkContextWrapper]
    implicit val sparkContext =
      sparkContextWrapper.sparkContext(sparkContextOptions)

    val taskClassName :: taskOptions = splitString(args.task())
    val taskMainClass =
      Class.forName(taskClassName).newInstance.asInstanceOf[Task]
    taskMainClass.run(taskOptions)

    //    val foo = Class.forName("Foo").newInstance.asInstanceOf[{ def hello(name: String): String }]
  }
}