package dragon.osc.ui

import dragon.osc.send._
import scala.swing.{Component}
import scala.swing.audio.ui._
import dragon.osc.state._
import scala.audio.osc._

object Listener {

    def withListener[B, A <: Component with SetWidget[B] with SetColor](st: St)(optId: Option[String], widget: A)(implicit codec: MessageCodec[B]): A = {
        optId.foreach(id => st.addListener(id, widget)(codec))
        widget
    }

    def withToggleListener[A <: Component with SetWidget[Boolean] with GetWidget[Boolean] with SetColor](st: St)(optId: Option[String], widget: A)(implicit codec: MessageCodec[Boolean]): A = {        
        val widget1 = withListener[Boolean,A](st)(optId, widget)             
        optId.foreach(id => st.addToggleListener(id, widget1))
        widget1
    }

    def withFloatListener[A <: Component with SetWidget[Float] with GetWidget[Float] with SetColor](st: St)(optId: Option[String], widget: A)(implicit codec: MessageCodec[Float]): A = {
        val widget1 = withListener[Float,A](st)(optId, widget)
        optId.foreach(id => st.addFloatListener(id, widget1)(codec))      
        widget1        
    } 

    def withIntListener[A <: Component with SetWidget[Int] with GetWidget[Int] with SetColor](st: St)(optId: Option[String], widget: A)(implicit codec: MessageCodec[Int]): A = {
        val widget1 = withListener[Int,A](st)(optId, widget)
        optId.foreach(id => st.addIntListener(id, widget1)(codec))
        widget1        
    } 
}


case class Listener(st: St, id: Option[String]) {
    def pure[B, A <: Component with SetWidget[B] with SetColor](widget: A)(implicit codec: MessageCodec[B]): State[Context,Component] = 
        pure(Listener.withListener[B,A](st)(id, widget))

    def float[A <: Component with SetWidget[Float] with GetWidget[Float] with SetColor](widget: A)(implicit codec: MessageCodec[Float]): State[Context,Component] = 
        pure(Listener.withFloatListener[A](st)(id, widget))

    def int[A <: Component with SetWidget[Int] with GetWidget[Int] with SetColor](widget: A)(implicit codec: MessageCodec[Int]): State[Context,Component] = 
        pure(Listener.withIntListener[A](st)(id, widget))

    def toggle[A <: Component with SetWidget[Boolean] with GetWidget[Boolean] with SetColor](widget: A)(implicit codec: MessageCodec[Boolean]): State[Context,Component] = 
        pure(Listener.withToggleListener(st)(id, widget))

    private def pure[A](a: A) = State.pure[Context,A](a)
}
