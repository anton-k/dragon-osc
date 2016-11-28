package dragon.osc.send

import java.io.File

import scala.swing.audio.ui.{SetWidget, SetColor, GetWidget, SetText, SetTextList, MultiToggle}
import scala.audio.osc._
import dragon.osc.readargs._
import dragon.osc.color._

case class OscClientPool(clients: Map[String,OscClient], defaultClient: OscClient, selfClient: OscClient) {
    def close {
        clients.values.foreach(_.close)
        defaultClient.close
        selfClient.close
    }

    def getClient(name: String) = 
        if (name == "self") selfClient
        else clients.get(name).getOrElse(defaultClient)
}

case class OscMsg(client: String, address: String, args: List[Object]) {
    def echo {
        val argStr = args.map(_.toString).mkString(" ")
        println(s"${client} ${address} : ${argStr}")
    }
}

case class Osc(clients: OscClientPool, server: OscServer, debugMode: Boolean) {
    def close {
        clients.close        
        server.close
    }
   
    def send(msg: OscMsg) {
        if (debugMode) {
            msg.echo
        }
        clients.getClient(msg.client).dynamicSend(msg.address, msg.args)
    }

    def addListener[A](id: String, widget: SetWidget[A])(implicit codec: MessageCodec[A]) {
        server.listen[A](s"/${id}")(msg => widget.set(msg, true))(codec)
        server.listen[A](s"/cold/${id}")(msg => widget.set(msg, false))(codec)
    }

    def addStringListener(id: String, widget: SetWidget[String])(implicit codec: MessageCodec[String]) {
        addListener(id, widget)(codec)
    }

    def addFileListener(id: String, widget: SetWidget[File])(implicit codec: MessageCodec[String]) {
        def go(prefix: String, fireCallback: Boolean) {
            server.listen[String](s"${prefix}/${id}")(msg => {
                val file = new File(msg)
                if (file.exists) {
                    widget.set(file, fireCallback)
                }
            })            
        }
        go("", true)
        go("/cold", false)
    }

    def addColorListener(id: String, widget: SetColor) {
        server.listen[String](s"/${id}/set-color")(colorName => widget.setColor(Palette.palette(colorName)))
    }

    def addTextListener(id: String, widget: SetText) {
        server.listen[String](s"/${id}/set-text")(name => widget.setText(name))
    }

    def addTextListListener(id: String, widget: SetTextList) {
        server.listen[(Int,String)](s"/${id}/set-text-list")({ case (pos, name) => widget.setTextAt(pos, name) })
    }

    def addToggleListener[A <: SetWidget[Boolean] with GetWidget[Boolean]](id: String, widget: A) {
        def go(prefix: String, isFireCallback: Boolean) {
            server.listen[Unit](s"${prefix}/${id}/toggle"){ msg => 
                val current = widget.get
                widget.set(!current, isFireCallback)
            }
        }

        go("", true)
        go("/cold", false)
    }   

    def addFloatListener[A <: SetWidget[Float] with GetWidget[Float]](id: String, widget: A)(implicit codec: MessageCodec[Float]) {
        def go(prefix: String, isFireCallback: Boolean) {
            server.listen[Float](s"${prefix}/${id}/add-float") { value =>
                val current = widget.get
                widget.set(current + value, isFireCallback)
            }
        }
        go("", true)
        go("/cold", false)
    }

    def addFloatListener2[A <: SetWidget[(Float,Float)] with GetWidget[(Float,Float)]](id: String, widget: A)(implicit codec: MessageCodec[(Float,Float)]) {
        def go(prefix: String, isFireCallback: Boolean) {
            server.listen[(Float,Float)](s"${prefix}/${id}/add-float") { value =>
                val current = widget.get
                widget.set((current._1 + value._1, current._2 + value._2), isFireCallback)
            }
        }
        go("", true)
        go("/cold", false)
    }    

    type Float2 = (Float, Float)

    def addFloatListener4[A <: SetWidget[(Float2,Float2)] with GetWidget[(Float2,Float2)]](id: String, widget: A)(implicit codec: MessageCodec[(Float2,Float2)]) {
        def go(prefix: String, isFireCallback: Boolean) {
            server.listen[((Float,Float), (Float,Float))](s"${prefix}/${id}/add-float") { value =>
                val current = widget.get
                widget.set(((current._1._1 + value._1._1, current._1._2 + value._1._2), (current._2._1 + value._2._1, current._2._2 + value._2._2)), isFireCallback)
            }
        }
        go("", true)
        go("/cold", false)
    } 

    def addIntListener[A <: SetWidget[Int] with GetWidget[Int]](id: String, widget: A)(implicit codec: MessageCodec[Int]) {
        def go(prefix: String, isFireCallback: Boolean) {
            server.listen[Int](s"${prefix}/${id}/add-int") { value =>
                val current = widget.get
                widget.set(current + value, isFireCallback)
            }
        }
        go("", true)
        go("/cold", false)
    }  

    def addMultiToggleListener(id: String, widget: MultiToggle)(implicit codec: MessageCodec[(Int, Int)]) {
         def go(prefix: String, isFireCallback: Boolean) {
            server.listen[(Int,Int)](s"${prefix}/${id}/multi-toggle"){ msg => 
                val current = widget.getAt(msg)
                widget.set((msg, !current), isFireCallback)
                println("got it")
            }
        }

        go("", true)
        go("/cold", false)       
    }

}

object Osc {
    def apply(args: Args): Osc = {
        val selfClient = OscClient(args.outPort)
        val defaultClient = selfClient     
        Thread.sleep(10)   
        println("server GET")
        val oscServer = OscServer.init(args.outPort).get
        Thread.sleep(10)           
        val clientPool = OscClientPool(args.inPort.map({ case (k, v) => (k, OscClient(v)) }), defaultClient, selfClient)
        Osc(clientPool, oscServer, args.debugMode)
    }
}