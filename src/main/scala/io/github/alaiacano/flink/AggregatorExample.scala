package io.github.alaiacano.flink

import org.apache.flink.api.scala._
import com.twitter.algebird.Aggregator
import com.twitter.algebird.Semigroup

object AggregatorExample {

  def main(args: Array[String]): Unit = {
    val env: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    val agg: Aggregator[String, Int, String] = Aggregator
      .prepareSemigroup((s: String) => s.toInt)(Semigroup.from[Int]((a, b) => math.min(a, b)))
      .andThenPresent(s => s.toString)

    env.fromElements(("a", "1"), ("a", "2"), ("b", "3"))
      .map(kv => (kv._1, agg.prepare(kv._2)))
      .groupBy(0)
      .reduce((kv1, kv2) => (kv1._1, agg.reduce(kv1._2, kv2._2)))
      .map(kb => (kb._1, agg.present(kb._2)))
    .print()      
  }
}
