
import dragon.osc.parse._
import dragon.osc.ui._
import dragon.osc.send._
import dragon.osc.readargs._

object App {

  def main(rawArgs: Array[String]) {  
    val args = ReadArgs(rawArgs)
    val st = St.init(args)
    val wins = Convert.convert(st, Parse.file(args.filename).get)
    wins.show(st.close)    
  }
}


