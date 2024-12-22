package io.cloudshiftdev.awscdklib.iam

import io.cloudshiftdev.awscdk.ArnComponents
import io.cloudshiftdev.awscdk.services.iam.Effect
import io.cloudshiftdev.awscdk.services.iam.PolicyStatement
import io.cloudshiftdev.awscdk.services.iam.PolicyStatementProps
import io.cloudshiftdev.constructs.Construct

public fun PolicyStatement.Builder.allow(): Unit = effect(Effect.ALLOW)

public fun PolicyStatement.Builder.deny(): Unit = effect(Effect.DENY)

public fun PolicyStatementProps.Builder.allow(): Unit = effect(Effect.ALLOW)

public fun PolicyStatementProps.Builder.deny(): Unit = effect(Effect.DENY)

public fun PolicyStatement.Builder.resource(arn: String): Unit = resources(arn)

public fun PolicyStatement.Builder.resourceArn(
    scope: Construct,
    block: (ArnComponents.Builder).() -> Unit,
): Unit = resources(io.cloudshiftdev.awscdklib.core.resourceArn(scope, block))

public fun PolicyStatement.Builder.action(action: String): Unit = actions(action)

public fun PolicyStatement.Builder.anyResource(): Unit = resources("*")

public fun PolicyStatementProps.Builder.resource(arn: String): Unit = resources(arn)

public fun PolicyStatementProps.Builder.resourceArn(
    scope: Construct,
    block: (ArnComponents.Builder).() -> Unit,
): Unit = resources(io.cloudshiftdev.awscdklib.core.resourceArn(scope, block))

public fun PolicyStatementProps.Builder.action(action: String): Unit = actions(action)

public fun PolicyStatementProps.Builder.anyResource(): Unit = resources("*")

public fun policyStatements(block: (PolicyStatementsBuilder).() -> Unit): List<PolicyStatement> =
    PolicyStatementsBuilder().apply(block).policyStatements

public class PolicyStatementsBuilder {
    internal val policyStatements = mutableListOf<PolicyStatement>()

    public fun policyStatement(block: PolicyStatement.Builder.() -> Unit) {
        policyStatements.add(PolicyStatement(block))
    }
}
