package io.cloudshiftdev.awscdklib.testing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Output(
    @SerialName("LogicalId") val logicalId: String,
    @SerialName("Value") val value: JsonElement,
    @SerialName("ExportedName") val exportedName: String? = null,
    @SerialName("Description") val description: String? = null
)
