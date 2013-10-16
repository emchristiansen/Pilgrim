import org.rogach.scallop.ScallopConf

import st.sparse.sundry.ExistingFile

class Conf(args: Seq[String]) extends ScallopConf(args) {
  banner("Pilgrim: A command line tool for running local feature experiments.")

  val runtimeConfigFile = opt[String](
    descr = "File with runtime environment information.",
    required = true,
    default = None) map ExistingFile.apply
}

object Pilgrim {
  def main(unparsedArgs: Array[String]) {
    val args = new Conf(unparsedArgs)
    println(args.summary)
  }
}