package pilgrim

import st.sparse.billy.experiments.RuntimeConfig

trait RuntimeConfigWrapper {
  def runtimeConfig(unparsedArgs: Seq[String]): RuntimeConfig
}