package dragon.osc.parse.syntax

import dragon.osc.parse.yaml._

trait Prim 
case class PrimInt(value: Int) extends Prim
case class PrimFloat(value: Float) extends Prim
case class PrimBoolean(value: Boolean) extends Prim
case class PrimString(value: String) extends Prim

trait Lang
case class PrimSym(value: Prim) extends Lang
case class ListSym(items: List[Lang]) extends Lang
case class MapSym(items: Map[String, Lang]) extends Lang

object Lang {
    def read(str: String): Option[Lang] = read(ReadYaml.loadString(str))

    def read(x: Object): Option[Lang] = 
        ReadYaml.readList(x).flatMap(xs => optionMapM(xs)(read).map(ListSym)) orElse
        ReadYaml.readMap(x).flatMap(m => optionMapM(m)(read).map(MapSym)) orElse
        readPrim(x).map(PrimSym)

    def readPrim(x: Object): Option[Prim] = 
        ReadYaml.readInt(x).map(PrimInt) orElse
        ReadYaml.readBoolean(x).map(PrimBoolean) orElse
        ReadYaml.readString(x).map(PrimString) orElse
        ReadYaml.readFloating(x).map(PrimFloat) 

    def optionMapM[A,B](xs: List[A])(f: A => Option[B]): Option[List[B]] = xs match {
        case Nil => Some(Nil)
        case a::as => f(a).flatMap { v => optionMapM(as)(f).map(vs => v :: vs) }
    }

    def optionMapM[K,A,B](xs: Map[K,A])(f: A => Option[B]): Option[Map[K,B]] = 
        optionMapM(xs.toList)({ case (k, v) => f(v).map(x => (k, x))}).map(_.toMap)
}
