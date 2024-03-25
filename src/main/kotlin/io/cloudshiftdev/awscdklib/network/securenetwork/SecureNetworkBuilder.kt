package io.cloudshiftdev.awscdklib.network.securenetwork

import io.cloudshiftdev.awscdk.services.ec2.DefaultInstanceTenancy
import io.cloudshiftdev.awscdk.services.ec2.IIpAddresses
import io.cloudshiftdev.awscdk.services.ec2.IIpv6Addresses
import io.cloudshiftdev.awscdk.services.ec2.IpAddresses
import io.cloudshiftdev.awscdk.services.ec2.IpProtocol
import io.cloudshiftdev.awscdk.services.ec2.NatGatewayProps
import io.cloudshiftdev.awscdk.services.ec2.NatInstanceProps
import io.cloudshiftdev.awscdk.services.ec2.NatProvider
import io.cloudshiftdev.awscdk.services.ec2.SubnetSelection
import io.cloudshiftdev.awscdk.services.ec2.SubnetType
import io.cloudshiftdev.awscdklib.network.CidrBlock
import io.cloudshiftdev.awscdklib.network.SubnetPredicates
import net.pearx.kasechange.toPascalCase

@DslMarker public annotation class NetworkDslMarker

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

    public fun routers(block: (RoutersBuilder).() -> Unit)
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
    private val naclBuilder = NaclBuilderImpl()
    private val subnetsBuilder = SubnetsBuilderImpl()
    private val routersBuilder = RoutersBuilderImpl()

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
            routerProviders = routersBuilder.build()
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

    override fun routers(block: RoutersBuilder.() -> Unit) {
        routersBuilder.apply(block)
    }
}

internal data class SecureNetworkProps(
    val maxAzs: Int,
    val reservedAzs: Int,
    val ipAddresses: IIpAddresses?,
    val ipV6Addresses: IIpv6Addresses?,
    val ipProtocol: IpProtocol,
    val enableDnsHostnames: Boolean,
    val enableDnsSupport: Boolean,
    val defaultInstanceTenancy: DefaultInstanceTenancy,
    val restrictDefaultSecurityGroup: Boolean,
    val availabilityZones: List<String>,
    val createInternetGateway: Boolean,
    val naclSpec: NaclSpec,
    val subnetGroups: List<SubnetGroupProps>,
    val routerProviders: List<RouterProvider>
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

    public fun subnetGroup(name: String, type: SubnetType, block: (SubnetGroupBuilder).() -> Unit)
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
        val effectiveType =
            when (type) {
                // transform deprecated type
                SubnetType.PRIVATE_WITH_NAT -> SubnetType.PRIVATE_WITH_EGRESS
                else -> type
            }
        val builder = subnetMap.computeIfAbsent(name) { SubnetGroupBuilder(name, effectiveType) }
        builder.apply(block)
    }
}

@NetworkDslMarker
public interface NaclBuilder {
    public fun allowBetween(subnet: String, peeredSubnet: String)

    public fun denyToLocalNetwork(cidr: String)

    public fun denyToAllPrivateNetworks()
}

internal class NaclBuilderImpl : NaclBuilder {
    private val peeredSubnets = mutableListOf<NaclPeering>()
    private val localNetworks = mutableListOf<CidrBlock>()

    override fun allowBetween(subnet: String, peeredSubnet: String) {
        validateSubnetGroupName(subnet)
        validateSubnetGroupName(peeredSubnet)
        val peering = NaclPeering(subnet, peeredSubnet)
        val noDuplicatePeering =
            peeredSubnets.none {
                (it.subnet == peering.subnet && it.peeredSubnet == peering.peeredSubnet) ||
                    (it.subnet == peering.peeredSubnet && it.peeredSubnet == peering.subnet)
            }
        require(noDuplicatePeering) { "Duplicate peering: $peering" }
        peeredSubnets.add(peering)
    }

    override fun denyToLocalNetwork(cidr: String) {
        localNetworks.add(CidrBlock.of(cidr))
    }

