package dragon.osc.ui

import scala.swing._

object Util {
    def tabs(body: List[(String, Component)]): Component =         
        new TabbedPane {
            import TabbedPane._
            body.foreach { case (name, ui) => 
                pages += new Page(name, ui)
            }

            reactions += {
              case SelectionChanged( x ) => println( "changed to %d" format(tp.selection.index))              
            }

            listenTo( this.selection )
        }
}