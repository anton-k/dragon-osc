import dragon.osc.parse._
import dragon.osc.parse.{ui => P}
import dragon.osc.ui._
import dragon.osc.send._
import dragon.osc.readargs._
import dragon.osc.parse.const._


object App {
  def main(rawArgs: Array[String]) {
    Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
      def uncaughtException(t: Thread, e: Throwable) {
        println("Uncaught exception in thread: " + t.getName, e)
      }
    })

    def runInitMessages(st: St, root: P.Root) {
        st.sendNoInputMsgs(root.initMessages)
    }

    val args = ReadArgs(rawArgs)

    Parse.file(args.file) match {
        case Some(parsed) => {
                val oscClients = P.GetOsc.getOsc(parsed)
                val st = St.init(args, oscClients)
                runInitMessages(st, parsed)
                val wins = Convert.convert(st, parsed)
                wins.show(st, args, wins.runTerminateMessages(st, parsed))
            }
        case None => {
                println(Names.Error.failedToParse)
            }
    }
  }
}
