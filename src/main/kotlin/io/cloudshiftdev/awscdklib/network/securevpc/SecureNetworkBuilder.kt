package io.cloudshiftdev.awscdklib.network.securevpc

import io.cloudshiftdev.awscdk.services.ec2.DefaultInstanceTenancy
import io.cloudshiftdev.awscdk.services.ec2.IIpAddresses
import io.cloudshiftdev.awscdk.services.ec2.IIpv6Addresses
import io.cloudshiftdev.awscdk.services.ec2.IpAddresses
import io.cloudshiftdev.awscdk.services.ec2.IpProtocol
import io.cloudshiftdev.awscdk.services.ec2.SubnetType
import io.cloudshiftdev.awscdklib.network.CidrBlock
import io.cloudshiftdev.awscdklib.network.routable.Routable
import net.pearx.kasechange.toPascalCase

@DslMarker
public annotation class NetworkDslMarker

@NetworkDslMarker
public interface SecureNetworkBuilder {
    public fun availabilityZones(azs: List<String>)
    public fun availabilityZones(vararg azs: String) {
        availabilityZones(azs.toList())
    }

    public fun cidrBlock(cidrBlock: String) {
        ipAddresses(IpAddresses.cidr(cidrBlock))
    }

    public fun createInternetGateway(value: Boolean)
    public fun defaultInstanceTenancy(value: DefaultInstanceTenancy)
    public fun enableDnsHostnames(value: Boolean)
    public fun enableDnsSupport(value: Boolean)

    public fun ipAddresses(ipAddresses: IIpAddresses)
    public fun ipV6Addresses(ipAddresses: IIpv6Addresses)

    public fun ipProtocol(protocol: IpProtocol)
    public fun maxAzs(azs: Int)
    public fun reservedAzs(azs: Int)
    public fun restrictDefaultSecurityGroup(value: Boolean)

    public fun nacl(block: (NaclBuilder).() -> Unit)

    public fun subnets(block: (SubnetsBuilder).() -> Unit)
}


internal class SecureNetworkBuilderImpl : SecureNetworkBuilder {
    var maxAzs = 2
    var reservedAzs = 0
    var ipAddresses: IIpAddresses? = null
    var ipV6Addresses: IIpv6Addresses? = null
    var enableDnsHostnames = true
    var enableDnsSupport = true
    var restrictDefaultSecurityGroup = true
    var ipProtocol = IpProtocol.IPV4_ONLY
    var defaultInstanceTenancy = DefaultInstanceTenancy.DEFAULT
    val availabilityZones = mutableListOf<String>()
    var createInternetGateway = true
    private val naclBuilder = NaclBuilder()
    private val subnetsBuilder = SubnetsBuilderImpl()


    internal fun build(): SecureNetworkProps {
        return SecureNetworkProps(
            maxAzs = this.maxAzs,
            reservedAzs = this.reservedAzs,
            availabilityZones = this.availabilityZones,
            ipAddresses = this.ipAddresses,
            ipV6Addresses = this.ipV6Addresses,
            ipProtocol = this.ipProtocol,
            enableDnsSupport = this.enableDnsSupport,
            enableDnsHostnames = this.enableDnsHostnames,
            restrictDefaultSecurityGroup = this.restrictDefaultSecurityGroup,
            defaultInstanceTenancy = this.defaultInstanceTenancy,
            createInternetGateway = this.createInternetGateway,
            naclSpec = naclBuilder.build(),
            subnetGroups = subnetsBuilder.build(),
            )
    }

    override fun availabilityZones(azs: List<String>) {
        availabilityZones.addAll(azs)
    }

    override fun createInternetGateway(value: Boolean) {
        createInternetGateway = value
    }

    override fun defaultInstanceTenancy(value: DefaultInstanceTenancy) {
        defaultInstanceTenancy = value
    }

    override fun enableDnsHostnames(value: Boolean) {
        enableDnsHostnames = value
    }

    override fun enableDnsSupport(value: Boolean) {
        enableDnsSupport = value
    }

    override fun ipAddresses(ipAddresses: IIpAddresses) {
        this.ipAddresses = ipAddresses
    }

    override fun ipV6Addresses(ipAddresses: IIpv6Addresses) {
        this.ipV6Addresses = ipAddresses
    }

    override fun ipProtocol(protocol: IpProtocol) {
        this.ipProtocol = protocol
    }

    override fun maxAzs(azs: Int) {
        require(maxAzs >= 1) { "Expected maxAzs to be >= 1" }
        maxAzs = azs
    }

    override fun reservedAzs(azs: Int) {
        require(reservedAzs >= 0) { "Expected reservedAzs to be >= 0" }
        reservedAzs = azs
    }

    override fun restrictDefaultSecurityGroup(value: Boolean) {
        this.restrictDefaultSecurityGroup = value
    }

    override fun nacl(block: (NaclBuilder).() -> Unit) {
        naclBuilder.apply(block)
    }

    override fun subnets(block: SubnetsBuilder.() -> Unit) {
        subnetsBuilder.apply(block)
    }
}

internal data class SecureNetworkProps(
    val maxAzs: Int,
    val reservedAzs: Int,
    val ipAddresses: IIpAddresses?,
    val ipV6Addresses: IIpv6Addresses?,
    val ipProtocol: IpProtocol,
    val naclSpec: NaclSpec,
    val subnetGroups: List<SubnetGroupProps>,
    val enableDnsHostnames: Boolean,
    val enableDnsSupport: Boolean,
    val defaultInstanceTenancy: DefaultInstanceTenancy,
    val restrictDefaultSecurityGroup: Boolean,
    val availabilityZones: List<String>,
    val createInternetGateway: Boolean
)

@NetworkDslMarker
public interface SubnetsBuilder {

    public fun publicSubnetGroup(
        name: String = "Public",
        block: (SubnetGroupBuilder).() -> Unit = {}
    ) {
        subnetGroup(name, SubnetType.PUBLIC, block)
    }

    public fun privateSubnetGroup(
        name: String = "Private",
        block: (SubnetGroupBuilder).() -> Unit = {}
    ) {
        subnetGroup(name, SubnetType.PRIVATE_WITH_EGRESS, block)
    }

    public fun isolatedSubnetGroup(
        name: String = "Isolated",
        block: (SubnetGroupBuilder).() -> Unit = {}
    ) {
        subnetGroup(name, SubnetType.PRIVATE_ISOLATED, block)
    }

    public fun subnetGroup(
        name: String,
        type: SubnetType,
        block: (SubnetGroupBuilder).() -> Unit
    )
}

internal class SubnetsBuilderImpl : SubnetsBuilder {
    private val subnetMap = mutableMapOf<String, SubnetGroupBuilder>()

    fun build(): List<SubnetGroupProps> = subnetMap.values.map { it.build() }

    override fun subnetGroup(
        name: String,
        type: SubnetType,
        block: (SubnetGroupBuilder).() -> Unit
    ) {
        validateSubnetGroupName(name)
        val effectiveType = when(type) {
            // transform deprecated type
            SubnetType.PRIVATE_WITH_NAT -> SubnetType.PRIVATE_WITH_EGRESS
            else -> type
        }
        val builder = subnetMap.computeIfAbsent(name) { SubnetGroupBuilder(name, effectiveType) }
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
internal constructor(private val name: String, private val type: SubnetType) {
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

    internal fun build(): SubnetGroupProps {
        val routes = routeBuilder.routes
        return SubnetGroupProps(
            name = name,
            subnetType = type,
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
