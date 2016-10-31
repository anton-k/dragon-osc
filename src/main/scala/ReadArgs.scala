import scala.util.Try

case class Args(port: Int, filename: String)

object ReadArgs {
    def apply(args: Array[String]) = {
        args.foreach(x => println(x))    
        val port = Try {
                args(0).toInt
            } getOrElse { println("Error: no port set"); 44720 }

        val filename = Try {
                args(1)
            } getOrElse { Utils.resourceFile("/ui-easy.yaml") }
        Args(port, filename)
    }
}