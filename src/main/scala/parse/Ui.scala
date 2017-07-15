package dragon.osc.parse.ui

import java.io.File

import dragon.osc.parse.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.attr._
import dragon.osc.parse.send._
import dragon.osc.parse.widget._
import dragon.osc.parse.hotkey._

case class Root(windows: List[Window], hotKeys: Keys, initMessages: List[Msg], terminateMessages: List[Msg])
case class Window(title: String, size: Option[(Int, Int)], content: Ui, hotKeys: Keys)

case class Ui(sym: Sym, param: Param = Param(None, None))
case class Param(id: Option[String], osc: Option[Send])

// ----------------------------------------
// compound widgets

trait Sym
case class Hor(items: List[Ui])                                 extends Sym
case class Ver(items: List[Ui])                                 extends Sym

case class Tabs(items: List[Page])                              extends Sym
case class Page(title: String, content: Ui, hotKeys: Keys)
object Space                                                    extends Sym
object Glue                                                     extends Sym

// ----------------------------------------
// primitive widgets

case class Dial(init: Float, color: String, range: (Float, Float))      extends Sym
case class HFader(init: Float, color: String, range: (Float, Float))    extends Sym
case class VFader(init: Float, color: String, range: (Float, Float))    extends Sym
case class Toggle(init: Boolean, color: String, text: String)   extends Sym
case class IntDial(init: Int, color: String, range: (Int, Int)) extends Sym
case class Button(color: String, text: String)                  extends Sym
case class Label(color: String, text: String)                   extends Sym
case class CircleButton(color: String)                          extends Sym
case class CircleToggle(init: Boolean, color: String)           extends Sym

case class MultiToggle(init: Set[(Int,Int)], size: (Int, Int), color: String, texts: List[String]) extends Sym

case class HCheck(init: Int, size: Int, color: String, texts: List[String], allowDeselect: Boolean) extends Sym
case class VCheck(init: Int, size: Int, color: String, texts: List[String], allowDeselect: Boolean) extends Sym

case class XYPad(init: (Float, Float), color: String)           extends Sym

case class HFaderRange(init: (Float, Float), color: String)     extends Sym
case class VFaderRange(init: (Float, Float), color: String)     extends Sym
case class XYPadRange(initX: (Float, Float), initY: (Float, Float), color: String) extends Sym

case class DropDownList(init: Int, texts: List[String])         extends Sym
case class TextInput(init: Option[String], color: String, textLength: Int) extends Sym

case class FileInput(init: Option[File], color: String, text: String) extends Sym

case class Orient(isFirst: Boolean, isFirstHor: Boolean, isSecondHor: Boolean)
case class DoubleCheck(init: (Int, Int), sizes: List[Int], color1: String, color2: String, texts: List[(String, List[String])], orient: Orient, allowDeselect: Boolean) extends Sym

// -----------------------------------------

object Read {
    import Attr._

    def emptyUi = Ui(Space, Param(None, None))

    def fromSym(sym: Widget[Sym]): Widget[Ui] =
        Widget.lift2(Ui, sym, param)

    def dial    = Widget.prim(Names.dial,    lift3(Dial,   initFloat, color, rangeFloat))
    def hfader  = Widget.prim(Names.hfader,  lift3(HFader, initFloat, color, rangeFloat))
    def vfader  = Widget.prim(Names.vfader,  lift3(VFader, initFloat, color, rangeFloat))
    def toggle  = Widget.prim(Names.toggle,  lift3(Toggle, initBoolean, color, text))
    def intDial = Widget.prim(Names.intDial, lift3(IntDial, initInt, color, rangeInt))
    def label   = Widget.prim(Names.label,   lift2(Label,  color, text))
    def button  = Widget.prim(Names.button,  lift2(Button, color, text))
    def hcheck  = Widget.prim(Names.hcheck,  lift5(HCheck, initInt, size1, color, texts, allowDeselect))
    def vcheck  = Widget.prim(Names.vcheck,  lift5(VCheck, initInt, size1, color, texts, allowDeselect))
    def xyPad   = Widget.prim(Names.xyPad,   lift2(XYPad, initFloat2, color))
    def circleButton = Widget.prim(Names.circleButton, color.map(x => CircleButton(x)))
    def circleToggle = Widget.prim(Names.circleToggle, lift2(CircleToggle, initBoolean, color))

    def multiToggle = Widget.prim(Names.multiToggle, lift4(MultiToggle, initMultiToggle, multiToggleSize, color, texts))

