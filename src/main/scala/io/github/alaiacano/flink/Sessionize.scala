package io.github.alaiacano.flink

import org.apache.flink.api.scala._
import io.github.alaiacano.algebird.{SessionSemigroup, Session}
import io.github.alaiacano.flink._
import com.twitter.algebird._

object Sessionize {

  @throws[Exception]
  def main(args: Array[String]): Unit = {

    val env: ExecutionEnvironment = ExecutionEnvironment.getExecutionEnvironment

    // Make a semigroup for combining Sessions together. This will be used in the reduceByKey
    implicit val sessionSg = new SessionSemigroup(1)

    val x = env.readTextFile("file:///Users/adam/code/scio-flink/data/sessions.csv")
      .map { line => 
        val id :: start :: end :: nil = line.trim.split(",").toList
        (id, List(Session(start.toInt, end.toInt)))
      }
      // I made a reduceByKey helper for any DataSet[(K, V)]. See package.scala for more.
      .reduceByKey
      .flatMap { idAndSessionList => 
        val (id, sessList) = idAndSessionList
        sessList.map { sess =>
          (id, List(sess.min, sess.max).mkString(", "))
        }        
      }

    x.print()

    /* output is:
       (a,1, 4)
       (a,7, 9)
       (b,1, 12)
    */
  }
}