package io.cloudshiftdev.awscdklib.testing

import io.cloudshiftdev.awscdk.assertions.Template

data class CdkTestContext(val template: Template, val json: String, val stack: Stack)

fun CdkTestContext.shouldEqualJsonResource(
    resource: String,
    block: (JsonCompareOptions).() -> Unit = {},
) {
    json.shouldEqualJsonResource(resource) {
        ignore("\$.Resources.*.Properties.Code.S3Key")
        apply(block)
    }
}
