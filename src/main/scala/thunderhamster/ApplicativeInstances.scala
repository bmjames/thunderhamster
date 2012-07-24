package thunderhamster

import scalaz.Applicative
import akka.dispatch.{ExecutionContext, Future}


trait ApplicativeInstances {

  implicit def FutureApplicative(implicit executor: ExecutionContext) = new Applicative[Future] {
    def point[A](a: => A) = Future(a)
    def ap[A,B](fa: => Future[A])(f: => Future[A => B]) =
      (f zip fa) map { case (fn, a1) => fn(a1) }
  }

}