    def hfaderRange  = Widget.prim(Names.hfaderRange,   lift2(HFaderRange, initRange, color))
    def vfaderRange  = Widget.prim(Names.vfaderRange,   lift2(VFaderRange, initRange, color))
    def xyPadRange   = Widget.prim(Names.xyPadRange, lift3(XYPadRange, initX, initY, color))

    def dropDownList = Widget.prim(Names.dropDownList, lift2(DropDownList, initInt, texts))
    def textInput = Widget.prim(Names.textInput, lift3(TextInput, initOptionString, color, textLength))

    def fileInput = Widget.prim(Names.fileInput, lift3(FileInput, initOptionFile, color, text))

    def doubleCheck = Widget.prim(Names.doubleCheck, lift7(DoubleCheck, initInt2, sizeList, color1, color2, doubleCheckTexts, orient, allowDeselect))

    def hor = list(Names.hor, Hor)
    def ver = list(Names.ver, Ver)

    def tabContent: Attr[Option[Ui]] = attr(Names.content, obj => ui.run(obj).map(x => Some(x)), None)
    def page: Widget[Option[Page]] = Widget.prim(Names.page, lift3((t: String, optCont: Option[Ui], keys: Keys) => optCont.map(cont => Page(t, cont, keys)), title, tabContent, HotKey.readAttr))
    def tabs = Widget.listBy[Tabs, Option[Page]](page)(Names.tabs, xs => Tabs(xs.flatten))

    def widgets: Stream[Widget[Sym]] =
        dial #::
        hfader #::
        vfader #::
        toggle #::
        intDial #::
        label #::
        button #::
        hcheck #::
        vcheck #::
        xyPad #::
        circleButton #::
        circleToggle #::
        hfaderRange #::
        vfaderRange #::
        xyPadRange #::
        dropDownList #::
        textInput #::
        multiToggle #::
        fileInput #::
        doubleCheck #::
        hor #::
        ver #::
        tabs #::
        Stream.empty[Widget[Sym]]

    def list[A](key: String, mk: List[Ui] => A) = Widget.listBy(ui)(key, mk)

    def param: Widget[Param] = {
        Widget.lift2(Param, Widget.fromOptionAttr(id), Widget.fromOptionAttr(Send.read))
    }

    def ui: Widget[Ui] = Widget.any(widgets.map(fromSym))

    def window: Widget[Window] = Widget.prim(Names.window, lift4(Window, title, size, windowContent, HotKey.readAttr))

    def windowContent: Attr[Ui] = attr(Names.content, obj => ui.run(obj), emptyUi)

    def root: Widget[Root] = Widget.lift4(Root, Widget.listBy(window)(Names.app, xs => xs), HotKey.read, Send.initMessages, Send.terminateMessages)
}

object GetOsc {

    def getOsc(root: Root): List[Client] =
        List( root.windows.flatMap(x => getOsc(x))
            , getOsc(root.hotKeys)
            , root.initMessages.flatMap(x => getOsc(x))).flatten

    def getOsc(win: Window): List[Client] =
        List( getOsc(win.content)
            , getOsc(win.hotKeys)).flatten

    def getOsc(keys: Keys): List[Client] =
        keys.keyEvents.flatMap(x => getOsc(x))

    def getOsc(msg: Msg): List[Client] = List(msg.client)

    def getOsc(ui: Ui): List[Client] =
        List( getOsc(ui.sym)
            , getOsc(ui.param)).flatten

    def getOsc(hkey: HotKeyEvent): List[Client] =
        getOsc(hkey.send)

    def getOsc(send: Send): List[Client] =
        List( send.default.flatMap(x => getOsc(x))
            , send.onValue.values.flatten.flatMap(x => getOsc(x))
            , send.onValueOff.values.flatten.flatMap(x => getOsc(x))
            ).flatten

    def getOsc(sym: Sym): List[Client] = sym match {
        case Hor(xs) => xs.flatMap(x => getOsc(x))
        case Ver(xs) => xs.flatMap(x => getOsc(x))
        case Tabs(items) => items.flatMap(x => getOsc(x))
        case _ => Nil
    }

    def getOsc(page: Page): List[Client] =
        List( getOsc(page.content)
            , getOsc(page.hotKeys)).flatten

    def getOsc(param: Param): List[Client] = param.osc match {
        case None => Nil
        case Some(a) => getOsc(a)
    }

}

