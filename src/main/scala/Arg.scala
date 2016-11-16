
import dragon.osc.parse.const.Names
import dragon.osc.act._
import dragon.osc.color._
import java.awt.Color
import dragon.osc.state._
import dragon.osc.parse.yaml.ReadYaml
import scala.util.Try

package scala.swing.audio.parse {

package object arg {
    
type Range = (Float, Float)

object UiDecl {
    def apply(x: Object): UiDecl = new UiDecl {
        val obj = x
    }

    def loadFile(filename: String) = UiDecl(ReadYaml.loadFile(filename))   
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
        ReadYaml.readList(decl.obj).map(x => x.map(y => UiDecl(y)))
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
    def unapply(decl: UiDecl): Option[Sym] = ReadYaml.readMap(decl.obj).map { x =>        
        val (name, obj) = x.toList.head
        val (widgetName, id) = splitId(name)
        val act = Act.fromMap(Settings(), x.mapValues(x => UiDecl(x)))
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
    def unapply(decl: UiDecl): Option[Map[String,UiDecl]] = ReadYaml.readMap(decl.obj).map { x =>
        x.map { case (name, x) => (name, UiDecl(x)) }
    }
}

class UiAct(val obj: Object) extends UiDecl

object UiAct {
    def unapply(decl: UiDecl): Option[Act] = decl match {
        case UiMap(m) => Act.fromMap(Settings(), m)
        case _        => None
    }
}

class UiInt(val obj: Object) extends UiDecl

object UiInt {
    def unapply(decl: UiDecl): Option[Int] = ReadYaml.readInt(decl.obj)    
}

class UiFloat(val obj: Object) extends UiDecl

object UiFloat {
    def unapply(decl: UiDecl): Option[Float] = ReadYaml.readFloating(decl.obj)    
}

class UiOscAddress(val obj: Object) extends UiDecl

object UiOscAddress {
    private def isOscAddress(str: String) = str.startsWith("/")

    def unapply(decl: UiDecl): Option[OscAddress] = decl match {
        case UiAnyString(str)                          if isOscAddress(str) => Some(OscAddress(str))
        case UiList(List(UiString(n), UiAnyString(str)))  if isOscAddress(str) => Some(OscAddress(str, Some(ClientId.fromString(n))))
        case UiList(List(UiInt(n), UiAnyString(str)))  if isOscAddress(str) => Some(OscAddress(str, Some(ClientId.fromString(n.toString))))
        case _ => None
    }
}

class UiRef(val obj: Object) extends UiDecl

object UiRef {
    def unapply(decl: UiDecl): Option[String] = decl match {
        case UiAnyString(str) if str.startsWith("$") => Some(str.drop(1))
        case _ => None
    }   
}

class UiArgRef(val obj: Object) extends UiDecl

object UiArgRef {
    private def readInt(s: String) = Try{ s.toInt }.toOption

    def unapply(decl: UiDecl): Option[Int] = decl match {
        case UiAnyString(str) if str.startsWith("$") => readInt(str.drop(1))
        case _ => None
    }   
}

class UiString(val obj: Object) extends UiDecl

object UiString {
    def unapply(decl: UiDecl): Option[String] = decl match {
        case UiAnyString(str) if (!str.startsWith("/")) => Some(str)
        case _ => None
    }
}

class UiAnyString(val obj: Object) extends UiDecl

object UiAnyString {
    def unapply(decl: UiDecl): Option[String] = ReadYaml.readString(decl.obj)
}

class UiBoolean(val obj: Object) extends UiDecl

object UiBoolean {
    def unapply(decl: UiDecl): Option[Boolean] = ReadYaml.readBoolean(decl.obj)    
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

    def isArgList[A](settings: Settings, str: String)(args: Arg[A]) = {
        isNameList(str).flatMap { xs => args.on(ArgSt(settings, xs)) }
    }

    def floatValue[A](settings: Settings, name: String, mk: (Option[Float], Option[String], OscFloat) => A): Option[A] = this.isArgList(settings, name) {
        for {
            init   <- Arg.float.orElse
            color  <- Arg.string.orElse
            osc    <- Arg.oscAddress
            range  <- Arg.float2.getOrElse((0.0f, 1.0f))
        } yield mk(init, color, OscFloat(osc, range))
    }

    def floatRangeValue[A](settings: Settings, name: String, mk: ((Float,Float), Option[String], OscFloat) => A): Option[A] = this.isArgList(settings, name) {
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


case class Settings(
    initColor: Option[String] = None, 
    initFloat: Option[Float] = None, 
    initBoolean: Option[Boolean] = None, 
    initInt: Option[Int] = None,
    title: Option[String] = None,
    oscClient: Option[Int] = None) {

    def setClientId(osc: OscAddress): OscAddress = oscClient match {
        case None => osc
        case Some(n) => osc.copy(clientId = Some(OutsideClientId(n.toString)))
    }

    def setClientId(osc: OscFloat): OscFloat = osc.copy(oscAddress = setClientId(osc.oscAddress))    
    def setClientId(osc: OscFloat2): OscFloat2 = osc.copy(oscAddress = setClientId(osc.oscAddress))    
    def setClientId(osc: OscBoolean): OscBoolean = osc.copy(oscAddress = setClientId(osc.oscAddress))    
    def setClientId(osc: OscInt): OscInt = osc.copy(oscAddress = setClientId(osc.oscAddress))   

    def setClientId(osc: DefaultOscSend): DefaultOscSend = osc match {
        case x: OscBoolean => setClientId(x)
        case x: OscFloat   => setClientId(x)
        case x: OscFloat2  => setClientId(x)
        case x: OscInt     => setClientId(x)
    }

    def setClientId(act: Option[Act]): Option[Act] = act.map { a => a.mapDefaultSend(x => this.setClientId(x)) }
}

trait SetParam
case class SetColor(color: String) extends SetParam
case class SetInitFloat(init: Float) extends SetParam
case class SetInitBoolean(init: Boolean) extends SetParam
case class SetInitInt(init: Int) extends SetParam
case class SetTitle(title: String) extends SetParam
case class SetOscClient(clientId: Int) extends SetParam

case class ArgSt(settings: Settings, args: List[UiDecl])

case class Arg[+A](state: State[ArgSt,Option[A]]) { self =>
    def map[B](f: A => B) = Arg(this.state.map(x => x.map(f)))

    def flatMap[B](f: A => Arg[B]) = Arg[B]{ this.state.flatMap { ma => ma match {
            case Some(a) => f(a).state
            case None    => State.pure(None)
        }}
    }

    def filter[S >: A](pred: S => Boolean): Arg[S] = Arg[S]{ this.state.map(x => x.filter(pred)) }

    def on(st: ArgSt): Option[A] = {
        val res = this.state.run(st)
        if (res._2.args.isEmpty) res._1 else None
    }

    def withSettings[B >: A](recover: Settings => B) = Arg[B] { (st: ArgSt) => {
        val (res, st2) = self.state.run(st)
        res match {
            case Some(a) => (res, st2)
            case None    => (Some(recover(st2.settings)), st2)
        }
    }}

    def orElse: Arg[Option[A]] = Arg(this.state.map(x => Some(x)))

    def ||[B >: A](that: Arg[B]) = Arg[B] { new State[ArgSt,Option[B]] {
        def run(s1: ArgSt) = {
            val (optA, s2) = self.state.run(s1)
            optA match {
                case Some(a) => (optA, s2)
                case None    => that.state.run(s1)                
            }
        }
    }}

    def getOrElse[S >: A](other: S): Arg[S] = Arg(this.state.map {
            case None => Some(other)
            case Some(x) => Some(x)
        })

    def eval(settings: Settings, xs: List[UiDecl]): Option[A] = 
        state.eval(ArgSt(settings, xs))

    def eval(settings: Settings, x: UiDecl): Option[A] = x match {
        case UiList(xs) => eval(settings, xs)
        case _          => None
    }

    def eval(raw: String): Option[A] = eval(Settings(), UiDecl(ReadYaml.loadString(raw)))
}


private object Utils {
    def fromRelative(range: Range)(x: Float) = {
        val minValue = range._1
        val maxValue = range._2
        minValue + (maxValue - minValue) * x
    }    
}

object ClientId {
    def fromString(str: String) = 
        if (str == "self") SelfClient
        else OutsideClientId(str)
}

trait ClientId 
case class OutsideClientId(name: String) extends ClientId
case object SelfClient extends ClientId

case class OscAddress(address: String, clientId: Option[ClientId] = None)

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
    def onList[A](f: List[UiDecl] => (Option[A], List[UiDecl])): Arg[A] = Arg(new State[ArgSt,Option[A]] { 
        def run(s: ArgSt) = {
            val (res, args2) = f(s.args)
            (res, s.copy(args = args2))
        } 
    })    

    def apply[A](f: ArgSt => (Option[A], ArgSt)): Arg[A] = Arg(new State[ArgSt,Option[A]] { 
        def run(s: ArgSt) = f(s) 
    })
    
    def pair[A,B](ma: Arg[A], mb: Arg[B]): Arg[(A,B)] = for {
        a <- ma
        b <- mb
    } yield (a, b)

    def many[A](ma: Arg[A]): Arg[List[A]] = Arg { new State[ArgSt,Option[List[A]]] {
        def run(xs: ArgSt) = {
            val (a, rest) = ma.state.run(xs)
            if (rest.args.isEmpty || xs.args.length == rest.args.length) {
                a match {
                    case None => (Some(Nil), rest)
                    case Some(x) => (Some(List(x)), rest)
                }
            } else {
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

        } 
    }}

    
    def int: Arg[Int] = Arg.onList { xs => 
        xs match {
            case UiInt(n) :: rest => (Some(n), rest)
            case _  => (None, xs)
        }
    } 

    def boolean: Arg[Boolean] = Arg.onList { xs => 
        xs match {
            case UiBoolean(n) :: rest => (Some(n), rest)
            case _  => (None, xs)
        }
    }

    def intList: Arg[List[Int]] = Arg.onList { xs => 
        xs match {
            case UiIntList(n) :: rest => (Some(n), rest)
            case _  => (None, xs)
        }
    } 

    def intListOrEmpty: Arg[List[Int]] = intList.getOrElse(List())

    def float: Arg[Float] = Arg.onList { xs => 
        xs match {
            case UiFloat(n) :: rest => (Some(n), rest)
            case UiInt(n)   :: rest => (Some(n.toFloat), rest)
            case _  => (None, xs)
        }
    } 

    def float2: Arg[(Float, Float)] = pair(float, float)

    def string: Arg[String] = Arg.onList { xs => 
        xs match {
            case UiString(n) :: rest => (Some(n), rest)
            case _  => (None, xs)
        }
    }

    def stringList: Arg[List[String]] = Arg.onList { xs => 
        xs match {
            case UiStringList(n) :: rest => (Some(n), rest)
            case _  => (None, xs)
        }
    } 

    def stringListOrEmpty: Arg[List[String]] = stringList.getOrElse(List())

    def memRef: Arg[String] = Arg.onList { xs =>
        xs match {
            case UiRef(str) :: rest => (Some(str), rest)
            case _ => (None, xs)
        }
    }

    def argRef: Arg[Int] = Arg.onList { xs =>
        xs match {
            case UiArgRef(n) :: rest => (Some(n), rest)
            case _ => (None, xs)
        }
    }

    def initBoolean: Arg[Boolean] = Arg.boolean.withSettings { settings => 
        settings.initBoolean.getOrElse(false)
    }

    def initInt: Arg[Int] = Arg.int.withSettings { settings =>
        settings.initInt.getOrElse(1)
    }

    def initFloat: Arg[Float] = Arg.float.withSettings { settings =>
        settings.initFloat.getOrElse(0.5f)
    }

    def initFloat2: Arg[(Float, Float)] = Arg.float2.withSettings { settings =>
        settings.initFloat.map(x => (x, x)).getOrElse((0.5f, 0.5f))
    }

    def color: Arg[Color] = Arg.string.withSettings { settings =>
        settings.initColor.getOrElse("blue")
    }.map(Palette.palette)

    def oscAddress: Arg[OscAddress] = Arg { st => 
        st.args match {
            case UiOscAddress(n) :: rest => (Some(st.settings.setClientId(n)), st.copy(args = rest))
            case _  => (None, st)
        }
    }
}

}

}