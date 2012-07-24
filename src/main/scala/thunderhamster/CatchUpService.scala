package thunderhamster

import akka.dispatch.{ExecutionContext, Future}
import blueeyes.BlueEyesServiceBuilder
import blueeyes.core.data.{BijectionsChunkJson, BijectionsChunkString}
import blueeyes.core.service.engines.HttpClientXLightWeb
import blueeyes.core.http.MimeTypes.{json, application}
import blueeyes.core.data.ByteChunk
import blueeyes.core.http.{HttpResponse, HttpRequest}
import blueeyes.json.JsonAST.JValue


trait CatchUpService extends BlueEyesServiceBuilder
    with BijectionsChunkString with BijectionsChunkJson {

  val catchUp = service("catchup", "1.0.0") { context =>

    startup {
      val contentApiUrl = context.config[String]("contentApiUrl")
      val client = (new HttpClientXLightWeb).path(contentApiUrl).contentType[JValue](application/json)
      Future(new ContentApi(client))
    } ->
    request { api =>
      produce(application/json) {

        path("/") {
          get { request: HttpRequest[ByteChunk] =>

            val shortStories = api.getArticles(limit=3, maxWordcount=500, minWordcount=50)
            val medStory = api.getArticles(limit=1, maxWordcount=1000, minWordcount=501)
            val longStory = api.getArticles(limit=1, maxWordcount=2000, minWordcount=1001)

            BundleResponse(shortStories, medStory, longStory)
          }
        }

      }
    } ->
    shutdown { _ => Future(()) }
  }

}

object BundleResponse {
  import scalaz._, Scalaz._
  import ApplicativeInstances._, MonoidInstances._
  def apply(responses: Future[Option[JValue]]*)(implicit ex: ExecutionContext): Future[HttpResponse[JValue]] =
    responses.toList.sequence map (stories => HttpResponse(content = stories.suml))
}
