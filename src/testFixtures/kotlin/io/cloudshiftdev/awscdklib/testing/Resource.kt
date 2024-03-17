package io.cloudshiftdev.awscdklib.testing

import io.kotest.assertions.asClue
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Resource(
    @SerialName("RawJson") val json: String,
    @SerialName("LogicalId") val logicalId: String,
    @SerialName("Type") val type: String,
    @SerialName("Properties") private val props: Map<String, JsonElement> = emptyMap(),
    @SerialName("CreationPolicy") val creationPolicy: JsonElement? = null,
    @SerialName("UpdatePolicy") val updatePolicy: JsonElement? = null,
    @SerialName("UpdateReplacePolicy") val updateReplacePolicy: String? = null,
    @SerialName("DeletionPolicy") val deletionPolicy: String? = null,
    @SerialName("Tags") val tags: Map<String, String> = emptyMap(),
    @SerialName("Metadata") val metadata: Map<String, String> = emptyMap(),
    @SerialName("DependsOn") val dependsOn: List<JsonElement> = emptyList()
) {
    @kotlinx.serialization.Transient
    val properties: Map<String, CfnProperty> =
        props.mapValues { CfnProperty(this@Resource, it.key, it.value) }

    override fun toString(): String {
        return "Resource(logicalId=$logicalId, type=$type)"
    }
}

fun Resource.shouldHaveProperty(name: String): CfnProperty {
    withClue("Property '$name' on $this") {
        return properties[name].shouldNotBeNull()
    }
}

fun List<Resource>.filterByType(type: String): List<Resource> {
    return filter { it.type == type }
}

fun Resource.shouldBeOfType(type: String) {
    this.asClue { this.type.shouldBe(type) }
}
