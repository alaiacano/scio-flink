package io.github.alaiacano.algebird

import com.twitter.algebird.Semigroup
import com.twitter.algebird.Operators._
import scala.annotation.tailrec


// This is what will hold our session data. It just has the start
// and end time.
case class Session(min: Int, max: Int)

object SessionSemigroup {
  val sessionOrd = Ordering.fromLessThan[Session]((l, r) => l.min < r.min)
  implicit val combineSession: Semigroup[Session] = Semigroup
    .from[Session]{ (l, r) =>
      Session(math.min(l.min, r.min), math.max(l.max, r.max))
    }
}



/**
  * A semigroup for summing / combining observed sessions. This is both associative and commutative, so it can be
  * run in a distributed manner. The algorithm is not the most computationally efficient, but if you have many
  * sessions for a given key (eg user or content ID), it can become beneficial to other sessionizing operations.
  *
  * @param threshold Number of time units between sessions.
  */
class SessionSemigroup(threshold: Int) extends Semigroup[List[Session]] {
  import SessionSemigroup._
  /**
    * Combines two lists of sessions together. It is assumed
    * that the head of the "left" input starts before the "right" input.
    *
    * This is a variant of a merge sort I guess?
    * 
    * Called "combineInternal" because there is a Semigroup#combine method already that serves a different 
    * puprpose.
    */
  @tailrec
  private def combineInternal(left: List[Session], right: List[Session]): List[Session] = {
    (left, right) match {
      // finishing conditions are if either list is empty. It's assumed that the other list is sorted
      // because of the logic below.
      // There could be an edge case that if you originally add an out-of-order List[Session] to an empty List,
      // this would get messed up.
      case (l, r) if l.isEmpty => compact(r)
      case (l, r) if r.isEmpty => compact(l)
      case (lower, higher) if higher.head.min - lower.head.max <= threshold =>
        // combine the first two sessions of each list into a single
        // session and put it at the beginning of the `lower` list,
        // and recurse
        val combined = lower.head + higher.head
        combineInternal(combined :: lower.tail, higher.tail)
      case (lower, higher) =>
        // do not combine the heads of the lists because they are separated by more than the threshold.
        // put the head of the lower range, then the head of the higher range into the left list, remove the head from the right,
        // and recurse.
        combineInternal(lower.head :: higher.head :: lower.tail, higher.tail)
    }
  }

  /**
    * After combining sessions from two lists, we might have some
    * that are closer together than we want. This goes through the
    * final list of sessions and joins any that are < threshold apart.
    *
    * It assumes the sessions are already sorted and that the list is not empty
    */
  private def compact(lst: List[Session]): List[Session] = {
    if (lst.tail.isEmpty) {
      lst
    } else {
      lst.tail.foldLeft(List(lst.head)){(accum, newSession) =>
        val lastInAccum: Session = accum.last
        if (newSession.min - lastInAccum.max <= threshold) {
          accum.dropRight(1) ++ List(newSession + lastInAccum)
        } else {
          accum ++ List(newSession)
        }
      }
    }
  }
  
  /**
   * Here's how we combine two lists of Sessions. If either is empty, we return,
   * otherwise call the combine() function with the earliest observed starting timestamp
   * on the left.
   */
  override def plus(left: List[Session], right: List[Session]): List[Session] = {
    (left, right) match {
      case (l, r) if l.isEmpty => r
      case (l, r) if r.isEmpty => l
      case (l, r) if sessionOrd.lt(l.head, r.head) => combineInternal(l, r)
      case (l, r) => combineInternal(r, l)
    }
  }
}