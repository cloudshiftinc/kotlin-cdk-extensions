package io.cloudshiftdev.awscdklib.network.securevpc

import com.google.common.hash.Hashing
import io.cloudshiftdev.awscdk.services.ec2.Subnet
import io.cloudshiftdev.awscdklib.network.CidrBlock
import io.cloudshiftdev.awscdklib.network.SubnetGroupType
import io.cloudshiftdev.awscdklib.network.routable.Routable
import java.nio.charset.StandardCharsets

public interface SubnetResolver {
    public fun resolve(subnetGroupName: String, availabilityZone: String): Subnet
}

public class SubnetGroupSpec(
    public val name: String,
    public val subnetGroupType: SubnetGroupType,
    public val cidrMask: Int?,
    public val reserved: Boolean,
    public val routes: List<SubnetRouteSpec>,
    public val allowCrossAzNaclFlows: Boolean
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubnetGroupSpec

        if (name != other.name) return false
        if (subnetGroupType != other.subnetGroupType) return false
        if (cidrMask != other.cidrMask) return false
        if (reserved != other.reserved) return false
        if (routes != other.routes) return false
        if (allowCrossAzNaclFlows != other.allowCrossAzNaclFlows) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + subnetGroupType.hashCode()
        result = 31 * result + (cidrMask ?: 0)
        result = 31 * result + reserved.hashCode()
        result = 31 * result + routes.hashCode()
        result = 31 * result + allowCrossAzNaclFlows.hashCode()
        return result
    }

    override fun toString(): String {
        return "SubnetGroupSpec(name='$name', subnetGroupType=$subnetGroupType, cidrMask=$cidrMask, reserved=$reserved, routes=$routes, allowCrossAzNaclFlows=$allowCrossAzNaclFlows)"
    }
}

public sealed class RouteDestination {
    public open fun name(): String {
        return javaClass.simpleName
    }

    public data object Internet : RouteDestination()

    public data object Vpc : RouteDestination()

    public class Cidr(public val cidr: CidrBlock) : RouteDestination() {
        override fun name(): String {
            return super.name() + Hashing.md5().hashString(cidr.toString(), StandardCharsets.UTF_8)
        }
    }
}

public class SubnetRouteSpec(
    public val destination: RouteDestination,
    public val routable: Routable
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SubnetRouteSpec

        if (destination != other.destination) return false
        if (routable != other.routable) return false

        return true
    }

    override fun hashCode(): Int {
        var result = destination.hashCode()
        result = 31 * result + routable.hashCode()
        return result
    }

    override fun toString(): String {
        return "SubnetRouteSpec(destination=$destination, routable=$routable)"
    }
}
