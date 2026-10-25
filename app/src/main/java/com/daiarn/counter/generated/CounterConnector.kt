
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

import com.google.firebase.dataconnect.getInstance as _fdcGetInstance
import kotlin.time.Duration.Companion.milliseconds as _milliseconds

public interface CounterConnector : com.google.firebase.dataconnect.generated.GeneratedConnector<CounterConnector> {
  override val dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect

  
    public val createVisit: CreateVisitMutation
  
    public val getVisit: GetVisitQuery
  
    public val listVisits: ListVisitsQuery

    public val updateVisit: UpdateVisitMutation
  

  public companion object {
    @Suppress("MemberVisibilityCanBePrivate")
    public val config: com.google.firebase.dataconnect.ConnectorConfig = com.google.firebase.dataconnect.ConnectorConfig(
      connector = "counter",
      location = "europe-central2",
      serviceId = "counter-f0787-service",
    )

    public fun getInstance(
      dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect
    ):CounterConnector = synchronized(instances) {
      instances.getOrPut(dataConnect) {
        CounterConnectorImpl(dataConnect)
      }
    }

    private val instances = java.util.WeakHashMap<com.google.firebase.dataconnect.FirebaseDataConnect, CounterConnectorImpl>()

    
  }
}

public val CounterConnector.Companion.instance:CounterConnector
  get() = getInstance(com.google.firebase.dataconnect.FirebaseDataConnect._fdcGetInstance(
    config
  ))

public fun CounterConnector.Companion.getInstance(
  settings: com.google.firebase.dataconnect.DataConnectSettings = com.google.firebase.dataconnect.DataConnectSettings()
):CounterConnector =
  getInstance(com.google.firebase.dataconnect.FirebaseDataConnect._fdcGetInstance(config, settings))

public fun CounterConnector.Companion.getInstance(
  app: com.google.firebase.FirebaseApp,
  settings: com.google.firebase.dataconnect.DataConnectSettings = com.google.firebase.dataconnect.DataConnectSettings()
):CounterConnector =
  getInstance(com.google.firebase.dataconnect.FirebaseDataConnect._fdcGetInstance(app, config, settings))

