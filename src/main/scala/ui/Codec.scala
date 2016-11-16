package dragon.osc.ui

import scala.audio.osc._
import dragon.osc.parse.send._
import dragon.osc.send._

object Codec {
    def onFloat(st: St, optSend: Option[Send])   = onArg[Float](st, optSend)(MessageCodec.floatOscMessageCodec, ToPlainList.floatArg)
    def onInt(st: St, optSend: Option[Send])     = onArg[Int](st, optSend)(MessageCodec.intOscMessageCodec, ToPlainList.intArg)
    def onBoolean(st: St, optSend: Option[Send]) = onArg[Boolean](st, optSend)(MessageCodec.booleanOscMessageCodec, ToPlainList.booleanArg)
    def onString(st: St, optSend: Option[Send]) = onArg[String](st, optSend)(MessageCodec.stringOscMessageCodec, ToPlainList.stringArg)

    def onArg[A](st: St, optSend: Option[Send])(oscCodec: MessageCodec[A], caseCodec: ToPlainList[A]): (A => Unit) = {
        val specSend = optSend.map(st.compileSend)

        def cbk(input: A) {
            specSend.foreach(f => f(oscCodec.toMessage(input), caseCodec.toPlainList(input)))
        }
        cbk
    } }

trait ToPlainList[A] {
    def toPlainList(a: A): List[Object]
}

object ToPlainList {    
    implicit val floatArg = new ToPlainList[Float] { def toPlainList(a: Float) = List(a.asInstanceOf[Object]) }
    implicit val intArg = new ToPlainList[Int] { def toPlainList(a: Int) = List(a.asInstanceOf[Object]) }
    implicit val booleanArg = new ToPlainList[Boolean] { def toPlainList(a: Boolean) = List(a.asInstanceOf[Object]) }
    implicit val stringArg = new ToPlainList[String] { def toPlainList(a: String) = List(a.asInstanceOf[Object]) }
    implicit def tupleArg2[A,B](argA: ToPlainList[A], argB: ToPlainList[B]) = new ToPlainList[(A,B)] { def toPlainList(a: (A,B)) = argA.toPlainList(a._1) ++ argB.toPlainList(a._2) }
    implicit def tupleArg3[A,B,C](argA: ToPlainList[A], argB: ToPlainList[B], argC: ToPlainList[C]) = new ToPlainList[(A,B,C)] { def toPlainList(a: (A,B,C)) = argA.toPlainList(a._1) ++ argB.toPlainList(a._2) ++ argC.toPlainList(a._3) }
    implicit def tupleArg4[A,B,C,D](argA: ToPlainList[A], argB: ToPlainList[B], argC: ToPlainList[C], argD: ToPlainList[D]) = new ToPlainList[(A,B,C,D)] { def toPlainList(a: (A,B,C,D)) = argA.toPlainList(a._1) ++ argB.toPlainList(a._2) ++ argC.toPlainList(a._3) ++ argD.toPlainList(a._4) }

    def toStringArg[A](a: A)(implicit codec: ToPlainList[A]) =
        codec.toPlainList(a).map(_.toString).mkString(" ")        
}