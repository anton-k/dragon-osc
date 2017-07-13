import org.scalatest._

import scala.swing.event.Key

import dragon.osc.parse.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.send._
import dragon.osc.parse.hotkey._
import dragon.osc.parse.widget._

class HotKeyTest extends FunSuite {

    def checkGuard(str: String, res: Option[KeyGuard]) =
        assert(Lang.read(str).flatMap(HotKey.Read.guard.run) == res)

    test ("hotkey guard 1") {
        checkGuard("when: { win1: \"1\" }", Some(KeyGuard("win1", "1")))
    }

    test ("hotkey guard 2") {
        checkGuard("when: { win1: 1 }", Some(KeyGuard("win1", 1.asInstanceOf[Object])))
    }

    def checkKey(str: String, res: Option[HotKey]) =
        assert(Lang.read(str).flatMap(HotKey.Read.key.run) == res)

    test ("read key number") {
        assert(Keyboard.keyFromString("1") == Some(Key.Key1))
    }

    test ("read key letter") {
        assert(Keyboard.keyFromString("1") == Some(Key.Key1))
    }

    test ("read key special") {
        assert(Keyboard.keyFromString("alt") == Some(Key.Alt))
        assert(Keyboard.keyFromString("shift") == Some(Key.Shift))
        assert(Keyboard.keyFromString("ctrl") == Some(Key.Control))
    }

    test ("prim key 1") {
        checkKey("key: [alt, \"1\"]", Some(HotKey(Set(Key.Alt), Key.Key1)))
    }

    test ("prim key 2") {
        checkKey("key: [alt, shift, a]", Some(HotKey(Set(Key.Alt, Key.Shift), Key.A)))
    }

    test ("prim key 3") {
        checkKey("key: abracadabra", None)
    }

    test ("prim key 4") {
        checkKey("key: a", Some(HotKey(Set.empty, Key.A)))
    }

    def checkEvent(str: String, res: Option[HotKeyEvent]) =
        assert(Lang.read(str).flatMap(HotKey.Read.hotKeyEvent.run) == res)

    test ("event 1") {
        checkEvent("{ key: a, send: [ msg: { path: /amp, args: [true]}] }",
            Some(HotKeyEvent(HotKey(Set.empty, Key.A), Send(List(Msg(NameClient(Defaults.client), "/amp", List(PrimArg(PrimBoolean(true))), None)), Map()), None)))
    }


    def check(str: String, res: Option[Keys]) =
        assert(Lang.read(str).flatMap(HotKey.read.run) == res)

    test ("keys 1") {
        check("""
            keys:
                - key: a
                  send:
                    - msg:
                        path: /amp
                        args: [true]
                - key: b
                  send:
                    - msg:
                        path: /cps
                        args: [false]
        """, Some(Keys(List(
                HotKeyEvent(HotKey(Set.empty, Key.A), Send(List(Msg(NameClient(Defaults.client), "/amp", List(PrimArg(PrimBoolean(true))), None)), Map()), None ),
                HotKeyEvent(HotKey(Set.empty, Key.B), Send(List(Msg(NameClient(Defaults.client), "/cps", List(PrimArg(PrimBoolean(false))), None)), Map()), None )
            ))))
    }

    test ("empty keys") {
        check("window: {}", Some(Keys(Nil)))
    }

}