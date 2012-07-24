package thunderhamster

import akka.dispatch.{ExecutionContext, Future}
import blueeyes.BlueEyesServiceBuilder
import blueeyes.core.data.{BijectionsChunkJson, BijectionsChunkString}
import blueeyes.core.service.engines.HttpClientXLightWeb
import blueeyes.core.http.MimeTypes.{json, application}
import blueeyes.core.data.ByteChunk
import blueeyes.core.http._
import blueeyes.json.JsonAST.JValue
import blueeyes.core.http.HttpStatusCodes.Found
import blueeyes.core.http.HttpHeaders.Location
import scalaz._, Scalaz._
import ApplicativeInstances._, MonoidInstances._


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
          get { _: HttpRequest[ByteChunk] =>
            FoundResponse(location="/quick")
          }
        } ~
        path("/quick") {
          get { _: HttpRequest[ByteChunk] =>

            val shortStories = api.getArticles(limit=3, maxWordcount=500, minWordcount=50)
            val medStory = api.getArticles(limit=1, maxWordcount=1000, minWordcount=501)
            val longStory = api.getArticles(limit=1, maxWordcount=2000, minWordcount=1001)

            BundleResponse(shortStories, medStory, longStory)
          }
        } ~
        path("/long") {
          get { _: HttpRequest[ByteChunk] =>

            val longStories = api.getArticles(limit=3, maxWordcount=20000, minWordcount=2000)

            BundleResponse(longStories)
          }
        }

      }
    } ->
    shutdown { _ => Future(()) }
  }

}

object BundleResponse {
  def apply(responses: Future[Option[JValue]]*)(implicit ex: ExecutionContext): Future[HttpResponse[JValue]] =
    responses.toList.sequence map (stories => HttpResponse(content = stories.suml))
}

object FoundResponse {
  def apply[T](location: T)(implicit ex: ExecutionContext, ev: T => URI): Future[HttpResponse[JValue]] =
    Future(HttpResponse[JValue](status=HttpStatus(Found), headers=HttpHeaders(Seq(Location(location)))))
}
