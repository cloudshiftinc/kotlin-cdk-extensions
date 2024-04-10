package io.cloudshiftdev.awscdklib.network.securenetwork

import io.cloudshiftdev.awscdk.services.ec2.FlowLogDestination
import io.cloudshiftdev.awscdk.services.ec2.FlowLogMaxAggregationInterval
import io.cloudshiftdev.awscdk.services.ec2.FlowLogTrafficType
import io.cloudshiftdev.awscdk.services.ec2.LogFormat
import io.cloudshiftdev.awscdk.services.ec2.SubnetConfiguration
import io.cloudshiftdev.awscdk.services.ec2.Vpc
import io.cloudshiftdev.awscdklib.network.SubnetPredicates
import io.cloudshiftdev.constructs.Construct

public class SecureNetwork(scope: Construct, id: String, block: (SecureNetworkBuilder).() -> Unit) :
    Construct(scope, id) {

    public val vpc: Vpc

    init {
        val props = secureNetworkProps(block)
        vpc = vpc(props)

        NetworkAclGenerator.generate(vpc, props.naclSpec, props.subnetGroups)

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
            logFormat(
                LogFormat.VERSION,
                LogFormat.ACCOUNT_ID,
                LogFormat.INTERFACE_ID,
                LogFormat.SRC_ADDR,
                LogFormat.DST_ADDR,
                LogFormat.SRC_PORT,
                LogFormat.DST_PORT,
                LogFormat.PROTOCOL,
                LogFormat.PACKETS,
                LogFormat.BYTES,
                LogFormat.START_TIMESTAMP,
                LogFormat.END_TIMESTAMP,
                LogFormat.ACTION,
                LogFormat.LOG_STATUS,
                LogFormat.VPC_ID,
                LogFormat.SUBNET_ID,
                LogFormat.INSTANCE_ID,
                LogFormat.TCP_FLAGS,
                LogFormat.TRAFFIC_TYPE,
                LogFormat.PKT_SRC_ADDR,
                LogFormat.PKT_DST_ADDR,
                LogFormat.REGION,
                LogFormat.AZ_ID,
                LogFormat.SUBLOCATION_TYPE,
                LogFormat.SUBLOCATION_ID,
                LogFormat.PKT_SRC_AWS_SERVICE,
                LogFormat.PKT_DST_AWS_SERVICE,
                LogFormat.FLOW_DIRECTION,
                LogFormat.TRAFFIC_PATH
            )
        }
    }

    private fun vpc(props: SecureNetworkProps): Vpc {
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

                props.routerProviders.filterIsInstance<NatGatewayConfigurer>().forEach {
                    it.configure(this)
                }
            }

        props.routerProviders.forEach { it.create(vpc) }
        return vpc
    }

    private fun SubnetGroupProps.toSubnetConfiguration() = SubnetConfiguration {
        name(name.value)
        cidrMask?.let { cidrMask(it) }
        reserved(reserved)
        subnetType(subnetType)
    }
}

public fun SecureNetworkBuilder.publicPrivateIsolatedNetwork() {
    subnets {
        publicSubnetGroup { cidrMask(26) }
        privateSubnetGroup { allowCrossAzNaclFlows() }
        isolatedSubnetGroup { allowCrossAzNaclFlows() }
    }

    nacl {
        allowBetween("Public", "Private")
        allowBetween("Private", "Isolated")
    }
}

public fun SecureNetworkBuilder.publicPrivateIsolatedNetworkWithFirewall() {
    routers {
        egressNetworkFirewall {
            routerSubnet(SubnetPredicates.groupNamed("NetworkFirewall"))
            routableSubnets(listOf(SubnetPredicates.groupNamed("Private")))
            egressSubnets(listOf(SubnetPredicates.groupNamed("Public")))
        }
    }

    subnets {
        publicSubnetGroup { cidrMask(26) }
        privateSubnetGroup("NetworkFirewall") { cidrMask(28) }
        isolatedSubnetGroup("Private")
        isolatedSubnetGroup()
    }

    nacl {
        allowBetween("Public", "NetworkFirewall")
        allowBetween("Private", "NetworkFirewall")
        allowBetween("Private", "Isolated")
        egressSubnets("Private")
    }
}
