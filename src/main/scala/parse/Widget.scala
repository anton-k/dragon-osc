package dragon.osc.parse.widget

import dragon.osc.parse.syntax._
import dragon.osc.parse.attr._

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
