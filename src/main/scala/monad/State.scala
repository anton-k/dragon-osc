
package dragon.osc.state

object State {
    def pure[S,A](a: A): State[S,A] = new State[S,A] {
        def run(s: S) = (a, s)
    }

    def get[S]: State[S,S] = new State[S,S] {
        def run(s: S) = (s, s)
    }

    def put[S](s: S): State[S,Unit] = new State[S,Unit] {
        def run(prevSt: S) = ({}, s)
    }

    def modify[S](f: S => S): State[S,Unit] = new State[S,Unit] {
        def run(s: S) = ({}, f(s))
    }

    def mapM[S,A,B](xs: List[A])(f: A => State[S,B]): State[S,List[B]] = xs match {
        case Nil => pure[S,List[B]](Nil)
        case a :: as => f(a).flatMap(hd => mapM(as)(f).map(tl => hd :: tl))
    }

    def mapSplitStateM[S,A,B](xs: List[A])(f: A => State[S,B]): State[S,List[B]] = xs match {
        case Nil => pure[S,List[B]](Nil)
        case a :: as => f(a).forgetFlatMap(hd => mapM(as)(f).map(tl => hd :: tl))
    }
}

trait State[S,+A] { self =>
    def run(st: S): (A, S)

    def exec(st: S): S = this.run(st)._2

    def eval(st: S): A = this.run(st)._1

    def map[B](f: A => B): State[S,B] = new State[S,B] {
        def run(st: S) = self.run(st) match {
            case (a, s) => (f(a), s)
        }
    }

    def flatMap[B](f: A => State[S,B]): State[S,B] = new State[S,B] {
        def run(st: S) = self.run(st) match {
            case (a, s1) => f(a).run(s1)
        }
    }

    def forgetFlatMap[B](f: A => State[S,B]): State[S,B] = new State[S,B] {
        def run(st: S) = self.run(st) match {
            case (a, s1) => f(a).run(st)
        }
    }

    def next[B](that: State[S,B]) = this.flatMap(x => that)
}
