package io.cloudshiftdev.awscdklib.network.firewall

import io.cloudshiftdev.awscdk.services.ec2.Vpc
import io.cloudshiftdev.awscdk.services.logs.LogGroup
import io.cloudshiftdev.awscdk.services.networkfirewall.CfnFirewall
import io.cloudshiftdev.awscdk.services.networkfirewall.CfnFirewallPolicy
import io.cloudshiftdev.awscdk.services.networkfirewall.CfnLoggingConfiguration
import io.cloudshiftdev.awscdklib.network.SubnetPredicates
import io.cloudshiftdev.constructs.Construct
import net.pearx.kasechange.toKebabCase

public class NetworkFirewall(
    scope: Construct,
    id: String,
    block: (NetworkFirewallPropsBuilder).() -> Unit
) : Construct(scope, id) {
    internal val azEndpointMap = mutableMapOf<String, String>()

    init {
        val props = NetworkFirewallPropsBuilder().apply(block).build()

        val networkFirewallPolicy =
            CfnFirewallPolicy(this, "FirewallPolicy") {
                firewallPolicyName(
                    this@NetworkFirewall.node().path().toKebabCase().replace("/", "-")
                )
                firewallPolicy(
                    CfnFirewallPolicy.FirewallPolicyProperty {
                        statelessDefaultActions(listOf("aws:pass"))
                        statelessFragmentDefaultActions(listOf("aws:pass"))
                        //                    statefulDefaultActions(listOf("aws:pass"))
                    }
                )
            }

        val networkFirewall = createFirewall(props, networkFirewallPolicy)

        val flowLogGroup =
            LogGroup(this, "${id}FlowLogs") {
                logGroupName("${this@NetworkFirewall.node().path()}/flow-logs")
            }

        val alertLogGroup =
            LogGroup(this, "${id}AlertLogs") {
                logGroupName("${this@NetworkFirewall.node().path()}/alert-logs")
            }

        CfnLoggingConfiguration(networkFirewall, "Logging") {
            firewallArn(networkFirewall.attrFirewallArn())
            loggingConfiguration(
                CfnLoggingConfiguration.LoggingConfigurationProperty {
                    logDestinationConfigs(
                        listOf(
                            CfnLoggingConfiguration.LogDestinationConfigProperty {
                                logDestination(mapOf("logGroup" to flowLogGroup.logGroupName()))
                                logDestinationType("CloudWatchLogs")
                                logType("FLOW")
                            },
                            CfnLoggingConfiguration.LogDestinationConfigProperty {
                                logDestination(mapOf("logGroup" to alertLogGroup.logGroupName()))
                                logDestinationType("CloudWatchLogs")
                                logType("ALERT")
                            }
                        )
                    )
                }
            )
        }
    }

    private fun createFirewall(
        props: NetworkFirewallProps,
        networkFirewallPolicy: CfnFirewallPolicy
    ): CfnFirewall {
        val vpc = props.vpc
        val nfSubnets =
            vpc.selectSubnets(SubnetPredicates.groupNamed(props.subnetPlacement)).subnets()

        val networkFirewall =
            CfnFirewall(this, "NFW") {
                firewallName(this@NetworkFirewall.node().path().toKebabCase().replace("/", "-"))
                vpcId(vpc.vpcId())
                subnetMappings(
                    nfSubnets.map { CfnFirewall.SubnetMappingProperty { subnetId(it.subnetId()) } }
                )
                firewallPolicyArn(networkFirewallPolicy.attrFirewallPolicyArn())
            }

        // determine the set of AZs that the NFW was deployed to
        val availabilityZones = nfSubnets.map { it.availabilityZone() }.toList()

        // determine the NFW endpoint ID in each deployed AZ
        //        azEndpointMap.putAll(mapAzToVpcEndpoint(networkFirewall, vpc, availabilityZones))

        return networkFirewall
    }

    //    private fun mapAzToVpcEndpoint(
    //        nfw: CfnFirewall,
    //        vpc: Vpc,
    //        availabilityZones: Iterable<String>
    //    ): Map<String, String> {
    //        val cr = DispatchingCustomResource.create(nfw, "LookupNetworkFirewallVpcEndpoints") {
    //            codeSource(FunctionCodeSource.fromResource("lambda-custom-resource-functions"))
    //            customResourceId("LookupNetworkFirewallVpcEndpoints")
    //            properties("VpcId" to vpc.vpcId(), "FirewallArn" to nfw.attrFirewallArn())
    //            policyStatement {
    //                allow()
    //                action("network-firewall:DescribeFirewall")
    //                anyResource()
    //            }
    //        }
    //        cr.node().addDependency(nfw)
    //        return availabilityZones.associateWith {
    //            cr.attString(it)
    //        }
    //    }
}

public class NetworkFirewallPropsBuilder internal constructor() {
    private var vpc: Vpc? = null
    private var subnetPlacement: String? = null

    public fun vpc(vpc: Vpc) {
        this.vpc = vpc
    }

    public fun subnetPlacement(value: String) {
        this.subnetPlacement = value
    }

    internal fun build(): NetworkFirewallProps {
        return NetworkFirewallProps(
            vpc = requireNotNull(vpc) { "vpc is required" },
            subnetPlacement = requireNotNull(subnetPlacement) { "subnetPlacement is required" }
        )
    }
}

internal data class NetworkFirewallProps(val vpc: Vpc, val subnetPlacement: String)
