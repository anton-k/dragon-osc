import dragon.osc.parse._
import dragon.osc.parse.ui._
import dragon.osc.ui._
import dragon.osc.send._
import dragon.osc.readargs._
import dragon.osc.parse.const._


object App {

  def main(rawArgs: Array[String]) {
    val args = ReadArgs(rawArgs)

    Parse.file(args.file) match {
        case Some(parsed) => {
                val oscClients = GetOsc.getOsc(parsed)
                val st = St.init(args, oscClients)
                val wins = Convert.convert(st, parsed)
                wins.show(st, args)
            }
        case None => {
                println(Names.Error.failedToParse)
            }
    }
  }
}
