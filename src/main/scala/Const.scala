package dragon.osc.const

object Names {
    val dial = "dial"
    val hfader = "hfader"
    val vfader = "vfader"
    val vcheck = "vcheck"
    val hcheck = "hcheck"
    val label = "label"
    val button = "button"    
    val toggle = "toggle"
    val multiToggle = "multi-toggle"
    val xyPad = "xy-pad"
    val intDial = "int-dial"
    val hfaderRange = "hfader-range"
    val vfaderRange = "vfader-range"
    val xyPadRange = "xy-pad-range"
    val dropDownList = "drop-down-list"
    val textInput = "text-input"
    val tabs = "tabs"
    val page = "page"
    val window = "window"
    val hor = "hor"
    val ver = "ver"
    val space = "space"
    val glue = "glue"    
    val content = "content"
    val app = "main"

    val idSet = Set(dial, hfader, vfader, vcheck, hcheck, toggle, multiToggle, xyPad, intDial, hfaderRange, vfaderRange, xyPadRange, dropDownList, textInput, tabs, window)

    val hot = "hot"        
    val cold = "cold"

    // action names
    val send = "send"
    val save = "save"
    val run  = "run"
}

object Attributes {    
    val color = "color"
    val init  = "init"
    val text = "text"
    val msg = "msg"
    val client = "client"
    val path = "path"
    val args = "args"
    val range = "range"
    val id = "id"
    val default = "default"   
    val msgCase = "case"
    val title = "title"
    val size = "size"
}

object Defaults {
    val int = 0
    val float = 0.5f
    val boolean = false
    val string = ""
    val float2 = (float, float)
    val range = (0f, 1f)
    val color = "blue"
    val rangeInt = (0, 100)
    val client = "self"
    val path = "/"
}