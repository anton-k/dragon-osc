package dragon.osc.parse.widget

import dragon.osc.parse.syntax._
import dragon.osc.parse.attr._

object Widget {
    def any[A](xs: Stream[Widget[A]]): Widget[A] = new Widget[A] {
        private def go(obj: Lang, xs: Stream[Widget[A]]): Option[A] = 
            if (xs.isEmpty) 
                None
            else
                xs match {            
                    case a #:: as => a.run(obj) match {
                        case Some(res) => Some(res)
                        case None => go(obj, as)
                    }
                }

        def run(obj: Lang) = go(obj, xs)
    }

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

    def fromOptionAttr[A](a: Attr[Option[A]]) = fromAttr(a).withOption  

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
