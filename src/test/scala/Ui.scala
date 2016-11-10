import org.scalatest._

import dragon.osc.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.yaml._
import dragon.osc.parse.ui._

class PrimWidgets extends FunSuite {
    import Read._

    def check[A](str: String, w: Widget[A], res: Option[A]) = 
        assert(Lang.read(str).flatMap(x => w.run(x)) == res)

    test("dial") {
        check("dial: { color: red, init: 0.2 }", dial, Some(Dial(0.2f, "red")))
        check("dial: { }", dial, Some(Dial(Defaults.float, Defaults.color)))
    }

    test("hfader") {
        check("hfader: { color: red, init: 0.2 }", hfader, Some(HFader(0.2f, "red")))
        check("hfader: { }", hfader, Some(HFader(Defaults.float, Defaults.color)))
    }

    test("vfader") {
        check("vfader: { color: red, init: 0.2 }", vfader, Some(VFader(0.2f, "red")))
        check("vfader: {}", vfader, Some(VFader(Defaults.float, Defaults.color)))
    }

    test("int-dial") {
        check("int-dial: { color: olive, init: 5, range: [0, 20] }", intDial, 
            Some(IntDial(5, "olive", (0, 20))))
    }

    test("toggle") {
        check("toggle: { init: false, color: red }", toggle,
            Some(Toggle(false, "red", Defaults.string)))
    }

    test("button") {
        check("button: { color: orange, text: start }", button,
            Some(Button("orange", "start")))
    }

    test("label") {
        check("label: { color: orange, text: start }", label,
            Some(Label("orange", "start")))
    }
}
