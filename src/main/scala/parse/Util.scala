package dragon.osc.parse.util

object Util {
    def optionMapM[A,B](xs: List[A])(f: A => Option[B]): Option[List[B]] = xs match {
        case Nil => Some(Nil)
        case a::as => f(a).flatMap { v => optionMapM(as)(f).map(vs => v :: vs) }
    }

    def optionMapM[K,A,B](xs: Map[K,A])(f: A => Option[B]): Option[Map[K,B]] = 
        optionMapM(xs.toList)({ case (k, v) => f(v).map(x => (k, x))}).map(_.toMap)
}