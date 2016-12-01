package dragon.osc.ui

import java.io.File

import dragon.osc.send._
import scala.swing.{Component}
import scala.swing.audio.ui._
import dragon.osc.state._
import scala.audio.osc._

object Listener {
    def withId[A](optId: Option[String], widget: A)(f: (String, A) => Unit): A = {
        optId.foreach(id => f(id, widget))
        widget
    }

    def withListener[B, A <: Component with SetWidget[B] with SetColor](st: St)(optId: Option[String], widget: A)(implicit codec: MessageCodec[B]): A = {
        optId.foreach(id => st.addListener(id, widget)(codec))
        widget
    }
}

case class Listener(st: St, id: Option[String]) {
    type Float2 = (Float, Float)
    import Listener._

    def pure[B, A <: Component with SetWidget[B] with SetColor](widget: A)(implicit codec: MessageCodec[B]): State[Context,A] = 
        pure(Listener.withListener[B,A](st)(id, widget))

    def float[A <: Component with SetWidget[Float] with GetWidget[Float] with SetColor](widget: A)(implicit codec: MessageCodec[Float]): State[Context,A] = 
        pure(withId(id, widget) { (ix, w) => st.osc.addFloatListener(ix, w) })

    def string[A <: Component with SetWidget[String] with GetWidget[String] with SetColor](widget: A)(implicit codec: MessageCodec[String]): State[Context,A] = 
        pure(withId(id, widget) { (ix, w) => st.osc.addStringListener(ix, w) })

    def int[A <: Component with SetWidget[Int] with GetWidget[Int] with SetColor](widget: A)(implicit codec: MessageCodec[Int]): State[Context,A] = 
        pure(withId(id, widget) { (ix, w) => st.osc.addIntListener(ix, w) })

    def toggle[A <: Component with SetWidget[Boolean] with GetWidget[Boolean] with SetColor](widget: A)(implicit codec: MessageCodec[Boolean]): State[Context,A] = 
        pure(withId(id, widget) { (ix, w) => st.osc.addToggleListener(ix, w) })

    def float2[A <: Component with SetWidget[(Float,Float)] with GetWidget[(Float,Float)] with SetColor](widget: A)(implicit codec: MessageCodec[(Float,Float)]): State[Context,A] = 
        pure(withId(id, widget) { (ix, w) => st.osc.addFloatListener2(ix, w) })

    def float4[A <: Component with SetWidget[(Float2,Float2)] with GetWidget[(Float2,Float2)] with SetColor](widget: A)(implicit codec: MessageCodec[(Float2,Float2)]): State[Context,A] = 
        pure(withId(id, widget) { (ix, w) => st.osc.addFloatListener4(ix, w) })

    def multiToggle(widget: MultiToggle) = 
        pure(withId(id, widget) { (ix, w) => st.osc.addMultiToggleListener(ix, w) })

    def text[A <: Component with SetText](widget: A): A = 
        withId(id, widget) { (ix, w) => st.osc.addTextListener(ix, w) }

    def textList[A <: Component with SetTextList](widget: A): A = 
        withId(id, widget) { (ix, w) => st.osc.addTextListListener(ix, w) }

    def file[A <: Component with SetWidget[File] with SetColor](widget: A): State[Context,A] = 
        pure(withId(id, widget) { (ix, w) => st.osc.addFileListener(ix, w) })

    def doubleCheck(widget: DoubleCheck): State[Context,DoubleCheck] = 
        pure(withId(id, widget) { (ix, w) => st.osc.addDoubleCheckListener(ix, w) })

    private def pure[A](a: A) = State.pure[Context,A](a)
}
