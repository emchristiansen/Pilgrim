package runtimeConfigs

import org.rogach.scallop.ScallopConf
import pilgrim.RuntimeConfigWrapper
import st.sparse.billy.experiments.RuntimeConfig
import st.sparse.persistentmap.ConnectionHelper
import st.sparse.sundry.ExistingDirectory
import st.sparse.billy._
import java.io.File

class TransformSQLite extends RuntimeConfigWrapper {
  class Conf(args: Seq[String]) extends ScallopConf(args) {
    banner("RuntimeConfig for Eric's laptop, using SQLite.")
  }

  override def runtimeConfig(unparsedArgs: Seq[String]) = {
    val database = ConnectionHelper.databaseSQLite(
      new File("/home/eric/t/2013_q4/pilgrimOutput/Pilgrim.sqlite"))

    val matlabLibraryRoot = MatlabLibraryRoot(ExistingDirectory(
      "/home/eric/Dropbox/t/2013_q4/matlabLibraryRoot"))

    RuntimeConfig(
      ExistingDirectory("/home/eric/Bitcasa/data"),
      database,
      ExistingDirectory("/home/eric/t/2013_q4/pilgrimOutput"),
      None,
      Some(matlabLibraryRoot),
      true,
      true)
  }
}