package dragon.osc.parse.tfm

import dragon.osc.parse.syntax._

object Tfm {
    def tfmLang(obj: Lang): Lang = 
        includeFromFile(obj)

    def includeFromFile(obj: Lang): Lang = obj match {
        case MapSym(m) => MapSym(m.toList.flatMap(includeInMap).toMap)
        case ListSym(xs) => ListSym(xs.map(includeFromFile))
        case _ => obj
    }

    def includeInMap(p: (String,Lang)): List[(String,Lang)] = 
        if (p._1 == "#include") {
            p._2 match {
                case PrimSym(PrimString(filename)) => includeWholeFile(filename)
                case MapSym(m) => m.toList match {
                    case List((field, PrimSym(PrimString(filename)))) => includeFileByField(field, filename)
                    case _ => Nil
                }
                case _ => Nil
            }
        } else List(p)

    def insertMap(obj: Lang): List[(String,Lang)] = obj match {
        case MapSym(m) => m.toList.map({ case (key, value) => (key, includeFromFile(value)) })
        case _ => Nil
    }

    def includeWholeFile(filename: String): List[(String,Lang)] = Lang.readFile(filename).map(insertMap).getOrElse(Nil)

    def includeFileByField(field: String, filename: String): List[(String,Lang)] = 
        Lang.readFile(filename).flatMap(_.getKey(field)).map(insertMap).getOrElse(Nil)
}

