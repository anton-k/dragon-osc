package dragon.osc.parse.ui

import dragon.osc.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.attr._
import dragon.osc.parse.send._
import dragon.osc.parse.widget._

case class App(windows: List[Window])
case class Window(title: String, size: (Int, Int), tabs: List[Tab]) 
case class Tab(title: String, content: Ui)

trait Ui

case class Elem(sym: Sym, param: Param) extends Ui

case class Param(id: Option[String], osc: Option[Send])

// ----------------------------------------
// compound widgets

trait Sym
case class Hor(items: List[Ui]) extends Sym
case class Ver(items: List[Ui]) extends Sym

// ----------------------------------------
// primitive widgets

case class Dial(init: Float, color: String) extends Sym
case class HFader(init: Float, color: String) extends Sym
case class VFader(init: Float, color: String) extends Sym
case class Toggle(init: Boolean, color: String, text: String) extends Sym
case class IntDial(init: Int, color: String, range: (Int, Int)) extends Sym
case class Button(color: String, text: String) extends Sym
case class Label(color: String, text: String) extends Sym

// -----------------------------------------


object Read {
    import Attr._

    def fromSym(sym: Widget[Sym]): Widget[Ui] = 
        Widget.lift2(Elem, sym, param)
   
    val dial    = primWidget(Names.dial,    lift2(Dial,   initFloat, color))
    val hfader  = primWidget(Names.hfader,  lift2(HFader, initFloat, color))
    val vfader  = primWidget(Names.vfader,  lift2(VFader, initFloat, color))
    val toggle  = primWidget(Names.toggle,  lift3(Toggle, initBoolean, color, text))
    val intDial = primWidget(Names.intDial, lift3(IntDial, initInt, color, rangeInt))
    val label   = primWidget(Names.label,   lift2(Label,  color, text))
    val button  = primWidget(Names.button,  lift2(Button, color, text))

    val hor = listWidget(Names.hor, Hor)
    val ver = listWidget(Names.ver, Ver)

    def listWidget[A](key: String, mk: List[Ui] => A) = new Widget[A] {
        def run(obj: Lang) = obj.getKey(key).flatMap {
            case ListSym(xs) => Some(mk(xs.map(ui.run).flatten))
            case _ => None
        }
    }  

    def primWidget(name: String, attr: Attr[Sym]) = new Widget[Sym] {
        def run(obj: Lang) = obj.getKey(name).map(attr.run)
    }     

    val widgets = List(dial, hfader, vfader, toggle, intDial, label, button, hor, ver)

    def param: Widget[Param] = {        
        Widget.lift2(Param, Widget.fromOptionAttr(id), Widget.fromOptionAttr(Send.read))
    } 

    def ui: Widget[Ui] = Widget.any(widgets.map(fromSym))
}

