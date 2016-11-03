
import scala.swing.audio.parse.arg._
import dragon.osc.const.Names
import dragon.osc.act.Act

package scala.swing.audio.parse {

object UiDefaults {
    val defColor = "blue"
    val defTextColor = "black"
    val defInit = 0.5f
    val defBool = false

    def getColor(color: Option[String], settings: Settings) = 
        color orElse settings.initColor getOrElse defColor

    def getTextColor(color: Option[String], settings: Settings) = 
        color orElse settings.initColor getOrElse defTextColor

    def getInitFloat(init: Option[Float], settings: Settings) = 
        init orElse settings.initFloat getOrElse defInit

    def getInitBoolean(init: Option[Boolean], settings: Settings) = 
        init orElse settings.initBoolean getOrElse defBool

    def getCheckInit(init: Option[Int], allowDeselect: Boolean) = 
        init.getOrElse(if (allowDeselect) -1 else 0)
}

case class Settings(
    initColor: Option[String] = None, 
    initFloat: Option[Float] = None, 
    initBoolean: Option[Boolean] = None, 
    initInt: Option[Int] = None,
    title: Option[String] = None,
    oscClient: Option[Int] = None) {

    def setClientId(osc: OscAddress): OscAddress = oscClient match {
        case None => osc
        case Some(n) => osc.copy(clientId = Some(OutsideClientId(n.toString)))
    }

    def setClientId(osc: OscFloat): OscFloat = osc.copy(oscAddress = setClientId(osc.oscAddress))    
    def setClientId(osc: OscFloat2): OscFloat2 = osc.copy(oscAddress = setClientId(osc.oscAddress))    
    def setClientId(osc: OscBoolean): OscBoolean = osc.copy(oscAddress = setClientId(osc.oscAddress))    
    def setClientId(osc: OscInt): OscInt = osc.copy(oscAddress = setClientId(osc.oscAddress))   

    def setClientId(osc: DefaultOscSend): DefaultOscSend = osc match {
        case x: OscBoolean => setClientId(x)
        case x: OscFloat   => setClientId(x)
        case x: OscFloat2  => setClientId(x)
        case x: OscInt     => setClientId(x)
    }

    def setClientId(act: Option[Act]): Option[Act] = act.map { a => a.mapDefaultSend(x => this.setClientId(x)) }
}

case class Context(aliases: Map[String,Ui] = Map(), settings: Settings = Settings(), isHor: Boolean = false) {
    def setHor = this.copy(isHor = true)
    def setVer = this.copy(isHor = false)

    def addAlias(name: String, body: Ui) = this.copy(aliases = this.aliases + (name -> body))
    def loadAlias(name: String) = { this.aliases.get(name) }

    def setParam(setter: SetParam) = {
        val newSettings = setter match {
            case SetColor(name)  => settings.copy(initColor = Some(name))
            case SetInitFloat(x) => settings.copy(initFloat = Some(x))
            case SetInitInt(x) => settings.copy(initInt = Some(x))
            case SetInitBoolean(x) => settings.copy(initBoolean = Some(x))
            case SetTitle(name) => settings.copy(title = Some(name))
            case SetOscClient(x) => settings.copy(oscClient = Some(x))
            case _ => settings
        }
        this.copy(settings = newSettings)
    }
}

// Window and tabs
case class TabSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym

// Layout
case class HorSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class VerSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class SpaceSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym

// Widgets
case class LabelSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class DialSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class IntDialSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class ButtonSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class ToggleSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class HFaderSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class VFaderSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class MultiToggleSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class XYPadSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class HCheckSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class VCheckSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym

case class HFaderRangeSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class VFaderRangeSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class XYPadRangeSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym

case class DropDownListSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class TextInputSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym

// Control
case class AliasSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class RefSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym
case class SetParamSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym


case class WindowSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym

object WindowSym {
    def unapply(s: Sym): Option[(String, Option[(Int, Int)], UiDecl)] = 
        s.isName(Names.window).flatMap { 
            case UiMap(m) => for {
                body <- m.get("body")
                UiString(title) <- m.get("title")
                size = m.get("size").flatMap {
                    case UiList(List(UiInt(width), UiInt(height))) => Some((width, height))
                    case _ => None
                } 
            } yield (title, size, body) 
            
        }
}

object TabSym {
    def unapply(s: Sym): Option[List[(String, UiDecl)]] = {
        s.isNameList(Names.tabs).map { x => x.map { decl => decl match {
                case UiSym(sym) => Some((sym.name, sym.body))
                case _          => None
            }}.flatten
        }
    }
}

object AliasSym {
    def unapply(s: Sym): Option[(String, UiDecl)] = 
        s.isAlias
}

object RefSym {
    def unapply(s: Sym): Option[(String, List[UiDecl])] = s.isRef.map { args => (s.name, args) }
}

object SetParamSym {
    def unapply(s: Sym): Option[(SetParam, UiDecl)] = s.isSetter.map(x => (x, s.body))
}

object HorSym {
    def unapply(s: Sym): Option[List[UiDecl]] = 
        s.isNameList(Names.hor)
}

object VerSym {
    def unapply(s: Sym): Option[List[UiDecl]] = 
        s.isNameList(Names.ver)
}

object SpaceSym {
    def unapply(s: Sym): Option[Int] =             
        s.isArgList(Names.space) {
            for { n <- Arg.int } yield n
        }
}

object GlueSym {
    def unapply(s: Sym): Boolean = 
        !(s.isName(Names.glue).isEmpty)
}

object LabelSym {
    def unapply(s: Sym): Option[LabelUndef] = 
        s.isArgList(Names.label){
            for {
                text  <- Arg.string
                color <- Arg.string.orElse
            } yield LabelUndef(text, color)
        }
}

object DialSym {
    def unapply(s: Sym): Option[DialUndef] = 
        s.floatValue(Names.dial, DialUndef)
}

object IntDialSym {
    def unapply(s: Sym): Option[IntDialUndef] = 
        s.isArgList(Names.intDial){
            for {
                init   <- Arg.int
                minVal <- Arg.int
                maxVal <- Arg.int
                color  <- Arg.string.orElse                
                addr   <- Arg.oscAddress
            } yield IntDialUndef(init, (minVal, maxVal), color, OscInt(addr))
        }
}

object HFaderRangeSym {
    def unapply(s: Sym): Option[HFaderRangeUndef] = 
        s.floatRangeValue(Names.hfaderRange, HFaderRangeUndef)
}

object VFaderRangeSym {
    def unapply(s: Sym): Option[VFaderRangeUndef] = 
        s.floatRangeValue(Names.vfaderRange, VFaderRangeUndef)
}

object XYPadRangeSym {
    def unapply(s: Sym): Option[XYPadRangeUndef] =
        s.isArgList(Names.xyPadRange) {
            for {
                initX  <- Arg.float2
                initY  <- Arg.float2
                color  <- Arg.string.orElse
                addr   <- Arg.oscAddress
                rangeX <- Arg.float2.getOrElse((0.0f, 1.0f))
                rangeY <- Arg.float2.getOrElse((0.0f, 1.0f))
            } yield XYPadRangeUndef(initX, initY, color, OscFloat2(addr, rangeX, rangeY))
        }
}

object HFaderSym {
    def unapply(s: Sym): Option[HFaderUndef] = 
        s.floatValue(Names.hfader, HFaderUndef)
}

object VFaderSym {
    def unapply(s: Sym): Option[VFaderUndef] = 
        s.floatValue(Names.vfader, VFaderUndef)
}

object ButtonSym {
    def unapply(s: Sym): Option[ButtonUndef] = 
        s.isArgList(Names.button){
            for {
                color <- Arg.string.orElse
                text  <- Arg.string.orElse
                addr  <- Arg.oscAddress          
            } yield ButtonUndef(color, text, OscBoolean(addr))
        }
}

object ToggleSym {
    def unapply(s: Sym): Option[ToggleUndef] = 
        s.isArgList(Names.toggle){ 
            for {
                init  <- Arg.boolean.orElse
                color <- Arg.string.orElse
                text  <- Arg.string.orElse
                osc   <- Arg.oscAddress.map(OscBoolean).orElse
            } yield ToggleUndef(init, color, text, s.act.map(_.withDefaultSend(osc)))
        }
}

object MultiToggleSym {
    def unapply(s: Sym): Option[MultiToggleUndef] = 
        s.isArgList(Names.multiToggle){
            for {
                rows <- Arg.int
                cols <- Arg.int
                inits <- Arg.intListOrEmpty
                texts <- Arg.stringListOrEmpty
                color <- Arg.string.orElse
                textColor <- Arg.string.orElse
                oscAddress <- Arg.oscAddress
            } yield MultiToggleUndef((rows, cols), inits, texts, color, textColor, oscAddress)
        }               
}

object XYPadSym {
    def unapply(s: Sym): Option[XYPadUndef] = 
        s.isArgList(Names.xyPad){
            for {
                inits  <- Arg.float2.orElse
                color  <- Arg.string.orElse
                addr   <- Arg.oscAddress
                rangeX <- Arg.float2.getOrElse((0f, 1f))
                rangeY <- Arg.float2.getOrElse((0f, 1f))
            } yield XYPadUndef(inits, color, OscFloat2(addr, rangeX, rangeY))
        }
}


object Check {
    def arg[A](mk: (Option[Int], Int, Option[String], List[String], Option[Boolean], OscInt) => A): Arg[A] = for {
        init  <- Arg.int.orElse
        size  <- Arg.int
        color <- Arg.string.orElse
        text  <- Arg.stringListOrEmpty
        allowDeselect <- Arg.boolean.orElse
        addr  <- Arg.oscAddress
    } yield mk(init, size, color, text, allowDeselect, OscInt(addr))
}

object HCheckSym {
    def unapply(s: Sym): Option[HCheckUndef] = 
        s.isArgList(Names.hcheck)(Check.arg(HCheckUndef))
}

object VCheckSym {
    def unapply(s: Sym): Option[VCheckUndef] = 
        s.isArgList(Names.vcheck)(Check.arg(VCheckUndef))
}

object DropDownListSym {
    def unapply(s: Sym): Option[DropDownList] = 
        s.isArgList(Names.dropDownList){
            for {
                init  <- Arg.int.getOrElse(0)
                names <- Arg.stringList
                osc   <- Arg.oscAddress
            } yield DropDownList(init, names, OscInt(osc))
        }
}

object TextInputSym {
    def unapply(s: Sym): Option[TextInputUndef] = 
        s.isArgList(Names.textInput){
            for {
                init  <- Arg.string.orElse
                color <- Arg.string.orElse
                osc   <- Arg.oscAddress
            } yield TextInputUndef(init, color, osc)
        }
}

trait Ui

case class WithId(id: String, ui: Ui) extends Ui
case class Tab(items: List[(String, Ui)]) extends Ui
case class Hor(items: List[Ui]) extends Ui
case class Ver(items: List[Ui]) extends Ui
case class HSpace(n: Int) extends Ui
case class VSpace(n: Int) extends Ui
object HGlue extends Ui
object VGlue extends Ui

case class LabelUndef(text: String, color: Option[String]) extends Ui
case class ButtonUndef(color: Option[String], text: Option[String], osc: OscBoolean) extends Ui

case class ToggleUndef(init: Option[Boolean], color: Option[String], text: Option[String], act: Option[Act]) extends Ui

case class DialUndef(init: Option[Float], color: Option[String], osc: OscFloat) extends Ui
case class IntDialUndef(init: Int, range: (Int, Int), color: Option[String], osc: OscInt) extends Ui
case class VFaderUndef(init: Option[Float], color: Option[String], osc: OscFloat) extends Ui
case class HFaderUndef(init: Option[Float], color: Option[String], osc: OscFloat) extends Ui 
case class MultiToggleUndef(size: (Int, Int), init: List[Int], texts: List[String], color: Option[String], textColor: Option[String], oscAddress: OscAddress) extends Ui
case class XYPadUndef(init: Option[(Float, Float)], color: Option[String], osc: OscFloat2) extends Ui
case class HFaderRangeUndef(init: (Float, Float), color: Option[String], osc: OscFloat) extends Ui
case class VFaderRangeUndef(init: (Float, Float), color: Option[String], osc: OscFloat) extends Ui
case class XYPadRangeUndef(initX: (Float, Float), initY: (Float, Float), color: Option[String], osc: OscFloat2) extends Ui
case class DropDownList(init: Int, names: List[String], osc: OscInt) extends Ui
case class TextInputUndef(init: Option[String], color: Option[String], oscAddress: OscAddress) extends Ui

trait CheckUndef {
    def init: Option[Int]
    def size: Int
    def color: Option[String]
    def text: List[String]
    def allowDeselect: Option[Boolean]
    def osc: OscInt 
}

case class HCheckUndef(init: Option[Int], size: Int, color: Option[String], text: List[String], allowDeselect: Option[Boolean], osc: OscInt) extends CheckUndef with Ui
case class VCheckUndef(init: Option[Int], size: Int, color: Option[String], text: List[String], allowDeselect: Option[Boolean], osc: OscInt) extends CheckUndef with Ui


case class Label(text: String, color: String) extends Ui
case class Button(color: String, text: Option[String], osc: OscBoolean) extends Ui
case class Toggle(init: Boolean, color: String, text: Option[String], act: Option[Act]) extends Ui

case class Dial(init: Float, color: String, osc: OscFloat) extends Ui
case class IntDial(init: Int, range: (Int, Int), color: String, osc: OscInt) extends Ui
case class VFader(init: Float, color: String, osc: OscFloat) extends Ui
case class HFader(init: Float, color: String, osc: OscFloat) extends Ui 
case class MultiToggle(size: (Int, Int), init: List[Int], texts: List[String], color: String, textColor: String, oscAddr: OscAddress) extends Ui
case class XYPad(init: (Float, Float), color: String, osc: OscFloat2) extends Ui
case class HCheck(init: Int, size: Int, color: String, text: List[String], allowDeselect: Boolean, osc: OscInt) extends Ui
case class VCheck(init: Int, size: Int, color: String, text: List[String], allowDeselect: Boolean, osc: OscInt) extends Ui
case class HFaderRange(init: (Float, Float), color: String, osc: OscFloat) extends Ui
case class VFaderRange(init: (Float, Float), color: String, osc: OscFloat) extends Ui
case class XYPadRange(initX: (Float, Float), initY: (Float, Float), color: String, osc: OscFloat2) extends Ui
case class TextInput(init: Option[String], color: String, oscAddress: OscAddress) extends Ui

case class Root(items: List[Ui]) extends Ui
case class Window(title: String, size: Option[(Int, Int)], items: Ui) extends Ui

object ReadUI {
    def contextFlatMap[A,B](a: State[Context,A])(f: A => State[Context,B]): State[Context,B] = new State[Context,B] {
        def run(ctx: Context) = a.run(ctx) match {
            case (a, s1) => f(a).run(ctx.copy(aliases = s1.aliases))
        }
    }

    def contextMapM[A,B](xs: List[A])(f: A => State[Context,B]): State[Context,List[B]] = xs match {
        case Nil => State.pure[Context,List[B]](Nil)
        case a :: as => contextFlatMap(f(a))(hd => contextMapM(as)(f).map(tl => hd :: tl))
    }

    def readListUi(concat: List[Ui] => Ui, items: List[UiDecl]): State[Context, Option[Ui]] = 
        contextMapM(items)(a => readUi(a)).map(x => Some(concat(x.flatten)))        

    def setFloatValue[A](x: { val init: Option[Float]; val color: Option[String]; val osc: OscFloat }, mkValue: (Float, String, OscFloat) => A)(settings: Settings) = 
        mkValue(UiDefaults.getInitFloat(x.init, settings), UiDefaults.getColor(x.color, settings), settings.setClientId(x.osc))

    def setFloatRangeValue[A](x: { val init: (Float, Float); val color: Option[String]; val osc: OscFloat }, mkValue: ((Float, Float), String, OscFloat) => A)(settings: Settings) = 
        mkValue(x.init, UiDefaults.getColor(x.color, settings), settings.setClientId(x.osc))

    def setButtonValue(x: ButtonUndef)(settings: Settings) = 
        Button(UiDefaults.getColor(x.color, settings), x.text, settings.setClientId(x.osc))

    def setToggleValue(x: ToggleUndef)(settings: Settings) = 
        Toggle(UiDefaults.getInitBoolean(x.init, settings), UiDefaults.getColor(x.color, settings), x.text, settings.setClientId(x.act))

    def setLabelValue(x: LabelUndef)(settings: Settings) =
        Label(x.text, x.color.getOrElse(UiDefaults.defTextColor))

    def setMultiToggleValue(x: MultiToggleUndef)(settings: Settings) =
        MultiToggle(x.size, x.init, x.texts, UiDefaults.getColor(x.color, settings), UiDefaults.getTextColor(x.textColor, settings), settings.setClientId(x.oscAddress))

    def setXYPad(x: XYPadUndef)(settings: Settings) = 
        XYPad(x.init.getOrElse((0.5f, 0.5f)), UiDefaults.getColor(x.color, settings), settings.setClientId(x.osc))

    def setCheck(x: CheckUndef, mk: (Int, Int, String, List[String], Boolean, OscInt) => Ui)(settings: Settings) = {
        val allowDeselect = x.allowDeselect.getOrElse(false)
        mk(UiDefaults.getCheckInit(x.init, allowDeselect), x.size, UiDefaults.getColor(x.color, settings), x.text, allowDeselect, settings.setClientId(x.osc))
    }

    def setXYPadRange(x: XYPadRangeUndef)(settings: Settings) = 
        XYPadRange(x.initX, x.initY, UiDefaults.getColor(x.color, settings), settings.setClientId(x.osc))

    def setTextInput(x: TextInputUndef)(settings: Settings) = 
        TextInput(x.init, UiDefaults.getColor(x.color, settings), settings.setClientId(x.oscAddress))

    def setIntDial(x: IntDialUndef)(settings: Settings) =
        IntDial(x.init, x.range, UiDefaults.getColor(x.color, settings), settings.setClientId(x.osc))

    def withSettings(f: Settings => Ui): State[Context, Option[Ui]] = 
        State.get.map(ctx => Some(f(ctx.settings)))

    def pure(a: Ui): State[Context, Option[Ui]] = 
        State.get.map(ctx => (Some(a)))

    val none: State[Context, Option[Ui]] = State.pure(None)

    def withOrient[A](onHor: A, onVer: A): State[Context, Option[A]] = 
        State.get.map{ context => 
            if (context.isHor) Some(onHor)
            else Some(onVer)
    }

    def wrapId(id: Option[String], ui: State[Context,Option[Ui]]) = 
        ui.map(x => id match {
            case None => x
            case Some(name) => x.map(y => WithId(name, y))
        })

    def setParam(setter: SetParam) = State.modify[Context](ctx => ctx.setParam(setter))

    def addAlias(name: String, body: Ui): State[Context,Unit] = 
        State.modify(context => context.addAlias(name, body))

    def loadAlias(name: String) = State.get[Context].map { context => context.loadAlias(name) }

    def readUi(x: UiDecl): State[Context, Option[Ui]] = x match {
        case UiList(items) => readListUi(Root, items)
        case UiSym(sym) => wrapId(sym.id, sym match {
            case HorSym(xs) => State.modify((x: Context) => x.setHor).next(readListUi(Hor, xs))
            case VerSym(xs) => State.modify((x: Context) => x.setVer).next(readListUi(Ver, xs))
            case SpaceSym(n) => withOrient(HSpace(n), VSpace(n))
            case sym @ GlueSym() => withOrient(HGlue, VGlue)
            
            case DialSym(d)   => withSettings(setFloatValue(d, Dial))
            case HFaderSym(d) => withSettings(setFloatValue(d, HFader))
            case VFaderSym(d) => withSettings(setFloatValue(d, VFader))
            case IntDialSym(d) => withSettings(setIntDial(d))

            case ButtonSym(d) => withSettings(setButtonValue(d))
            case ToggleSym(d) => withSettings(setToggleValue(d))
            case LabelSym(d)  => withSettings(setLabelValue(d))
            case MultiToggleSym(d) => withSettings(setMultiToggleValue(d))
            case XYPadSym(d) => withSettings(setXYPad(d))
            case HCheckSym(d) => withSettings(setCheck(d, HCheck))
            case VCheckSym(d) => withSettings(setCheck(d, VCheck))

            case HFaderRangeSym(d) => withSettings(setFloatRangeValue(d, HFaderRange))
            case VFaderRangeSym(d) => withSettings(setFloatRangeValue(d, VFaderRange))
            case XYPadRangeSym(d) => withSettings(setXYPadRange(d))

            case TextInputSym(d) => withSettings(setTextInput(d))

            case DropDownListSym(d) => pure(d)

            case SetParamSym(setter, body) => setParam(setter).next(readUi(body))

            case TabSym(xs) => contextMapM(xs) { case (name, body) => readUi(body).map(x => x.map(content => (name, content))) }.map(xs => Some(Tab(xs.flatten)))   // (xs.map({ case (name, body) =>readUi(body).map(x => x.map(content => (name, content))) }))(x => State.pure(x))

            case WindowSym(title, size, body) => readUi(body).map(x => x.map(y => Window(title, size, y)))

            case AliasSym(name, body) => State.get.flatMap { st => readUi(body).eval(st) match {
                case None => State.pure[Context,Option[Ui]](None)
                case Some(x) => addAlias(name, x).next(State.pure[Context,Option[Ui]](None))
            }}

            case RefSym(name, args) => loadAlias(name)           

            case _ => none                        
        })
        case UiString(name) => loadAlias(name)
    }
}

}

