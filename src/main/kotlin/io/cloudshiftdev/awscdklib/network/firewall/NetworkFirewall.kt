package io.cloudshiftdev.awscdklib.network.firewall

import io.cloudshiftdev.awscdk.customresources.AwsCustomResource
import io.cloudshiftdev.awscdk.customresources.AwsCustomResourcePolicy
import io.cloudshiftdev.awscdk.customresources.AwsSdkCall
import io.cloudshiftdev.awscdk.customresources.PhysicalResourceId
import io.cloudshiftdev.awscdk.services.ec2.ISubnet
import io.cloudshiftdev.awscdk.services.ec2.SubnetSelection
import io.cloudshiftdev.awscdk.services.ec2.Vpc
import io.cloudshiftdev.awscdk.services.networkfirewall.CfnFirewall
import io.cloudshiftdev.awscdk.services.networkfirewall.CfnFirewallPolicy
import io.cloudshiftdev.awscdklib.core.address
import io.cloudshiftdev.constructs.Construct

public enum class StatelessStandardAction(internal val value: String) {
    FORWARD("aws:forward_to_sfe"),
    PASS("aws:pass"),
    DROP("aws:drop"),
}

public enum class StatefulStandardAction(internal val value: String) {
    ALERT("ALERT"),
    PASS("PASS"),
    DROP("DROP"),
}

public enum class StatefulStrictAction(internal val value: String) {
    DROP_STRICT("aws:drop_strict"),
    DROP_ESTABLISHED("aws:drop_established"),
    ALERT_STRICT("aws:alert_strict"),
    ALERT_ESTABLISHED("aws:alert_established"),
}

public class FirewallPolicy(
    scope: Construct,
    id: String,
    firewallPolicyName: String? = null,
    description: String? = null,
    statelessDefaultActions: List<StatelessStandardAction> = emptyList(),
    statelessFragmentDefaultActions: List<StatelessStandardAction> = emptyList(),
    statefulDefaultActions: List<StatefulStandardAction> = emptyList(),
) : Construct(scope, id) {
    public val firewallPolicyId: String
    public val firewallPolicyArn: String

    init {
        val policy =
            CfnFirewallPolicy(this, "CfnFirewallPolicy") {
                firewallPolicyName("${firewallPolicyName ?: id}$address")
                description?.let(::description)
                firewallPolicy(
                    CfnFirewallPolicy.FirewallPolicyProperty {
                        statelessDefaultActions(statelessDefaultActions.map { it.value })
                        statelessFragmentDefaultActions(
                            statelessFragmentDefaultActions.map { it.value }
                        )
                        statefulDefaultActions(statefulDefaultActions.map { it.value })
                    }
                )
            }

        firewallPolicyId = policy.attrFirewallPolicyId()
        firewallPolicyArn = policy.attrFirewallPolicyArn()
    }
}

public class NetworkFirewall(
    scope: Construct,
    id: String,
    vpc: Vpc,
    subnetMappings: SubnetSelection,
    firewallName: String,
    firewallPolicy: FirewallPolicy
) : Construct(scope, id) {
    public val endpointIds: List<String>
    public val firewallArn: String
    public val firewallId: String
    public val availabilityZoneEndpointMap: Map<String, String>

    init {
        val placementSubnets = vpc.selectSubnets(subnetMappings).subnets()

        val nfw =
            CfnFirewall(vpc, "Firewall") {
                firewallName("${firewallName}$address")
                vpcId(vpc.vpcId())
                subnetMappings(placementSubnets.toSubnetMappings())
                firewallPolicyArn(firewallPolicy.firewallPolicyArn)
            }

        // WTF AWS:
        // https://github.com/aws-cloudformation/aws-cloudformation-resource-providers-networkfirewall/issues/15

        val describeFirewall = AwsSdkCall {
            service("networkfirewall")
            action("DescribeFirewall")
            parameters(mapOf("FirewallArn" to nfw.attrFirewallArn()))
            outputPaths(vpc.availabilityZones().map(::vpcEndpointIdResponsePath))
            physicalResourceId(PhysicalResourceId.of(nfw.attrFirewallArn()))
        }
        val lookupNfwEndpoints =
            AwsCustomResource(scope, "FirewallCustomResource") {
                onCreate(describeFirewall)
                onUpdate(describeFirewall)
                installLatestAwsSdk(false)
                policy(AwsCustomResourcePolicy.fromSdkCalls { resources(nfw.attrFirewallArn()) })
            }

        lookupNfwEndpoints.node().addDependency(nfw)

        val azs = placementSubnets.map { it.availabilityZone() }
        availabilityZoneEndpointMap =
            azs.associateWith {
                lookupNfwEndpoints.responseField(vpcEndpointIdResponsePath(it))
            }

        endpointIds = nfw.attrEndpointIds()
        firewallArn = nfw.attrFirewallArn()
        firewallId = nfw.attrFirewallId()
    }

    private fun vpcEndpointIdResponsePath(az: String): String {
        return "FirewallStatus.SyncStates.${az}.Attachment.EndpointId"
    }

    private fun List<ISubnet>.toSubnetMappings(): List<CfnFirewall.SubnetMappingProperty> {
        return map { CfnFirewall.SubnetMappingProperty { subnetId(it.subnetId()) } }
    }
}
