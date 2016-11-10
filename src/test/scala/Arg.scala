import org.scalatest._
import scala.swing.audio.parse.arg._
import dragon.osc.act._
import dragon.osc.parse.yaml.ReadYaml


class TestUtils

class ArgTest extends FunSuite {

    test("Arg list parse int") {
        assert(
               Arg.int.eval("[1]") == Some(1) 
            && Arg.int.eval("[1.0]") == None
            && Arg.int.eval("[]") == None
            && Arg.int.eval("[qerty]") == None)
    }

    test("Arg list parse string") {
        assert(
               Arg.string.eval("[qwerty]") == Some("qwerty") 
            && Arg.int.eval("[]") == None
            && Arg.string.eval("[1,2,3]") == None
            && Arg.int.eval("[true]") == None)
    }

    test("Arg list parse float") {
        assert(
               Arg.float.eval("[0.2]") == Some(0.2f)
            && Arg.float.eval("[1]") == Some(1)
            && Arg.float.eval("[]") == None
            && Arg.float.eval("[Hello]") == None
        )
    }

    test("Arg list parse string list") {
        assert(
               Arg.stringList.eval("[[a,b,c,d]]") == Some(List("a", "b", "c", "d"))
            && Arg.stringList.eval("[hello, [a,b,c,d]]") == None
            && Arg.stringList.eval("[]") == None
        )
    }

    test("Arg list orElse") {
        assert(
               Arg.int.orElse.eval("[1]") == Some(Some(1))
            && Arg.int.orElse.eval("[]") == Some(None)
        )
    }

    test("Arg list pair") {
        assert(
            Arg.pair(Arg.int, Arg.string).eval("[234, hi]") == Some((234, "hi"))
        )
    }

    test("Arg list many") {
        assert(
               Arg.many(Arg.int).eval("[1,2,3,hi]") == Some(List(1,2,3)) 
            && Arg.many(Arg.int).eval("[hi, 1,2,3]") == Some(Nil) 
            && Arg.many(Arg.int.orElse).eval("[hi, 1,2,3]") == Some(List(None))
            && Arg.many(Arg.int).eval("[]") == Some(Nil) 
        )
    }

    test("Arg osc addr") {
        assert(
               Arg.oscAddress.eval("[/msg, 1, 2, 3]") == Some(OscAddress("/msg"))
            && Arg.oscAddress.eval("[msg, 1, 2, 3]") == None
            && Arg.oscAddress.eval("[[2, /msg], 1, 2, 3]") == Some(OscAddress("/msg", Some(OutsideClientId("2"))))
        )        
    }
}

class MsgParseTest extends FunSuite {
    test("Val parse") { assert {
           Val.readList.eval("[1, 2, hi, $1, $mem]") == Some(List(IntVal(1), IntVal(2), StringVal("hi"), ArgRef(1), MemRef("mem"))) &&
           Val.readList.eval("[]") == Some(List())
    }}


    val valList = List(IntVal(1), FloatVal(1.3f), BooleanVal(true), StringVal("hi"), ArgRef(1), MemRef("mem"))

    test("Msg parse") { assert {
        Msg.read.eval("[/msg/amp, 1, 1.3, true, hi, $1, $mem]") == Some(Msg(OscAddress("/msg/amp"), valList))
    }}

    test("Msg parse with osc port") { assert {
        Msg.read.eval("[[2, /msg/amp], 1, 1.3, true, hi, $1, $mem]") == Some(Msg(OscAddress("/msg/amp", Some(OutsideClientId("2"))), valList))
    }}

}

class ActParseTest extends FunSuite {
    def parse(str: String): Option[Act] = UiDecl(ReadYaml.loadString(str)) match {
        case UiMap(m) => Act.fromMap(Settings(), m)
        case _ => None
    }

    val msg1 = Msg(OscAddress("/msg1"), List(StringVal("hi")))
    val msg2 = Msg(OscAddress("/msg2"), List(StringVal("msg")))

    test("Empty act list") { assert {
        parse("{dial: []}") == Some(Act(None, None, None))
    }}

    val actList = List(msg1, msg2)
    test("Simple act list") { assert {
        parse("{ act: [[/msg1, hi], [/msg2, msg]] }") == Some(Act(Some(actList), None))
    }}

    val valMap = Map[String,List[Msg]]("1" -> List(msg1), "2" -> List(msg2))
    test("Act with map and no list") { assert {
        parse("{ act 1: [[/msg1, hi]], act 2: [[/msg2, msg]] }") == Some(Act(None, Some(valMap)))
    }}

    val booleanMap = Map[String,List[Msg]]("true" -> List(msg1), "false" -> List(msg2))
    test("Act with boolean map and no list") { assert {
        parse("{ act true: [[/msg1, hi]], act false: [[/msg2, msg]] }") == Some(Act(None, Some(booleanMap)))
    }}

    test("Act with list and map") { assert { 
        parse("{ act: [[/msg1, hi], [/msg2, msg]], act true: [[/msg1, hi]], act false: [[/msg2, msg]] }") == Some(Act(Some(actList), Some(booleanMap)))
    }}
}