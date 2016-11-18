package dragon.osc.parse.syntax

import dragon.osc.parse.yaml._
import dragon.osc.parse.util._

trait Prim {
    def toObject: Object = this match {
        case PrimInt(x)     => x.asInstanceOf[Object]
        case PrimFloat(x)   => x.asInstanceOf[Object]
        case PrimBoolean(x) => x.asInstanceOf[Object]
        case PrimString(x)  => x.asInstanceOf[Object]
    }
}

case class PrimInt(value: Int) extends Prim
case class PrimFloat(value: Float) extends Prim
case class PrimBoolean(value: Boolean) extends Prim
case class PrimString(value: String) extends Prim

trait Lang {
    def getKey(key: String) = this match {
        case MapSym(m) => m.get(key)
        case _ => None
    }
}

case class PrimSym(value: Prim) extends Lang
case class ListSym(items: List[Lang]) extends Lang
case class MapSym(items: Map[String, Lang]) extends Lang

object Lang {
    def readFile(filename: String): Option[Lang] = read(ReadYaml.loadFile(filename))

    def read(str: String): Option[Lang] = read(ReadYaml.loadString(str))

    def read(x: Object): Option[Lang] = 
        ReadYaml.readList(x).flatMap(xs => Util.optionMapM(xs)(read).map(ListSym)) orElse
        ReadYaml.readMap(x).flatMap(m => Util.optionMapM(m)(read).map(MapSym)) orElse
        readPrim(x).map(PrimSym)

    def readPrim(x: Object): Option[Prim] = 
        ReadYaml.readInt(x).map(PrimInt) orElse
        ReadYaml.readBoolean(x).map(PrimBoolean) orElse
        ReadYaml.readString(x).map(PrimString) orElse
        ReadYaml.readFloating(x).map(PrimFloat) 
}
