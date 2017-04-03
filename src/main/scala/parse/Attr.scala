package dragon.osc.parse.attr

import java.io.File

import dragon.osc.parse.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.util._
import dragon.osc.parse.ui._

trait Attr[A] { self =>
    def run(obj: Lang): A

    def map[B](f: A => B): Attr[B] = new Attr[B] {
        def run(obj: Lang) = f(self.run(obj))
    }
}

object Attr {    
    // generic functions

    def pure[A](a: A): Attr[A] = new Attr[A] {
        def run(obj: Lang) = a
    }

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

    def lift5[A,B,C,D,E,F](f: (A,B,C,D,E) => F, ma: Attr[A], mb: Attr[B], mc: Attr[C], md: Attr[D], me: Attr[E]): Attr[F] = {
        ap(lift4[A,B,C,D,E=>F]((a, b, c, d) => (e: E) => f(a,b,c,d, e), ma, mb, mc, md), me)
    }

    def lift6[A,B,C,D,E,F,G](phi: (A,B,C,D,E,F) => G, ma: Attr[A], mb: Attr[B], mc: Attr[C], md: Attr[D], me: Attr[E], mf: Attr[F]) = {
        ap(lift5[A,B,C,D,E,F=>G]((a, b, c, d, e) => (f: F) => phi(a,b,c,d,e,f), ma, mb, mc, md, me), mf)
    }

    def lift7[A,B,C,D,E,F,G,H](phi: (A,B,C,D,E,F,G) => H, ma: Attr[A], mb: Attr[B], mc: Attr[C], md: Attr[D], me: Attr[E], mf: Attr[F], mg: Attr[G]) = {
        ap(lift6[A,B,C,D,E,F,G=>H]((a, b, c, d, e, f) => (g: G) => phi(a,b,c,d,e,f, g), ma, mb, mc, md, me, mf), mg)
    }

    def attr[A](name: String, extract: Lang => Option[A], default: A) = new Attr[A] {
        def run(obj: Lang) = (obj match {
            case MapSym(m) => m.get(name).flatMap(extract)
            case _ => None
        }).getOrElse(default)        
    }

    def optAttr[A](name: String, extract: Lang => Option[A]) = attr[Option[A]](name, x => extract(x).map(a => Some(a)), None)

    // -------------------------------------------------------
    // specific attributes

    def initFloat   = attr[Float](Names.init, readFloat, Defaults.float)
    def initFloat2  = attr[(Float, Float)](Names.init, readFloat2, Defaults.float2)
    def initBoolean = attr[Boolean](Names.init, readBoolean, Defaults.boolean)
    def initInt = attr[Int](Names.init, readInt, Defaults.int)
    def initInt2 = attr[(Int,Int)](Names.init, readInt2, Defaults.int2)
    def initString = attr[String](Names.init, readString, Defaults.string)

    def colorBy(name: String) = attr[String](name, readString, Defaults.color)
    def color = colorBy(Names.color)
    def color1 = colorBy(Names.color1)
    def color2 = colorBy(Names.color2)
    def text = attr[String](Names.text, readString, Defaults.string)
    def rangeInt = attr[(Int, Int)](Names.range, readRangeInt, Defaults.rangeInt)
    def rangeFloat = attr[(Float, Float)](Names.range, readRangeFloat, Defaults.range)
    def id = attr[Option[String]](Names.id, x => Some(readString(x)), None)
    def client = attr[String](Names.client, readString, Defaults.client)
    def path = attr[String](Names.path, readString, Defaults.path)
    def title = attr[String](Names.title, readString, Defaults.string)
    def size = attr[Option[(Int,Int)]](Names.size, readSize, None)
    def size1 = attr[Int](Names.size, readInt, Defaults.size1)
    def allowDeselect = attr[Boolean](Names.allowDeselect, readBoolean, Defaults.allowDeselect)
    def texts = attr[List[String]](Names.texts, readStringList, Nil)
    def initOptionString = attr[Option[String]](Names.init, readOptionString, None)
    def textLength = attr[Int](Names.textLength, readInt, Defaults.textLength)
    def initRange = attr[(Float,Float)](Names.init, readFloat2, Defaults.range)
    def initX = attr[(Float,Float)](Names.initX, readFloat2, Defaults.range)
    def initY = attr[(Float,Float)](Names.initY, readFloat2, Defaults.range)
    def initMultiToggle = attr[Set[(Int,Int)]](Names.init, readMultiToggleInit, Set())
    def multiToggleSize = attr[(Int,Int)](Names.size, readRangeInt, Defaults.multiToggleSize)
    def orient = attr[Orient](Names.orient, readOrient, Orient(Defaults.orientIsFirst, Defaults.orientIsFirstHor, Defaults.orientIsSecondHor))
    def sizeList = attr[List[Int]](Names.sizeList, readIntList, Defaults.sizeList)
    def doubleCheckTexts = attr[List[(String, List[String])]](Names.texts, readDoubleCheckTexts, Defaults.doubleCheckTexts)

    def initOptionFile = initOptionString.map(_.flatMap(filename => {
        val file = new File(filename)
        if (file.exists) Some(file)
        else None
    }))

    //---------------------------------------------------------
    // field readers

    def readFloat(obj: Lang) = obj match {
        case PrimSym(PrimFloat(x)) => Some(x)
        case PrimSym(PrimInt(x)) => Some(x.toFloat)
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
    
    def readRangeFloat(obj: Lang) = obj match {
        case ListSym(List(s1, s2)) => for { min <- readFloat(s1); max <- readFloat(s2) } yield (min, max)
        case _ => None
    }

    def readStringList(obj: Lang) = obj match {
        case ListSym(xs) => Util.optionMapM(xs)(readString)
        case _ => None
    }

    def readIntList(obj: Lang) = obj match {
        case ListSym(xs) => Util.optionMapM(xs)(readInt)
        case _ => None
    }    

    def readFloat2(obj: Lang) = obj match {
        case ListSym(List(s1, s2)) => for { x <- readFloat(s1); y <- readFloat(s2) } yield (x, y)
        case _ => None
    }

    def readInt2(obj: Lang) = obj match {
        case ListSym(List(PrimSym(PrimInt(x)), PrimSym(PrimInt(y)))) => Some((x, y))
        case _ => None
    }    

    def readOptionString(obj: Lang) = obj match {
        case PrimSym(PrimString(x)) => if (x.isEmpty) None else Some(Some(x))
        case _ => None
    }

    def readOptionFloat(obj: Lang) = readFloat(obj) match {
        case Some(x) => Some(Some(x))
        case None => None
    }

    def readMultiToggleInit(obj: Lang) = obj match {
        case ListSym(xs) => Util.optionMapM(xs)(readRangeInt).map(_.toSet)
        case _ => None
    }

    def readSize(obj: Lang) = readRangeInt(obj).map(x => Some(x))

    def readOrient(obj: Lang) = obj match {
        case ListSym(List(PrimSym(PrimBoolean(isFirst)), PrimSym(PrimBoolean(isFirstHor)), PrimSym(PrimBoolean(isSecondHor))))  => Some(Orient(isFirst, isFirstHor, isSecondHor))
        case _ => None
    }

    def readDoubleCheckTexts(obj: Lang) = obj match {
        case ListSym(xs) => Util.optionMapM(xs)(readDoubleCheckItem)
        case _ => None
    }

    def readDoubleCheckItem(obj: Lang) = obj match {
        case ListSym(List(PrimSym(PrimString(name)), subnames)) => readStringList(subnames).map(xs => (name, xs))
        case _ => None
    }
} 