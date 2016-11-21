package dragon.osc.ui

import scala.swing.{Component,Swing,MainFrame,Dimension,BoxPanel,Orientation}
import scala.swing.audio.ui._
import scala.audio.osc._
import scala.swing.event._
import java.awt.event._

import dragon.osc.state._
import dragon.osc.color._
import dragon.osc.parse.{ui => P}
import dragon.osc.parse.send._
import dragon.osc.parse.hotkey._
import dragon.osc.send._

case class Root(windows: List[Window]) {
    def show(st: St) = windows.foreach(_.show(st, st.close))
}

case class Window(title: String, size: Option[(Int,Int)], content: Component, hotKeys: WindowKeys) { 
    def show(st: St, onClose: => Unit) = {
        val window = this
        val ui = new MainFrame { self => 
            title = window.title

            contents = new BoxPanel(Orientation.Vertical) {
                listenTo(keys)

                contents += window.content 
                reactions += {
                    case KeyPressed(_, key, _, _) => hotKeys.act(st, HotKey(Nil, key))
                }
                focusable = true
                requestFocus
            }           

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
        State.mapM(app.windows)(convertWindow(st, WindowKeys.fromRootKeys(st, app.hotKeys))).map(Root)

    def convertWindow(st: St, rootKeys: WindowKeys)(window: P.Window): State[Context,Window] = {
        val winKeys = rootKeys.appendWindowKeys(st, window.hotKeys)
        convertUi(st, winKeys)(window.content).map(ui => Window(window.title, window.size, ui, winKeys))
    }

    def pure[A](a: A) = State.pure[Context,A](a)
    def modify(f: Context => Context) = State.modify[Context](f)
    def group(mk: List[Component] => Component, xs: List[P.Ui], st: St, keys: WindowKeys): State[Context,Component] =
        State.mapM(xs)(convertUi(st, keys)).map(mk)

    def withOrient[A](onHor: A, onVer: A): State[Context,A] = State.get.map(ctx => if (ctx.isHor) onHor else onVer)

    def mkTabs(st: St, keys: WindowKeys, xs: List[P.Page]) = 
        State.mapM(xs)({ page => convertUi(st, keys)(page.content).map(x => (page.title, x))}).map(Util.tabs)

    def withListener[B, A <: Component with SetWidget[B] with SetColor](st: St)(optId: Option[String], widget: A)(implicit codec: MessageCodec[B]): A = {
        optId.foreach(id => st.addListener(id, widget)(codec))
        widget
    }

    def withToggleListener[A <: Component with SetWidget[Boolean] with GetWidget[Boolean] with SetColor](st: St)(optId: Option[String], widget: A)(implicit codec: MessageCodec[Boolean]): A = {        
        val widget1 = withListener[Boolean,A](st)(optId, widget)             
        optId.foreach(id => st.addToggleListener(id, widget1))
        widget1
    }

    def withFloatListener[A <: Component with SetWidget[Float] with GetWidget[Float] with SetColor](st: St)(optId: Option[String], widget: A)(implicit codec: MessageCodec[Float]): A = {
        val widget1 = withListener[Float,A](st)(optId, widget)
        optId.foreach(id => st.addFloatListener(id, widget1)(codec))      
        widget1        
    } 

    def withIntListener[A <: Component with SetWidget[Int] with GetWidget[Int] with SetColor](st: St)(optId: Option[String], widget: A)(implicit codec: MessageCodec[Int]): A = {
        val widget1 = withListener[Int,A](st)(optId, widget)
        optId.foreach(id => st.addIntListener(id, widget1)(codec))
        widget1        
    } 


    def convertUi(st: St, keys: WindowKeys)(ui: P.Ui): State[Context, Component] = {
        import Palette._
        import Codec._
        val id = ui.param.id
        val send = ui.param.osc

        def listen[B, A <: Component with SetWidget[B] with SetColor](widget: A)(implicit codec: MessageCodec[B]): State[Context,Component] = 
            pure(withListener[B,A](st)(id, widget))

        def listenFloat[A <: Component with SetWidget[Float] with GetWidget[Float] with SetColor](widget: A)(implicit codec: MessageCodec[Float]): State[Context,Component] = 
            pure(withFloatListener[A](st)(id, widget))

        def listenInt[A <: Component with SetWidget[Int] with GetWidget[Int] with SetColor](widget: A)(implicit codec: MessageCodec[Int]): State[Context,Component] = 
            pure(withIntListener[A](st)(id, widget))

        ui.sym match {
            case P.Hor(xs)      => modify(_.setHor).next(group(hor, xs, st, keys))
            case P.Ver(xs)      => modify(_.setVer).next(group(ver, xs, st, keys))
            case P.Tabs(xs)     => mkTabs(st, keys, xs)
            case P.Space                            => withOrient(Swing.HStrut(10), Swing.VStrut(10))

            case P.Dial(init, color)                => listenFloat(Dial(init, palette(color))(onFloat(st, send)))
            case P.HFader(init, color)              => listenFloat(HFader(init, palette(color))(onFloat(st, send)))
            case P.VFader(init, color)              => listenFloat(VFader(init, palette(color))(onFloat(st, send)))
            case P.Toggle(init, color, text)        => pure(withToggleListener(st)(id, ToggleButton(init, palette(color), Some(text))(onBoolean(st, send))))
            case P.IntDial(init, color, range)      => listenInt(IntDial(init, range, palette(color))(onInt(st, send)))                     
        }
    }       
}
