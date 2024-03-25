package io.cloudshiftdev.awscdklib.network.securenetwork

import com.google.common.collect.ListMultimap
import com.google.common.collect.MultimapBuilder
import com.google.common.hash.Hashing
import inet.ipaddr.IPAddress
import inet.ipaddr.IPAddressSegmentSeries
import inet.ipaddr.IPAddressString
import io.cloudshiftdev.awscdk.services.ec2.AclTraffic
import io.cloudshiftdev.awscdk.services.ec2.Action
import io.cloudshiftdev.awscdk.services.ec2.NetworkAcl
import io.cloudshiftdev.awscdk.services.ec2.NetworkAclEntry
import io.cloudshiftdev.awscdk.services.ec2.SubnetType
import io.cloudshiftdev.awscdk.services.ec2.TrafficDirection
import io.cloudshiftdev.awscdk.services.ec2.Vpc
import io.cloudshiftdev.awscdklib.core.addComment
import io.cloudshiftdev.awscdklib.core.tag
import io.cloudshiftdev.awscdklib.network.CidrBlock
import io.cloudshiftdev.awscdklib.network.SubnetPredicates
import io.cloudshiftdev.awscdklib.network.cidrBlock
import java.nio.charset.StandardCharsets
import net.pearx.kasechange.splitToWords
import net.pearx.kasechange.toPascalCase

internal data class NetworkAclFlow(
    val peerName: String,
    val cidrBlocks: List<CidrBlock>,
    val ruleAction: Action,
    val traffic: AclTraffic,
    val type: String
)

internal data class NaclSpec(
    val peeredSubnets: List<NaclPeering>,
    val localNetworks: List<CidrBlock>
)

internal data class NaclPeering(val subnet: String, val peeredSubnet: String)

internal object NetworkAclGenerator {

    fun generate(
        vpc: Vpc,
        peeredSubnets: List<NaclPeering>,
        subnetSpecs: List<SubnetGroupProps>,
        localNetworks: List<CidrBlock>
    ) {
        val peeredSubnetMap = generateSubnetMap(peeredSubnets, subnetSpecs)

        peeredSubnetMap.asMap().forEach { entry ->
            val subnet = entry.key
            val flows = generateFlows(subnet, entry.value, vpc, localNetworks)

            // create a hash of flows such that any changes (new subnet, etc) will force a new NACL
            // to be provisioned
            // which will flip over the existing one
            // this is done as changing nacl entries is problematic due to rule number clashes,
            // entry limits, and the need
            // to keep traffic flowing (to the extent possible) during updates
            val hasher = Hashing.murmur3_128().newHasher()
            flows.forEach { flow ->
                hasher.putString(subnet.name, StandardCharsets.UTF_8)
                hasher.putString(flow.peerName, StandardCharsets.UTF_8)
                hasher.putString(flow.cidrBlocks.toString(), StandardCharsets.UTF_8)
                hasher.putString(flow.ruleAction.toString(), StandardCharsets.UTF_8)
            }
            val flowHash = hasher.hash()

            val ruleCount = flows.fold(1) { r, it -> r + it.cidrBlocks.size }
            check(ruleCount <= 20) { "Network ACL rule count must be <= 20" }

            val nacl =
                NetworkAcl(vpc, "${subnet.name}Subnets$flowHash") {
                    vpc(vpc)
                    subnetSelection(SubnetPredicates.groupNamed(subnet.name))
                }
            nacl.tag("Name", nacl.node().path())

            createNetworkAclEntries(nacl, subnet.name, flows)
        }
    }

    private fun generateSubnetMap(
        peeredSubnets: List<NaclPeering>,
        subnetSpecs: List<SubnetGroupProps>
    ): ListMultimap<SubnetGroupProps, SubnetGroupProps> {
        val peeredSubnetMap =
            MultimapBuilder.treeKeys<SubnetGroupProps>(compareBy { it.name })
                .arrayListValues()
                .build<SubnetGroupProps, SubnetGroupProps>()

        // create structure (example below) with unique set of subnets to process and their peers
        // Public -> Private
        // Private -> Public, Isolated
        // Isolated -> Private
        peeredSubnets.forEach { peering ->
            val subnet =
                subnetSpecs.firstOrNull { it.name == peering.subnet }
                    ?: error("Subnet group not found: ${peering.subnet}")
            val peeredSubnet =
                subnetSpecs.firstOrNull { it.name == peering.peeredSubnet }
                    ?: error("Subnet group not found: ${peering.peeredSubnet}")
            if (subnet.reserved || peeredSubnet.reserved) {
                return@forEach
            }
            peeredSubnetMap.put(subnet, peeredSubnet)
            peeredSubnetMap.put(peeredSubnet, subnet)
        }
        return peeredSubnetMap
    }

