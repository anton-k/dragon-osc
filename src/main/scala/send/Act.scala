package dragon.osc.send

import dragon.osc.readargs._
import dragon.osc.parse.send._
import dragon.osc.parse.syntax._
import dragon.osc.parse.util.{Util => ParseUtil}

private object Util {
    def convertMsg(st: St)(input: List[Object], msg: Msg): Option[OscMsg] = 
        ParseUtil.optionMapM(msg.args)(primArg(st)(input)).map(args => OscMsg(msg.client, msg.address, args))

    def primArg(st: St)(input: List[Object])(arg: Arg): Option[Object] = arg match {
        case PrimArg(PrimInt(x)) => Some(x.asInstanceOf[Object])
        case PrimArg(PrimString(x)) => Some(x.asInstanceOf[Object])
        case PrimArg(PrimFloat(x)) => Some(x.asInstanceOf[Object])
        case PrimArg(PrimBoolean(x)) => Some(x.asInstanceOf[Object])
        case MemRef(id) => st.memory.get(id).map(_.asInstanceOf[Object])
        case ArgRef(id) => if (id < input.length) Some(input(id)) else None
    }
               

    def msgList(input: List[Object], send: Send): List[Msg] = 
        send.onValue.get(getStringRepr(input)).getOrElse(send.default)

    def getStringRepr(input: List[Object]) = 
        input.map(_.toString).mkString(" ")
}

case class St(osc: Osc, memory: Memory) {
    def close {
        osc.close        
    }

    def compileSend(send: Send)(oscInput: List[Object], caseArgInput: List[Object]) {
        Util.msgList(caseArgInput, send).foreach(msg => Util.convertMsg(this)(oscInput, msg).foreach(x => osc.send(x)))
    }
}

object St {
    def init(args: Args) = St(Osc(args), Memory.init)   
}

case class Memory(var memory: Map[String, Object]) {
    def get(name: String) = memory.get(name)

    def register(key: String, value: Object) {
        memory += (key -> value)
    }
}

object Memory {
    def init = Memory(Map[String,Object]())
}
