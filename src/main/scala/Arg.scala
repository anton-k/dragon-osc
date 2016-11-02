import org.yaml.snakeyaml.Yaml
import java.io.{FileInputStream, File}
import collection.JavaConverters._
import collection.JavaConversions._ 
import scala.util.Try
import dragon.osc.const.Names
import dragon.osc.act._


package scala.swing.audio.parse {

package object arg {
    
type Range = (Float, Float)

object UiDecl {
    def apply(x: Object): UiDecl = new UiDecl {
        val obj = x
    }

    def loadFile(filename: String) = UiDecl(Yaml.loadFile(filename))   
}

trait UiDecl {
    val obj: Object

    def isList = this match {
        case UiList(_) => true
        case _ => false
    }
}

class UiList(val obj: Object) extends UiDecl

object UiList {
    def unapply(decl: UiDecl): Option[List[UiDecl]] = 
        Yaml.readList(decl.obj).map(x => x.map(y => UiDecl(y)))
} 

class UiIntList(val obj: Object) extends UiDecl

object UiIntList {
    def unapply(decl: UiDecl): Option[List[Int]] = decl match { 
        case UiList(xs) => {
            val init: Option[List[Int]] = Some(List[Int]())
            xs.foldRight(init) { (ma, mb) => for {
                    a <- ma match { case UiInt(n) => Some(n); case _ => None }
                    b <- mb            
                } yield (a :: b)
            }
        }            
        case _ => None
    }
}

class UiStringList(val obj: Object) extends UiDecl

object UiStringList {
    def unapply(decl: UiDecl): Option[List[String]] = decl match { 
        case UiList(xs) => {
            val init: Option[List[String]] = Some(List[String]())
            xs.foldRight(init) { (ma, mb) => for {
                    a <- ma match { case UiString(n) => Some(n); case _ => None }
                    b <- mb            
                } yield (a :: b)
            }
        }            
        case _ => None
    }
}

class UiSym(val obj: Object) extends UiDecl

object UiSym {
    def unapply(decl: UiDecl): Option[Sym] = Yaml.readMap(decl.obj).map { x =>        
        val (name, obj) = x.toList.head
        val (widgetName, id) = splitId(name)
        val act = Act.fromMap(x.mapValues(x => UiDecl(x)))
        Sym(widgetName, id, UiDecl(obj), act)
    }

    private def splitId(name: String) = {
        val ar = name.split(" ").filter(_ != "")        
        if (ar.length < 2) (name, None)
        else 
            if (Names.idSet.contains(ar(0))) (ar(0), Some(ar(1)))
            else (name, None)
    }
}

class UiMap(val obj: Object) extends UiDecl

object UiMap {
    def unapply(decl: UiDecl): Option[Map[String,UiDecl]] = Yaml.readMap(decl.obj).map { x =>
        x.map { case (name, x) => (name, UiDecl(x)) }
    }
}

class UiAct(val obj: Object) extends UiDecl

object UiAct {
    def unapply(decl: UiDecl): Option[Act] = decl match {
        case UiMap(m) => Act.fromMap(m)
        case _        => None
    }
}

class UiInt(val obj: Object) extends UiDecl

object UiInt {
    def unapply(decl: UiDecl): Option[Int] = Yaml.readInt(decl.obj)    
}

class UiFloat(val obj: Object) extends UiDecl

object UiFloat {
    def unapply(decl: UiDecl): Option[Float] = Yaml.readFloating(decl.obj)    
}

class UiOscAddress(val obj: Object) extends UiDecl

object UiOscAddress {
    def unapply(decl: UiDecl): Option[String] = Yaml.readString(decl.obj).flatMap { x => 
        if (x.startsWith("/")) Some(x) else None
    }    
}

class UiString(val obj: Object) extends UiDecl

object UiString {
    def unapply(decl: UiDecl): Option[String] = Yaml.readString(decl.obj).flatMap { x => 
        if (!x.startsWith("/")) Some(x) else None
    }        
}

class UiBoolean(val obj: Object) extends UiDecl

object UiBoolean {
    def unapply(decl: UiDecl): Option[Boolean] = Yaml.readBoolean(decl.obj)    
}


trait Sym {
    val name: String
    val id: Option[String]
    val body: UiDecl 
    val act: Option[Act]

    def isName(str: String) = 
        if (this.name == str)
            Some(this.body)
        else 
            None

    def isNameList(str: String) =
        this.isName(str).flatMap { 
            case UiList(items) => Some(items)
            case s @ UiString(_) => Some(List(s))
            case s @ UiInt(_) => Some(List(s))
            case s @ UiOscAddress(_) => Some(List(s))
            case s @ UiFloat(_) => Some(List(s))
            case s @ UiBoolean(_) => Some(List(s))
            case _ => None
        }

    def isArgList[A](str: String)(args: Arg[A]) = {
        isNameList(str).flatMap { xs => args.on(xs) }
    }

    def floatValue[A](name: String, mk: (Option[Float], Option[String], OscFloat) => A): Option[A] = this.isArgList(name) {
        for {
            init   <- Arg.float.orElse
            color  <- Arg.string.orElse
            osc    <- Arg.oscAddress
            range  <- Arg.float2.getOrElse((0.0f, 1.0f))
        } yield mk(init, color, OscFloat(osc, range))
    }

    def floatRangeValue[A](name: String, mk: ((Float,Float), Option[String], OscFloat) => A): Option[A] = this.isArgList(name) {
        for {
            init  <- Arg.float2
            color <- Arg.string.orElse
            osc   <- Arg.oscAddress
            range <- Arg.float2.getOrElse((0.0f, 1.0f)) 
        } yield mk(init, color, OscFloat(osc, range))
    }

    private def getCommandName(cmd: String): Option[List[String]] = this.name.split(" ").map(_.trim).toList match {
        case a :: rest => if (a == cmd) Some(rest) else None
        case _           => None
    }

    def isAlias = getCommandName("let").map(x => {println(x); (x.head, this.body)})

    def isRef = 
        if (this.body.obj == null) 
            Some(List())
        else this.body match {
            case UiList(items) => Some(items)
            case _             => None
        }

    private def getInt(str: String) = Try(str.toInt).toOption
    private def getFloat(str: String) = Try(str.toDouble.toFloat).toOption
    private def getBoolean(str: String) = Try(str.toBoolean).toOption

    def isSetter: Option[SetParam] = 
        getCommandName("set-color").map(x => SetColor(x.head)) orElse
        getCommandName("set-init-float").flatMap(x => getFloat(x.head).map(SetInitFloat)) orElse 
        getCommandName("set-init-bool").flatMap(x => getBoolean(x.head).map(SetInitBoolean)) orElse
        getCommandName("set-init-int").flatMap(x => getInt(x.head).map(SetInitInt)) orElse
        getCommandName("set-title").map(x => SetTitle(x.mkString(" "))) orElse
        getCommandName("set-osc").flatMap(x => getInt(x.head).map(SetOscClient))

}

object Sym {
    def apply(n: String, i: Option[String], b: UiDecl, a: Option[Act]): Sym = new Sym {
        val name = n
        val id = i
        val body = b
        val act = a
    }    
}


trait SetParam
case class SetColor(color: String) extends SetParam
case class SetInitFloat(init: Float) extends SetParam
case class SetInitBoolean(init: Boolean) extends SetParam
case class SetInitInt(init: Int) extends SetParam
case class SetTitle(title: String) extends SetParam
case class SetOscClient(clientId: Int) extends SetParam

case class Arg[+A](state: State[List[UiDecl],Option[A]]) {
    def map[B](f: A => B) = Arg(this.state.map(x => x.map(f)))

    def flatMap[B](f: A => Arg[B]) = Arg[B]{ this.state.flatMap { ma => ma match {
            case Some(a) => f(a).state
            case None    => State.pure(None)
        }}
    }

    def filter[S >: A](pred: S => Boolean): Arg[S] = Arg[S]{ this.state.map(x => x.filter(pred)) }

    def on(argList: List[UiDecl]): Option[A] = {
        val res = this.state.run(argList)
        if (res._2.isEmpty) res._1 else None
    }

    def orElse: Arg[Option[A]] = Arg(this.state.map(x => Some(x)))

    def getOrElse[S >: A](other: S): Arg[S] = Arg(this.state.map {
            case None => Some(other)
            case Some(x) => Some(x)
        })
}


private object Utils {
    def fromRelative(range: Range)(x: Float) = {
        val minValue = range._1
        val maxValue = range._2
        minValue + (maxValue - minValue) * x
    }    
}

case class OscAddress(address: String, clientId: Int = 0)

case class OscFloat(oscAddress: OscAddress, range: Range = (0, 1)) extends DefaultOscSend {
    def fromRelative(x: Float) = Utils.fromRelative(range)(x)
}

case class OscFloat2(oscAddress: OscAddress, rangeX: Range, rangeY: Range) extends DefaultOscSend {
    def fromRelative(p: (Float, Float)) = (Utils.fromRelative(rangeX)(p._1), Utils.fromRelative(rangeY)(p._2))
}

trait DefaultOscSend

case class OscBoolean(oscAddress: OscAddress) extends DefaultOscSend
case class OscInt(oscAddress: OscAddress) extends DefaultOscSend

object Arg {

    def apply[A](f: List[UiDecl] => (Option[A], List[UiDecl])): Arg[A] = Arg(new State[List[UiDecl],Option[A]] { 
        def run(s: List[UiDecl]) = f(s) 
    })
    
    def pair[A,B](ma: Arg[A], mb: Arg[B]): Arg[(A,B)] = for {
        a <- ma
        b <- mb
    } yield (a, b)

    def many[A](ma: Arg[A]): Arg[List[A]] = Arg { new State[List[UiDecl],Option[List[A]]] {
        def run(xs: List[UiDecl]) = {
            val (a, rest) = ma.state.run(xs)
            a match {
                case None => (Some(Nil), xs)
                case Some(x) => {
                    val (as, rest2) = many(ma).state.run(rest)
                    as match {
                        case None => (Some(Nil), rest2)
                        case Some(xs) => (Some(x :: xs), rest2)
                    }
                }
            }
        } 
    }}

    
    def int: Arg[Int] = Arg{ xs => 
        xs match {
            case UiInt(n) :: rest => (Some(n), rest)
            case _  => (None, xs)
        }
    } 

    def boolean: Arg[Boolean] = Arg{ xs => 
        xs match {
            case UiBoolean(n) :: rest => (Some(n), rest)
            case _  => (None, xs)
        }
    }

    def intList: Arg[List[Int]] = Arg{ xs => 
        xs match {
            case UiIntList(n) :: rest => (Some(n), rest)
            case _  => (None, xs)
        }
    } 

    def intListOrEmpty: Arg[List[Int]] = intList.getOrElse(List())

    def float: Arg[Float] = Arg{ xs => 
        xs match {
            case UiFloat(n) :: rest => (Some(n), rest)
            case UiInt(n)   :: rest => (Some(n.toFloat), rest)
            case _  => (None, xs)
        }
    } 

    def float2: Arg[(Float, Float)] = pair(float, float)

    def string: Arg[String] = Arg{ xs => 
        xs match {
            case UiString(n) :: rest => (Some(n), rest)
            case _  => (None, xs)
        }
    }

    def stringList: Arg[List[String]] = Arg{ xs => 
        xs match {
            case UiStringList(n) :: rest => (Some(n), rest)
            case _  => (None, xs)
        }
    } 

    def stringListOrEmpty: Arg[List[String]] = stringList.getOrElse(List())

    def oscAddress: Arg[OscAddress] = Arg{ xs => 
        xs match {
            case UiOscAddress(n) :: rest => (Some(n), rest)
            case _  => (None, xs)
        }
    }.map(x => OscAddress(x))
}

object Yaml {
    def loadFile(filename: String) = {
        val input = new FileInputStream(new File(filename))
        val yaml = new Yaml()
        val res = yaml.load(input)
        input.close()
        res
    }

    def readList(x: Object) = Try {
        val q: collection.mutable.Seq[Object] = x.asInstanceOf[java.util.List[Object]]
        q.toList
    }.toOption

    def readMap(x: Object) = Try {
        val q: collection.mutable.Map[String,Object] = x.asInstanceOf[java.util.Map[String,Object]]
        q.toMap
    }.toOption   

    def readInt(x: Object) = Try {
        x.asInstanceOf[Int]
    }.toOption   

    def readFloating(x: Object) = 
        readDouble(x).map(_.toFloat) orElse readFloat(x)

    def readDouble(x: Object) = Try {
        x.asInstanceOf[Double]
    }.toOption

    def readFloat(x: Object) = Try {
        x.asInstanceOf[Float]
    }.toOption

    def readString(x: Object) = Try {
        x.asInstanceOf[String]
    }.toOption

    def readBoolean(x: Object) = Try {
        x.asInstanceOf[Boolean]
    }.toOption
}

}

}