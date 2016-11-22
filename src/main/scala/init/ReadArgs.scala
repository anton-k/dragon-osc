package dragon.osc.readargs

import java.io._
import scala.util.Try
import scopt.OptionParser

case class Args(
    inPort: Map[String,Int] = Map("self" -> 7711),
    outPort: Int = 7711, 
    file: File = new File("."), // new File(Utils.resourceFile("/ui.yaml")), 
    debugMode: Boolean = false, 
    lockClose: Boolean = false)

object Utils {
    def resourceFile(p: String): String = 
        Option(getClass.getResource(p).getPath()).getOrElse(throw new FileNotFoundException(p))
}

object ReadArgs {
    def apply(args: Array[String]): Args = {
        parser.parse(args, Args()) match {
            case Some(args) => { println(args.inPort); args }
            case None => { println("Please use --help argument for usage"); Args() }
        }
    }

    val parser = new scopt.OptionParser[Args]("dragon-osc") {
        head("dragon-osc", "0.1")

        opt[Map[String,Int]]('c',"clients").action( (x, c) => 
            c.copy(inPort = x) ).text("clients with names and ports in the format name1=<int>,name2=<int>,...")

        opt[Int]('s',"server").action(  (x, c) =>
            c.copy(outPort = x)).text("server is OSC port for server (the app listens on this port)")

        opt[File]('i', "input").required().valueName("<file>")
            .action( (x, c) => c.copy(file = x) ).text("file with layout in YAML format")

        opt[Unit]("verbose").action( (_, c) => c.copy(debugMode = true) ).text("print trace messages")

        opt[Unit]("lock-on-exit").action( (_, c) => c.copy(lockClose = true)).text("locks the screen on exit to enter the pass phrase.")

        help("help").text("some help")
        
    }
}