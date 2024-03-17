package io.cloudshiftdev.awscdklib.testing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.serializer

data class CfnProperty(
    val resource: Resource,
    val propertyName: String,
    val jsonElement: JsonElement
) {
    val json: String
        get() {
            val format = Json { prettyPrint = true }
            return format.encodeToString(serializer(), jsonElement)
        }
}
