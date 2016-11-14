package dragon.osc.parse.ui

import dragon.osc.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.attr._
import dragon.osc.parse.send._
import dragon.osc.parse.widget._

case class Root(windows: List[Window])
case class Window(title: String, size: Option[(Int, Int)], content: Ui) 

trait Ui

case class Elem(sym: Sym, param: Param = Param(None, None)) extends Ui
case class Param(id: Option[String], osc: Option[Send])

// ----------------------------------------
// compound widgets

trait Sym
case class Hor(items: List[Ui]) extends Sym
case class Ver(items: List[Ui]) extends Sym

case class Tabs(items: List[Page]) extends Sym
case class Page(title: String, content: Ui)
object Space extends Sym
object Glue  extends Sym

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

    def emptyUi = Elem(Space, Param(None, None))

    def fromSym(sym: Widget[Sym]): Widget[Ui] = 
        Widget.lift2(Elem, sym, param)
   
    def dial    = primWidget(Names.dial,    lift2(Dial,   initFloat, color))
    def hfader  = primWidget(Names.hfader,  lift2(HFader, initFloat, color))
    def vfader  = primWidget(Names.vfader,  lift2(VFader, initFloat, color))
    def toggle  = primWidget(Names.toggle,  lift3(Toggle, initBoolean, color, text))
    def intDial = primWidget(Names.intDial, lift3(IntDial, initInt, color, rangeInt))
    def label   = primWidget(Names.label,   lift2(Label,  color, text))
    def button  = primWidget(Names.button,  lift2(Button, color, text))

    def hor = listWidget(Names.hor, Hor)
    def ver = listWidget(Names.ver, Ver)

    def tabContent: Attr[Option[Ui]] = attr(Names.content, obj => ui.run(obj).map(x => Some(x)), None)
    def page: Widget[Option[Page]] = primWidget(Names.page, lift2((t: String, optCont: Option[Ui]) => optCont.map(cont => Page(t, cont)), title, tabContent))
    def tabs = listWidgetBy[Tabs, Option[Page]](page)(Names.tabs, xs => Tabs(xs.flatten))  

    def listWidgetBy[A,B](elem: Widget[B])(key: String, mk: List[B] => A) = new Widget[A] {
        def run(obj: Lang) = obj.getKey(key).flatMap {
            case ListSym(xs) => Some(mk(xs.map(elem.run).flatten))
            case _ => None
        }
    }  

    def listWidget[A](key: String, mk: List[Ui] => A) = listWidgetBy(ui)(key, mk)

    def primWidget[A](name: String, attr: Attr[A]) = new Widget[A] {
        def run(obj: Lang) = obj.getKey(name).map(attr.run)
    }     

    def widgets: Stream[Widget[Sym]] = 
        dial #:: 
        hfader #:: 
        vfader #:: 
        toggle #:: 
        intDial #:: 
        label #:: 
        hor #:: 
        ver #:: 
        tabs #:: 
        Stream.empty[Widget[Sym]]

    def param: Widget[Param] = {        
        Widget.lift2(Param, Widget.fromOptionAttr(id), Widget.fromOptionAttr(Send.read))
    } 

    def ui: Widget[Ui] = Widget.any(widgets.map(fromSym))

    def window: Widget[Window] = primWidget(Names.window, lift3(Window, title, size, windowContent))

    def windowContent: Attr[Ui] = attr(Names.content, obj => ui.run(obj), emptyUi)
    
    def root: Widget[Root] = listWidgetBy(window)(Names.app, Root)

}
