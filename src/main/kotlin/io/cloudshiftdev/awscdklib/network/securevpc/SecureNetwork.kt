package io.cloudshiftdev.awscdklib.network.securevpc

import io.cloudshiftdev.awscdk.services.ec2.FlowLogDestination
import io.cloudshiftdev.awscdk.services.ec2.FlowLogMaxAggregationInterval
import io.cloudshiftdev.awscdk.services.ec2.FlowLogTrafficType
import io.cloudshiftdev.awscdk.services.ec2.RouterType
import io.cloudshiftdev.awscdk.services.ec2.Subnet
import io.cloudshiftdev.awscdk.services.ec2.SubnetConfiguration
import io.cloudshiftdev.awscdk.services.ec2.SubnetType
import io.cloudshiftdev.awscdk.services.ec2.Vpc
import io.cloudshiftdev.awscdklib.network.SubnetPredicates
import io.cloudshiftdev.awscdklib.network.cidrBlock
import io.cloudshiftdev.awscdklib.network.routable.NatGatewayRoutable
import io.cloudshiftdev.awscdklib.network.routable.NetworkFirewallRoutable
import io.cloudshiftdev.awscdklib.network.routable.RoutableContext
import io.cloudshiftdev.constructs.Construct

public class SecureNetwork(scope: Construct, id: String, block: (SecureNetworkBuilder).() -> Unit) :
    Construct(scope, id) {

    public val vpc: Vpc

    init {
        val props = secureNetworkProps(block)
        vpc = vpc(props)

        NetworkAclGenerator.generate(
            vpc,
            props.naclSpec.peeredSubnets,
            props.subnetGroups,
            props.naclSpec.localNetworks
        )

        setupRouting(vpc, props)
        createFlowLog(vpc)
    }

    private fun secureNetworkProps(block: SecureNetworkBuilder.() -> Unit): SecureNetworkProps {
        val builder = SecureNetworkBuilderImpl()
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

    private fun setupRouting(vpc: Vpc, networkDef: SecureNetworkProps) {
        networkDef.subnetGroups.forEach { subnetGroup ->
            val routes = subnetGroup.routes.toMutableList()
            when (subnetGroup.subnetType) {
                SubnetType.PRIVATE_WITH_EGRESS -> {
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

    private fun vpc(props:SecureNetworkProps): Vpc {
        val vpc =
            Vpc(this, "Vpc") {
                props.availabilityZones.takeIf { it.isNotEmpty() }?.let(::availabilityZones)
                props.maxAzs.let(::maxAzs)
                props.reservedAzs.let(::reservedAzs)

                props.ipAddresses?.let(::ipAddresses)
                props.ipV6Addresses?.let(::ipv6Addresses)
                props.ipProtocol.let(::ipProtocol)

                props.enableDnsSupport.let(::enableDnsSupport)
                props.enableDnsHostnames.let(::enableDnsHostnames)
                props.defaultInstanceTenancy.let(::defaultInstanceTenancy)

                props.restrictDefaultSecurityGroup.let(::restrictDefaultSecurityGroup)

                props.createInternetGateway.let(::createInternetGateway)

                subnetConfiguration(props.subnetGroups.map { it.toSubnetConfiguration() })

                // TODO: temporary
                natGateways(0)
            }

        return vpc
    }

    private fun SubnetGroupProps.toSubnetConfiguration() = SubnetConfiguration {
        name(name)
        cidrMask?.let { cidrMask(it) }
        reserved(reserved)
        subnetType(subnetType)
    }
}

public fun SecureNetworkBuilder.publicPrivateIsolatedNetwork() {
    subnets {
        publicSubnetGroup { cidrMask(26) }
        privateSubnetGroup {
            routing.internet(NatGatewayRoutable())
            allowCrossAzNaclFlows()
        }
        isolatedSubnetGroup { allowCrossAzNaclFlows() }
    }

    nacl {
        allowBetweenPeeredSubnets("Public", "Private")
        allowBetweenPeeredSubnets("Private", "Isolated")
    }
}

public fun SecureNetworkBuilder.publicPrivateIsolatedNetworkWithFirewall(nfw: NetworkFirewallRoutable) {
    subnets {
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
    }

    nacl {
        allowBetweenPeeredSubnets("Public", "NetworkFirewall")
        allowBetweenPeeredSubnets("Private", "NetworkFirewall")
        allowBetweenPeeredSubnets("Private", "Isolated")
    }
}
