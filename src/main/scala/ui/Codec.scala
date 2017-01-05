package dragon.osc.ui

import java.io.File

import scala.audio.osc._
import dragon.osc.parse.send._
import dragon.osc.send._

object Codec {
    def unPair[A,B,C](f: ((A,B)) => C): (A,B) => C = (x, y) => f((x, y))

    val oscFloat2 = MessageCodec.tuple2(MessageCodec.floatOscMessageCodec, MessageCodec.floatOscMessageCodec)
    val argFloat2 = ToPlainList.tupleArg2(ToPlainList.floatArg, ToPlainList.floatArg)    

    val oscFloat4 = MessageCodec.tuple2(oscFloat2, oscFloat2)
    val argFloat4 = ToPlainList.tupleArg2(argFloat2, argFloat2) 

    val oscInt2 = MessageCodec.tuple2(MessageCodec.intOscMessageCodec, MessageCodec.intOscMessageCodec)
    val argInt2 = ToPlainList.tupleArg2(ToPlainList.intArg, ToPlainList.intArg)    
   
    val multiToggleOsc = MessageCodec.tuple2(oscInt2, MessageCodec.booleanOscMessageCodec)
    val multiToggleArg = ToPlainList.tupleArg2(argInt2, ToPlainList.booleanArg)

    implicit val fileMessageCodec = new MessageCodec[File] {
        def toMessage(msg: File) = ToPlainList.fileArg.toPlainList(msg)
        def fromMessage(msg: List[Object]) = (new File(msg.head.asInstanceOf[java.lang.String]), msg.tail)
    }


    def onFloat(st: St, optSend: Option[Send])   = onArg[Float](st, optSend)(MessageCodec.floatOscMessageCodec, ToPlainList.floatArg)    
    def onInt(st: St, optSend: Option[Send])     = onArg[Int](st, optSend)(MessageCodec.intOscMessageCodec, ToPlainList.intArg)
    def onBoolean(st: St, optSend: Option[Send]) = onArg[Boolean](st, optSend)(MessageCodec.booleanOscMessageCodec, ToPlainList.booleanArg)
    def onString(st: St, optSend: Option[Send])  = onArg[String](st, optSend)(MessageCodec.stringOscMessageCodec, ToPlainList.stringArg)
    def onFloat2(st: St, optSend: Option[Send])  = unPair(onArg[(Float,Float)](st, optSend)(oscFloat2, argFloat2))
    def onFloat4(st: St, optSend: Option[Send])  = unPair(onArg[((Float,Float), (Float,Float))](st, optSend)(oscFloat4, argFloat4))
    def onMultiToggle(st: St, optSend: Option[Send]) = unPair(onArg[((Int,Int), Boolean)](st, optSend)(multiToggleOsc, multiToggleArg))
    def onFile(st: St, optSend: Option[Send]) = onArg[File](st, optSend)(fileMessageCodec, ToPlainList.fileArg)
    def onInt2(st: St, optSend: Option[Send]) = unPair(onArg[(Int,Int)](st, optSend)(oscInt2, argInt2))

    def onArg[A](st: St, optSend: Option[Send])(oscCodec: MessageCodec[A], caseCodec: ToPlainList[A]): (A => Unit) = {
        val specSend = optSend.map(st.compileSend)

        def cbk(input: A) {
            specSend.foreach(f => f(oscCodec.toMessage(input), caseCodec.toPlainList(input)))
        }
        cbk
    }

    def onButton(st: St, optSend: Option[Send]) = {
        val specSend = optSend.map(st.compileSend)

        def cbk {
            specSend.foreach(f => f(Nil, Nil))
        }
        
        cbk
    }

    def onIntWithDeselect(init: Int, st: St, optSend: Option[Send]) = onArgWithDeselect(init, st, optSend)(MessageCodec.intOscMessageCodec, ToPlainList.intArg)
    def onInt2WithDeselect(init: (Int, Int), st: St, optSend: Option[Send]) = unPair(onArgWithDeselect[(Int,Int)](init, st, optSend)(oscInt2, argInt2))

    def onArgWithDeselect[A](init: A, st: St, optSend: Option[Send])(oscCodec: MessageCodec[A], caseCodec: ToPlainList[A]): (A => Unit) = {
        var initState = init
        val specSend = optSend.map(st.compileSendWithDeselect)

        def cbk(input: A) {
            specSend.foreach( p => {                                       
                    p._2(oscCodec.toMessage(initState), caseCodec.toPlainList(initState))
                    initState = input
                    p._1(oscCodec.toMessage(input), caseCodec.toPlainList(input))
                }                
            )            
        }
        cbk
    }    
}


trait ToPlainList[A] {
    def toPlainList(a: A): List[Object]
}

object ToPlainList {  
    implicit val unitArg = new ToPlainList[Unit] { def toPlainList(a: Unit) = Nil }  
    implicit val floatArg = new ToPlainList[Float] { def toPlainList(a: Float) = List(a.asInstanceOf[Object]) }
    implicit val intArg = new ToPlainList[Int] { def toPlainList(a: Int) = List(a.asInstanceOf[Object]) }
    implicit val booleanArg = new ToPlainList[Boolean] { def toPlainList(a: Boolean) = List(a.asInstanceOf[Object]) }
    implicit val stringArg = new ToPlainList[String] { def toPlainList(a: String) = List(a.asInstanceOf[Object]) }
    implicit val fileArg = new ToPlainList[File] { def toPlainList(a: File) = List(a.getAbsolutePath.asInstanceOf[Object]) }
    implicit def tupleArg2[A,B](argA: ToPlainList[A], argB: ToPlainList[B]) = new ToPlainList[(A,B)] { def toPlainList(a: (A,B)) = argA.toPlainList(a._1) ++ argB.toPlainList(a._2) }
    implicit def tupleArg3[A,B,C](argA: ToPlainList[A], argB: ToPlainList[B], argC: ToPlainList[C]) = new ToPlainList[(A,B,C)] { def toPlainList(a: (A,B,C)) = argA.toPlainList(a._1) ++ argB.toPlainList(a._2) ++ argC.toPlainList(a._3) }
    implicit def tupleArg4[A,B,C,D](argA: ToPlainList[A], argB: ToPlainList[B], argC: ToPlainList[C], argD: ToPlainList[D]) = new ToPlainList[(A,B,C,D)] { def toPlainList(a: (A,B,C,D)) = argA.toPlainList(a._1) ++ argB.toPlainList(a._2) ++ argC.toPlainList(a._3) ++ argD.toPlainList(a._4) }

    def toStringArg[A](a: A)(implicit codec: ToPlainList[A]) =
        codec.toPlainList(a).map(_.toString).mkString(" ")        
}