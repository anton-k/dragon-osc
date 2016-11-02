import scala.swing._
import scala.swing.event._
import java.awt.{Color,Graphics2D,BasicStroke}

import scala.swing.audio.{parse => P}
import scala.swing.audio.parse.{arg => Arg}
import scala.swing.audio.ui._

import scala.audio.osc._

import dragon.osc.OscClientPool
import dragon.osc.input._

package scala.swing.audio.convert {    

    case class Window(title: Option[String], size: Option[(Int, Int)], content: Component)

    object Convert {  

        def echo[A](msg: String, oscAddress: Arg.OscAddress, value: A) =
            println(s"${msg}${oscAddress.address} ${value}")

        def readFile(osc: OscClientPool, filename: String) = P.ReadUI.readUi(Arg.UiDecl.loadFile(filename)).eval(P.Context()).map(x => convertTopLevel(osc)(x).run(InputBase())).get

        def mkTabbed(osc: OscClientPool)(body: List[(String, P.Ui)]) = 
            mapM(body){{ case (name, content) => convert(osc)(content).map(ui => (name, ui)) }}.map { body =>
                new TabbedPane {
                    import TabbedPane._
                    body.foreach { case (name, ui) => 
                        pages += new Page(name, ui)
                    }
                }
            }
        

        def genBox(orient: Orientation.Value)(items: List[Component])(implicit code: Unit = {}) = new BoxPanel(orient) {
            items.foreach( x => {contents += x; code} )
        }

        def hor(items: List[Component])(implicit code: Unit = {}) = genBox(Orientation.Horizontal)(items)(code)
        def ver(items: List[Component])(implicit code: Unit = {}) = genBox(Orientation.Vertical)(items)(code)

        def mkFloatValue[A](message: String, osc: OscClientPool, init: Float, color: String, oscVal: Arg.OscFloat, mk: (Float, Color) => (Float => Unit) => A) = {
            val addr = oscVal.oscAddress
            val chn  = osc.channel[Float](addr)
            mk(init, palette(color)) { x => { val res = oscVal.fromRelative(x); echo(message, addr, res); chn.send(res) }}            
        }

        def mkFloatRangeValue[A](message: String, osc: OscClientPool, init: (Float,Float), color: String, oscVal: Arg.OscFloat, mk: ((Float,Float), Color) => ((Float, Float) => Unit) => A) = {
            val addr = oscVal.oscAddress
            val chn  = osc.channel[(Float,Float)](addr)
            mk(init, palette(color)) { (x1, x2) => { 
                val res = (oscVal.fromRelative(x1), oscVal.fromRelative(x2))
                println(s"${message}${addr} ${res}")
                chn.send(res) 
            }}            
        }

        def mkPushButton(osc: OscClientPool, color: String, text: Option[String], oscVal: Arg.OscBoolean) = {
            val addr = oscVal.oscAddress
            val chn  = osc.channel[Boolean](addr)
            PushButton(palette(color), text) { echo("PushButton", addr, ""); chn.send(true) }
        }

        def mkToggleButton(osc: OscClientPool, init: Boolean, color: String, text: Option[String], oscVal: Arg.OscBoolean) = {
            val addr = oscVal.oscAddress
            val chn  = osc.channel[Boolean](addr)
            ToggleButton(init, palette(color), text) { x => echo("Toggle", addr, x); chn.send(x) }
        }

        def mkXYPad(osc: OscClientPool, init: (Float, Float), color: String, oscVal: Arg.OscFloat2) = {
            val addr = oscVal.oscAddress
            val chn  = osc.channel[(Float, Float)](addr)
            XYPad(init._1, init._2, palette(color)) { (x, y) => val res = oscVal.fromRelative((x, y)); echo("XYPad", addr, res); chn.send(res) }
        }

        def toLinInt(size: (Int, Int))(p: (Int, Int)) = p._1 + p._2 * size._1
        def fromLinInt(size: (Int, Int))(n: Int) = (n % size._1, n / size._1)

        def mkMultiToggle(osc: OscClientPool, size: (Int, Int), init: List[Int], texts: List[String], color: String, textColor: String, oscAddress: Arg.OscAddress) = {
            val chn = osc.channel[(Int, Boolean)](oscAddress)
            MultiToggle(init.toSet.map((x: Int) => fromLinInt(size)(x)), size._1, size._2, palette(color), palette(textColor), texts) { (p, trig) =>
                val v = toLinInt(size)(p)
                echo("MultiToggle", oscAddress, (v, trig))                
                chn.send((v, trig))
            }
        }

        def mkIntDial(osc: OscClientPool, init: Int, range: (Int, Int), color: String, oscVal: Arg.OscInt) = {
            val addr = oscVal.oscAddress
            val chn  = osc.channel[Int](addr)
            IntDial(init, range, palette(color)){ n => 
                echo("IntDial", addr, n)                
                chn.send(n)
            }
        }

        def mkDropDownList(osc: OscClientPool, init: Int, names: List[String], oscVal: Arg.OscInt) = {
            val addr = oscVal.oscAddress
            val chn  = osc.channel[Int](addr)
            DropDownList(init, names) { n =>                 
                echo("DropDownList", addr, n)
                chn.send(n)
            }
        }

        def mkTextInput(osc: OscClientPool, init: Option[String], color: String, addr: Arg.OscAddress) = {
            val chn = osc.channel[String](addr)
            TextInput(init, palette(color)) { str =>                
                echo("TextInput", addr, str)
                chn.send(str)
            }
        }

        def mkXYPadRange(osc: OscClientPool, initX: (Float, Float), initY: (Float, Float), color: String, oscVal: Arg.OscFloat2) = {
            val addr = oscVal.oscAddress
            val chn = osc.channel[(Arg.Range, Arg.Range)](addr)
            XYPadRange(initX, initY, palette(color)) { (xs, ys) =>
                val resMins = oscVal.fromRelative((xs._1, ys._1))
                val resMaxs = oscVal.fromRelative((xs._2, ys._2))
                val resX = (resMins._1, resMaxs._1)
                val resY = (resMins._2, resMaxs._2)
                echo("XYPadRange", addr, (resX, resY))                
                chn.send((resX, resY))
            }
        }

        def convertTopLevel(osc: OscClientPool)(x: P.Ui): State[InputBase,List[Window]] = x match {
            case P.Window(title, size, items) => (convert(osc)(items)).map { ui => List(Window(Some(title), size, ui)) }
            case P.Root(xs) => (State.mapM(xs) { convertTopLevel(osc) }).map(_.flatten)
            case _ => (convert(osc)(x)).map { ui => List(Window(None, None, ui)) }
        }

        def pure[A](a: A): State[InputBase,A] = State.pure[InputBase,A](a)

        def mapM[A,B](as: List[A])(f: A => State[InputBase,B]): State[InputBase,List[B]] = State.mapM[InputBase,A,B](as)(f)

        def convert(osc: OscClientPool)(x: P.Ui): State[InputBase,Component] = x match {
            case P.WithId(id, widget) => convert(osc)(widget).flatMap(ui => State.modify[InputBase](_.addWidgetSet(id, ui)).next(State.pure(ui))) 
            case P.Tab(xs)  => mkTabbed(osc)(xs)            
            case P.Root(xs) => mapM(xs)(convert(osc)).map(uis => hor(uis))
            case P.Hor(xs)  => mapM(xs)(convert(osc)).map(uis => hor(uis))
            case P.Ver(xs)  => mapM(xs)(convert(osc)).map(uis => ver(uis))
            case P.Window(title, size, items) => convert(osc)(items)

            case simple => pure { simple match {                
                case P.VSpace(n) => Swing.VStrut(n)
                case P.HSpace(n) => Swing.HStrut(n)
                case P.HGlue => Swing.HGlue
                case P.VGlue => Swing.VGlue                
                case P.Label(text, color) => Text(text, palette(color))
                case P.Dial(init, color, oscVal)   => mkFloatValue("Dial", osc, init, color, oscVal, (init, color) => f => Dial(init, color)(f))
                case P.HFader(init, color, oscVal) => mkFloatValue("HFader", osc, init, color, oscVal, (init, color) => f => HFader(init, color)(f))
                case P.VFader(init, color, oscVal) => mkFloatValue("VFader", osc, init, color, oscVal, (init, color) => f => VFader(init, color)(f))
                case P.Button(color, text, oscVal) => mkPushButton(osc, color, text, oscVal)
                case P.Toggle(init, color, text, oscVal) => mkToggleButton(osc, init, color, text, oscVal)
                case P.MultiToggle(size, init, texts, color, textColor, oscAddr) => mkMultiToggle(osc, size, init, texts, color, textColor, oscAddr)
                case P.XYPad(init, color, oscVal) => mkXYPad(osc, init, color, oscVal)
                case P.IntDial(init, range, color, oscVal) => mkIntDial(osc, init, range, color, oscVal)
                case P.HFaderRange(init, color, oscVal) => mkFloatRangeValue("HFaderRange", osc, init, color, oscVal, (init, color) => f => HFaderRange(init, color)(f))
                case P.VFaderRange(init, color, oscVal) => mkFloatRangeValue("VFaderRange", osc, init, color, oscVal, (init, color) => f => VFaderRange(init, color)(f))
                case P.XYPadRange(initX, initY, color, oscVal) => mkXYPadRange(osc, initX, initY, color, oscVal)
                case P.DropDownList(init, names, oscVal) => mkDropDownList(osc, init, names, oscVal)
                case P.TextInput(init, color, oscVal) => mkTextInput(osc, init, color, oscVal)            
            }}
        }

        def palette(colorName: String) = colorName match {
            case "any" => Color.decode(randomColorName)
            case _     => Color.decode(paletteMap.get(colorName.toLowerCase).getOrElse(paletteMap.get("blue").get))
        }

        val rand = scala.util.Random
         
        def randomColorName =             
            brightPaletteValues(rand.nextInt(brightPaletteSize)) // except monochrome colors

        val paletteMap = Map(
              "navy" -> "#001f3f"
            , "blue" -> "#0074D9"
            , "aqua" -> "#7FDBFF"
            , "teal" -> "#39CCCC"
            , "olive" -> "#3D9970"
            , "green" -> "#2ECC40"
            , "lime"  -> "#01FF70"
            , "yellow" -> "#FFDC00"
            , "orange" -> "#FF851B"
            , "red" -> "#FF4136"
            , "maroon" -> "#85144B"
            , "fuchsia" -> "#F012BE"
            , "purple" -> "#B10DC9"
            , "black" -> "#111111"
            , "gray" -> "#AAAAAA"
            , "silver" -> "#DDDDDD"
            , "white" -> "#FFFFFF")

        val brightColors = paletteMap - "gray" - "black" - "silver" - "white"

        val brightPaletteValues = brightColors.values.toArray
        val brightPaletteSize = brightColors.size
    }
}
 