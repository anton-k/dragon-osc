package dragon.osc.parse.tfm

object Tfm {
    def includeFromFile(obj: Lang): Lang = obj match {
        case MapSym(m) => MapSym(m.map(includeInMap))
        case ListSym(xs) => ListSym(xs.map(includeFromFile))
        case _ => obj
    }
}

