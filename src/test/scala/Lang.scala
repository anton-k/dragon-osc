import org.scalatest._

import dragon.osc.parse.syntax._
import dragon.osc.parse.yaml._


class LangTest extends FunSuite {
    def check(str: String, value: Option[Lang]) = assert(Lang.read(str) == value)

    def checkPrim(str: String, value: Prim) = check(str, Some(PrimSym(value)))

    test("Parse int prim") {
        val n = 10
        checkPrim(n.toString,  PrimInt(n))
    }

    test("Parse string prim") {
        val str = "Hello World"
        checkPrim(str, PrimString(str))
    }

    test("Parse boolean prim") {
        val x = true
        checkPrim(x.toString, PrimBoolean(x))
    }

    test("Parse float prim") {
        val x = 1.4f
        checkPrim("1.4", PrimFloat(x))
    }


    test("Parse empty list") {
        check("[]", Some(ListSym(Nil)))
    }

    val simpleListRes = ListSym(List(PrimSym(PrimInt(1)), PrimSym(PrimString("hello")), PrimSym(PrimFloat(2.0f)), PrimSym(PrimBoolean(true))))

    test("Parse simple list") {
        val str = "[1, hello, 2.0, true]"        
        check(str, Some(simpleListRes))
    }

    test("Parse multi-line list") {
        val str = """
            - 1
            - hello
            - 2.0
            - true
        """
        check(str, Some(simpleListRes))
    }

    test("Parse simple map") {
        val str = " { a: 1, b: hello } "
        val res = MapSym(List("a" -> PrimSym(PrimInt(1)), "b" -> PrimSym(PrimString("hello"))).toMap)
        check(str, Some(res))
    }

    test("Empty map") {
        val str = "{}"
        val res = MapSym(Map())
        check(str, Some(res))
    }

    test("Parse multi-line map") {
        val str = """
            dial:
                color: blue
                init: 0.25
                osc: /cps                    
        """

        val res = MapSym(Map(
            "dial" -> MapSym(List(
                    "color" -> PrimSym(PrimString("blue")), 
                    "init" -> PrimSym(PrimFloat(0.25f)), 
                    "osc" -> PrimSym(PrimString("/cps"))
            ).toMap)))
        check(str, Some(res))
    }
} 

class LangJsonTest extends FunSuite {
    def check(str: String, value: Option[Lang]) = assert(LangJson.read(str) == value)
    val simpleListRes = ListSym(List(PrimSym(PrimInt(1)), PrimSym(PrimString("hello")), PrimSym(PrimFloat(2.0f)), PrimSym(PrimBoolean(true))))

    test("Parse simple list") {
        val str = """[1, "hello", 2.0, true]"""
        check(str, Some(simpleListRes))
    }

    test("Parse multi-line list") {
        val str = """
            [ 1
            , "hello"
            , 2.0
            , true ]
        """
        check(str, Some(simpleListRes))
    }

    test("Parse simple map") {
        val str = """ { "a": 1, "b": "hello" } """
        val res = MapSym(List("a" -> PrimSym(PrimInt(1)), "b" -> PrimSym(PrimString("hello"))).toMap)
        check(str, Some(res))
    }

    test("Empty map") {
        val str = "{}"
        val res = MapSym(Map())
        check(str, Some(res))
    }

    test("Parse multi-line map") {
        val str = """
            { "dial":
                 { "color": "blue",
                   "init": 0.25,
                   "osc": "/cps",
                   "range": [0, 10] } 
            }                  
        """

        val res = MapSym(Map(
            "dial" -> MapSym(List(
                    "color" -> PrimSym(PrimString("blue")), 
                    "init" -> PrimSym(PrimFloat(0.25f)), 
                    "osc" -> PrimSym(PrimString("/cps")),
                    "range" -> ListSym(List(PrimSym(PrimInt(0)), PrimSym(PrimInt(10))))
            ).toMap)))
        check(str, Some(res))
    }

}