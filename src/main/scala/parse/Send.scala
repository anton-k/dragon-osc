package dragon.osc.parse.send

import scala.util.Try

import dragon.osc.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.attr._
import dragon.osc.parse.util._

case class Send(default: List[Msg], onValue: Map[String, List[Msg]] = Map())
case class Msg(client: String, address: String, args: List[Arg])

trait Arg
case class PrimArg(value: Prim) extends Arg
case class MemRef(name: String) extends Arg
case class ArgRef(id: Int) extends Arg

trait Guard
case class ArgEquals(value: Prim) extends Guard

object Send {

    val read: Attr[Option[Send]] = Attr.attr(Names.send, x => Some(readMessages(x)), None)

    def readMessages(obj: Lang) = obj match {
        case ListSym(xs) => Some(Send(xs.map(readMsg).flatten))
        case MapSym(m)   => Some(readMap(m))
        case _ => None
    }

    def readMsg(obj: Lang): Option[Msg] = obj.getKey(Attributes.msg).map(Attr.lift3(Msg, Attr.client, Attr.path, args).run)

    def args = Attr.attr[List[Arg]](Attributes.args, readArgs, List())

    def readArgs(obj: Lang) = obj match {
        case ListSym(xs) => Util.optionMapM(xs)(readSingleArg)
        case _ => None
    }

    def readSingleArg(obj: Lang): Option[Arg] = obj match {
        case PrimSym(PrimString(str)) if isRef(str) => getArgRef(str).orElse(getMemRef(str))
        case PrimSym(prim) => Some(PrimArg(prim))        
        case _ => None
    }

    def isRef(str: String) = str.startsWith("$")
    def getArgRef(str: String) = Try { str.drop(1).toInt }.toOption.map(ArgRef)
    def getMemRef(str: String) = Some(MemRef(str.drop(1)))
        
    def readMsgList(obj: Lang): Option[List[Msg]] = obj match {
        case ListSym(xs) => Some(xs.map(readMsg).flatten)
        case _ => None
    }

    def readMap(m: Map[String,Lang]) = Send(readDefault(m), readOnValue(m))

    def readDefault(m: Map[String,Lang]) = m.get(Attributes.default).flatMap(readMsgList).getOrElse(Nil)

    def readOnValue(m: Map[String,Lang]) = m.toList.filter(keyValue => keyValue._1.startsWith(Attributes.msgCase + " "))
        .map(keyValue => (keyValue._1.drop(Attributes.msgCase.length + 1), readMsgList(keyValue._2)))
        .filter(keyValue => ! keyValue._2.isEmpty)
        .map(keyValue => (keyValue._1, keyValue._2.get))
        .toMap
}
