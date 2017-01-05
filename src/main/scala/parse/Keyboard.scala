package dragon.osc.parse.hotkey

import scala.util.Try
import scala.swing.event.Key

// see full list of keys at: http://www.scala-lang.org/api/2.11.2/scala-swing/#scala.swing.event.Key$@ValueextendsOrdered[Enumeration.this.Value]withSerializable

object Keyboard {
    import Key._

    def keyFromString(str: String): Option[Key.Value] = Try { str match {
        case str if str.length == 1 && str.forall(_.isLetter) => withName(str.map(_.toUpper))
        case str if str.forall(_.isDigit) => withName(str)
        // case str if str.startsWith("numpad") && str.length == 7 && str.last.isDigit => withName(s"Numpad${str.last}")
        case str if str.startsWith("f") && str.drop(1).forall(_.isDigit) => withName(s"F${str.drop(1)}")

        case "space" => Space
        case "alt" => Alt
        case "shift" => Shift
        case "windows" => Windows
        case "ctrl" => Control

        case "delete" => Delete
        case "insert" => Insert
        case "home" => Home
        case "end" => End
        case "page-up" => PageUp
        case "page-down" => PageDown

        case "up" => KpUp
        case "down" => KpDown
        case "left" => KpLeft
        case "right" => KpRight

        case "-" => Minus
        case "=" => Equals
        case "\\" => Slash 
        case "/" => BackSlash
        case "back-space" => BackSpace

        case ":" => Colon
        case "'" => Quote
        case "<" => Less
        case ">" => Greater    
        case "[" => OpenBracket
        case "]" => CloseBracket

        case "enter" => Enter
    }}.toOption
}
