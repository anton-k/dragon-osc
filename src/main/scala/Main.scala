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
import java.util.Date
import java.awt.Color
import java.text.SimpleDateFormat
import javax.swing.{Icon, ImageIcon}

import dragon.osc.Osc
import dragon.osc.input.SetupOscServer

object Utils {
    def resourceFile(p: String): String = 
        Option(getClass.getResource(p).getPath()).getOrElse(throw new FileNotFoundException(p))
}

object App {

  def main(rawArgs: Array[String]) {  
    val args = ReadArgs(rawArgs)
    val oscClient = Osc.client(args.inPort)
    val (wins, inputBase) = Convert.readFile(oscClient, args.filename)
    val oscServer = Osc.server(args.outPort, inputBase) 
    println("INPUT BASE:")
    println(inputBase) 

    wins.zipWithIndex.foreach { case (window, ix) => { 
        val ui = new MainFrame { self => 
            title = window.title.getOrElse("dragon" + ix.toString)
            contents = window.content

            window.size.foreach { case (width, height) => 
                println(s"Set sizes ${width} ${height}")
                self.minimumSize = new Dimension(width, height)
            }

            override def closeOperation {
                println("Close now")
                oscClient.close  
                oscServer.close
                Thread.sleep(1)          
                System.exit(0)                
            }
        }
        ui.visible = true        
    }}   

  }
}
