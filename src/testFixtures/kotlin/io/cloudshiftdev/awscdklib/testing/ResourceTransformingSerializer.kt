package io.cloudshiftdev.awscdklib.testing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.serializer

object ResourceTransformingSerializer : JsonTransformingSerializer<List<Resource>>(serializer()) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        val resources =
            element.jsonObject.map { entry ->
                val key = entry.key
                val value = entry.value
                val resourceObj = value.jsonObject.toMutableMap()

                val format = Json { prettyPrint = true }

                val json = format.encodeToString(serializer(), value)
                resourceObj["RawJson"] = JsonPrimitive(json)

                // put the logical id where it belongs
                resourceObj["LogicalId"] = JsonPrimitive(key)

                // hoist Tags up to root of resource
                resourceObj["Properties"]?.jsonObject?.let { props ->
                    props["Tags"]?.jsonArray?.let { tags ->
                        resourceObj["Tags"] =
                            JsonObject(
                                tags.associate {
                                    it.jsonObject["Key"]!!.jsonPrimitive.content to
                                        it.jsonObject["Value"]!!
                                }
                            )
                    }
                    val propsMap = props.toMutableMap()
                    propsMap.remove("Tags")
                    resourceObj["Properties"] = JsonObject(propsMap)
                }
                JsonObject(resourceObj)
            }
        return JsonArray(resources)
    }
}
