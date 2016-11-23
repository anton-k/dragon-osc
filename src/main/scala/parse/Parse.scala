package dragon.osc.parse

import java.io._

import dragon.osc.parse.syntax._
import dragon.osc.parse.ui._
import dragon.osc.parse.tfm._

object Parse {

    def file(filename: String): Option[Root] =
        Lang.readFile(filename).flatMap(symbol)

    def file(file: File): Option[Root] =
        if (file.exists() && !file.isDirectory()) { 
            Lang.readFile(file).flatMap(symbol)        
        } else None
        

    def string(str: String): Option[Root] = 
        Lang.read(str).flatMap(symbol)

    def symbol(obj: Lang): Option[Root] =
        Read.root.run(Tfm.tfmLang(obj))
}
