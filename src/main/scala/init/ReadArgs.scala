package dragon.osc.readargs

import java.io._
import scala.util.Try

case class Args(inPort: Int, outPort: Int, filename: String, debugMode: Boolean = false)

object Utils {
    def resourceFile(p: String): String = 
        Option(getClass.getResource(p).getPath()).getOrElse(throw new FileNotFoundException(p))
}

object ReadArgs {
    def apply(args: Array[String]) = {
        args.foreach(x => println(x))    
        val inPort = Try {
                args(0).toInt
            } getOrElse { println("Error: no port set"); 44720 }

        val filename = Try {
                args(1)
            } getOrElse { Utils.resourceFile("/ui.yaml") }

        val outPort = 7711

        val debugMode = true
        Args(inPort, outPort, filename, debugMode)
    }
}