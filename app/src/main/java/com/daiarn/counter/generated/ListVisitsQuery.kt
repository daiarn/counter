
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


import kotlinx.coroutines.flow.filterNotNull as _flow_filterNotNull
import kotlinx.coroutines.flow.map as _flow_map


public interface ListVisitsQuery :
    com.google.firebase.dataconnect.generated.GeneratedQuery<
      CounterConnector,
      ListVisitsQuery.Data,
      ListVisitsQuery.Variables
    >
{
  
    @kotlinx.serialization.Serializable
  public data class Variables(
  
    val offset: Int,
  
  ) {
    
    
  }
  

  
    @kotlinx.serialization.Serializable
  public data class Data(
  
    val visits: List<VisitsItem>,
  
  ) {
    
      
        @kotlinx.serialization.Serializable
  public data class VisitsItem(
  
    val id: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.UUIDSerializer::class) java.util.UUID,
  
    val type: String,
  
    val occurredAt: @kotlinx.serialization.Serializable(with = com.google.firebase.dataconnect.serializers.TimestampSerializer::class) com.google.firebase.Timestamp,
  
    val note: String,
  
    val deleted: Boolean,
  
    val revision: Long,
  
  ) {
    
    
  }
      
    
    
  }
  

  public companion object {
    public val operationName: String = "ListVisits"

    public val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data> =
      kotlinx.serialization.serializer()

    public val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables> =
      kotlinx.serialization.serializer()
  }
}

public fun ListVisitsQuery.ref(
  
    offset: Int,

  
  
): com.google.firebase.dataconnect.QueryRef<
    ListVisitsQuery.Data,
    ListVisitsQuery.Variables
  > =
  ref(
    
      ListVisitsQuery.Variables(
        offset=offset,
  
      )
    
  )

public suspend fun ListVisitsQuery.execute(

  
    
      offset: Int,

  

  ): com.google.firebase.dataconnect.QueryResult<
    ListVisitsQuery.Data,
    ListVisitsQuery.Variables
  > =
  ref(
    
      offset=offset,
  
    
  ).execute()


  public fun ListVisitsQuery.flow(
    
      offset: Int,

  
    
    ): kotlinx.coroutines.flow.Flow<ListVisitsQuery.Data> =
    ref(
        
          offset=offset,
  
        
      ).subscribe()
      .flow
      ._flow_map { querySubscriptionResult -> querySubscriptionResult.result.getOrNull() }
      ._flow_filterNotNull()
      ._flow_map { it.data }