    override fun denyToAllPrivateNetworks() {
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
public interface RoutersBuilder {

    public fun routerProvider(provider: RouterProvider)

    public fun egressNatGateway(block: (NatGatewayRouterBuilder).() -> Unit = {})

    public fun egressNatInstance(block: (NatInstanceRouterBuilder).() -> Unit = {})

    public fun egressNetworkFirewall(block: (NetworkFirewallRouterBuilder).() -> Unit = {})
}

internal class RoutersBuilderImpl : RoutersBuilder {
    private val providers = mutableListOf<RouterProvider>()

    fun build(): List<RouterProvider> = providers.toList()

    override fun routerProvider(provider: RouterProvider) {
        providers.add(provider)
    }

    override fun egressNatGateway(block: NatGatewayRouterBuilder.() -> Unit) {
        routerProvider(NatGatewayRouterBuilderImpl().apply(block).build())
    }

    override fun egressNatInstance(block: NatInstanceRouterBuilder.() -> Unit) {
        routerProvider(NatInstanceRouterBuilderImpl().apply(block).build())
    }

    override fun egressNetworkFirewall(block: NetworkFirewallRouterBuilder.() -> Unit) {
        routerProvider(NetworkFirewallRouterBuilderImpl().apply(block).build())
    }
}

@NetworkDslMarker
public interface RouterBuilder {
    public fun routerSubnet(subnetSelection: SubnetSelection)

    public fun routableSubnets(subnetSelections: List<SubnetSelection>)
}

public interface NatRouterBuilder : RouterBuilder {
    public fun natGateways(count: Int)
}

public interface NatGatewayRouterBuilder : NatRouterBuilder {
    public fun props(props: (NatGatewayProps.Builder).() -> Unit = {})
}

public interface NatInstanceRouterBuilder : NatRouterBuilder {
    public fun props(props: (NatInstanceProps.Builder).() -> Unit = {})
}

public interface NetworkFirewallRouterBuilder : RouterBuilder {
    public fun egressSubnets(subnetSelections: List<SubnetSelection>)
}

internal abstract class BaseRouterBuilder : RouterBuilder {
    protected var routerSubnet: SubnetSelection? = null
    protected val routableSubnets = mutableListOf<SubnetSelection>()

    override fun routerSubnet(subnetSelection: SubnetSelection) {
        routerSubnet = subnetSelection
    }

    override fun routableSubnets(subnetSelections: List<SubnetSelection>) {
        routableSubnets.addAll(subnetSelections)
    }
}

internal abstract class BaseNatRouterBuilder : BaseRouterBuilder(), NatRouterBuilder {
    protected var natGateways = 99

    override fun natGateways(count: Int) {
        natGateways = count
    }
}

internal class NatGatewayRouterBuilderImpl : BaseNatRouterBuilder(), NatGatewayRouterBuilder {
    private var props: (NatGatewayProps.Builder.() -> Unit) = {}

    override fun props(props: NatGatewayProps.Builder.() -> Unit) {
        this.props = props
    }

    fun build(): RouterProvider {
        return NatRouterProvider(
            routerSubnet = routerSubnet ?: SubnetPredicates.publicSubnets(),
            natGatewayCount = natGateways,
            NatProvider.gateway(props)
        )
    }
}

internal class NatInstanceRouterBuilderImpl : BaseNatRouterBuilder(), NatInstanceRouterBuilder {
    private var props: (NatInstanceProps.Builder.() -> Unit) = {}

    override fun props(props: NatInstanceProps.Builder.() -> Unit) {
        this.props = props
    }

    fun build(): RouterProvider {
        return NatRouterProvider(
            routerSubnet = routerSubnet ?: SubnetPredicates.publicSubnets(),
            natGatewayCount = natGateways,
            NatProvider.instanceV2(props)
        )
    }
}

internal class NetworkFirewallRouterBuilderImpl :
    BaseRouterBuilder(), NetworkFirewallRouterBuilder {
    private val egressSubnets = mutableListOf<SubnetSelection>()

    fun build(): RouterProvider {
        return EgressNetworkFirewallRouterProvider(
            routerSubnet ?: SubnetPredicates.privateSubnets(),
            routableSubnets,
            egressSubnets
        )
    }

    override fun egressSubnets(subnetSelections: List<SubnetSelection>) {
        egressSubnets.addAll(subnetSelections)
    }
}

@NetworkDslMarker
public class SubnetGroupBuilder
internal constructor(private val name: String, private val type: SubnetType) {
    private var reserved = false
    private var cidrMask: Int? = null
    private var allowCrossAzNaclFlows: Boolean = false

    public fun cidrMask(value: Int) {
        require(value in 16..28) {
            "Subnet CIDR mask must be between 16 and 28 (inclusive); got $value"
        }
        cidrMask = value
    }

    public fun reserved() {
        reserved = true
    }

    public fun allowCrossAzNaclFlows() {
        this.allowCrossAzNaclFlows = true
    }

    internal fun build(): SubnetGroupProps {
        return SubnetGroupProps(
            name = name,
            subnetType = type,
            cidrMask = cidrMask,
            reserved = reserved,
            allowCrossAzNaclFlows = allowCrossAzNaclFlows
        )
    }
}
