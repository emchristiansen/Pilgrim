package pilgrim

import nebula._

import org.rogach.scallop._

/////////////////////////////////////////////////////////////////

class Conf(args: Seq[String]) extends ScallopConf(args) {
  banner("Pilgrim: A command line tool for running local feature experiments.")

  val runtimeConfigFile = opt[String](
    descr = "File with runtime environment information.",
    required = true,
    default = None) map ExistingFile.apply
  
  val experimentConfigFiles = opt[List[String]](
    descr = "Files where each file specifies a table where each entry is an experiment.",
    required = false,
    default = Some(Nil)) map (_ map ExistingFile.apply)
    
//  val imageStream = opt[String](
//    descr = "Scale expression with implicit view to ImageStream. The ImageStream controls which images will be fetched.",
//    default = Some("StreamBing()"))
//
//  val style = opt[String](
//    descr = "Scale expression with implicit view to DisplayStyle. The DisplayStyle controls how sets of images are arranged into a larger image.",
//    default = Some("BlockStyle"))
//
//  val extraSourceFiles = opt[List[String]](
//    descr = "Extra Scala source files to compile against at runtime. Use this to add additional behavior without modifying the core Tangram source. To ease debugging, each file must independently be valid Scala source.",
//    required = false,
//    default = Some(Nil))
//
//  // This indirection is apparently necessary to work around a Scallop bug:
//  // https://github.com/Rogach/scallop/issues/40
//  val extraSourceExistingFiles = extraSourceFiles map (_ map ExistingFile.apply)
//
//  val refreshDelay = opt[Int](
//    descr = "Time in seconds each tangram will remain on the screen.",
//    default = Some(60))
//
//  val archiveDirectory = opt[String](
//    descr = "An optional save directory for downloaded images. This directory must already exist.",
//    required = false)
//
//  // This indirection is apparently necessary to work around a Scallop bug:
//  // https://github.com/Rogach/scallop/issues/40
//  val archiveExistingDirectory =
//    archiveDirectory map ExistingDirectory.apply
}

object Main {
  def main(unparsedArgs: Array[String]) {
    val args = new Conf(unparsedArgs)
    println(args.summary)
  }
}
