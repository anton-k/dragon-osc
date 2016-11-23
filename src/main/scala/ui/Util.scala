package dragon.osc.ui

import scala.swing._
import scala.swing.event._

import scala.swing.audio.ui.{SetWidget, GetWidget, SetColor}

object Util {
    def tabs(body: List[(String, Component)], onSet: Int => Unit): Component with SetWidget[Int] with GetWidget[Int] with SetColor =         
        new TabbedPane with SetWidget[Int] with GetWidget[Int] with SetColor {
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
                val boundedN = mod(n, pageSize)
                this.selection.index = boundedN

                if (fireCallback) {
                    onSet(boundedN)
                }
                repaint
            }

            def get = this.selection.index

            def setColor(c: Color) {}
        }


    def mod(a: Int, b: Int) = {
        val res = a % b
        if (res < 0) (res + b)
        else res
    }
}