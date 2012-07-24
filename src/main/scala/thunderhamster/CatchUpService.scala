package thunderhamster

import akka.dispatch.Future
import blueeyes.BlueEyesServiceBuilder
import blueeyes.core.data.{BijectionsChunkJson, BijectionsChunkString}
import blueeyes.core.service.engines.HttpClientXLightWeb
import blueeyes.core.http.MimeTypes.{json, application}
import blueeyes.core.data.ByteChunk
import blueeyes.core.http.{HttpResponse, HttpRequest}
import blueeyes.json.JsonAST
import scalaz._, Scalaz._


trait CatchUpService extends BlueEyesServiceBuilder
    with BijectionsChunkString with BijectionsChunkJson with ApplicativeInstances with MonoidInstances {

  val catchUp = service("catchup", "1.0.0") { context =>

    startup {
      val contentApiUrl = context.config[String]("contentApiUrl")
      val client = (new HttpClientXLightWeb).path(contentApiUrl).contentType[JsonAST.JValue](application/json)
      Future(new ContentApi(client))
    } ->
    request { api =>
      produce(application/json) {

        path("/") {
          get { request: HttpRequest[ByteChunk] =>

            val shortStories = api.getArticles(limit=3, maxWordcount=500, minWordcount=50)
            val medStories = api.getArticles(limit=1, maxWordcount=1000, minWordcount=501)
            val longStories = api.getArticles(limit=1, maxWordcount=2000, minWordcount=1001)

            (shortStories |@| medStories |@| longStories) { (s, m, l) =>
              HttpResponse[JsonAST.JValue](content = s |+| m |+| l)
            }

          }
        }

      }
    } ->
    shutdown { _ => Future(()) }
  }

}
