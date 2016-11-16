package dragon.osc.ui

import scala.swing._
import scala.swing.event._
import java.awt.{Color,Graphics2D,BasicStroke,Font}
import java.awt.geom._

import scala.swing.audio.ui._
import scala.audio.osc._

import dragon.osc.state._

import dragon.osc.color._
import dragon.osc.parse.{ui => P}
import dragon.osc.parse.send._
import dragon.osc.send._

case class Root(windows: List[Window]) {
    def show(onClose: => Unit) = windows.foreach(_.show(onClose))
}

case class Window(title: String, size: Option[(Int,Int)], content: Component) { 
    def show(onClose: => Unit) = {
        val window = this
        val ui = new MainFrame { self => 
            title = window.title
            contents = window.content

            window.size.foreach { case (width, height) =>                 
                self.minimumSize = new Dimension(width, height)
            }
            
            override def closeOperation {                
                println("Close now")
                onClose                
                Thread.sleep(1)          
                System.exit(0)                
            }
        }
        ui.visible = true        
    }
}

case class Context(isHor: Boolean = true, idMap: IdMap = IdMap(Map[String,Component]())) {
    def setHor = this.copy(isHor = true)
    def setVer = this.copy(isHor = false)
}

case class IdMap(m: Map[String,Component])

object Convert {      
    def convert(st: St, app: P.Root): (Root, IdMap) = {        
        val res = convert(st)(app).run(Context())
        (res._1, res._2.idMap)
    }

    def convert(st: St)(app: P.Root): State[Context,Root] = 
        State.mapM(app.windows)(convertWindow(st)).map(Root)

    def convertWindow(st: St)(window: P.Window): State[Context,Window] = 
        convertUi(st)(window.content).map(ui => Window(window.title, window.size, ui))

    def convertUi(st: St)(ui: P.Ui): State[Context, Component] = 
        mkComponent(st)(ui).map { widget => {
            registerId(st, widget, ui.param.id)
            widget
        }}    

    def registerId(st: St, ui: Component, optId: Option[String]) {
        optId.foreach(id => st.memory.register(id, ui))
    }

    def pure[A](a: A) = State.pure[Context,A](a)
    def modify(f: Context => Context) = State.modify[Context](f)
    def group(mk: List[Component] => Component, xs: List[P.Ui], st: St): State[Context,Component] =
        State.mapM(xs)(convertUi(st)).map(mk)

    def mkTabs(st: St, xs: List[P.Page]) = 
        State.mapM(xs)({ page => convertUi(st)(page.content).map(x => (page.title, x))}).map(Util.tabs)

    def mkComponent(st: St)(ui: P.Ui): State[Context, Component] = {
        import Palette._
        import Codec._

        val send = ui.param.osc //  ui.param.osc.map(st.compileSend).getOrElse(defaultCallback _)
        ui.sym match {
            case P.Hor(xs) => modify(_.setHor).next(group(hor, xs, st))
            case P.Ver(xs) => modify(_.setVer).next(group(ver, xs, st))
            case P.Tabs(xs) => mkTabs(st, xs)

            case P.Dial(init, color)                => pure(Dial(init, palette(color))(onFloat(st, send)))
            case P.HFader(init, color)              => pure(HFader(init, palette(color))(onFloat(st, send)))
            case P.VFader(init, color)              => pure(VFader(init, palette(color))(onFloat(st, send)))
            case P.Toggle(init, color, text)        => pure(ToggleButton(init, palette(color), Some(text))(onBoolean(st, send)))
            case P.IntDial(init, color, range)      => pure(IntDial(init, range, palette(color))(onInt(st, send)))
        }
    }       
}
