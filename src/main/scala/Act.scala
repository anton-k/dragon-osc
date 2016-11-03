package dragon.osc.act

import scala.swing.audio.parse.arg.{Arg, DefaultOscSend, Sym, UiSym, OscAddress, UiDecl, UiList}
import dragon.osc.const.Names
import dragon.osc.input.InputBase

case class Act(single: Option[List[Msg]], valueMap: Option[Map[String, List[Msg]]], defaultSend: Option[DefaultOscSend] = None) {
    def withDefaultSend(x: Option[DefaultOscSend]) = this.copy(defaultSend = x)
    def mapDefaultSend(f: DefaultOscSend => DefaultOscSend) = this.copy(defaultSend = this.defaultSend.map(f))

    def compileDial(base: InputBase)(x: Float): Unit = ???
    def compileToggle(base: InputBase)(x: Boolean): Unit = ???
}

case class Msg(oscAddress: OscAddress, args: List[Val])

object Msg {
    def read: Arg[Msg] = for {
        addr <- Arg.oscAddress
        vals <- Val.readList
    } yield Msg(addr, vals)
}

trait Val
case class IntVal(value: Int) extends Val
case class FloatVal(value: Float) extends Val
case class BooleanVal(value: Boolean) extends Val
case class StringVal(value: String) extends Val
case class ArgRef(id: Int) extends Val
case class MemRef(ref: String) extends Val

object Val {
    def readList: Arg[List[Val]] = Arg.many(Val.read)

    def read: Arg[Val] = 
           Arg.argRef.map(ArgRef) || 
           Arg.memRef.map(MemRef) || 
           Arg.int.map(IntVal)    ||
           Arg.float.map(FloatVal)||
           Arg.boolean.map(BooleanVal) ||
           Arg.string.map(StringVal)    
}

object Act {
    def fromMap(m: Map[String, UiDecl]): Option[Act] = {
        val acts = m.toList
            .filter(x => x._1.startsWith("act") && x._2.isList)
            .map({ case (name, body) => (name.drop(3).trim, body) })
        if (acts.isEmpty) None
        else {
            val (singles, values) = acts.partition(x => x._1 == "")
            Some(Act(someIfNotEmpty(singles.map(_._2), mkSingles), someIfNotEmpty(values, mkValues)))
        }
    }

    private def someIfNotEmpty[A,B](xs: List[A], f: List[A] => B): Option[B] = 
        if (xs.isEmpty) None
        else Some(f(xs))

    private def mkSingles(xs: List[UiDecl]): List[Msg] = 
        parseActionList(xs.flatMap { x => x match {
            case UiList(ys) => ys
            case _ => Nil
        }})

    private def mkValues(xs: List[(String, UiDecl)]): Map[String, List[Msg]] = 
        xs.flatMap { x => x match {
            case (name, UiList(acts)) => List((name, parseActionList(acts)))
            case _ => Nil
        }}.toMap

    private def parseActionList(xs: List[UiDecl]) = xs.map(parseAction).flatten

    private def parseAction(x: UiDecl): Option[Msg] = x match {
        case UiList(xs) => Msg.read.eval(xs)
        case _ => None
    }
}
