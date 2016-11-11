import org.scalatest._

import dragon.osc.const._
import dragon.osc.parse.syntax._
import dragon.osc.parse.send._
import dragon.osc.parse.widget._

class SendTest extends FunSuite {
    def checkArgs(str: String, res: List[Arg]) = assert(Lang.read(str).map(obj => Send.args.run(obj)) == Some(res))

    test("read prim args") {
        checkArgs("args: [1, true, hello, 0.5]", List(PrimArg(PrimInt(1)), PrimArg(PrimBoolean(true)), PrimArg(PrimString("hello")), PrimArg(PrimFloat(0.5f))))
    }

    test("read mem ref args") {
        checkArgs(
            "args: [$mem1, $mem2]",
            List(MemRef("mem1"), MemRef("mem2")))
    }

    test("read arg ref args") {
        checkArgs(
            "args: [$1, $2]",
            List(ArgRef(1), ArgRef(2)))
    }

    test("read mixed args") {
        checkArgs(
            "args: [start, $mem1, $1, true]",
            List(PrimArg(PrimString("start")), MemRef("mem1"), ArgRef(1), PrimArg(PrimBoolean(true)))
        )
    }

    test("read no args") {
        checkArgs(
            "args: []",
            Nil
        )
    }

    def checkMsg(str: String, res: Msg) = assert(Lang.read(str).flatMap(Send.readMsg) == Some(res))

    test("simple msg") {
        checkMsg(
            "msg: { client: flow, path: /amp, args: [$1] }",
            Msg("flow", "/amp", List(ArgRef(1)))
        )
    }

    test("default msg") {
        checkMsg(
            "msg: {}",
            Msg(Defaults.client, Defaults.path, Nil)
        )
    }

    def checkMsgList(str: String, res: List[Msg]) = assert(Lang.read(str).flatMap(Send.readMsgList) == Some(res)) 

    test("msg list 1") {
        checkMsgList(
            "[msg: { client: flow, path: /amp, args: [0.2] }, msg: { client: sampler, path: /start, args: [false] }]",
            List(
                Msg("flow", "/amp", List(PrimArg(PrimFloat(0.2f)))),
                Msg("sampler", "/start", List(PrimArg(PrimBoolean(false)))))
        )
    }

    def checkSend(str: String, res: Send) = assert(Lang.read(str).flatMap(Send.read.run) == Some(res))

    test ("simple send 1") {
        checkSend(
            "send: [msg: { client: flow, path: /amp, args: [$1] }]",
            Send(List(Msg("flow", "/amp", List(ArgRef(1)))))
        )
    }

    test ("simple send 2") {
        checkSend(
            "send: [msg: { client: flow, path: /amp, args: [0.2] }, msg: { client: sampler, path: /start, args: [false] }]",
            Send(List(
                Msg("flow", "/amp", List(PrimArg(PrimFloat(0.2f)))),
                Msg("sampler", "/start", List(PrimArg(PrimBoolean(false))))))
        )
    }  

    test ("send with cases") {
        checkSend(
            "send: { case true: [msg: { client: flow, path: /start }], case false: [msg: { client: flow, path: /stop }]}",
            Send(Nil, List("true" -> List(Msg("flow", "/start", Nil)), "false" -> List(Msg("flow", "/stop", Nil))).toMap)
        )
    }

    test ("send with cases and default") {
        checkSend(
            "send: { default: [msg: { client: flow, path: /default }], case true: [msg: { client: flow, path: /start }], case false: [msg: { client: flow, path: /stop }]}",
            Send(List(Msg("flow", "/default", Nil)), List("true" -> List(Msg("flow", "/start", Nil)), "false" -> List(Msg("flow", "/stop", Nil))).toMap)
        )
    }
} 
