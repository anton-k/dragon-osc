package dragon.osc.parse.send

import scala.util.Try

import dragon.osc.parse.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.attr._
import dragon.osc.parse.util._
import dragon.osc.parse.widget._

case class Send(default: List[Msg], onValue: Map[String, List[Msg]] = Map(), onValueOff: Map[String, List[Msg]] = Map())
case class Msg(client: String, address: String, args: List[Arg])

trait Arg
case class PrimArg(value: Prim) extends Arg
case class MemRef(name: String) extends Arg
case class ArgRef(id: Int) extends Arg

trait Guard
case class ArgEquals(value: Prim) extends Guard

object Send {

    val read: Attr[Option[Send]] = Attr.attr(Names.send, (x: Lang) => Some(readMessages(unwind(x))), None)

    val initMessages: Widget[List[Msg]] = new Widget[List[Msg]] {
        def run(obj: Lang) = readMsgList(unwind(obj))
    }

    def readMessages(obj: Lang) = obj match {
        case ListSym(xs) => Some(Send(xs.map(readMsg).flatten))
        case MapSym(m)   => Some(readMap(m))
        case _ => None
    }

    def readMsg(obj: Lang): Option[Msg] = obj.getKey(Names.msg).map(Attr.lift3(Msg, Attr.client, Attr.path, args).run)

    def args = Attr.attr[List[Arg]](Names.args, readArgs, List())

    def readArgs(obj: Lang) = obj match {
        case ListSym(xs) => Util.optionMapM(xs)(readSingleArg)
        case PrimSym(prim) => readSingleArg(obj).map(x => List(x))
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

    def readMap(m: Map[String,Lang]) = Send(readDefault(m), readOnValue(m), readOnValueOff(m))

    def readDefault(m: Map[String,Lang]) = m.get(Names.default).flatMap(readMsgList).getOrElse(Nil)

    def readOnValue(m: Map[String,Lang])    = readOnValueBy(Names.msgCase, m)
    def readOnValueOff(m: Map[String,Lang]) = readOnValueBy(Names.msgCaseOff, m)

    def readOnValueBy(caseName: String, m: Map[String,Lang]) = m.toList.filter(keyValue => keyValue._1.startsWith(caseName + " "))
        .map(keyValue => (keyValue._1.drop(caseName.length + 1), readMsgList(keyValue._2)))
        .filter(keyValue => ! keyValue._2.isEmpty)
        .map(keyValue => (keyValue._1, keyValue._2.get))
        .toMap


    // ---------------------------------------------------------------------

    def unwind(obj: Lang): Lang = unwindRootDefs(obj, unwindMessages(obj))

    def unwindRootDefs(obj: Lang, msgs: Lang): Lang = {
        def getElem(m: Map[String,Lang], key: String) = m.get(key).map(value => (key, value))

        def insert(xs: List[(String, Lang)])(elem: Lang) = elem match {
            case MapSym(m) => MapSym(m ++ xs.toMap)
            case _ => elem
        }

        def insertInMsgList(adds: List[(String, Lang)], elem: Lang) = elem match {
            case ListSym(msgs) => ListSym(msgs.map(insertInMsg(adds)))                
            case _ => elem
        }

        def insertInMsg(adds: List[(String,Lang)])(elem: Lang) = {
            val msgContent = elem.getKey(Names.msg).map(insert(adds))
            msgContent.map(content => MapSym(List(Names.msg -> content).toMap)).getOrElse(elem)
        }

        obj match {
            case MapSym(m) => {
                val adds = List(getElem(m, Names.client), getElem(m, Names.path)).flatten
                msgs match {
                    case MapSym(msgMap) => MapSym(msgMap.map({ case (key, value) => if (key.startsWith(Names.msgCase) || key.startsWith(Names.msgCaseOff) || key.startsWith(Names.default)) (key, insertInMsgList(adds, value)) else (key, value)}))
                    case _ => msgs
                }                
            }                        
            case _ => obj
        }
    }

    def unwindMessages(obj: Lang) = {
        def inCase(x: String) = Names.msgCase + " " + x
        def fromArg(elem: Lang) = ListSym(List(MapSym(List(Names.msg -> MapSym(List(Names.args -> elem).toMap)).toMap)))

        def ints(obj: Lang): Option[Map[String,Lang]] = obj match {
            case ListSym(xs) => Some(xs.zipWithIndex.map( { case (elem, ix) => (inCase(ix.toString), fromArg(elem)) } ).toMap)
            case _ => None
        }
        def booleans(obj: Lang): Option[Map[String,Lang]] = obj match {            
            case ListSym(List(onTrue, onFalse)) => Some(List(inCase(Names.trueStr) -> fromArg(onTrue), inCase(Names.falseStr) -> fromArg(onFalse)).toMap)
            case _ => None
        }
        def strings(obj: Lang): Option[Map[String,Lang]] = obj match {
            case MapSym(m) => Some(m.map({case (key, value) => (inCase(key), fromArg(value)) }))
            case _ => None
        }

        def getMsgs(name: String, extract: Lang => Option[Map[String,Lang]], m: Map[String,Lang]) =         
            m.get(name).flatMap(extract)

        obj match {
            case MapSym(m) => MapSym((getMsgs(Names.ints, ints, m) orElse getMsgs(Names.booleans, booleans, m) orElse getMsgs(Names.strings, strings, m)).getOrElse(m))
            case _ => obj
        }
    }
}
