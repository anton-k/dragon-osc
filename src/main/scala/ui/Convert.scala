package dragon.osc.convert.ui

import scala.swing._
import scala.swing.event._

import scala.swing.audio.ui._
import dragon.osc.state._

import dragon.osc.color._
import dragon.osc.parse.{ui => P}


case class Root(widows: List[Window])
case class Window(title: String, size: Option[(Int,Int)], content: Component)

case class Context(isHor: Boolean = true, idMap: IdMap)

case class IdMap(m: Map[String,Component])

object Convert {    
    def convert(app: P.Root): State[Context,Root] = 
        State.mapM(app.windows)(convertWindow).map(Root)

    def convertWindow(window: P.Window): State[Context,Window] = 
        convertUi(window.content).map(ui => Window(window.title, window.size, ui))

    def convertUi(ui: P.Ui): State[Context, Component] = {
/*        val id  = ui.param.id
        val osc = ui.param.osc

        val idMap = updateIdMap(id, context.idMap)

        = ui.sym match {
            case P.Dial(init, color) => Dial(init, Color.palette(color))
            case P.HFader(init, color) => HFader(init, Color.palette(color))
            case P.VFader(init, color) => HFader(init, Color.palette(color))
        }
*/        
        ???
    }
}