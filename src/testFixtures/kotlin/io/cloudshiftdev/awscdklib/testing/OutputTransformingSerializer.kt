package io.cloudshiftdev.awscdklib.testing

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

object OutputTransformingSerializer : JsonTransformingSerializer<List<Output>>(serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val outputs =
            element.jsonObject.map { entry ->
                val key = entry.key
                val value = entry.value
                val outputObj = value.jsonObject.toMutableMap()
                // put the logical id where it belongs
                outputObj["LogicalId"] = JsonPrimitive(key)

                outputObj["Export"]?.jsonObject?.let { export ->
                    export["Name"]?.jsonPrimitive?.let { name ->
                        outputObj["ExportedName"] = name
                        outputObj.remove("Export")
                    }
                }
                JsonObject(outputObj)
            }
        return JsonArray(outputs)
    }
}
