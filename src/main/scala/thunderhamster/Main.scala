package thunderhamster

import blueeyes.BlueEyesServer


object Main extends BlueEyesServer with CatchUpService {

  override def main(args: Array[String]) {
    super.main(Array("--configFile", "default.conf"))
  }

}
