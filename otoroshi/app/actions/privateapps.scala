package actions

import akka.http.scaladsl.util.FastFuture
import akka.http.scaladsl.util.FastFuture._
import auth.GenericOauth2Module
import cluster.ClusterMode
import env.Env
import models.PrivateAppsUser
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import utils.RequestImplicits._

case class PrivateAppsActionContext[A](request: Request[A],
                                       user: Option[PrivateAppsUser],
                                       globalConfig: models.GlobalConfig) {
  def connected: Boolean              = user.isDefined
  def from(implicit env: Env): String = request.theIpAddress
  def ua: String                      = request.theUserAgent
}

class PrivateAppsAction(val parser: BodyParser[AnyContent])(implicit env: Env)
    extends ActionBuilder[PrivateAppsActionContext, AnyContent]
    with ActionFunction[Request, PrivateAppsActionContext] {

  implicit lazy val ec = env.otoroshiExecutionContext

  override def invokeBlock[A](request: Request[A],
                              block: (PrivateAppsActionContext[A]) => Future[Result]): Future[Result] = {
    val host = request.theDomain // if (request.host.contains(":")) request.host.split(":")(0) else request.host
    host match {
      case env.privateAppsHost => {
        env.datastores.globalConfigDataStore.singleton().flatMap { globalConfig =>
          val cookieOpt = request.cookies.find(c => c.name.startsWith("oto-papps-"))
          cookieOpt.flatMap(env.extractPrivateSessionId).map { id =>
            // request.cookies.get("oto-papps").flatMap(env.extractPrivateSessionId).map { id =>
            env.datastores.privateAppsUserDataStore.findById(id).flatMap {
              case Some(user) =>
                user.withAuthModuleConfig(a => GenericOauth2Module.handleTokenRefresh(a, user))
                block(PrivateAppsActionContext(request, Some(user), globalConfig))
              case None if env.clusterConfig.mode == ClusterMode.Worker => {
                env.clusterAgent.isSessionValid(id).flatMap {
                  case Some(user) => block(PrivateAppsActionContext(request, Some(user), globalConfig))
                  case None       => block(PrivateAppsActionContext(request, None, globalConfig))
                }
              }
              case None => block(PrivateAppsActionContext(request, None, globalConfig))
            }
          } getOrElse {
            cookieOpt match {
              case None => block(PrivateAppsActionContext(request, None, globalConfig))
              case Some(cookie) =>
                block(PrivateAppsActionContext(request, None, globalConfig)).fast
                  .map(
                    _.discardingCookies(
                      env.removePrivateSessionCookiesWithSuffix(host, cookie.name.replace("oto-papps-", "")): _*
                    )
                  )
            }
          }
        }
      }
      case _ => {
        // TODO : based on Accept header
        FastFuture.successful(Results.NotFound(views.html.otoroshi.error("Not found", env)))
      }
    }
  }

  override protected def executionContext: ExecutionContext = ec
}
