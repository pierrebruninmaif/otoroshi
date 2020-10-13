package otoroshi.openapi

import endpoints4s.{algebra, generic}
import otoroshi.models.{EntityLocation, TeamId}

trait Schemas extends algebra.JsonEntitiesFromSchemas with generic.JsonSchemas {

  implicit lazy val TenantIdSchema: JsonSchema[otoroshi.models.TenantId] = genericJsonSchema[otoroshi.models.TenantId]
  implicit lazy val TeamIdSchema: JsonSchema[otoroshi.models.TeamId] = genericJsonSchema[otoroshi.models.TeamId]
  implicit lazy val seqTeamIdSchema: JsonSchema[Seq[TeamId]] = arrayJsonSchema
  implicit lazy val MapStrStrSchema: JsonSchema[Map[String, String]] = mapJsonSchema
  implicit lazy val EntityLocationSchema: JsonSchema[EntityLocation] = genericJsonSchema[EntityLocation]
  implicit lazy val ServiceGroupSchema: JsonSchema[models.ServiceGroup] = genericJsonSchema[models.ServiceGroup]
  implicit lazy val SeqServiceGroupSchema: JsonSchema[Seq[models.ServiceGroup]] = arrayJsonSchema
}
