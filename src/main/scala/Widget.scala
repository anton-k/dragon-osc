package dragon.osc.widget

import scala.swing._

import scala.swing.audio.ui._

import dragon.osc.act._
import dragon.osc.const._
import scala.swing.audio.parse.Settings
import scala.swing.audio.parse.arg._

trait Widget {   
    val key: String
    def arg(optActs: Option[Act], st: St): Arg[Component]
}

object Util {
    def booleanDefaultSend(addr: Option[OscAddress]): List[Msg] = ???
}

object Widget {
    val widgets = List(toggle, intDial, dial, hfader, vfader).map(x => (x.key -> x.arg _)).toMap

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
}

//case class Toggle(init: Option[Boolean], color: Option[String], text: Option[String], act: Option[Act])