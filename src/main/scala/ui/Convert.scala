package dragon.osc.ui

import scala.swing.{Component,Swing,MainFrame,Dimension,BoxPanel,Orientation,Dialog}
import scala.swing.audio.ui._
import scala.audio.osc._
import scala.swing.event._
import java.awt.event._

import dragon.osc.state._
import dragon.osc.color._
import dragon.osc.parse.{ui => P}
import dragon.osc.parse.send._
import dragon.osc.parse.const._
import dragon.osc.parse.hotkey._
import dragon.osc.send._
import dragon.osc.readargs._

case class Root(windows: List[Window]) {
    var isTerminated = false

    def runTerminateMessages(st: St, root: P.Root) {
        if (!isTerminated) {
            st.sendNoInputMsgs(root.terminateMessages)
            isTerminated = true
        }
    }

    def show(st: St, args: Args, onTerminate: => Unit) = windows.foreach(_.show(args, st, {onTerminate;  st.close}))
}

case class Window(title: String, size: Option[(Int,Int)], content: Component, hotKeys: WindowKeys) {
    def show(args: Args, st: St, onClose: => Unit) = {
        val window = this
        val ui = new MainFrame { self =>
            import javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE
            peer.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE)
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
              def uncaughtException(t: Thread, e: Throwable) {
                println("Uncaught exception in thread: " + t.getName, e)
              }
            })

            title = window.title

            contents = new BoxPanel(Orientation.Vertical) {
                listenTo(keys)

                contents += window.content
                reactions += {
                    case keyPressed@KeyPressed(_, _, _, _) => hotKeys.act(st, HotKey.fromKeyPress(keyPressed))
                }
                focusable = true
                requestFocus
            }

            window.size.foreach { case (width, height) =>
                self.minimumSize = new Dimension(width, height)
            }

            override def closeOperation {
                if (!args.lockClose) {
                    terminate
                } else {
                    val r = Dialog.showInput(contents.head, s"The app works in lock mode. Type ${Names.unlockPass} to close the app", initial="")
                    r match {
                      case Some(str) => if (str == Names.unlockPass) { terminate }
                      case None =>
                    }
                }
            }

            def terminate {
                println("Close now")
                onClose
                System.exit(0)
            }
        }
        ui.visible = true
        // ui.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE )
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

    def mkTabs(st: St, send: Option[Send], keys: WindowKeys, xs: List[P.Page]) = {
        val hotKeys = xs.map(page => st.getKeyMap(page.hotKeys)).toArray
        val size = hotKeys.length

        def cbk(n: Int) = {
            Codec.onInt(st, send)(n)
            if (n < size) {
                keys.setSpecific(hotKeys(n))
            }
        }

        State.mapM(xs)({ page => convertUi(st, keys)(page.content).map(x => (page.title, x))}).map(pages => Util.tabs(pages, cbk))
    }



    def convertUi(st: St, keys: WindowKeys)(ui: P.Ui): State[Context, Component] = {
        import Palette._
        import Codec._
        val id = ui.param.id
        val send = ui.param.osc
        val listen = Listener(st, id)

        ui.sym match {
            case P.Hor(xs)      => modify(_.setHor).next(group(hor, xs, st, keys))
            case P.Ver(xs)      => modify(_.setVer).next(group(ver, xs, st, keys))
            case P.Tabs(xs)     => mkTabs(st, send, keys, xs).flatMap(listen.int)
            case P.Space                            => withOrient(Swing.HStrut(10), Swing.VStrut(10))

            case P.Dial(init, color, range)         => listen.float(Dial(init, palette(color), range)(onFloat(st, send)))
            case P.HFader(init, color, range)       => listen.float(HFader(init, palette(color), range)(onFloat(st, send)))
            case P.VFader(init, color, range)       => listen.float(VFader(init, palette(color), range)(onFloat(st, send)))
            case P.Toggle(init, color, text)        => listen.toggle(ToggleButton(init, palette(color), Some(text))(onBoolean(st, send))).map(listen.text)
            case P.Button(color, text)              => listen.pure[Unit,PushButton](PushButton(palette(color), Some(text))(onButton(st, send))).map(listen.text)
            case P.Label(color, text)               => pure(Text(text, palette(color))).map(listen.text)
            case P.IntDial(init, color, range)      => listen.int(IntDial(init, range, palette(color))(onInt(st, send)))
            case P.HCheck(init, len, color, texts, allowDeselect)  => listen.int(HCheck(init, len, palette(color), texts, allowDeselect)(onIntWithDeselect(init, st, send))).map(listen.textList)
            case P.VCheck(init, len, color, texts, allowDeselect)  => listen.int(VCheck(init, len, palette(color), texts, allowDeselect)(onIntWithDeselect(init, st, send))).map(listen.textList)
            case P.CircleButton(color)              => listen.pure[Unit,CirclePushButton](CirclePushButton(palette(color))(onButton(st, send)))
            case P.CircleToggle(init, color)        => listen.toggle(CircleToggleButton(init, palette(color))(onBoolean(st, send)))

            case P.MultiToggle(init, (sizeX, sizeY), color, texts) => listen.multiToggle(MultiToggle(init, sizeX, sizeY, palette(color), palette(Defaults.textColor), texts)(onMultiToggle(st, send))).map(listen.textList)

            case P.XYPad((initX, initY), color)     => listen.float2(XYPad(initX, initY, palette(color))(onFloat2(st, send)))
            case P.HFaderRange((initX, initY), color)     => listen.float2(HFaderRange((initX, initY), palette(color))(onFloat2(st, send)))
            case P.VFaderRange((initX, initY), color)     => listen.float2(VFaderRange((initX, initY), palette(color))(onFloat2(st, send)))
            case P.XYPadRange(initX, initY, color) => listen.float4(XYPadRange(initX, initY, palette(color))(onFloat4(st, send)))

            case P.DropDownList(init, texts)        => listen.int(DropDownList(init, texts)(onInt(st, send)))
            case P.TextInput(init, color, textLength) => listen.string(TextInput(init, palette(color), textLength)(onString(st, send)))
            case P.FileInput(init, color, text) => listen.file(FileInput(init, palette(color), text)(onFile(st, send)))
            case P.DoubleCheck(init, sizes, color1, color2, texts, orient, allowDeselect) => listen.doubleCheck(DoubleCheck(init, sizes, palette(color1), palette(color2), texts,
                        DoubleCheck.Orient(orient.isFirst, orient.isFirstHor, orient.isSecondHor), allowDeselect)(onInt2WithDeselect(init, st, send)))
        }
    }
}
