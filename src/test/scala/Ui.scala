import org.scalatest._

import dragon.osc.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.ui._
import dragon.osc.parse.widget._

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

class ParseGenericParams extends FunSuite {
    import Read._

    def check[A](str: String, w: Widget[Sym], res: Option[Elem]) = 
        assert(Lang.read(str).flatMap(x => fromSym(w).run(x)) == res)


    test ("widget with no params") {
        check("dial: { color: red }", dial, Some(Elem(Dial(Defaults.float, "red"), Param(None, None))))
    }

    test ("widget with id") {
        check( "{ dial: { color: red, init: 0.0 }, id: amp }", dial, Some(Elem(Dial(0.0f, "red"), Param(Some("amp"), None))))
    }
}

class PrimUi extends FunSuite {
    import Read._

    def check[A](str: String, res: Option[Ui]) = 
        assert(Lang.read(str).flatMap(x => ui.run(x)) == res)

    val d = Elem(Dial(Defaults.float, Defaults.color))

    test("ui") {
        check("dial: {}", Some(d))
    }

    val hf = Elem(HFader(Defaults.float, Defaults.color))
    test("ui") {
        check("hfader: {}", Some(hf))
    }
}

class CompoundWidgets extends FunSuite {
    import Read._

    def check[A](str: String, w: Widget[A], res: Option[A]) = 
        assert(Lang.read(str).flatMap(x => w.run(x)) == res)

    val p = Elem(Dial(Defaults.float, Defaults.color))

    test("hor") {
        check("hor: [dial: {}, dial: {}, dial: {}]", hor, 
            Some(Hor(List(p, p, p))))
    }

    test("ver") {
        check("ver: [dial: {}, dial: {}, dial: {}]", ver, 
            Some(Ver(List(p, p, p))))
    }
}

class UiCompoundWidgets extends FunSuite {
    import Read._

    def check(str: String, res: Option[Ui]) = 
        assert(Lang.read(str).flatMap(x => ui.run(x)) == res)

    val p = Elem(Dial(Defaults.float, Defaults.color))

    test("hor") {
        check("hor: [dial: {}, dial: {}, dial: {}]",  
            Some(Elem(Hor(List(p, p, p)))))
    }

    test("ver") {
        check("ver: [dial: {}, dial: {}, dial: {}]",  
            Some(Elem(Ver(List(p, p, p)))))
    }

    test("tabs") {
        check("tabs: [ page: { title: page1, content: {dial: {}} }, page: { title: page2, content: {dial: {}} } ]",
            Some(Elem(Tabs(List(Page("page1", p), Page("page2", p))))))
    }

     def check[A](str: String, widget: Widget[A], res: A) = 
        assert(Lang.read(str).flatMap(x => widget.run(x)) == Some(res))

    test("window") {
        check("window: { title: Supper App, content: {dial: {}} }", window, Window("Supper App", None, p))
        check("window: { title: Supper App, size: [230, 500], content: {dial: {}} }", window, Window("Supper App", Some((230, 500)), p))
    }

    val w = Window(Defaults.string, None, emptyUi)
    test("root") {
        check[Root]("app: [window: {}, window: {}]", root, Root(List(w,w)))
    }

}
