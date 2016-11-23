import dragon.osc.parse._
import dragon.osc.ui._
import dragon.osc.send._
import dragon.osc.readargs._
import dragon.osc.parse.const._

object App {

  def main(rawArgs: Array[String]) {  
    val args = ReadArgs(rawArgs)
    val st = St.init(args)

    Parse.file(args.file) match {
        case Some(parsed) => {
                val wins = Convert.convert(st, parsed)
                wins.show(st, args)
            }
        case None => {
                println(Names.Error.failedToParse)
                st.close
            } 
    }
  }
}
