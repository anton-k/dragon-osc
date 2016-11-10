package dragon.osc.widget

import scala.swing._
import scala.swing.event._
import java.awt.{Color,Graphics2D,BasicStroke}

import scala.swing.audio.ui._

import dragon.osc.act._
import dragon.osc.const._
import scala.swing.audio.parse
import scala.swing.audio.parse.arg._

import scala.swing.audio.ui._
import dragon.osc.state._

case class Window(title: Option[String], size: Option[(Int, Int)], content: Component)

case class Context(settings: Settings = Settings(), isHor: Boolean = false) {
    def setHor = this.copy(isHor = true)
    def setVer = this.copy(isHor = false)

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

trait Widget {   
    val key: String
    def arg(optActs: Option[Act], st: St): Arg[Component]
}

object Util {
    def booleanDefaultSend(addr: Option[OscAddress]): List[Msg] = ???
}

case class PrimSym(name: String, id: Option[String], body: UiDecl, act: Option[Act]) extends Sym

object PrimSym {
    def unapply(s: Sym): Option[(String, List[UiDecl], Option[Act])] = s.body match {
        case UiList(items) => Some((s.name, items, s.act))
        case _ => None
    }
}

object Widget {
    def contextMapM[A,B](xs: List[A])(f: A => State[Context,B]): State[Context,List[B]] = xs match {
        case Nil => State.pure[Context,List[B]](Nil)
        case a :: as => f(a).flatMap(hd => contextMapM(as)(f).map(tl => hd :: tl))
    }

    def readListUi(st: St)(concat: List[Component] => Component, items: List[UiDecl]): State[Context, Component] = 
        contextMapM(items)(readUi(st)).map(concat)

    def withSettings: State[Context,Settings] = State.get.map(_.settings)

    def withOrient[A](onHor: A, onVer: A): State[Context, Option[A]] = 
        State.get.map{ context => 
            if (context.isHor) Some(onHor)
            else Some(onVer)
        }

    def readFile(st: St, filename: String) = readUi(st)(UiDecl.loadFile(filename)).eval(Context())

    def readUi(st: St)(x: UiDecl): State[Context, Component] = x match { 
        case UiList(items) => readListUi(st)(hor, items)
        case UiSym(sym) => sym match {
            //case HorSym(items) => State.modify((x: Context) => x.setHor).next(readListUi(st)(hor, items))
            //case VerSym(items) => State.modify((x: Context) => x.setVer).next(readListUi(st)(ver, items))
            //case SpaceSym(n) => withOrient(Swing.HStrut(n), Swing.VStrut(n))
            //case sym @ GlueSym() => withOrient(Swing.HGlue, Swing.VGlue)
            case PrimSym(name, items, acts) => withSettings.map( settings => widgets.get(name).flatMap(w => w.arg(acts, st).eval(settings, items)).getOrElse(Swing.HGlue))  
        } 
    }   

    val toggle = new Widget {
        val key = Names.toggle

        def arg(optActs: Option[Act], st: St) =
            for {
                init  <- Arg.initBoolean
                color <- Arg.color
                text  <- Arg.string.orElse
                osc   <- Arg.oscAddress.map(OscBoolean).orElse
            } yield {
                val acts = optActs.map(_.withDefaultSend(osc).compileToggle)
                ToggleButton(init, color, text) { x => acts.foreach(_.act(x, st)) }
            }
    }

    val intDial = new Widget {
        val key = Names.intDial

        def arg(optActs: Option[Act], st: St) = 
            for {
                init   <- Arg.int
                minVal <- Arg.int
                maxVal <- Arg.int
                color  <- Arg.color
                osc    <- Arg.oscAddress.map(OscInt).orElse
            } yield {
                val acts = optActs.map(_.withDefaultSend(osc).compileIntDial)
                IntDial(init, (minVal, maxVal), color){ n => acts.foreach(_.act(n, st)) }
            }  
    }

    val dial   = floatWidget(Names.dial,   (init, color) => f => Dial(init, color)(f))
    val hfader = floatWidget(Names.hfader, (init, color) => f => HFader(init, color)(f))
    val vfader = floatWidget(Names.vfader, (init, color) => f => VFader(init, color)(f))

    def floatWidget(name: String, mk: (Float, Color) => (Float => Unit) => Component) = new Widget {
        val key = name

        def arg(optActs: Option[Act], st: St) =
            for {
                init   <- Arg.initFloat
                color  <- Arg.color
                osc    <- Arg.oscAddress.map(x => OscFloat(x, (0, 1))).orElse
                // range  <- Arg.float2.getOrElse((0.0f, 1.0f))
            } yield {
                val acts = optActs.map(_.withDefaultSend(osc).compileFloat)
                mk(init, color) { x => acts.foreach(_.act(x, st)) }
            }
    }

    val widgets: Map[String, Widget] = Map[String, Widget](List(toggle, intDial, dial, hfader, vfader).map(x => (x.key -> x)): _*)
}


//case class Toggle(init: Option[Boolean], color: Option[String], text: Option[String], act: Option[Act])