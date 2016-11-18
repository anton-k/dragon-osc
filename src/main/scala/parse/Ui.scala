package dragon.osc.parse.ui

import dragon.osc.parse.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.attr._
import dragon.osc.parse.send._
import dragon.osc.parse.widget._
import dragon.osc.parse.hotkey._

case class Root(windows: List[Window], hotKeys: List[HotKeyEvent])
case class Window(title: String, size: Option[(Int, Int)], content: Ui) 

case class Ui(sym: Sym, param: Param = Param(None, None))
case class Param(id: Option[String], osc: Option[Send])

// ----------------------------------------
// compound widgets

trait Sym
case class Hor(items: List[Ui])             extends Sym
case class Ver(items: List[Ui])             extends Sym

case class Tabs(items: List[Page])          extends Sym
case class Page(title: String, content: Ui)
object Space                                extends Sym
object Glue                                 extends Sym

// ----------------------------------------
// primitive widgets

case class Dial(init: Float, color: String)                     extends Sym
case class HFader(init: Float, color: String)                   extends Sym
case class VFader(init: Float, color: String)                   extends Sym
case class Toggle(init: Boolean, color: String, text: String)   extends Sym
case class IntDial(init: Int, color: String, range: (Int, Int)) extends Sym
case class Button(color: String, text: String)                  extends Sym
case class Label(color: String, text: String)                   extends Sym

// -----------------------------------------

object Read {    
    import Attr._

    def emptyUi = Ui(Space, Param(None, None))

    def fromSym(sym: Widget[Sym]): Widget[Ui] = 
        Widget.lift2(Ui, sym, param)
   
    def dial    = Widget.prim(Names.dial,    lift2(Dial,   initFloat, color))
    def hfader  = Widget.prim(Names.hfader,  lift2(HFader, initFloat, color))
    def vfader  = Widget.prim(Names.vfader,  lift2(VFader, initFloat, color))
    def toggle  = Widget.prim(Names.toggle,  lift3(Toggle, initBoolean, color, text))
    def intDial = Widget.prim(Names.intDial, lift3(IntDial, initInt, color, rangeInt))
    def label   = Widget.prim(Names.label,   lift2(Label,  color, text))
    def button  = Widget.prim(Names.button,  lift2(Button, color, text))

    def hor = list(Names.hor, Hor)
    def ver = list(Names.ver, Ver)

    def tabContent: Attr[Option[Ui]] = attr(Names.content, obj => ui.run(obj).map(x => Some(x)), None)
    def page: Widget[Option[Page]] = Widget.prim(Names.page, lift2((t: String, optCont: Option[Ui]) => optCont.map(cont => Page(t, cont)), title, tabContent))
    def tabs = Widget.listBy[Tabs, Option[Page]](page)(Names.tabs, xs => Tabs(xs.flatten))  

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

    def list[A](key: String, mk: List[Ui] => A) = Widget.listBy(ui)(key, mk)        

    def param: Widget[Param] = {        
        Widget.lift2(Param, Widget.fromOptionAttr(id), Widget.fromOptionAttr(Send.read))
    } 

    def ui: Widget[Ui] = Widget.any(widgets.map(fromSym))

    def window: Widget[Window] = Widget.prim(Names.window, lift3(Window, title, size, windowContent))

    def windowContent: Attr[Ui] = attr(Names.content, obj => ui.run(obj), emptyUi)
    
    def root: Widget[Root] = Widget.lift2(Root, Widget.listBy(window)(Names.app, xs => xs), HotKey.read)
}
