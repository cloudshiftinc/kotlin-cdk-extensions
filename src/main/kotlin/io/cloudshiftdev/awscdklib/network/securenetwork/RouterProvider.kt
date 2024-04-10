package io.cloudshiftdev.awscdklib.network.securenetwork

import io.cloudshiftdev.awscdk.customresources.AwsCustomResource
import io.cloudshiftdev.awscdk.customresources.AwsCustomResourcePolicy
import io.cloudshiftdev.awscdk.customresources.PhysicalResourceId
import io.cloudshiftdev.awscdk.services.ec2.NatProvider
import io.cloudshiftdev.awscdk.services.ec2.RouterType
import io.cloudshiftdev.awscdk.services.ec2.Subnet
import io.cloudshiftdev.awscdk.services.ec2.SubnetSelection
import io.cloudshiftdev.awscdk.services.ec2.Vpc
import io.cloudshiftdev.awscdk.services.ec2.VpcProps
import io.cloudshiftdev.awscdklib.core.resourceArn
import io.cloudshiftdev.awscdklib.network.CidrBlock
import io.cloudshiftdev.awscdklib.network.firewall.FirewallPolicy
import io.cloudshiftdev.awscdklib.network.firewall.NetworkFirewall
import io.cloudshiftdev.awscdklib.network.firewall.StatelessStandardAction

public interface RouterProvider {
    public fun create(vpc: Vpc)
}

internal interface NatGatewayConfigurer {
    fun configure(props: VpcProps.Builder)
}

internal abstract class BaseRouterProvider(protected val routerSubnet: SubnetSelection) :
    RouterProvider

internal class NatRouterProvider(
    routerSubnet: SubnetSelection,
    private val natGatewayCount: Int,
    private val natProvider: NatProvider
) : BaseRouterProvider(routerSubnet), NatGatewayConfigurer {

    override fun configure(props: VpcProps.Builder) {
        props.apply {
            natGateways(natGatewayCount)
            natGatewaySubnets(routerSubnet)
            natGatewayProvider(natProvider)
        }
    }

    override fun create(vpc: Vpc) {
        // nothing to do, we use the vpc-provided NAT gateway creation
    }
}

internal class EgressNetworkFirewallRouterProvider(
    routerSubnet: SubnetSelection,
    private val egressSubnets: List<SubnetSelection>,
    private val ingressSubnets: List<SubnetSelection>,
) : BaseRouterProvider(routerSubnet) {
    override fun create(vpc: Vpc) {

        val firewallPolicy =
            FirewallPolicy(
                scope = vpc,
                id = "FirewallPolicy",
                // TODO: make these configurable
                statelessDefaultActions = listOf(StatelessStandardAction.PASS),
                statelessFragmentDefaultActions = listOf(StatelessStandardAction.PASS)
            )

        val firewall =
            NetworkFirewall(
                scope = vpc,
                id = "NetworkFirewall",
                vpc = vpc,
                subnetMappings = routerSubnet,
                firewallName = "Firewall",
                firewallPolicy = firewallPolicy
            )

        val protectedSubnets =
            egressSubnets.flatMap { vpc.selectSubnets(it).subnets() }.map { it as Subnet }
        val ingress =
            ingressSubnets.flatMap { vpc.selectSubnets(it).subnets() }.map { it as Subnet }

        data class AzRouteInfo(
            val az: String,
            val endpointId: String,
            val protectedSubnets: List<Subnet>,
            val ingressSubnets: List<Subnet>
        )

        val azMap = firewall.availabilityZoneEndpointMap

        val azRouteInfo =
            azMap.entries.map { azEntry ->
                AzRouteInfo(
                    az = azEntry.key,
                    endpointId = azEntry.value,
                    protectedSubnets =
                        protectedSubnets.filter { it.availabilityZone() == azEntry.key },
                    ingressSubnets = ingress.filter { it.availabilityZone() == azEntry.key }
                )
            }
        azRouteInfo.forEach { routeInfo ->
            routeInfo.protectedSubnets.forEach { protectedSubnet ->

                // forward flow from protected subnet to firewall
                protectedSubnet.addRoute("NetworkFirewallEgressRoute") {
                    routerId(routeInfo.endpointId)
                    routerType(RouterType.VPC_ENDPOINT)
                    enablesInternetConnectivity(true)
                    destinationCidrBlock(CidrBlock.allIPv4().toString())
                }
            }

            // reverse flow back from NAT gateway to firewall
            routeInfo.ingressSubnets.forEach { ingressSubnet ->
                val routeTableId = ingressSubnet.routeTable().routeTableId()

                AwsCustomResource(ingressSubnet, "ReplaceLocalRouteNetworkFirewall") {
                    onCreate {
                        service("ec2")
                        action("ReplaceRoute")
                        parameters(
                            mapOf(
                                "DestinationCidrBlock" to vpc.vpcCidrBlock(),
                                "RouteTableId" to routeTableId,
                                "VpcEndpointId" to routeInfo.endpointId
                            )
                        )
                        physicalResourceId(PhysicalResourceId.of(routeTableId))
                    }
                    onUpdate {
                        service("ec2")
                        action("ReplaceRoute")
                        parameters(
                            mapOf(
                                "DestinationCidrBlock" to vpc.vpcCidrBlock(),
                                "RouteTableId" to routeTableId,
                                "VpcEndpointId" to routeInfo.endpointId
                            )
                        )
                        physicalResourceId(PhysicalResourceId.of(routeTableId))
                    }
                    onDelete {
                        service("ec2")
                        action("ReplaceRoute")
                        parameters(
                            mapOf(
                                "DestinationCidrBlock" to vpc.vpcCidrBlock(),
                                "RouteTableId" to routeTableId,
                                "LocalTarget" to true
                            )
                        )
                        physicalResourceId(PhysicalResourceId.of(routeTableId))
                    }
                    installLatestAwsSdk(false)
                    policy(
                        AwsCustomResourcePolicy.fromSdkCalls {
                            resources(
                                resourceArn(firewall) {
                                    service("ec2")
                                    resource("route-table")
                                    resourceName(routeTableId)
                                }
                            )
                        }
                    )
                }
            }
        }
    }
}
