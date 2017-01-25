package dragon.osc.parse.const

object Names {
    val dial = "dial"
    val hfader = "hfader"
    val vfader = "vfader"
    val vcheck = "vcheck"
    val hcheck = "hcheck"
    val label = "label"
    val button = "button"    
    val toggle = "toggle"
    val circleButton = "circle-button"    
    val circleToggle = "circle-toggle"
    val multiToggle = "multi-toggle"    
    val xyPad = "xy-pad"
    val intDial = "int-dial"
    val hfaderRange = "hfader-range"
    val vfaderRange = "vfader-range"
    val xyPadRange = "xy-pad-range"
    val dropDownList = "drop-down-list"
    val textInput = "text-input"
    val fileInput = "file-input"
    val texts = "texts"
    val allowDeselect = "deselect"
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
    val ints = "int"
    val booleans = "bool"
    val strings = "string"
    val trueStr = "true"
    val falseStr = "false"

    // action names
    val send = "send"
    val save = "save"
    val run  = "run"

    val color = "color"
    val color1 = "color1"
    val color2 = "color2"
    val init  = "init"
    val initSend = "init-send"
    val initX = "init-x"
    val initY = "init-y"
    val text = "text"
    val msg = "msg"
    val client = "client"
    val path = "path"
    val args = "args"
    val range = "range"
    val id = "id"
    val default = "default"   
    val msgCase = "case"
    val msgCaseOff = "case-off"
    val title = "title"
    val size = "size"
    val keys = "keys"    
    val keyGuard = "when"
    val key = "key"
    val unlockPass = "exit"
    val textLength = "text-length"
    val orient = "orient"
    val sizeList = "sizes"
    val doubleCheck = "double-check"

    object Error {
        val failedToParse = "File parsing failed. Check if the file exists and if it is in yaml format."
    }
}

object Defaults {
    val int = 0
    val float = 0.5f
    val boolean = false
    val string = ""
    val float2 = (float, float)
    val int2 = (int, int)
    val range = (0f, 1f)
    val color = "blue"
    val rangeInt = (0, 100)
    val client = "self"
    val path = "/"
    val size1 = 5
    val allowDeselect = true
    val textLength = 7    
    val multiToggleSize = (2, 4)
    val textColor = "black"
    val orientIsFirst = true
    val orientIsFirstHor = true
    val orientIsSecondHor = true
    val sizeList = List(4, 4, 4, 4)
    val doubleCheckTexts = Nil
}