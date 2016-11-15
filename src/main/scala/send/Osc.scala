package dragon.osc.send

import scala.audio.osc._
import scala.swing.audio.parse.arg.{OscAddress, ClientId, OutsideClientId, SelfClient}
import dragon.osc.input.{SetupOscServer, InputBase}
import dragon.osc.readargs._

case class OscClientPool(clients: Map[String,OscClient], defaultClient: OscClient, selfClient: OscClient) {
    def channel[A](oscAddress: OscAddress)(implicit codec: MessageCodec[A]): Channel[A] = 
        getClient(oscAddress.clientId).channel[A](oscAddress.address)(codec)

    def getClient(clientId: Option[ClientId]) = clientId match {        
        case Some(SelfClient) => selfClient
        case Some(OutsideClientId(name)) => clients.get(name) match {
            case Some(c) => c
            case None    => throw new Exception(s"Client with name ${name} not found")
        }
        case _ => defaultClient
    }

    def close {
        clients.values.foreach(_.close)
        defaultClient.close
        selfClient.close
    }
}

case class Osc(clients: OscClientPool, server: OscServer) {
    def close {
        clients.close        
        server.close
    }

    def addListeners(inputBase: InputBase) {
        SetupOscServer.addListeners(server, inputBase)
    }

    def channel[A](oscAddress: OscAddress)(implicit codec: MessageCodec[A]): Channel[A] = clients.channel[A](oscAddress)(codec)

    def dynamicSend(oscAddress: OscAddress, args: List[Object]) {
        clients.getClient(oscAddress.clientId).dynamicSend(oscAddress.address, args)
    }
}

object Osc {
    def apply(args: Args): Osc = {
        val defaultClient = OscClient(args.inPort)
        val selfClient = OscClient(args.outPort)
        val oscServer = OscServer(args.outPort)        
        val clientPool = OscClientPool(Map[String,OscClient](), defaultClient, selfClient)
        Osc(clientPool, oscServer)
    }
}