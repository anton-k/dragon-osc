import scala.swing._
import scala.swing.event._
import java.awt.{Color,Graphics2D,BasicStroke,Font}
import java.awt.geom._
import java.io._

import scala.swing.audio.ui._
import scala.swing.audio.convert.{Convert, Window}
import scala.audio.osc._

import swing._
import event._

import dragon.osc.Osc
import dragon.osc.input.SetupOscServer
import dragon.osc.readargs.ReadArgs
import dragon.osc.act.{Memory, St}

object App {

  def main(rawArgs: Array[String]) {  
    val args = ReadArgs(rawArgs)
    val osc = Osc(args)
    var memory = Memory.init
    val (wins, inputBase) = Convert.readFile(St(osc, memory), args.filename)    
    osc.addListeners(inputBase)

    wins.zipWithIndex.foreach { case (window, ix) => { 
        val ui = new MainFrame { self => 
            title = window.title.getOrElse("dragon" + ix.toString)
            contents = window.content

            window.size.foreach { case (width, height) =>                 
                self.minimumSize = new Dimension(width, height)
            }

            override def closeOperation {
                println("Close now")
                osc.close                  
                Thread.sleep(1)          
                System.exit(0)                
            }
        }
        ui.visible = true        
    }}
  }
}
