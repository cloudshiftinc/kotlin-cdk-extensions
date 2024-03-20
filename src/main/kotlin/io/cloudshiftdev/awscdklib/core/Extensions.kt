package io.cloudshiftdev.awscdklib.core

import io.cloudshiftdev.awscdk.ArnComponents
import io.cloudshiftdev.awscdk.CfnResource
import io.cloudshiftdev.awscdk.Names
import io.cloudshiftdev.awscdk.Stack
import io.cloudshiftdev.awscdk.TagProps
import io.cloudshiftdev.awscdk.Tags
import io.cloudshiftdev.constructs.Construct
import io.cloudshiftdev.constructs.IConstruct

public fun Construct.tag(key: String, value: String, block: TagProps.Builder.() -> Unit = {}) {
    Tags.of(this).add(key, value, block)
}

public fun Construct.addComment(comment: String) {
    node().children().filterIsInstance<CfnResource>().forEach { it.addComment(comment) }
}

public fun Construct.uniqueId(): String = Names.uniqueId(this)

public inline fun <reified T : CfnResource> Construct.addPropertyOverride(
    property: String,
    value: String
) {
    val resource = node().children().filterIsInstance<T>().first()
    resource.addPropertyOverride(property, value)
}

public fun CfnResource.addComment(comment: String) {
    addMetadata(mapOf("cloudshift:comment" to comment))
}

public fun CfnResource.addMetadata(newMetadata: Map<String, Any>) {
    if (newMetadata.isEmpty()) {
        return
    }
    var metadata = cfnOptions().metadata()
    metadata = metadata.toMutableMap()
    metadata.putAll(newMetadata)
    cfnOptions().metadata(metadata.toMap())
}

public fun IConstruct.allChildren(): List<IConstruct> {
    val list = mutableListOf<IConstruct>()
    node().children().forEach {
        list.add(it)
        list.addAll(it.allChildren())
    }
    return list.sortedBy { it.node().path() }
}

public fun resourceArn(scope: Construct, block: (ArnComponents.Builder).() -> Unit): String =
    Stack.of(scope).formatArn(block)

public fun anyResource(): String = "*"

public inline fun <reified T : Construct> Construct.withSingleton(
    id: String,
    block: (String) -> T
): T {
    return allChildren().filterIsInstance<T>().firstOrNull { it.node().id() == id } ?: block(id)
}

public inline fun <reified T> Construct.withSingleton(
    predicate: (T) -> Boolean = { true },
    block: () -> T
): T {
    return allChildren().filterIsInstance<T>().firstOrNull(predicate) ?: block()
}
