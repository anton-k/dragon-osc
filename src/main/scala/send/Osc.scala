package dragon.osc.send

import scala.swing.audio.ui.{SetWidget, SetColor, GetWidget}
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

    def addColorListener(id: String, widget: SetColor) {
        server.listen[String](s"/${id}/color")(colorName => widget.setColor(Palette.palette(colorName)))
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
}

object Osc {
    def apply(args: Args): Osc = {
        val defaultClient = OscClient(args.inPort)
        val selfClient = OscClient(args.outPort)
        val oscServer = OscServer(args.outPort)        
        val clientPool = OscClientPool(Map[String,OscClient](), defaultClient, selfClient)
        Osc(clientPool, oscServer, args.debugMode)
    }
}