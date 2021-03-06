/**
  * This code supplements instructions.org
  * Once you've gone through the instructions you can do
  * whatever you want with it.
  */
package Ex0

import chisel3._
import chisel3.iotesters.PeekPokeTester
import org.scalatest.{Matchers, FlatSpec}
import TestUtils._

// class MyVector() extends Module {
//   val io = IO(
//     new Bundle {
//       val idx = Input(UInt(32.W))
//       val out = Output(UInt(32.W))
//     }
//   )

//   val values = List(1, 2, 3, 4)

//   io.out := values(io.idx)
// }

// class MyVector() extends Module {
//   val io = IO(
//     new Bundle {
//       val idx = Input(UInt(32.W))
//       val out = Output(UInt(32.W))
//     }
//   )

//   // val values: List[Int] = List(1, 2, 3, 4)
//   val values = Vec(1, 2, 3, 4)

//   io.out := values(io.idx)
// }

class MyVector() extends Module {
  val io = IO(
    new Bundle {
      val idx = Input(UInt(32.W))
      val out = Output(UInt(32.W))
    }
  )

  val values = Vec(0.U, 1.U, 2.U, 3.U)

  io.out := values(io.idx)
}


class MyVector2() extends Module {
  val io = IO(
    new Bundle {
      val idx = Input(UInt(2.W))
      val out = Output(UInt(32.W))
    }
  )

  val values = Array(0.U, 1.U, 2.U, 3.U)

  val myWire = Wire(UInt(4.W))
  io.out := values(0)
  for(ii <- 0 until 4){
    when(io.idx === ii.U){
      io.out := values(ii)
    }
  }
}


class MyVecSpec extends FlatSpec with Matchers {
  behavior of "MyVec"

  it should "Output whatever idx points to" in {
    wrapTester(
      chisel3.iotesters.Driver(() => new MyVector2) { c =>
        new MyVecTester(c)
      } should be(true)
    )
  }
}


class MyVecTester(c: MyVector2) extends PeekPokeTester(c)  {
  for(ii <- 0 until 4){
    poke(c.io.idx, ii)
    expect(c.io.out, ii)
  }
}


class Invalid() extends Module {
  val io = IO(new Bundle{})

  val myVec = Module(new MyVector)

  // Uncomment line below to make the circuit valid
  // myVec.io.idx := 0.U
}


/**
  * This goes a little beyond the example in exercise.org.
  * WrapTest is a simple wrapper that catches Unconnected wires
  * and prints them with a less scary stacktrace.
  * Additionally, we throw a RunTimeException instead of ??? for
  * similar reasons
  * 
  */
class InvalidSpec extends FlatSpec with Matchers {
  behavior of "Invalid"

  it should "Fail with a RefNotInitializedException" in {
    try {
      wrapTester(
        chisel3.iotesters.Driver(() => new Invalid) { c =>

          // Just a placeholder so it compiles
          throw new RuntimeException with scala.util.control.NoStackTrace
        } should be(true)
      )
    }
    catch {
      case e: RuntimeException => println("all good!")
      case e: Exception => throw e
    }
  }
}


class SimpleDelay() extends Module {
  val io = IO(
    new Bundle {
      val dataIn  = Input(UInt(32.W))
      val dataOut = Output(UInt(32.W))
    }
  )
  val delayReg = RegInit(UInt(32.W), 0.U)

  delayReg   := io.dataIn
  io.dataOut := delayReg
}


class DelaySpec extends FlatSpec with Matchers {
  behavior of "SimpleDelay"

  it should "Delay input by one timestep" in {
    wrapTester(
      chisel3.iotesters.Driver(() => new SimpleDelay) { c =>
        new DelayTester(c)
      } should be(true)
    )
  }
}


// class DelayTester(c: SimpleDelay) extends PeekPokeTester(c)  {
//   for(ii <- 0 until 10){
//     val input = scala.util.Random.nextInt(10)
//     poke(c.io.dataIn, input)
//     expect(c.io.dataOut, input)
//   }
// }

class DelayTester(c: SimpleDelay) extends PeekPokeTester(c)  {
  for(ii <- 0 until 10){
    val input = scala.util.Random.nextInt(10)
    poke(c.io.dataIn, input)
    step(1)
    expect(c.io.dataOut, input)
  }
}

class DPCsimulatorSpec extends FlatSpec with Matchers {

  case class DotProdCalculator(vectorLen: Int, timeStep: Int = 0, accumulator: Int = 0){
    def update(inputA: Int, inputB: Int): (Int, Boolean, DotProdCalculator) = {
      val product = inputA * inputB
      if(((timeStep + 1) % vectorLen) == 0)
        (accumulator + product, true, this.copy(timeStep = 0, accumulator = 0))
      else
        (accumulator + product, false, this.copy(timeStep = this.timeStep + 1, accumulator = accumulator + product))
    }
  }

  val myDPC = DotProdCalculator(4)
  val dpcStream = Stream.iterate((0, myDPC)){ case(ts, dpc) =>
    val a = scala.util.Random.nextInt(4)
    val b = scala.util.Random.nextInt(4)
    val (output, valid, nextDPC) = dpc.update(a, b)
    val validString = if(valid) "yes" else "no"
    println(s"at timestep $ts:")
    println(s"INPUTS:")
    println(s"inputA: $a, inputB: $b")
    println(s"OUTPUTS:")
    println(s"output: $output, valid: $validString\n\n")

    (ts + 1, nextDPC)
  }.take(20)


  behavior of "Dot product simulator"

  it should "Be shoehorned into a test" in {
    dpcStream.last
  }
}


class EvilPrintfSpec extends FlatSpec with Matchers {

  class CountTo3() extends Module {
    val io = IO(
      new Bundle {
        val dataOut     = Output(UInt(32.W))
        val validOutput = Output(Bool())
      }
    )
    val count = RegInit(UInt(32.W), 0.U)
    io.dataOut := count

    printf(p"according to printf output is: ${io.dataOut}\n")

    when(count != 3.U){
      count := count + 1.U
      io.validOutput := false.B
      io.dataOut := 0.U
    }.otherwise{
      io.validOutput := true.B
      io.dataOut := 1.U
    }

  }


  class CountTo3Test(c: CountTo3) extends PeekPokeTester(c)  {
    for(ii <- 0 until 5){
      println(s"\nIn cycle $ii the output of counter is: ${peek(c.io.dataOut)}")
      step(1)
    }
  }

  behavior of "EvilPrintf"

  it should "tell a lie and hurt you" in {
    wrapTester(
      chisel3.iotesters.Driver(() => new CountTo3) { c =>
        new CountTo3Test(c)
      } should be(true)
    )
  }
}
