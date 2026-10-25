
@file:Suppress(
  "KotlinRedundantDiagnosticSuppress",
  "PropertyName",
  "MayBeConstant",
  "RedundantVisibilityModifier",
  "RedundantCompanionReference",
  "RemoveEmptyClassBody",
  "SpellCheckingInspection",
  "unused",
)

package com.daiarn.counter.generated



public interface UpdateVisitMutation :
    com.google.firebase.dataconnect.generated.GeneratedMutation<
      CounterConnector,
      UpdateVisitMutation.Data,
      UpdateVisitMutation.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
  
    val type: String,
  
    val occurredAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
  
    val note: String,
  
    val deleted: Boolean,
  
    val revision: Long,
  
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val visit_updateMany: Int,
  
  ) {
    
    
  }
  

  public companion object {
    public val operationName: String = "UpdateVisit"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun UpdateVisitMutation.ref(
  
    id: java.util.UUID,type: String,occurredAt: com.google.firebase.Timestamp,note: String,deleted: Boolean,revision: Long,

  
  
): com.google.firebase.dataconnect.MutationRef<
    UpdateVisitMutation.Data,
    UpdateVisitMutation.Variables
  > =
  ref(
    
      UpdateVisitMutation.Variables(
        id=id,type=type,occurredAt=occurredAt,note=note,deleted=deleted,revision=revision,
  
      )
    
  )

public suspend fun UpdateVisitMutation.execute(

  
    
      id: java.util.UUID,type: String,occurredAt: com.google.firebase.Timestamp,note: String,deleted: Boolean,revision: Long,

  

  ): com.google.firebase.dataconnect.MutationResult<
    UpdateVisitMutation.Data,
    UpdateVisitMutation.Variables
  > =
  ref(
    
      id=id,type=type,occurredAt=occurredAt,note=note,deleted=deleted,revision=revision,
  
    
  ).execute()


