package thunderhamster

import scalaz.Monoid
import blueeyes.json.JsonAST
import blueeyes.json.JsonAST.{JValue, JNothing}


object MonoidInstances {

  implicit object JValueMonoid extends Monoid[JsonAST.JValue] {
    def zero = JNothing
    def append(f1: JValue, f2: => JValue) = f1 ++ f2
  }

}
