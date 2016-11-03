package dragon.osc

import scala.audio.osc._
import scala.swing.audio.parse.arg.{OscAddress, OutsideClientId}
import dragon.osc.input.{SetupOscServer, InputBase}

case class OscClientPool(clients: Map[String,OscClient]) {
    def channel[A](oscAddress: OscAddress)(implicit codec: MessageCodec[A]): Channel[A] = {
        val defaultClient = clients.values.head      
        val client = oscAddress.clientId.flatMap(id => id match {
            case OutsideClientId(name) => clients.get(name)
            case _ => None
        }).getOrElse(defaultClient)
        client.channel[A](oscAddress.address)(codec)
    }

    def close = clients.values.foreach(_.close)
}

object Osc {
    def client(port: Int) = 
        OscClientPool(Map("0" -> OscClient(port)))

    def server(port: Int, inputBase: InputBase) = {
        val srv = OscServer(port)
        SetupOscServer.addListeners(srv, inputBase)
        srv
    }
}