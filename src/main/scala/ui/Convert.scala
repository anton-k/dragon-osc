package dragon.osc.ui

import scala.swing.{Component,Swing,MainFrame,Dimension}
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

case class Context(isHor: Boolean = true) {
    def setHor = this.copy(isHor = true)
    def setVer = this.copy(isHor = false)
}

object Convert {      
    def convert(st: St, app: P.Root): Root = 
        convert(st)(app).eval(Context())        

    def convert(st: St)(app: P.Root): State[Context,Root] = 
        State.mapM(app.windows)(convertWindow(st)).map(Root)

    def convertWindow(st: St)(window: P.Window): State[Context,Window] = 
        convertUi(st)(window.content).map(ui => Window(window.title, window.size, ui))

    def pure[A](a: A) = State.pure[Context,A](a)
    def modify(f: Context => Context) = State.modify[Context](f)
    def group(mk: List[Component] => Component, xs: List[P.Ui], st: St): State[Context,Component] =
        State.mapM(xs)(convertUi(st)).map(mk)

    def withOrient[A](onHor: A, onVer: A): State[Context,A] = State.get.map(ctx => if (ctx.isHor) onHor else onVer)

    def mkTabs(st: St, xs: List[P.Page]) = 
        State.mapM(xs)({ page => convertUi(st)(page.content).map(x => (page.title, x))}).map(Util.tabs)

    def withListener[B, A <: Component with SetWidget[B]](st: St)(optId: Option[String], widget: A)(implicit codec: MessageCodec[B]): Component = {
        optId.foreach(id => st.addListener(id, widget)(codec))
        widget
    }

    def convertUi(st: St)(ui: P.Ui): State[Context, Component] = {
        import Palette._
        import Codec._
        val id = ui.param.id
        val send = ui.param.osc

        def listen[B, A <: Component with SetWidget[B]](widget: A)(implicit codec: MessageCodec[B]): State[Context,Component] = 
            pure(withListener[B,A](st)(id, widget))

        ui.sym match {
            case P.Hor(xs)      => modify(_.setHor).next(group(hor, xs, st))
            case P.Ver(xs)      => modify(_.setVer).next(group(ver, xs, st))
            case P.Tabs(xs)     => mkTabs(st, xs)
            case P.Space                            => withOrient(Swing.HStrut(10), Swing.VStrut(10))

            case P.Dial(init, color)                => listen[Float,Dial](Dial(init, palette(color))(onFloat(st, send)))
            case P.HFader(init, color)              => listen[Float,HFader](HFader(init, palette(color))(onFloat(st, send)))
            case P.VFader(init, color)              => listen[Float,VFader](VFader(init, palette(color))(onFloat(st, send)))
            case P.Toggle(init, color, text)        => listen[Boolean,ToggleButton](ToggleButton(init, palette(color), Some(text))(onBoolean(st, send)))
            case P.IntDial(init, color, range)      => listen[Int,IntDial](IntDial(init, range, palette(color))(onInt(st, send)))                     
        }
    }       
}