    private fun generateFlows(
        subnetGroup: SubnetGroupProps,
        peeredSubnetGroups: Collection<SubnetGroupProps>,
        vpc: Vpc,
        localNetworkCidrBlocks: List<CidrBlock>
    ): MutableList<NetworkAclFlow> {
        // order of flows is important as they reflect the order that nacl entries are processed in
        val flows = mutableListOf<NetworkAclFlow>()

        val peeredSubnetGroupList = peeredSubnetGroups.toMutableList()

        // cross-az flows for this subnet (subnet peering with itself)
        if (subnetGroup.allowCrossAzNaclFlows) {
            peeredSubnetGroupList.add(subnetGroup)
        }

        // collapse all inter-and-intra subnet traffic to a single flow with a list of all CIDR
        // blocks, such that we
        // can attempt to collapse sequential CIDR blocks to reduce the number of rules / improve
        // efficiency
        flows.add(
            NetworkAclFlow(
                peerName = peeredSubnetGroupList.joinToString(separator = " and ") { it.name },
                type = "Subnets",
                cidrBlocks =
                    collapseCidrs(
                        peeredSubnetGroupList.flatMap { peer ->
                            vpc.selectSubnets(SubnetPredicates.groupNamed(peer.name))
                                .subnets()
                                .map { it.cidrBlock }
                        }
                    ),
                ruleAction = Action.ALLOW,
                traffic = AclTraffic.allTraffic()
            )
        )

        // allow internet flows for PUBLIC, PRIVATE subnets
        when (subnetGroup.subnetType) {
            SubnetType.PRIVATE_WITH_EGRESS,
            SubnetType.PUBLIC -> {
                // deny traffic to other local networks
                // only necessary when followed by allow as there's a default DENY ALL for nacls as
                // the last rule
                flows.add(
                    NetworkAclFlow(
                        peerName = "Local",
                        type = "Network",
                        cidrBlocks = collapseCidrs(localNetworkCidrBlocks, true),
                        ruleAction = Action.DENY,
                        traffic = AclTraffic.allTraffic()
                    )
                )

                // allow traffic to internet
                flows.add(
                    NetworkAclFlow(
                        peerName = "Internet",
                        type = "",
                        cidrBlocks = listOf(CidrBlock.allIPv4()),
                        ruleAction = Action.ALLOW,
                        traffic = AclTraffic.allTraffic()
                    )
                )
            }
            else -> {}
        }
        return flows
    }

    private fun collapseCidrs(
        cidrBlocks: List<CidrBlock>,
        sortByPrefixList: Boolean = false
    ): List<CidrBlock> {
        if (cidrBlocks.size <= 1) {
            // nothing to merge!
            return cidrBlocks
        }

        val blocks =
            cidrBlocks
                .map(CidrBlock::toString)
                .map(::IPAddressString)
                .map(IPAddressString::getAddress)

        val first = blocks.first()
        val rest = blocks.drop(1)

        val comparator =
            when (sortByPrefixList) {
                true -> IPAddressSegmentSeries.getPrefixLenComparator().reversed()
                else -> naturalOrder()
            }
        return first
            .mergeToPrefixBlocks(*rest.toTypedArray())
            .toList()
            .sortedWith(comparator)
            .map(IPAddress::toString)
            .map { CidrBlock.of(it) }
    }

    private fun createNetworkAclEntries(
        nacl: NetworkAcl,
        subnet: String,
        flows: List<NetworkAclFlow>
    ) {
        var ruleBase = 1000
        flows.forEach { flow ->
            // convert to AclCidr for subsequent ops
            val cidrBlocks = flow.cidrBlocks.map { it.toAclCidr() }

            setOf(TrafficDirection.INGRESS, TrafficDirection.EGRESS).forEach { direction ->
                var ruleNumber = ruleBase
                cidrBlocks.forEach { cidrBlock ->
                    val id =
                        "${flow.ruleAction.actionText()}${direction.directionText()}${flow.peerName.toPascalCase()}${flow.type}Rule$ruleNumber"
                    val entry =
                        NetworkAclEntry(nacl, id) {
                            networkAcl(nacl)
                            traffic(flow.traffic)
                            direction(direction)
                            cidr(cidrBlock)
                            ruleAction(flow.ruleAction)
                            ruleNumber(ruleNumber)
                        }
                    entry.addComment("$subnet Subnets " + id.splitToWords().joinToString(" "))

                    // increment each rule by 5 to allow room between, in case of emergencies
                    ruleNumber += 5
                }
            }

            // increment each flow by 200
            ruleBase += 200
        }
    }

    private fun Action.actionText(): String {
        return toString().toPascalCase()
    }

    private fun TrafficDirection.directionText(): String {
        return when (this) {
            TrafficDirection.EGRESS -> "OutboundTo"
            TrafficDirection.INGRESS -> "InboundFrom"
        }
    }
}
