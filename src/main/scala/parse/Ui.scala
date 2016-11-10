package dragon.osc.parse.ui

import dragon.osc.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.attr._

trait Ui

case class Elem(sym: Sym, id: Option[String], osc: Option[Send]) extends Ui

case class Send(msg: List[Msg], guard: Option[Guard])
case class Msg(client: String, addr: String, args: List[Arg])

trait Arg
case class PrimArg(value: Prim) extends Arg
case class MemRef(name: String) extends Arg
case class ArgRef(id: Int) extends Arg

trait Guard
case class ArgEquals(value: Prim) extends Guard

// ----------------------------------------
// compound widgets

trait Sym
case class Hor(items: List[Ui]) extends Sym
case class Ver(items: List[Ui]) extends Sym

case class Tab(pages: List[Page])
case class Page(title: String, content: Ui)

// ----------------------------------------
// primitive widgets

case class Dial(init: Float, color: String) extends Sym
case class HFader(init: Float, color: String) extends Sym
case class VFader(init: Float, color: String) extends Sym
case class Toggle(init: Boolean, color: String, text: String) extends Sym
case class IntDial(init: Int, color: String, range: (Int, Int)) extends Sym
case class Button(color: String, text: String) extends Sym
case class Label(color: String, text: String) extends Sym

// -----------------------------------------


object Read {
    import Attr._

    def getKey(obj: Lang, key: String) = obj match {
        case MapSym(m) => m.get(key)
        case _ => None
    }

    object Widget {
        def any[A](xs: List[Widget[A]]): Widget[A] = xs.tail.foldLeft(xs.head)(_ orElse _)

        def ap[A,B](mf: Widget[A => B], ma: Widget[A]): Widget[B] = new Widget[B] {
            def run(obj: Lang) = (mf.run(obj), ma.run(obj)) match {
                case (Some(f), Some(a)) => Some(f(a))
                case _ => None
            }
        }

        def lift2[A,B,C](f: (A,B) => C, ma: Widget[A], mb: Widget[B]): Widget[C] = {
            ap(ma.map(a => (b: B) => f(a, b)), mb)
        }

        def fromAttr[A](a: Attr[Option[A]]): Widget[A] = new Widget[A] {
            def run(obj: Lang) = a.run(obj)
        }

        def pure[A](a: A) = new Widget[A] {
            def run(obj: Lang) = Some(a)
        }
    }

    trait Widget[+A] { self =>
        def run(obj: Lang): Option[A]

        def orElse[B >: A](that: Widget[B]): Widget[B] = new Widget[B] {
            def run(obj: Lang) = self.run(obj) orElse that.run(obj)
        }   

        def map[B](f: A => B): Widget[B] = new Widget[B] {
            def run(obj: Lang) = self.run(obj).map(f)
        }

        def withOption: Widget[Option[A]] = new Widget[Option[A]] {
            def run(obj: Lang) = Some(self.run(obj))
        }
    }

    def primWidget(name: String, attr: Attr[Sym]) = new Widget[Sym] {
        def run(obj: Lang) = getKey(obj, name).map(attr.run)
    }

    def fromSym(sym: Widget[Sym]): Widget[Ui] = 
        Widget.lift2( (s: Sym, gens: (Option[String], Option[Send])) => Elem(s, gens._1, gens._2), sym, genericParams)
   
    val dial    = primWidget(Names.dial,    lift2(Dial,   initFloat, color))
    val hfader  = primWidget(Names.hfader,  lift2(HFader, initFloat, color))
    val vfader  = primWidget(Names.vfader,  lift2(VFader, initFloat, color))
    val toggle  = primWidget(Names.toggle,  lift3(Toggle, initBoolean, color, text))
    val intDial = primWidget(Names.intDial, lift3(IntDial, initInt, color, rangeInt))
    val label   = primWidget(Names.label,   lift2(Label,  color, text))
    val button  = primWidget(Names.button,  lift2(Button, color, text))

    val hor = listWidget(Names.hor, Hor)
    val ver = listWidget(Names.ver, Ver)

    def listWidget[A](key: String, mk: List[Ui] => A) = new Widget[A] {
        def run(obj: Lang) = getKey(obj, key).flatMap {
            case ListSym(xs) => Some(mk(xs.map(ui.run).flatten))
            case _ => None
        }
    }    

    val widgets = List(dial, hfader, vfader, toggle, intDial, label, button, hor, ver)

    val send: Widget[Option[Send]] = Widget.pure(None)

    def genericParams: Widget[(Option[String], Option[Send])] = {        
        Widget.lift2( (a: Option[String], b: Option[Send]) => (a, b), Widget.fromAttr(id).withOption, send)
    } 

    def ui: Widget[Ui] = Widget.any(widgets.map(fromSym))
}

