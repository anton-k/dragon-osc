
package dragon.osc.parse.yaml

import java.io.{FileInputStream, File}
import org.yaml.snakeyaml.Yaml
import collection.JavaConverters._
import collection.JavaConversions._ 
import scala.util.Try

object ReadYaml {

    def loadFile(filename: String): Object = loadFile(new File(filename))

    def loadFile(file: File): Object = {
        val input = new FileInputStream(file)
        val yaml = new Yaml()
        val res = yaml.load(input)
        input.close()
        res
    }

    def loadString(str: String) = {
        val yaml = new Yaml()
        yaml.load(str)
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