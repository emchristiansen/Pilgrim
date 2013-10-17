package runtimeConfigs

import org.rogach.scallop.ScallopConf

import pilgrim.RuntimeConfigWrapper
import st.sparse.billy.experiments.RuntimeConfig
import st.sparse.persistentmap.ConnectionHelper
import st.sparse.sundry.ExistingDirectory

class TransformMariaDB extends RuntimeConfigWrapper {
  class Conf(args: Seq[String]) extends ScallopConf(args) {
    banner("RuntimeConfig for Eric's laptop, using MariaDB.")

    val mariadbPassword = opt[String](
      descr = "Password for the MariaDB root user.",
      required = true)
  }

  override def runtimeConfig(unparsedArgs: Seq[String]) = {
    val args = new Conf(unparsedArgs)

    val database = ConnectionHelper.databaseMariaDB(
      "localhost",
      "Pilgrim",
      "root",
      args.mariadbPassword())

    RuntimeConfig(
      ExistingDirectory("/home/eric/Bitcasa/data"),
      database,
      ExistingDirectory("/home/eric/t/2013_q4/pilgrimOutput"),
      None,
      true,
      true)
  }
}