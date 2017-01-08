package dragon.osc.parse.syntax

import java.io._

import dragon.osc.parse.yaml._
import dragon.osc.parse.util._

import org.json4s._
import org.json4s.jackson.JsonMethods

trait Prim {
    def toObject: Object = this match {
        case PrimInt(x)     => x.asInstanceOf[Object]
        case PrimFloat(x)   => x.asInstanceOf[Object]
        case PrimBoolean(x) => x.asInstanceOf[Object]
        case PrimString(x)  => x.asInstanceOf[Object]
    }
}

case class PrimInt(value: Int) extends Prim
case class PrimFloat(value: Float) extends Prim
case class PrimBoolean(value: Boolean) extends Prim
case class PrimString(value: String) extends Prim

trait Lang {
    def getKey(key: String) = this match {
        case MapSym(m) => m.get(key)
        case _ => None
    }
}

case class PrimSym(value: Prim) extends Lang
case class ListSym(items: List[Lang]) extends Lang
case class MapSym(items: Map[String, Lang]) extends Lang

object Lang {
    def readFile(filename: String): Option[Lang] = LangYaml.readFile(filename) orElse LangJson.readFile(filename)
    def readFile(file: File): Option[Lang] = LangYaml.readFile(file) orElse LangJson.readFile(file)

    def read(str: String): Option[Lang] = LangYaml.read(str) orElse LangJson.read(str)
}

object LangYaml {
    def readFile(filename: String): Option[Lang] = read(ReadYaml.loadFile(filename))
    def readFile(file: File): Option[Lang] = read(ReadYaml.loadFile(file))

    def read(str: String): Option[Lang] = read(ReadYaml.loadString(str))

    def read(x: Object): Option[Lang] = 
        ReadYaml.readList(x).flatMap(xs => Util.optionMapM(xs)(read).map(ListSym)) orElse
        ReadYaml.readMap(x).flatMap(m => Util.optionMapM(m)(read).map(MapSym)) orElse
        readPrim(x).map(PrimSym)

    def readPrim(x: Object): Option[Prim] = 
        ReadYaml.readInt(x).map(PrimInt) orElse
        ReadYaml.readBoolean(x).map(PrimBoolean) orElse
        ReadYaml.readString(x).map(PrimString) orElse
        ReadYaml.readFloating(x).map(PrimFloat) 
}

object LangJson {
    def readFile(filename: String): Option[Lang] = json2lang(JsonMethods.parse(readString(filename)))
    def readFile(file: File): Option[Lang] = json2lang(JsonMethods.parse(readString(file)))

    private def readString(filename: String) = io.Source.fromFile(filename).mkString
    private def readString(filename: File) = io.Source.fromFile(filename).mkString

    def read(str: String): Option[Lang] = json2lang(JsonMethods.parse(str))

    private def json2lang(value: JValue): Option[Lang] = value match {
        case JNothing => None
        case JNull    => None
        case JString(str) => Some(PrimSym(PrimString(str)))
        case JDouble(d)   => Some(PrimSym(PrimFloat(d.toFloat)))
        case JDecimal(d)  => Some(PrimSym(PrimFloat(d.toFloat)))
        case JInt(n)      => Some(PrimSym(PrimInt(n.toInt)))
        case JLong(n)     => Some(PrimSym(PrimInt(n.toInt)))
        case JBool(b)     => Some(PrimSym(PrimBoolean(b)))
        case JObject(fields) => Util.optionMapM(fields)(parseField).map(xs => MapSym(xs.toMap))
        case JArray(values)  => Util.optionMapM(values)(json2lang).map(xs => ListSym(xs))   
    }

    private def parseField(a: JField): Option[(String, Lang)] = {
        val (name, value) = a
        json2lang(value).map(lang => (name, lang))
    }
}
