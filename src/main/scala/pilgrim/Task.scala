package pilgrim

import st.sparse.billy.experiments.RuntimeConfig

trait Task {
  def run(unparsedArgs: Seq[String])(
    implicit runtimeConfig: RuntimeConfig)
}