import scala.swing.{Component}
import scala.swing.audio.ui._

package dragon.osc {
    package object input {
        type IdMap[A] = Map[String,A]
        def emptyMap[A] = Map[String,A]()

    }
}

package dragon.osc.input {

trait Input {
    val id: String    
}

case class TabSet(id: String, value: Int) extends Input

case class DialSet(id: String, value: Float, fireCallback: Boolean) extends Input
case class HFaderSet(id: String, value: Float, fireCallback: Boolean) extends Input
case class VFaderSet(id: String, value: Float, fireCallback: Boolean) extends Input
case class ButtonSet(id: String, fireCallback: Boolean) extends Input
case class ToggleSet(id: String, value: Boolean, fireCallback: Boolean) extends Input
case class MultiToggleSet(id: String, value: ((Int, Int), Boolean), fireCallback: Boolean) extends Input
case class XYPadSet(id: String, value: (Float, Float), fireCallback: Boolean) extends Input
case class IntDialSet(id: String, value: Int, fireCallback: Boolean) extends Input
case class HFaderRangeSet(id: String, value: (Float, Float), fireCallback: Boolean) extends Input
case class VFaderRangeSet(id: String, value: (Float, Float), fireCallback: Boolean) extends Input
case class XYPadRangeSet(id: String, value: ((Float, Float), (Float, Float)), fireCallback: Boolean) extends Input
case class DropDownListSet(id: String, value: Int, fireCallback: Boolean) extends Input
case class TextInputSet(id: String, value: String, fireCallback: Boolean) extends Input

case class InputBase(
    dialSet:            IdMap[Dial]             = emptyMap[Dial],
    hfaderSet:          IdMap[HFader]           = emptyMap[HFader],
    vfaderSet:          IdMap[VFader]           = emptyMap[VFader],
    buttonSet:          IdMap[PushButton]       = emptyMap[PushButton],
    toggleSet:          IdMap[ToggleButton]     = emptyMap[ToggleButton],
    multiToggleSet:     IdMap[MultiToggle]      = emptyMap[MultiToggle],
    xyPadSet:           IdMap[XYPad]            = emptyMap[XYPad],
    intDialSet:         IdMap[IntDial]          = emptyMap[IntDial],
    hfaderRangeSet:     IdMap[HFaderRange]      = emptyMap[HFaderRange],
    vfaderRangeSet:     IdMap[VFaderRange]      = emptyMap[VFaderRange],
    xyPadRangeSet:      IdMap[XYPadRange]       = emptyMap[XYPadRange],
    dropDownListSet:    IdMap[DropDownList]     = emptyMap[DropDownList],
    textInputSet:       IdMap[TextInput]        = emptyMap[TextInput] ) {

    def act(input: Input) = input match {
        case DialSet(id, value, fireCallback)         => on(dialSet, id, value, fireCallback)
        case HFaderSet(id, value, fireCallback)       => on(hfaderSet, id, value, fireCallback)
        case VFaderSet(id, value, fireCallback)       => on(vfaderSet, id, value, fireCallback)
        case ButtonSet(id, fireCallback)              => on(buttonSet, id, {}, fireCallback)
        case ToggleSet(id, value, fireCallback)       => on(toggleSet, id, value, fireCallback)
        case MultiToggleSet(id, value, fireCallback)  => on(multiToggleSet, id, value, fireCallback)
        case XYPadSet(id, value, fireCallback)        => on(xyPadSet, id, value, fireCallback)
        case IntDialSet(id, value, fireCallback)      => on(intDialSet, id, value, fireCallback)
        case HFaderRangeSet(id, value, fireCallback)  => on(hfaderRangeSet, id, value, fireCallback)
        case VFaderRangeSet(id, value, fireCallback)  => on(vfaderRangeSet, id, value, fireCallback)
        case XYPadRangeSet(id, value, fireCallback)   => on(xyPadRangeSet, id, value, fireCallback)
        case DropDownListSet(id, value, fireCallback) => on(dropDownListSet, id, value, fireCallback)
        case TextInputSet(id, value, fireCallback)    => on(textInputSet, id, value, fireCallback)
    }

    def addWidgetSet[A](id: String, widget: Component) = widget match {
        case x: Dial            => this.copy(dialSet     = addIdMap(dialSet, id, x))
        case x: HFader          => this.copy(hfaderSet   = addIdMap(hfaderSet, id, x))
        case x: VFader          => this.copy(vfaderSet   = addIdMap(vfaderSet, id, x))  
        case x: PushButton      => this.copy(buttonSet   = addIdMap(buttonSet, id, x))
        case x: ToggleButton    => this.copy(toggleSet   = addIdMap(toggleSet, id, x))
        case x: MultiToggle     => this.copy(multiToggleSet = addIdMap(multiToggleSet, id, x))
        case x: XYPad           => this.copy(xyPadSet    = addIdMap(xyPadSet, id, x))
        case x: IntDial         => this.copy(intDialSet  = addIdMap(intDialSet, id, x))
        case x: HFaderRange     => this.copy(hfaderRangeSet = addIdMap(hfaderRangeSet, id, x))
        case x: VFaderRange     => this.copy(vfaderRangeSet = addIdMap(vfaderRangeSet, id, x))
        case x: XYPadRange      => this.copy(xyPadRangeSet = addIdMap(xyPadRangeSet, id, x))
        case x: DropDownList    => this.copy(dropDownListSet = addIdMap(dropDownListSet, id, x))
        case x: TextInput       => this.copy(textInputSet = addIdMap(textInputSet, id, x))
        case x                  => { println("Uknown widget"); this }
    }

    private def on[A <: SetWidget[B], B](m: IdMap[A], id: String, value: B, fireCallback: Boolean) = m.get(id).foreach(_.set(value, fireCallback))
    private def addIdMap[A](m: IdMap[A], id: String, a: A) = m + (id -> a)
}

}