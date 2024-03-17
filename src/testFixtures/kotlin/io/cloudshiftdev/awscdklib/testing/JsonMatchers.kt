package io.cloudshiftdev.awscdklib.testing

import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import io.kotest.assertions.json.CompareJsonOptions
import io.kotest.assertions.json.shouldContainJsonKeyValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue

private object Holder

fun String.shouldEqualJson(expectedJson: String, block: (JsonCompareOptions).() -> Unit = {}) {
    val opts = JsonCompareOptions()
    opts.apply(block)

    val expected = processIgnores(opts.ignored, expectedJson)
    val actual = processIgnores(opts.ignored, this)
    withClue(opts) { actual.shouldEqualJson { expected } }
}

fun String.shouldEqualJsonResource(resource: String, block: (JsonCompareOptions).() -> Unit = {}) {
    val opts = JsonCompareOptions()
    opts.apply(block)

    val expectedJson =
        Holder.javaClass.getResourceAsStream(resource).bufferedReader().use { it.readText() }
    withClue("json from resource '$resource'") { shouldEqualJson(expectedJson, block) }
}

fun String.shouldEqualJsonValueAtPath(jsonPath: String, value: String) {
    this.shouldContainJsonKeyValue(jsonPath, value)
}

private fun processIgnores(ignoredPaths: List<String>, json: String): String {
    if (ignoredPaths.isEmpty()) {
        return json
    }

    val doc = JsonPath.parse(json)
    ignoredPaths.forEach {
        try {
            doc.delete(it)
        } catch (ignored: PathNotFoundException) {}
    }
    return doc.jsonString()
}

public class JsonCompareOptions {
    internal val ignored = mutableListOf<String>()
    internal val compareJsonOptions = CompareJsonOptions()

    fun ignore(jsonPaths: Iterable<String>) {
        ignored.addAll(jsonPaths)
    }

    fun ignore(jsonPath: String) {
        ignored.add(jsonPath)
    }

    override fun toString(): String {
        return "JsonCompareOptions(ignored=$ignored)"
    }
}
