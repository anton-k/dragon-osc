package dragon.osc.parse.send

import dragon.osc.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.attr._
import dragon.osc.parse.widget._

case class Send(default: List[Msg], onValue: Map[String, List[Msg]] = Map())
case class Msg(client: String, addr: String, args: List[Arg])

trait Arg
case class PrimArg(value: Prim) extends Arg
case class MemRef(name: String) extends Arg
case class ArgRef(id: Int) extends Arg
case class DefaultMsg(name: String) extends Arg

trait Guard
case class ArgEquals(value: Prim) extends Guard

object Send {

    val read: Attr[Option[Send]] = Attr.attr(Names.const, readMessages, None)

    def readMessages(obj: Lang) = obj match {
        case ListSym(xs) => Some(Send(xs.map(readMsg).flatten))
        case MapSym(m)   => Some(readMap(m))
        case PrimSym(PrimString(name)) => Some(Send(List(DefaultMsg(name))))
        case _ => None
    }

    def readMsg(obj: Lang): Option[Msg] = ???

    def readMsgList(obj: Lang): Option[List[Msg]] = obj match {
        case ListSym(xs) => Some(xs.map(readMsg).flatten)
        case _ => None
    }

    def readMap(m: Map[String,Lang]) = Send(readDefault(m), readOnValue(m))

    def readDefault(m: Map[String,Lang]) = m.get(Attributes.default).flatMap(readMsgList).getOrElse(Nil)

    def readOnValue(m: Map[String,Lang]) = m.toList.filter(keyValue => keyValue._1.startsWith(Names.msgCase + " "))
}
