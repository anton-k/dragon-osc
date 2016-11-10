package dragon.osc.parse.ui

import dragon.osc.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.attr._

trait Ui

case class Elem(sym: Sym, id: Option[String], osc: Option[Send]) extends Ui

case class Send(msg: List[Msg], guard: Option[Guard])
case class Msg(client: String, addr: String, args: List[Arg])

trait Arg
case class PrimArg(value: Prim) extends Arg
case class MemRef(name: String) extends Arg
case class ArgRef(id: Int) extends Arg

trait Guard
case class ArgEquals(value: Prim) extends Guard

// ----------------------------------------
// compound widgets

trait Sym
case class Hor(items: List[Ui]) extends Sym
case class Ver(items: List[Ui]) extends Sym

case class Tab(pages: List[Page])
case class Page(title: String, content: Ui)

// ----------------------------------------
// primitive widgets

case class Dial(init: Float, color: String)
case class HFader(init: Float, color: String)
case class VFader(init: Float, color: String)
case class Toggle(init: Boolean, color: String, text: String)
case class IntDial(init: Int, color: String, range: (Int, Int))
case class Button(color: String, text: String)
case class Label(color: String, text: String)

// -----------------------------------------


object Read {
    import Attr._

    def getKey(obj: Lang, key: String) = obj match {
        case MapSym(m) => m.get(key)
        case _ => None
    }

    case class Widget[A](name: String, attr: Attr[A]) {
        def run(obj: Lang) = getKey(obj, name).map(attr.run)
    }

    val dial    = Widget(Names.dial,    lift2(Dial,   initFloat, color))
    val hfader  = Widget(Names.hfader,  lift2(HFader, initFloat, color))
    val vfader  = Widget(Names.vfader,  lift2(VFader, initFloat, color))
    val toggle  = Widget(Names.toggle,  lift3(Toggle, initBoolean, color, text))
    val intDial = Widget(Names.intDial, lift3(IntDial, initInt, color, rangeInt))
    val label   = Widget(Names.label,   lift2(Label,  color, text))
    val button  = Widget(Names.button,  lift2(Button, color, text))
}

