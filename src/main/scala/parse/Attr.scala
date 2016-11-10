package dragon.osc.parse.attr

import dragon.osc.const._
import dragon.osc.parse.syntax._

trait Attr[A] { self =>
    def run(obj: Lang): A

    def map[B](f: A => B): Attr[B] = new Attr[B] {
        def run(obj: Lang) = f(self.run(obj))
    }
}

object Attr {    
    // generic functions

    def ap[A,B](f: Attr[A => B], a: Attr[A]): Attr[B] = new Attr[B] {
        def run(obj: Lang) = f.run(obj)(a.run(obj))
    }

    def lift2[A,B,C](f: (A,B) => C, ma: Attr[A], mb: Attr[B]): Attr[C] = {
        ap(ma.map(a => (b: B) => f(a, b)), mb)
    }

    def lift3[A,B,C,D](f: (A,B,C) => D, ma: Attr[A], mb: Attr[B], mc: Attr[C]): Attr[D] = {
        ap(lift2[A,B,C=>D]((a, b) => (c: C) => f(a,b, c), ma, mb), mc)
    }

    def lift4[A,B,C,D,E](f: (A,B,C,D) => E, ma: Attr[A], mb: Attr[B], mc: Attr[C], md: Attr[D]): Attr[E] = {
        ap(lift3[A,B,C,D=>E]((a, b, c) => (d: D) => f(a,b,c,d), ma, mb, mc), md)   
    }


    def attr[A](name: String, extract: Lang => Option[A], default: A) = new Attr[A] {
        def run(obj: Lang) = (obj match {
            case MapSym(m) => m.get(name).flatMap(extract)
            case _ => None
        }).getOrElse(default)        
    }

    // -------------------------------------------------------
    // specific attributes

    def initFloat   = attr[Float](Attributes.init, readFloat, Defaults.float)
    def initBoolean = attr[Boolean](Attributes.init, readBoolean, Defaults.boolean)
    def initInt = attr[Int](Attributes.init, readInt, Defaults.int)
    def initString = attr[String](Attributes.init, readString, Defaults.string)

    def color = attr[String](Attributes.color, readString, Defaults.color)
    def text = attr[String](Attributes.text, readString, Defaults.string)
    def rangeInt = attr[(Int, Int)](Attributes.range, readRangeInt, Defaults.rangeInt)

    //---------------------------------------------------------
    // field readers

    def readFloat(obj: Lang) = obj match {
        case PrimSym(PrimFloat(x)) => Some(x)
        case _ => None
    }

    def readString(obj: Lang) = obj match {
        case PrimSym(PrimString(x)) => Some(x)
        case _ => None
    }    

    def readBoolean(obj: Lang) = obj match {
        case PrimSym(PrimBoolean(x)) => Some(x)
        case _ => None
    }    

    def readInt(obj: Lang) = obj match {
        case PrimSym(PrimInt(x)) => Some(x)
        case _ => None
    }

    def readRangeInt(obj: Lang) = obj match {
        case ListSym(List(PrimSym(PrimInt(min)), PrimSym(PrimInt(max)))) => Some((min, max))
        case _ => None
    }
}