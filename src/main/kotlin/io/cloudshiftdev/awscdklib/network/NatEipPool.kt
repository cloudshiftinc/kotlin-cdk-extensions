package io.cloudshiftdev.awscdklib.network

import io.cloudshiftdev.awscdk.CfnOutput
import io.cloudshiftdev.awscdk.RemovalPolicy
import io.cloudshiftdev.awscdk.services.ec2.CfnEIP
import io.cloudshiftdev.constructs.Construct

public class NatEipPool(scope: Construct, id: String, poolSize: Int) : Construct(scope, id) {
    public val eips: List<String>

    init {
        var count = 1
        val eipConstructs =
            generateSequence { (count++).takeIf { it <= poolSize } }
                .map {
                    val eip = CfnEIP(this, "EIP$it") { domain("vpc") }
                    eip.applyRemovalPolicy(RemovalPolicy.RETAIN)
                    eip
                }
                .toList()

        eips = eipConstructs.map { it.attrAllocationId() }

        eipConstructs.forEachIndexed { idx, eip ->
            val outputId = "natPublicIp${idx + 1}"
            CfnOutput(scope, outputId) {
                exportName(outputId)
                value(eip.attrPublicIp())
            }
        }
    }
}