private class CounterConnectorImpl(
  override val dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect
) : CounterConnector {
  
    override val createVisit by lazy(LazyThreadSafetyMode.PUBLICATION) {
      CreateVisitMutationImpl(this)
    }
  
    override val getVisit by lazy(LazyThreadSafetyMode.PUBLICATION) {
      GetVisitQueryImpl(this)
    }
  
    override val listVisits by lazy(LazyThreadSafetyMode.PUBLICATION) {
      ListVisitsQueryImpl(this)
    }

    override val updateVisit by lazy(LazyThreadSafetyMode.PUBLICATION) {
      UpdateVisitMutationImpl(this)
    }
  

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun operations(): List<com.google.firebase.dataconnect.generated.GeneratedOperation<CounterConnector, *, *>> =
    queries() + mutations()

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun mutations(): List<com.google.firebase.dataconnect.generated.GeneratedMutation<CounterConnector, *, *>> =
    listOf(
      createVisit,
        updateVisit,
        
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun queries(): List<com.google.firebase.dataconnect.generated.GeneratedQuery<CounterConnector, *, *>> =
    listOf(
      getVisit,
        listVisits,
        
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun copy(dataConnect: com.google.firebase.dataconnect.FirebaseDataConnect) =
    CounterConnectorImpl(dataConnect)

  override fun equals(other: Any?): Boolean =
    other is CounterConnectorImpl &&
    other.dataConnect == dataConnect

  override fun hashCode(): Int =
    java.util.Objects.hash(
      "CounterConnectorImpl",
      dataConnect,
    )

  override fun toString(): String =
    "CounterConnectorImpl(dataConnect=$dataConnect)"
}



private open class CounterConnectorGeneratedQueryImpl<Data, Variables>(
  override val connector: CounterConnector,
  override val operationName: String,
  override val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
  override val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
) : com.google.firebase.dataconnect.generated.GeneratedQuery<CounterConnector, Data, Variables> {

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun copy(
    connector: CounterConnector,
    operationName: String,
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
    variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
  ) =
    CounterConnectorGeneratedQueryImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewVariables> withVariablesSerializer(
    variablesSerializer: kotlinx.serialization.SerializationStrategy<NewVariables>
  ) =
    CounterConnectorGeneratedQueryImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewData> withDataDeserializer(
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<NewData>
  ) =
    CounterConnectorGeneratedQueryImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun equals(other: Any?): Boolean =
    other is CounterConnectorGeneratedQueryImpl<*,*> &&
    other.connector == connector &&
    other.operationName == operationName &&
    other.dataDeserializer == dataDeserializer &&
    other.variablesSerializer == variablesSerializer

  override fun hashCode(): Int =
    java.util.Objects.hash(
      "CounterConnectorGeneratedQueryImpl",
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun toString(): String =
    "CounterConnectorGeneratedQueryImpl(" +
    "operationName=$operationName, " +
    "dataDeserializer=$dataDeserializer, " +
    "variablesSerializer=$variablesSerializer, " +
    "connector=$connector)"
}

private open class CounterConnectorGeneratedMutationImpl<Data, Variables>(
  override val connector: CounterConnector,
  override val operationName: String,
  override val dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
  override val variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
) : com.google.firebase.dataconnect.generated.GeneratedMutation<CounterConnector, Data, Variables> {

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun copy(
    connector: CounterConnector,
    operationName: String,
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<Data>,
    variablesSerializer: kotlinx.serialization.SerializationStrategy<Variables>,
  ) =
    CounterConnectorGeneratedMutationImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewVariables> withVariablesSerializer(
    variablesSerializer: kotlinx.serialization.SerializationStrategy<NewVariables>
  ) =
    CounterConnectorGeneratedMutationImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  @com.google.firebase.dataconnect.ExperimentalFirebaseDataConnect
  override fun <NewData> withDataDeserializer(
    dataDeserializer: kotlinx.serialization.DeserializationStrategy<NewData>
  ) =
    CounterConnectorGeneratedMutationImpl(
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun equals(other: Any?): Boolean =
    other is CounterConnectorGeneratedMutationImpl<*,*> &&
    other.connector == connector &&
    other.operationName == operationName &&
    other.dataDeserializer == dataDeserializer &&
    other.variablesSerializer == variablesSerializer

  override fun hashCode(): Int =
    java.util.Objects.hash(
      "CounterConnectorGeneratedMutationImpl",
      connector, operationName, dataDeserializer, variablesSerializer
    )

  override fun toString(): String =
    "CounterConnectorGeneratedMutationImpl(" +
    "operationName=$operationName, " +
    "dataDeserializer=$dataDeserializer, " +
    "variablesSerializer=$variablesSerializer, " +
    "connector=$connector)"
}



private class CreateVisitMutationImpl(
  connector: CounterConnector
):
  CreateVisitMutation,
  CounterConnectorGeneratedMutationImpl<
      CreateVisitMutation.Data,
      CreateVisitMutation.Variables
  >(
    connector,
    CreateVisitMutation.Companion.operationName,
    CreateVisitMutation.Companion.dataDeserializer,
    CreateVisitMutation.Companion.variablesSerializer,
  )


private class GetVisitQueryImpl(
  connector: CounterConnector
):
  GetVisitQuery,
  CounterConnectorGeneratedQueryImpl<
      GetVisitQuery.Data,
      GetVisitQuery.Variables
  >(
    connector,
    GetVisitQuery.Companion.operationName,
    GetVisitQuery.Companion.dataDeserializer,
    GetVisitQuery.Companion.variablesSerializer,
  )


private class ListVisitsQueryImpl(
  connector: CounterConnector
):
  ListVisitsQuery,
  CounterConnectorGeneratedQueryImpl<
      ListVisitsQuery.Data,
      ListVisitsQuery.Variables
  >(
    connector,
    ListVisitsQuery.Companion.operationName,
    ListVisitsQuery.Companion.dataDeserializer,
    ListVisitsQuery.Companion.variablesSerializer,
  )


private class UpdateVisitMutationImpl(
  connector: CounterConnector
):
  UpdateVisitMutation,
  CounterConnectorGeneratedMutationImpl<
      UpdateVisitMutation.Data,
      UpdateVisitMutation.Variables
  >(
    connector,
    UpdateVisitMutation.Companion.operationName,
    UpdateVisitMutation.Companion.dataDeserializer,
    UpdateVisitMutation.Companion.variablesSerializer,
  )


