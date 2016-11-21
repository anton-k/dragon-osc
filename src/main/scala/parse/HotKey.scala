package dragon.osc.parse.hotkey

import scala.swing.event.Key

import dragon.osc.parse.widget._
import dragon.osc.parse.util._
import dragon.osc.parse.attr._
import dragon.osc.parse.const._
import dragon.osc.parse.send._
import dragon.osc.parse.syntax._

case class Keys(keyEvents: List[HotKeyEvent])

case class HotKeyEvent(key: HotKey, send: Send, guard: Option[KeyGuard])

case class HotKey(modifiers: List[HotKey.Modifier], key: Key.Value)
case class KeyGuard(name: String, value: Object)

object HotKey {
    type Modifier = Key.Value

    def read: Widget[Keys] = Read.hotKeyEvents.map(Keys)

    def readAttr: Attr[Keys] = new Attr[Keys] {
        def run(obj: Lang) = read.run(obj).getOrElse(Keys(Nil))
    }

    object Read {
        def hotKeyEvents: Widget[List[HotKeyEvent]] = Widget.listBy(hotKeyEvent)(Names.keys, xs => xs)

        def hotKeyEvent: Widget[HotKeyEvent] = {
            def mk(optKey: Option[HotKey], optSend: Option[Send], guard: Option[KeyGuard]) = for {
                key <- optKey
                send <- optSend
            } yield HotKeyEvent(key, send, guard)

            Widget.fromAttr(Attr.lift3(mk, key, Send.read, guard))
        }
            

        def key: Attr[Option[HotKey]] = Attr.optAttr(Names.key, readKey)    
        def guard: Attr[Option[KeyGuard]]    = Attr.optAttr(Names.keyGuard, readKeyGuard)

        def readKey(obj: Lang): Option[HotKey] = {
            def readSingleKey(obj: Lang): Option[Key.Value] = obj match {
                case PrimSym(PrimString(str)) => Keyboard.keyFromString(str)
                case _ => None
            }

            def fromList(keys: List[Key.Value]) = 
                if (keys.isEmpty) None
                else Some(HotKey(keys.init, keys.last))

            obj match {
                case ListSym(xs) => Util.optionMapM(xs)(readSingleKey).flatMap(fromList)
                case _ => readSingleKey(obj).map(key => HotKey(Nil, key))
            }
        }

        def readKeyGuard(obj: Lang): Option[KeyGuard] = obj match {
            case MapSym(m) => m.toList match {
                case List((name, PrimSym(p))) => Some(KeyGuard(name, p.toObject))
                case _ => None
            }
            case _ => None
        }    
    }
}
