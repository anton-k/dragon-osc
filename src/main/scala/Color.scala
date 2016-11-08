package dragon.osc.color

import java.awt.Color

object Palette {

    def palette(colorName: String) = colorName match {
        case "any" => Color.decode(randomColorName)
        case _     => Color.decode(paletteMap.get(colorName.toLowerCase).getOrElse(paletteMap.get("blue").get))
    }

    private val rand = scala.util.Random
     
    private def randomColorName =             
        brightPaletteValues(rand.nextInt(brightPaletteSize)) // except monochrome colors

    private val paletteMap = Map(
          "navy" -> "#001f3f"
        , "blue" -> "#0074D9"
        , "aqua" -> "#7FDBFF"
        , "teal" -> "#39CCCC"
        , "olive" -> "#3D9970"
        , "green" -> "#2ECC40"
        , "lime"  -> "#01FF70"
        , "yellow" -> "#FFDC00"
        , "orange" -> "#FF851B"
        , "red" -> "#FF4136"
        , "maroon" -> "#85144B"
        , "fuchsia" -> "#F012BE"
        , "purple" -> "#B10DC9"
        , "black" -> "#111111"
        , "gray" -> "#AAAAAA"
        , "silver" -> "#DDDDDD"
        , "white" -> "#FFFFFF")

    private val brightColors = paletteMap - "gray" - "black" - "silver" - "white"

    private val brightPaletteValues = brightColors.values.toArray
    private val brightPaletteSize = brightColors.size
}