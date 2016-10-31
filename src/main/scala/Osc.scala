import scala.audio.osc._
import scala.swing.audio.parse.arg.OscAddress

case class OscClientPool(clients: List[OscClient]) {
    def channel[A](oscAddress: OscAddress)(implicit codec: MessageCodec[A]): Channel[A] = 
        if (oscAddress.clientId >= clients.length) {
            throw new Exception(s"The osc client id is out of bounds ${oscAddress.clientId}")
        } else {
            clients(oscAddress.clientId).channel[A](oscAddress.address)(codec)
        }

    def close = clients.foreach(_.close)
}