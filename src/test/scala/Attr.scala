import org.scalatest._

import dragon.osc.parse.syntax._
import dragon.osc.parse.attr._
import dragon.osc.parse.syntax._
import dragon.osc.const._


class ReadConstants extends FunSuite {
    import Attr._
    // checkPrim[A](attr: Attr[A], obj: Prim, res: Option[A]) = assert(attr.run(obj) == res)

    val float = 0.5f
    test ("Read float") {
        assert(
            readFloat(PrimSym(PrimFloat(float))) == Some(float) &&  
            readFloat(PrimSym(PrimBoolean(true))) == None
        )  
    }

    val str = "hello"
    test ("Read string") {
            readString(PrimSym(PrimString(str))) == Some(str) &&  
            readString(PrimSym(PrimBoolean(true))) == None        
    }

    val int = 0
    test ("Read int") {
            readInt(PrimSym(PrimInt(int))) == Some(int) &&  
            readInt(PrimSym(PrimBoolean(true))) == None        
    }    

    val boolean = true
    test ("Read boolean") {
            readInt(PrimSym(PrimBoolean(boolean))) == Some(boolean) &&  
            readInt(PrimSym(PrimInt(1))) == None        
    }  
}

class ReadAttrs extends FunSuite {
    import Attr._

    def check[A](str: String, attr: Attr[A], res: A) = assert {
        val obj = Lang.read(str)
        obj.map(x => attr.run(x)) == Some(res)
    }

    // color

    test ("Read color") { 
        check("{ color: green }", color, "green") 
    }

    test ("Read default color") { 
        check("{ dummy: foo }", color, Defaults.color) 
    }

    // init int

    test ("Read init int") {
        check("{ init: 23 }", initInt, 23)
    }

    test ("Read default init int") {
        check("{ foo: bar }", initInt, Defaults.int)
    }

    // init float


    test ("Read init float") {
        check("{ init: 2.3 }", initFloat, 2.3f)
    }

    test ("Read default float int") {
        check("{ foo: bar }", initFloat, Defaults.float)
    }   

    // init boolean

    test ("Read init boolean") {
        check("{ init: true }", initBoolean, true)
    }

    test ("Read default init boolean") {
        check("{ foo: bar }", initBoolean, Defaults.boolean)
    }

    // text  
    test ("Read text") {
        check("{ text: start }", text, "start")
    }

    test ("Read default text") {
        check("{ foo: bar }", text, Defaults.string)
    }

    // int range
    test ("Read int range") {
        check("{ range: [0, 10] }", rangeInt, (0, 10))
    }
}

class GenericAttrs extends FunSuite {
    import Attr._

    def check[A](str: String, attr: Attr[A], res: A) = assert {
        val obj = Lang.read(str)
        obj.map(x => attr.run(x)) == Some(res)
    }

    test ("lift2") {
        check("{ color: red, init: 0.5 }", lift2((a: String, b: Float) => (a, b), color, initFloat), ("red", 0.5f))
    }

    test ("lift3") {
        check(
            "{ color: red, init: 0.5, text: Hi! }", 
            lift3((a: String, b: Float, c: String) => (a, b, c), 
            color, initFloat, text), 
            ("red", 0.5f, "Hi!"))
    }

    test ("lift4") {
        check(
            "{ color: red, init: 0.5, text: Hi! }", 
            lift4((a: String, b: Float, c: String, d: (Int, Int)) => (a, b, c, d), 
            color, initFloat, text, rangeInt), 
            ("red", 0.5f, "Hi!", Defaults.rangeInt))
    }
}