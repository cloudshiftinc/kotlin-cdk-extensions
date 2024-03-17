package io.cloudshiftdev.awscdklib.testing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Stack(
    @SerialName("Resources")
    @Serializable(with = ResourceTransformingSerializer::class)
    val resources: List<Resource>,
    @SerialName("RawResources") val resourceMap: Map<String, JsonElement>,
    @SerialName("Outputs")
    @Serializable(with = OutputTransformingSerializer::class)
    val outputs: List<Output> = emptyList()
)
