package io.cloudshiftdev.awscdklib.network.routable

import io.cloudshiftdev.awscdk.CfnOutput
import io.cloudshiftdev.awscdk.RemovalPolicy
import io.cloudshiftdev.awscdk.services.ec2.CfnEIP
import io.cloudshiftdev.awscdk.services.ec2.PublicSubnet
import io.cloudshiftdev.awscdk.services.ec2.RouterType
import io.cloudshiftdev.constructs.Construct

public class NatGatewayRoutable(block: (NatGatewayRoutableBuilder).() -> Unit = {}) :
    SingletonRoutable<List<String>>({ vpc, networkDefinition ->
        val eipPool =
            EipPool(vpc, "EipPool", networkDefinition.maxAzs + networkDefinition.reservedAzs)
        eipPool.eips
    }) {

    private val props: NatGatewayRoutableProps = NatGatewayRoutableBuilder().apply(block).build()
    private var eipIndex = 0

    override fun routeTarget(eips: List<String>, context: RoutableContext): RouteTarget {
        val targetSubnet =
            context.subnetResolver.resolve(props.subnetPlacement, context.availabilityZone)
        require(targetSubnet is PublicSubnet) {
            "Expected NAT Gateway placement to be in public subnet; got $targetSubnet"
        }

        if (eipIndex >= eips.size) {
            error("Not enough EIPs in pool (${eips.size}) to allocate for NAT gateway")
        }
        val eip = eips[eipIndex++]
        val natGateway = targetSubnet.addNatGateway(eip)

        return RouteTarget(
            routerId = natGateway.ref(),
            routerType = RouterType.NAT_GATEWAY,
            enablesInternetConnectivity = true
        )
    }

    private class EipPool(scope: Construct, id: String, poolSize: Int) : Construct(scope, id) {
        val eips: List<String>

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
}

public class NatGatewayRoutableBuilder internal constructor() {
    private var subnetPlacement: String = "Public"

    public fun subnetPlacements(subnetGroupName: String) {
        subnetPlacement = subnetGroupName
    }

    internal fun build(): NatGatewayRoutableProps {
        return NatGatewayRoutableProps(subnetPlacement = subnetPlacement)
    }
}

internal data class NatGatewayRoutableProps(val subnetPlacement: String)
