package io.github.alaiacano

import com.spotify.scio._

/*
sbt "runMain io.github.alaiacano.WordCount --runner=FlinkRunner --input=file:///Users/adam/code/scio-flink/shakespeare.txt --output=counts.txt"
*/

object WordCount {
  def main(cmdlineArgs: Array[String]): Unit = {
    val (sc, args) = ContextAndArgs(cmdlineArgs)

    val exampleData = "gs://dataflow-samples/shakespeare/kinglear.txt"
    val input = args.getOrElse("input", exampleData)
    val output = args("output")

    sc.textFile(input)
      .map(_.trim)
      .flatMap(_.split("[^a-zA-Z']+").filter(_.nonEmpty))
      .countByValue
      .filter{ case (k, v) => v > 1}
      .map(t => t._1 + ": " + t._2)
      .saveAsTextFile(output, numShards=1)

    val result = sc.run().waitUntilFinish()
  }
}
