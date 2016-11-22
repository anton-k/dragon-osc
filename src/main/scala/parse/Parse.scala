package dragon.osc.parse

import java.io._

import dragon.osc.parse.syntax._
import dragon.osc.parse.ui._
import dragon.osc.parse.tfm._

object Parse {

    def file(filename: String): Option[Root] =
        Lang.readFile(filename).flatMap(symbol)

    def file(filename: File): Option[Root] =
        Lang.readFile(filename).flatMap(symbol)        

    def string(str: String): Option[Root] = 
        Lang.read(str).flatMap(symbol)

    def symbol(obj: Lang): Option[Root] =
        Read.root.run(Tfm.tfmLang(obj))
}
