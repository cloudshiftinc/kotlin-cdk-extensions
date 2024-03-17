package io.cloudshiftdev.awscdklib.network.securevpc

import io.cloudshiftdev.awscdk.services.ec2.DefaultInstanceTenancy
import io.cloudshiftdev.awscdk.services.ec2.FlowLogDestination
import io.cloudshiftdev.awscdk.services.ec2.FlowLogMaxAggregationInterval
import io.cloudshiftdev.awscdk.services.ec2.FlowLogTrafficType
import io.cloudshiftdev.awscdk.services.ec2.IpAddresses
import io.cloudshiftdev.awscdk.services.ec2.RouterType
import io.cloudshiftdev.awscdk.services.ec2.Subnet
import io.cloudshiftdev.awscdk.services.ec2.SubnetConfiguration
import io.cloudshiftdev.awscdk.services.ec2.Vpc
import io.cloudshiftdev.awscdklib.network.CidrBlock
import io.cloudshiftdev.awscdklib.network.SubnetGroupType
import io.cloudshiftdev.awscdklib.network.SubnetPredicates
import io.cloudshiftdev.awscdklib.network.cidrBlock
import io.cloudshiftdev.awscdklib.network.routable.NatGatewayRoutable
import io.cloudshiftdev.awscdklib.network.routable.NetworkFirewallRoutable
import io.cloudshiftdev.awscdklib.network.routable.RoutableContext
import io.cloudshiftdev.constructs.Construct

public class SecureVpc(scope: Construct, id: String, block: (SecureVpcBuilder).() -> Unit) :
    Construct(scope, id) {
    init {
        val networkDef = buildNetworkDefinition(block)
        val vpc = createVpc(networkDef)

        NetworkAclGenerator.generate(
            vpc,
            networkDef.naclSpec.peeredSubnets,
            networkDef.subnetGroups,
            networkDef.naclSpec.localNetworks
        )

        setupRouting(vpc, networkDef)
        createFlowLog(vpc)
    }

    private fun buildNetworkDefinition(block: SecureVpcBuilder.() -> Unit): NetworkDefinition {
        val builder = SecureVpcBuilder()
        builder.apply(block)
        return builder.build()
    }

    private fun createFlowLog(vpc: Vpc) {
        vpc.addFlowLog("FlowLog") {
            destination((FlowLogDestination.toCloudWatchLogs()))
            trafficType(FlowLogTrafficType.REJECT)
            maxAggregationInterval(FlowLogMaxAggregationInterval.ONE_MINUTE)
        }
    }

    private fun setupRouting(vpc: Vpc, networkDef: NetworkDefinition) {
        networkDef.subnetGroups.forEach { subnetGroup ->
            val routes = subnetGroup.routes.toMutableList()
            when (subnetGroup.subnetGroupType) {
                SubnetGroupType.Private -> {
                    // no internet route specified for Private subnet;
                    require(
                        subnetGroup.routes.any { it.destination is RouteDestination.Internet }
                    ) {
                        "Internet route required in Private subnet; none provided"
                    }
                }
                else -> {}
            }

            if (routes.isEmpty()) {
                return@forEach
            }

            val subnetResolver = SubnetResolverImpl(vpc)

            val subnets =
                vpc.selectSubnets(SubnetPredicates.groupNamed(subnetGroup.name)).subnets().map {
                    it as Subnet
                }
            subnets.forEach { subnet ->
                val context =
                    RoutableContext(
                        subnetResolver = subnetResolver,
                        availabilityZone = subnet.availabilityZone(),
                        vpc = vpc,
                        subnetScope = subnet,
                        networkDefinition = networkDef
                    )
                routes.forEach { route ->
                    val target = route.routable.routeTarget(context)
                    when {
                        route.destination is RouteDestination.Vpc &&
                            target.routerType == RouterType.VPC_ENDPOINT -> {
                            //
                            // subnet.replaceLocalRouteWithVpcEndpoint(vpc.cidrBlock,
                            // target.routerId)
                        }
                        else -> {
                            subnet.addRoute("${route.destination.name()}Route") {
                                routerId(target.routerId)
                                routerType(target.routerType)
                                val cidr =
                                    when (val dest = route.destination) {
                                        is RouteDestination.Internet -> "0.0.0.0/0"
                                        is RouteDestination.Vpc -> vpc.cidrBlock.toString()
                                        is RouteDestination.Cidr -> dest.cidr.toString()
                                    }
                                destinationCidrBlock(cidr)
                            }
                        }
                    }
                }
            }
        }
    }

    private class SubnetResolverImpl(private val vpc: Vpc) : SubnetResolver {
        override fun resolve(subnetGroupName: String, availabilityZone: String): Subnet {
            return vpc.selectSubnets(SubnetPredicates.groupNamed(subnetGroupName))
                .subnets()
                .map { it as Subnet }
                .firstOrNull { it.availabilityZone() == availabilityZone }
                ?: error("No $subnetGroupName subnet in $availabilityZone")
        }
    }

    private fun createVpc(networkDef: NetworkDefinition): Vpc {
        val vpc =
            Vpc(this, "Vpc") {
                ipAddresses(IpAddresses.cidr(networkDef.cidrBlock.toString()))
                maxAzs(networkDef.maxAzs)
                reservedAzs(networkDef.reservedAzs)
                subnetConfiguration(networkDef.subnetGroups.map { it.toSubnetConfiguration() })

                // disable auto-provisioning of NAT gateways; these will be provisioned if/when
                // needed based
                // on configuration (not all egress flows use a NAT gateway, and not all private
                // subnets egress via NAT gateway)
                natGateways(0)
                enableDnsSupport(true)
                enableDnsHostnames(true)
                defaultInstanceTenancy(DefaultInstanceTenancy.DEFAULT)
                restrictDefaultSecurityGroup(true)
            }

        //    vpc.deleteDefaultNetworkAcls()

        return vpc
    }

    private fun SubnetGroupSpec.toSubnetConfiguration() = SubnetConfiguration {
        name(name)
        cidrMask?.let { cidrMask(it) }
        reserved(reserved)
        subnetType(subnetGroupType.subnetType)
    }
}

public data class NetworkDefinition(
    val maxAzs: Int,
    val reservedAzs: Int,
    val cidrBlock: CidrBlock,
    val naclSpec: NaclSpec,
    val subnetGroups: List<SubnetGroupSpec>
)

public fun SecureVpcBuilder.publicPrivateIsolatedNetwork() {
    publicSubnetGroup { cidrMask(26) }
    privateSubnetGroup {
        routing.internet(NatGatewayRoutable())
        allowCrossAzNaclFlows()
    }
    isolatedSubnetGroup { allowCrossAzNaclFlows() }

    nacl {
        allowBetweenPeeredSubnets("Public", "Private")
        allowBetweenPeeredSubnets("Private", "Isolated")
    }
}

public fun SecureVpcBuilder.publicPrivateIsolatedNetworkWithFirewall(nfw: NetworkFirewallRoutable) {
    publicSubnetGroup {
        cidrMask(26)
        routing.local(nfw)
    }
    privateSubnetGroup("NetworkFirewall") {
        cidrMask(28)
        routing.internet(NatGatewayRoutable())
    }
    privateSubnetGroup { routing.internet(nfw) }
    isolatedSubnetGroup {}

    nacl {
        allowBetweenPeeredSubnets("Public", "NetworkFirewall")
        allowBetweenPeeredSubnets("Private", "NetworkFirewall")
        allowBetweenPeeredSubnets("Private", "Isolated")
    }
}
