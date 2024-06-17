package io.cloudshiftdev.awscdklib.testing

import io.cloudshiftdev.awscdk.App
import io.cloudshiftdev.awscdk.assertions.Template
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

fun testStack(block: (io.cloudshiftdev.awscdk.Stack) -> Unit): CdkTestContext {
    val app = App()
    val stack = io.cloudshiftdev.awscdk.Stack(app, "TestStack")
    block(stack)

    val template = Template.fromStack(stack)
    val jsonRoot = template.toJSON().toJsonElement().jsonObject
    val jsonStr = jsonRoot.toString()
    println(jsonStr)

    val map = jsonRoot.toMutableMap()
    map["RawResources"] = map["Resources"]!!

    val json = Json { ignoreUnknownKeys = true }

    val stk = json.decodeFromJsonElement<Stack>(JsonObject(map))

    return CdkTestContext(template, jsonStr, stk)
}

fun Collection<*>.toJsonElement(): JsonElement = JsonArray(mapNotNull { it.toJsonElement() })

fun Map<*, *>.toJsonElement(): JsonElement =
    JsonObject(
        mapNotNull { (it.key as? String ?: return@mapNotNull null) to it.value.toJsonElement() }
            .toMap()
    )

fun Any?.toJsonElement(): JsonElement =
    when (this) {
        null -> JsonNull
        is Map<*, *> -> toJsonElement()
        is Collection<*> -> toJsonElement()
        else -> JsonPrimitive(toString())
    }
