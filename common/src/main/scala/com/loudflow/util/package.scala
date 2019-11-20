package com.loudflow

import java.util.UUID

import scala.collection.mutable.ArrayBuffer

package object util {

  def shuffle[T](list: Seq[T], random: JavaRandom): Seq[T] = {
    val buf = new ArrayBuffer[T] ++= list
    def swap(i1: Int, i2: Int): Unit = {
      val tmp = buf(i1)
      buf(i1) = buf(i2)
      buf(i2) = tmp
    }
    for (n <- buf.length to 2 by -1) {
      val k = random.nextInt(n)
      swap(n - 1, k)
    }
    buf
  }

  def randomId: String = UUID.randomUUID().toString
  def randomSeed: Long = JavaRandom.seedUniquifier ^ System.nanoTime

}
