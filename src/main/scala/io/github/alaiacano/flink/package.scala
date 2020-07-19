package io.github.alaiacano

import com.twitter.algebird.Semigroup
import org.apache.flink.api.scala.DataSet

/**
  * This object adds some helper functions that make Flink's API a little more like Scio.
  */
package object flink {
  implicit class RichKVDataSet[K, V](val gds: DataSet[(K, V)]) extends AnyVal {

    /**
      * This is equivalent to grouping by the first element in a DataSet[(K, V)] and calling
      * reduce() on just the values.
      *
      * Usage:
      * 
      * env.fromElements("a", "a", "b")
      *   .map(k => (k, 1))
      *   .reduceByKey((v1, v2) => v1 + v2)
      * 
      * Produces
      *   (a,2)
      *   (b,1)
      * 
      * @param reduceFn A function of (V, V) => V for combining values that share the same key.
      * @return A DataSet[(K, V)] with a unique element per value of K
      */
    def reduceByKey(reduceFn: (V, V) => V): DataSet[(K, V)] = {
      val reduceKV = (kv1: (K, V), kv2: (K, V)) => (kv1._1, reduceFn(kv1._2, kv2._2))
      gds.groupBy(0).reduce(reduceKV(_, _))
    }

    /**
      * This is similar to the previous function but expects an implicit Semigroup[V] to be in scope.
      *
      * Usage:
      * 
      * import com.twitter.algebird._
      * 
      * env.fromElements("a", "a", "b")
      *   .map(k => (k, 1))
      *   .reduceByKey
      * 
      * Produces
      *   (a,2)
      *   (b,1)
      * 
      * @param reduceFn A function of (V, V) => V for combining values that share the same key.
      * @return A DataSet[(K, V)] with a unique element per value of K
      */
    def reduceByKey()(implicit sg: Semigroup[V]): DataSet[(K, V)] = {
      val reduceKV = (kv1: (K, V), kv2: (K, V)) => (kv1._1, sg.plus(kv1._2, kv2._2))
      gds.groupBy(0).reduce(reduceKV(_, _))
    }
  }
}
