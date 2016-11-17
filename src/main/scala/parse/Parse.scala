package dragon.osc.parse

import dragon.osc.parse.syntax._
import dragon.osc.parse.ui._
import dragon.osc.parse.tfm._

object Parse {

    def file(filename: String): Option[Root] =
        Lang.readFile(filename).flatMap(x => Read.root.run(Tfm.tfmLang(x)))    

}
