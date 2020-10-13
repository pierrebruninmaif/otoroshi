package otoroshi.openapi

import endpoints4s.algebra.Documentation
import endpoints4s.{Tupler, algebra, generic, openapi}
import endpoints4s.openapi.model._
import play.api.libs.json.Json

abstract class ControllerDoc[T] extends algebra.Endpoints
  with openapi.Endpoints
  with openapi.JsonEntitiesFromSchemas
  with Schemas {

  implicit lazy val _schema: JsonSchema[T] = schema()
  implicit lazy val seqSchema: JsonSchema[Seq[T]] = arrayJsonSchema

  def schema(): JsonSchema[T]
  def root(path: Path[Unit]): Url[Unit]
  def entityName: String

  implicit val strseg = stringSegment

  final def created[A, B, R](
    entity: ResponseEntity[A],
    docs: Documentation = None,
    headers: ResponseHeaders[B] = emptyResponseHeaders
  )(implicit tupler: Tupler.Aux[A, B, R]): Response[R] = response(Created, entity, docs, headers)

  val findAll: Endpoint[Unit, Seq[T]] = endpoint(
    get(root(path)),
    ok(jsonResponse[Seq[T]])
  )
  val findById: Endpoint[String, T] = endpoint(
    get(root(path) / segment[String](s"$entityName-id")),
    ok(jsonResponse[T])
  )
  val createEntity: Endpoint[T, T] = endpoint(
    post(root(path), jsonRequest[T]),
    created(jsonResponse[T])
  )
  val updateEntity: Endpoint[(String, T), T] = endpoint(
    put(root(path) / segment[String](s"$entityName-id"), jsonRequest[T]),
    ok(jsonResponse[T])
  )
  val deleteEntity: Endpoint[Unit, T] = endpoint(
    delete(root(path) / segment[String](s"$entityName-id")),
    ok(emptyResponse)
  )

  def endpoints: Seq[DocumentedEndpoint] = Seq(
    findById, findAll, createEntity, updateEntity, deleteEntity
  )

  def print(): Unit = {
    val api: OpenApi = openApi(Info(title = s"API to manipulate ${entityName}s", version = "1.0.0"))(endpoints: _*)
    println(Json.prettyPrint(Json.parse(OpenApi.stringEncoder.encode(api))))
  }
}