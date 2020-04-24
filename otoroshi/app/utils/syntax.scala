package otoroshi.utils.syntax

import akka.http.scaladsl.util.FastFuture
import akka.util.ByteString
import com.github.blemale.scaffeine.Cache
import play.api.Logger
import play.api.libs.json._
import utils.{Regex, RegexPool}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

object implicits {
  implicit class BetterSyntax[A](private val obj: A) extends AnyVal {
    def some: Option[A] = Some(obj)
    def option: Option[A] = Some(obj)
    def left[B]: Either[A, B] = Left(obj)
    def right[B]: Either[B, A] = Right(obj)
    def future: Future[A] = FastFuture.successful(obj)
    def asFuture: Future[A] = FastFuture.successful(obj)
    def toFuture: Future[A] = FastFuture.successful(obj)
    def somef: Future[Option[A]] = FastFuture.successful(Some(obj))
    def leftf[B]: Future[Either[A, B]] = FastFuture.successful(Left(obj))
    def rightf[B]: Future[Either[B, A]] = FastFuture.successful(Right(obj))
    def debug(f: A => Any): A = {
      f(obj)
      obj
    }
    def debugPrintln: A = {
      println(obj)
      obj
    }
    def debugLogger(logger: Logger): A = {
      logger.debug(s"$obj")
      obj
    }
  }
  implicit class BetterString(private val obj: String) extends AnyVal {
    import otoroshi.utils.string.Implicits._
    def slugify: String = obj.slug
    def wildcard: Regex = RegexPool.apply(obj)
    def regex: Regex = RegexPool.regex(obj)
    def byteString: ByteString = ByteString(obj)
    def json: JsValue = JsString(obj)
  }
  implicit class BetterBoolean(private val obj: Boolean) extends AnyVal {
    def json: JsValue = JsBoolean(obj)
  }
  implicit class BetterDouble(private val obj: Double) extends AnyVal {
    def json: JsValue = JsNumber(obj)
  }
  implicit class BetterInt(private val obj: Int) extends AnyVal {
    def json: JsValue = JsNumber(obj)
  }
  implicit class BetterLong(private val obj: Long) extends AnyVal {
    def json: JsValue = JsNumber(obj)
  }
  implicit class BetterJsValue(private val obj: JsValue) extends AnyVal {
    def stringify: String = Json.stringify(obj)
    def prettify: String = Json.prettyPrint(obj)
  }
  implicit class BetterFuture[A](private val obj: Future[A]) extends AnyVal {
    def fleft[B](implicit ec: ExecutionContext): Future[Either[A, B]] = obj.map(v => Left(v))
    def fright[B](implicit ec: ExecutionContext): Future[Either[B, A]] = obj.map(v => Right(v))
    def asLeft[R](implicit executor: ExecutionContext): Future[Either[A, R]] = obj.map(a => Left[A, R](a))
    def asRight[R](implicit executor: ExecutionContext): Future[Either[R, A]] = obj.map(a => Right[R, A](a))
    def fold[U](pf: PartialFunction[Try[A], U])(implicit executor: ExecutionContext): Future[U] = {
      val promise = Promise[U]
      obj.andThen {
        case underlying: Try[A] => {
          try {
            promise.trySuccess(pf(underlying))
          } catch {
            case e: Throwable => promise.tryFailure(e)
          }
        }
      }
      promise.future
    }

    def foldM[U](pf: PartialFunction[Try[A], Future[U]])(implicit executor: ExecutionContext): Future[U] = {
      val promise = Promise[U]
      obj.andThen {
        case underlying: Try[A] => {
          try {
            pf(underlying).andThen {
              case Success(v) => promise.trySuccess(v)
              case Failure(e) => promise.tryFailure(e)
            }
          } catch {
            case e: Throwable => promise.tryFailure(e)
          }
        }
      }
      promise.future
    }
  }
  implicit class BetterCache[A, B](val cache: Cache[A, B]) extends AnyVal {
    def getOrElse(key: A, el: => B): B = {
      cache.getIfPresent(key).getOrElse {
        val res = el
        cache.put(key, res)
        res
      }
    }
  }
}