import scala.util.Try

case class Args(inPort: Int, outPort: Int, filename: String)

object ReadArgs {
    def apply(args: Array[String]) = {
        args.foreach(x => println(x))    
        val inPort = Try {
                args(0).toInt
            } getOrElse { println("Error: no port set"); 44720 }

        val filename = Try {
                args(1)
            } getOrElse { Utils.resourceFile("/ui-easy.yaml") }

        val outPort = 7711
        Args(inPort, outPort, filename)
    }
}