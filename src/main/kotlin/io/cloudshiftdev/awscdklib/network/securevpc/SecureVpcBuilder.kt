package io.cloudshiftdev.awscdklib.network.securevpc

import io.cloudshiftdev.awscdklib.network.CidrBlock
import io.cloudshiftdev.awscdklib.network.SubnetGroupType
import io.cloudshiftdev.awscdklib.network.routable.Routable
import net.pearx.kasechange.toPascalCase

@DslMarker public annotation class NetworkDslMarker

@NetworkDslMarker
public class SecureVpcBuilder {
    private var maxAzs: Int = 2
    private var reservedAzs: Int = 0
    private var cidrBlock: CidrBlock? = null

    private val naclBuilder = NaclBuilder()
    private val subnetMap = mutableMapOf<String, SubnetGroupBuilder>()

    // TODO: flow log customizer
    // TODO: vpc customizer

    internal fun build(): NetworkDefinition {
        return NetworkDefinition(
            maxAzs = this.maxAzs,
            reservedAzs = this.reservedAzs,
            cidrBlock = this.cidrBlock ?: error("cidrBlock is required"),
            naclSpec = naclBuilder.build(),
            subnetGroups = subnetMap.values.map { it.build() }
        )
    }

    public fun maxAzs(azs: Int) {
        require(maxAzs >= 1) { "Expected maxAzs to be >= 1" }
        maxAzs = azs
    }

    public fun reservedAzs(azs: Int) {
        require(reservedAzs >= 0) { "Expected reservedAzs to be >= 0" }
        reservedAzs = azs
    }

    public fun cidrBlock(cidrBlock: String) {
        require(this.cidrBlock == null) { "Cannot set cidr block more than once" }
        this.cidrBlock = CidrBlock.of(cidrBlock)
    }

    public fun publicSubnetGroup(
        name: String = "Public",
        block: (SubnetGroupBuilder).() -> Unit = {}
    ) {
        subnetGroup(name, SubnetGroupType.Public, block)
    }

    public fun privateSubnetGroup(
        name: String = "Private",
        block: (SubnetGroupBuilder).() -> Unit = {}
    ) {
        subnetGroup(name, SubnetGroupType.Private, block)
    }

    public fun isolatedSubnetGroup(
        name: String = "Isolated",
        block: (SubnetGroupBuilder).() -> Unit = {}
    ) {
        subnetGroup(name, SubnetGroupType.Isolated, block)
    }

    public fun nacl(block: (NaclBuilder).() -> Unit) {
        naclBuilder.apply(block)
    }

    private fun subnetGroup(
        name: String,
        type: SubnetGroupType,
        block: (SubnetGroupBuilder).() -> Unit = {}
    ) {
        validateSubnetGroupName(name)
        val builder = subnetMap.computeIfAbsent(name) { SubnetGroupBuilder(name, type) }
        builder.apply(block)
    }
}

@NetworkDslMarker
public class NaclBuilder {
    private val peeredSubnets = mutableListOf<NaclPeering>()
    private val localNetworks = mutableListOf<CidrBlock>()

    public fun allowBetweenPeeredSubnets(
        subnet: String,
        peeredSubnet: String,
        block: (NaclPeeringBuilder).() -> Unit = {}
    ) {
        validateSubnetGroupName(subnet)
        validateSubnetGroupName(peeredSubnet)
        val builder = NaclPeeringBuilder(subnet, peeredSubnet)
        builder.apply(block)
        val peering = builder.build()
        val noDuplicatePeering =
            peeredSubnets.none {
                (it.subnet == peering.subnet && it.peeredSubnet == peering.peeredSubnet) ||
                    (it.subnet == peering.peeredSubnet && it.peeredSubnet == peering.subnet)
            }
        require(noDuplicatePeering) { "Duplicate peering: $peering" }
        peeredSubnets.add(peering)
    }

    public fun denyToLocalNetwork(cidr: String) {
        localNetworks.add(CidrBlock.of(cidr))
    }

    public fun denyToAllPrivateNetworks() {
        localNetworks.addAll(CidrBlock.privateNetworks())
    }

    internal fun build(): NaclSpec {
        return NaclSpec(peeredSubnets, localNetworks.ifEmpty { CidrBlock.privateNetworks() })
    }
}

internal fun validateSubnetGroupName(name: String) {
    require(name == name.toPascalCase()) {
        "Subnet group name must match convention; expected '${name.toPascalCase()}', got '$name'"
    }
}

@NetworkDslMarker
public class NaclPeeringBuilder(private val subnet: String, private val peeredSubnet: String) {
    internal fun build(): NaclPeering {
        return NaclPeering(subnet, peeredSubnet)
    }
}

@NetworkDslMarker
public class SubnetGroupBuilder
internal constructor(private val name: String, private val type: SubnetGroupType) {
    private var reserved = false
    private var cidrMask: Int? = null
    private var allowCrossAzNaclFlows: Boolean = false

    private val routeBuilder = RouteBuilder()

    public fun cidrMask(value: Int) {
        require(value in 16..28) {
            "Subnet CIDR mask must be between 16 and 28 (inclusive); got $value"
        }
        cidrMask = value
    }

    public fun reserved() {
        reserved = true
    }

    public val routing: RouteBuilder
        get() = routeBuilder

    public fun routing(block: (RouteBuilder).() -> Unit) {
        routeBuilder.apply(block)
    }

    public fun allowCrossAzNaclFlows() {
        this.allowCrossAzNaclFlows = true
    }

    internal fun build(): SubnetGroupSpec {
        val routes = routeBuilder.routes
        return SubnetGroupSpec(
            name = name,
            subnetGroupType = type,
            cidrMask = cidrMask,
            reserved = reserved,
            routes = routes,
            allowCrossAzNaclFlows = allowCrossAzNaclFlows
        )
    }
}

@NetworkDslMarker
public class RouteBuilder {
    internal val routes = mutableListOf<SubnetRouteSpec>()

    public fun internet(provider: Routable) {
        routes.add(SubnetRouteSpec(RouteDestination.Internet, provider))
    }

    public fun local(provider: Routable) {
        routes.add(SubnetRouteSpec(RouteDestination.Vpc, provider))
    }
}
