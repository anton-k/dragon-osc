package dragon.osc.send

import dragon.osc.readargs._

case class St(osc: Osc, memory: Memory) {
    def close {
        osc.close        
    }
}

object St {
    def init(args: Args) = St(Osc(args), Memory.init)   
}

case class Memory(var memory: Map[String, Object]) {
    def get(name: String) = memory.get(name)

    def register(key: String, value: Object) {
        memory += (key -> value)
    }
}

object Memory {
    def init = Memory(Map[String,Object]())
}
