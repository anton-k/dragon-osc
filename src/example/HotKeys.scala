// Minimal app with hotkeys.

import scala.swing._
import scala.swing.event._
import java.awt.event._

object App extends SimpleSwingApplication {
    def top = new MainFrame {
        val label = new Label {
            text = "No click yet"
        }
        contents = new BoxPanel(Orientation.Vertical) {
            contents += label
            border = Swing.EmptyBorder(30,30,10,10)
            listenTo(keys)
            reactions += {
                case KeyPressed(_, Key.Space, _, _) =>
                    label.text = "Space is down"
                case KeyReleased(_, Key.Space, _, _) =>
                    label.text = "Space is up"
            }
            focusable = true
            requestFocus
        }
    }
}