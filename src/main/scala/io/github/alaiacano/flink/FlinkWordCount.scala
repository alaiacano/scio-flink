package io.github.alaiacano.flink

import org.apache.flink.api.scala._
import org.apache.flink.api.java.aggregation.Aggregations
import org.apache.flink.streaming.api.scala.extensions.impl.acceptPartialFunctions

object WordCount {

  @throws[Exception]
  def main(args: Array[String]): Unit = {
    val env: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    val text = env.fromElements("To be, or not to be,--that is the question:--",
      "Whether 'tis nobler in the mind to suffer", "The slings and arrows of outrageous fortune",
      "Or to take arms against a sea of troubles,")

    val counts = text.flatMap { _.toLowerCase.split("\\W+") }
      .map { (_, 1) }
      .groupBy(0)
      .aggregate(Aggregations.SUM, 1)
      .filter { (kv: (String, Int)) => kv._2 > 1}

    // execute and print result
    counts.print()
  }
}