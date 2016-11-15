package dragon.osc.act

import scala.swing.audio.parse.arg.{Arg, Settings, DefaultOscSend, OscBoolean, OscInt, OscFloat, Sym, UiSym, OscAddress, UiDecl, UiList}
import dragon.osc.const.Names
import dragon.osc.input.InputBase
import dragon.osc.send.Osc
import scala.audio.osc.MessageCodec
import scala.util.Try

case class St(osc: Osc, memory: Memory)

private object Utils {
    def toBooleanMap[A](m: Map[String,A]): Map[Boolean,A] = {
        val boleanKeys = List("true", "false")
        m.filterKeys(key => boleanKeys.contains(key)).toList.map(p => (p._1.toBoolean, p._2)).toMap
    }

    def toIntMap[A](m: Map[String,A]): Map[Int,A] = 
        m.map({ case (k, v) => Try { k.toInt }.toOption.map( n => (n -> v)) }).flatten.toMap[Int,A]

    def traverseOption[A,B](xs: List[A])(f: A => Option[B]): Option[List[B]] = xs match {
        case Nil => Some(Nil)
        case a :: as => f(a).flatMap { x => 
            traverseOption(as)(f).map(tail => x :: tail)
        }
    }
}

case class Act(single: Option[List[Msg]], valueMap: Option[Map[String, List[Msg]]], defaultSend: Option[DefaultOscSend] = None) {
    def withDefaultSend(x: Option[DefaultOscSend]) = this.copy(defaultSend = x)
    def mapDefaultSend(f: DefaultOscSend => DefaultOscSend) = this.copy(defaultSend = this.defaultSend.map(f))
    
    def compileToggle: SpecAct[Boolean] = toSpecBoolean 
    def compileButton: SpecAct[Boolean] = 
        SpecAct[Boolean](getDefaultSendList(buttonDefaultSend), None)

    def compileIntDial: SpecAct[Int] = compileInt

    def compileInt: SpecAct[Int] = 
        SpecAct[Int](getDefaultSendList(fromDefaultSend), valueMap.map(Utils.toIntMap))    

    def compileFloat: SpecAct[Float] = 
        SpecAct[Float](getDefaultSendList(buttonDefaultSend), None)

    private def getDefaultSendList(onDefault: DefaultOscSend => Msg) = 
        defaultSend.map(onDefault).toList ++ single.getOrElse(Nil)

    private def fromDefaultSend(x: DefaultOscSend) = x match {
        case OscBoolean(addr) => Msg(addr, List(ArgRef(1)))
        case OscInt(addr) => Msg(addr, List(ArgRef(1)))
        case OscFloat(addr, _) => Msg(addr, List(ArgRef(1)))        
    }

    private def buttonDefaultSend(x: DefaultOscSend) = x match {
        case OscBoolean(addr) => Msg(addr, List(BooleanVal(true)))
    }

    def toSpecBoolean: SpecAct[Boolean] = 
        SpecAct[Boolean](getDefaultSendList(fromDefaultSend), valueMap.map(Utils.toBooleanMap))

    def toSpecInt: SpecAct[Int] = 
        SpecAct[Int](getDefaultSendList(fromDefaultSend), valueMap.map(Utils.toIntMap))
}


object Memory {
    def init = Memory(Map[String,Object]())
}

case class Memory(var memory: Map[String, Object]) {
    def get(name: String) = memory.get(name)
}

case class SpecAct[A](msgList: List[Msg], valueMap: Option[Map[A,List[Msg]]]) {
    def act(a: A, st: St)(implicit codec: MessageCodec[A]) = {
        val osc = st.osc
        val memory = st.memory
        msgList.foreach(sendMsg(a, memory, osc))
        valueMap.foreach(m => m.get(a).foreach(xs => xs.foreach(msg => sendMsg(a, memory, osc)(msg)(codec))))
    } 

    private def sendMsg(a: A, memory: Memory, osc: Osc)(msg: Msg)(implicit codec: MessageCodec[A]) {
        PrimMsg.fromMsg[A](a, memory)(msg)(codec).foreach(_.send(osc))
    }
}

