package dragon.osc.send

import scala.swing.audio.ui.{SetWidget, SetColor, SetText, SetTextList, GetWidget}
import scala.audio.osc.MessageCodec

import dragon.osc.readargs._
import dragon.osc.ui._
import dragon.osc.parse.send._
import dragon.osc.parse.syntax._
import dragon.osc.parse.util.{Util => ParseUtil}
import dragon.osc.parse.hotkey._

private object Util {
    def convertMsg(st: St)(input: List[Object], msg: Msg): Option[OscMsg] =
        ParseUtil.optionMapM(msg.args)(primArg(st)(input)).map(args => OscMsg(msg.client, msg.address, args, msg.delay))

    def primArg(st: St)(input: List[Object])(arg: Arg): Option[Object] = arg match {
        case PrimArg(PrimInt(x)) => Some(x.asInstanceOf[Object])
        case PrimArg(PrimString(x)) => Some(x.asInstanceOf[Object])
        case PrimArg(PrimFloat(x)) => Some(x.asInstanceOf[Object])
        case PrimArg(PrimBoolean(x)) => Some((if (x) 1 else 0).asInstanceOf[Object])   // Booleans are encoded as numbers 1 or 0
        case MemRef(id) => st.memory.get(id).map(_.asInstanceOf[Object])
        case ArgRef(id) => if (id < input.length) Some(input(id)) else None
    }


    def msgList(input: List[Object], send: Send): List[Msg] =
        send.onValue.get(getStringRepr(input)).getOrElse(send.default)

    def msgOffList(input: List[Object], send: Send): List[Msg] =
        send.onValueOff.get(getStringRepr(input)).getOrElse(Nil)

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

    def compileSendWithDeselect(send: Send) = {
        def select(oscInput: List[Object], caseArgInput: List[Object]) = compileSend(send)(oscInput, caseArgInput)

        def deselect(oscInput: List[Object], caseArgInput: List[Object]) {
            Util.msgOffList(caseArgInput, send).foreach(msg => Util.convertMsg(this)(oscInput, msg).foreach(x => osc.send(x)))
        }

        (select _, deselect _)
    }

    def sendNoInputMsgs(msgs: List[Msg]) =
        convertNoInput(Send(msgs)).foreach(x => osc.send(x))

    def convertNoInput(send: Send): List[OscMsg] =
        Util.msgList(Nil, send).map(msg => Util.convertMsg(this)(Nil, msg)).flatten

    def getKeyMap(keys: Keys) =
        keys.keyEvents.map(event => (event.key -> this.convertNoInput(event.send))).toMap


    def addListener[A](id: String, widget: SetWidget[A] with SetColor)(implicit codec: MessageCodec[A]) {
        osc.addListener(id, widget)(codec)
        osc.addColorListener(id, widget)
    }

}

object St {
    def init(args: Args, clients: List[Client]) = St(Osc(args, clients), Memory.init)
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


object WindowKeys {
    def fromRootKeys(st: St, keys: Keys) =
        WindowKeys(st.getKeyMap(keys), Map())
}

case class WindowKeys(common: Map[HotKey, List[OscMsg]], var specific: Map[HotKey, List[OscMsg]]) {
    def act(st: St, key: HotKey) {
        val msgs = common.get(key) orElse specific.get(key)  getOrElse (Nil)
        msgs.foreach(msg => st.osc.send(msg))
    }

    def setSpecific(x: Map[HotKey, List[OscMsg]]) {
        this.specific = x
    }

    def appendWindowKeys(st: St, keys: Keys) = {
        val localKeys = st.getKeyMap(keys)
        this.copy(common = this.common ++ localKeys)
    }
}
