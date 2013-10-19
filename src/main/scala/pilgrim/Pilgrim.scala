package pilgrim

import org.rogach.scallop.ScallopConf
import st.sparse.sundry.ExistingFile
import org.rogach.scallop.Subcommand
import st.sparse.billy._

class Conf(args: Seq[String]) extends ScallopConf(args) {
  banner("Pilgrim: A command line tool for running local feature experiments.")

  val runtimeConfig = opt[String](
    descr = "Fully-qualified class name of RuntimeConfigWrapper, with optional arguments",
    required = true)

  val task = opt[String](
    descr = "Fully-qualified class name of Task to run, with optional arguments.",
    required = true)
}

object Pilgrim {
  /**
   * This unfortunate bit of magic is a workaround for an
   * apparent concurrency bug in SLF4J.
   */
  def initializeLogger() {
    val classLoader = this.getClass.getClassLoader
    classLoader.loadClass("org.slf4j.LoggerFactory").
      getMethod("getLogger", classLoader.loadClass("java.lang.String")).
      invoke(null, "ROOT")
  }

  def main(unparsedArgs: Array[String]) {
    initializeLogger()
    
    val args = new Conf(unparsedArgs)
    //    println(args.summary)

    def splitString(string: String) =
      string.split(" ").map(_.trim).filterNot(_.isEmpty).toList

    val runtimeConfigClassName :: runtimeConfigOptions =
      splitString(args.runtimeConfig())
    val runtimeConfigWrapper =
      Class.forName(runtimeConfigClassName).newInstance.
        asInstanceOf[RuntimeConfigWrapper]
    implicit val runtimeConfig =
      runtimeConfigWrapper.runtimeConfig(runtimeConfigOptions)

    val taskClassName :: taskOptions = splitString(args.task())
    val taskMainClass =
      Class.forName(taskClassName).newInstance.asInstanceOf[Task]

    loadOpenCV
    taskMainClass.run(taskOptions)
  }
}