case class Msg(oscAddress: OscAddress, args: List[Val])

object PrimMsg {
    def fromMsg[A](a: A, memory: Memory)(msg: Msg)(implicit codec: MessageCodec[A]): Option[PrimMsg] = Utils.traverseOption(msg.args)(_.getPrimVal(a, memory)(codec)).map(primVals => PrimMsg(msg.oscAddress, primVals))
}

case class PrimMsg(oscAddress: OscAddress, args: List[Object]) {
    def send(osc: Osc): Unit = {
        println(s"${oscAddress.clientId} ${oscAddress.address} ${args}")
        osc.dynamicSend(oscAddress, args)
    }
}

object Msg {
    def read: Arg[Msg] = for {
        addr <- Arg.oscAddress
        vals <- Val.readList
    } yield Msg(addr, vals)
}

trait Val {
    def getPrimVal[A](a: A, memory: Memory)(implicit codec: MessageCodec[A]): Option[Object] = this match {
        case ArgRef(nHuman) => {
            val n = nHuman - 1
            val list = codec.toMessage(a)
            if (n < list.size) Some(list(n)) else None
        }
        case MemRef(name) => memory.get(name)
        case other => Some { other match {
            case IntVal(n)     => n.asInstanceOf[Object]
            case FloatVal(f)   => f.asInstanceOf[Object]
            case StringVal(s)  => s.asInstanceOf[Object]
            case BooleanVal(b) => b.asInstanceOf[Object]
        }}
    }

}
trait PrimVal

case class IntVal(value: Int) extends Val with PrimVal
case class FloatVal(value: Float) extends Val with PrimVal
case class BooleanVal(value: Boolean) extends Val with PrimVal
case class StringVal(value: String) extends Val with PrimVal
case class ArgRef(id: Int) extends Val
case class MemRef(ref: String) extends Val

object Val {
    def readList: Arg[List[Val]] = Arg.many(Val.read)

    def read: Arg[Val] = 
           Arg.argRef.map(ArgRef) || 
           Arg.memRef.map(MemRef) || 
           Arg.int.map(IntVal)    ||
           Arg.float.map(FloatVal)||
           Arg.boolean.map(BooleanVal) ||
           Arg.string.map(StringVal)    
}

object Act {
    def fromMap(settings: Settings, m: Map[String, UiDecl]): Option[Act] = {
        val acts = m.toList
            .filter(x => x._1.startsWith("act") && x._2.isList)
            .map({ case (name, body) => (name.drop(3).trim, body) })
        if (acts.isEmpty) Some(Act(None, None))
        else {
            val (singles, values) = acts.partition(x => x._1 == "")
            Some(Act(someIfNotEmpty(singles.map(_._2), mkSingles(settings)), someIfNotEmpty(values, mkValues(settings))))
        }
    }

    private def someIfNotEmpty[A,B](xs: List[A], f: List[A] => B): Option[B] = 
        if (xs.isEmpty) None
        else Some(f(xs))

    private def mkSingles(settings: Settings)(xs: List[UiDecl]): List[Msg] = 
        parseActionList(settings, xs.flatMap { x => x match {
            case UiList(ys) => ys
            case _ => Nil
        }})

    private def mkValues(settings: Settings)(xs: List[(String, UiDecl)]): Map[String, List[Msg]] = 
        xs.flatMap { x => x match {
            case (name, UiList(acts)) => List((name, parseActionList(settings, acts)))
            case _ => Nil
        }}.toMap

    private def parseActionList(settings: Settings, xs: List[UiDecl]) = xs.map(a => parseAction(settings, a)).flatten

    private def parseAction(settings: Settings, x: UiDecl): Option[Msg] = x match {
        case UiList(xs) => Msg.read.eval(settings, xs)
        case _ => None
    }
}
