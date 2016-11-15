package dragon.osc.ui

import scala.audio.osc._

object Codec {
    def onFloat(f: List[Object] => Unit):   (Float => Unit)     = x => f(MessageCodec.floatOscMessageCodec.toMessage(x))
    def onInt(f: List[Object] => Unit):     (Int => Unit)       = x => f(MessageCodec.intOscMessageCodec.toMessage(x))
    def onBoolean(f: List[Object] => Unit): (Boolean => Unit)   = x => f(MessageCodec.booleanOscMessageCodec.toMessage(x))
    def onString(f: List[Object] => Unit):  (String => Unit)    = x => f(MessageCodec.stringOscMessageCodec.toMessage(x))
}