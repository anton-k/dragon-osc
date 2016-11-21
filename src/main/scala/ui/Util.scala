package dragon.osc.ui

import scala.swing._
import scala.swing.event._

import scala.swing.audio.ui.{SetWidget, GetWidget}

object Util {
    def tabs(body: List[(String, Component)], onSet: Int => Unit): Component with SetWidget[Int] with GetWidget[Int] =         
        new TabbedPane with SetWidget[Int] with GetWidget[Int] {
            import TabbedPane._
            val pageSize = body.length

            body.foreach { case (name, ui) => 
                pages += new Page(name, ui)
            }

            reactions += {
              case SelectionChanged( x ) => onSet(this.selection.index)
            }

            listenTo( this.selection )

            def set(n: Int, fireCallback: Boolean) {
                val boundedN = if (n < 0) 0 else (if (n >= pageSize) (pageSize - 1) else n)
                this.selection.index = boundedN

                if (fireCallback) {
                    onSet(boundedN)
                }
                repaint
            }

            def get = this.selection.index
        }
}