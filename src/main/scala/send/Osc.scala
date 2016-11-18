package dragon.osc.send

import scala.swing.audio.ui.SetWidget
import scala.audio.osc._
import dragon.osc.readargs._

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