package io.cloudshiftdev.awscdklib.customresource

import io.cloudshiftdev.awscdk.customresources.AwsCustomResource
import io.cloudshiftdev.awscdk.customresources.AwsCustomResourcePolicy
import io.cloudshiftdev.awscdk.customresources.SdkCallsPolicyOptions
import io.cloudshiftdev.awscdk.services.iam.PolicyStatement
import io.cloudshiftdev.awscdklib.iam.PolicyStatementsBuilder

public fun AwsCustomResource.Builder.policyStatements(block: (PolicyStatementsBuilder).() -> Unit) {
    policy(
        AwsCustomResourcePolicy.fromStatements(
            io.cloudshiftdev.awscdklib.iam.policyStatements(block)
        )
    )
}

public fun AwsCustomResource.Builder.policyStatement(block: (PolicyStatement.Builder).() -> Unit) {
    policy(AwsCustomResourcePolicy.fromStatements(PolicyStatement(block)))
}

public fun AwsCustomResource.Builder.policyFromSdkCalls(vararg resources: String) {
    policy(
        AwsCustomResourcePolicy.fromSdkCalls(
            SdkCallsPolicyOptions { resources(resources.toList()) }
        )
    )
}
