package io.github.alaiacano

import io.github.alaiacano.algebird._
import com.spotify.scio._
import scala.language.higherKinds

/*
sbt "runMain io.github.alaiacano.Sessionize --runner=FlinkRunner --input=file:///Users/adam/code/scio-flink/sessions.csv --output=sessionized"
*/
object Sessionize {

  def main(cmdlineArgs: Array[String]): Unit = {
    val (sc, args) = ContextAndArgs(cmdlineArgs)

    val sessionSg = new SessionSemigroup(1)
    val sessions  = sc.textFile(args("input"))
      .map { line => 
        val id :: start :: end :: nil = line.trim.split(",").toList
        (id, List(Session(start.toInt, end.toInt)))
      }
      .reduceByKey(sessionSg.plus)
      .flatMap { case (id, sessList) => 
        sessList.map { sess =>
          List(id, sess.min, sess.max).mkString(", ") 
        }        
      }
      .saveAsTextFile(args("output"), numShards=1)

    val result = sc.run().waitUntilFinish()
  }
}
