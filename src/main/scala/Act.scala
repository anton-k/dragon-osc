package dragon.osc.act

import scala.swing.audio.parse.arg.{Arg, Sym, UiSym, OscAddress, UiDecl, UiList}
import dragon.osc.const.Names

trait Action

case class Send(msg: Msg) extends Action
case class Save(name: String, value: Val) extends Action
case class Run(file: String)  extends Action
case class Set[A](id: String, value: A, fireCallback: Boolean) extends Action

case class Act(single: Option[List[Action]], valueMap: Option[Map[String, List[Action]]])

case class Msg(oscAddress: OscAddress, args: List[Val])
case class Val(value: String)

object Val {
    def read = Arg.string.map(x => Val(x))
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

    private def mkSingles(xs: List[UiDecl]): List[Action] = 
        parseActionList(xs.flatMap { x => x match {
            case UiList(ys) => ys
            case _ => Nil
        }})

    private def mkValues(xs: List[(String, UiDecl)]): Map[String, List[Action]] = 
        xs.flatMap { x => x match {
            case (name, UiList(acts)) => List((name, parseActionList(acts)))
            case _ => Nil
        }}.toMap

    private def parseActionList(xs: List[UiDecl]) = xs.map(parseAction).flatten

    private def parseAction(x: UiDecl): Option[Action] = x match {
        case UiSym(sym) => sym match {
            case SendSym(msg)           => Some(Send(msg))
            case SaveSym(name, value)   => Some(Save(name, value))
            case RunSym(file)           => Some(Run(file))
            case _                      => None
        }
        case _ => None
    }
}

case class SendSym(name: String, id: Option[String], body: UiDecl) extends Sym
case class SaveSym(name: String, id: Option[String], body: UiDecl) extends Sym
case class RunSym(name: String, id: Option[String], body: UiDecl) extends Sym

object SendSym {
    def unapply(s: Sym): Option[Msg] = s.isArgList(Names.send) {
        for {
            clientId <- Arg.int.getOrElse(0)
            addr <- Arg.string
            values <- Arg.many(Val.read)
        } yield Msg(OscAddress(addr, clientId), values)
    }
}

object SaveSym {
    def unapply(s: Sym): Option[(String,Val)] = s.isArgList(Names.save) {
        for {
            name  <- Arg.string
            value <- Val.read
        } yield (name, value)
    }
}

object RunSym {
    def unapply(s: Sym): Option[String] = s.isArgList(Names.run) { Arg.string }
}

