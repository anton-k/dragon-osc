package dragon.osc.parse

import dragon.osc.parse.syntax._
import dragon.osc.parse.ui._

object Parse {

    def file(filename: String): Option[Root] =
        Lang.readFile(filename).flatMap(Read.root.run _)    

}
