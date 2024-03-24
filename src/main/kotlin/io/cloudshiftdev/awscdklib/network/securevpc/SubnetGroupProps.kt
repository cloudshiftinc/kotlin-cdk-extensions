package io.cloudshiftdev.awscdklib.network.securevpc

import com.google.common.hash.Hashing
import io.cloudshiftdev.awscdk.services.ec2.Subnet
import io.cloudshiftdev.awscdk.services.ec2.SubnetType
import io.cloudshiftdev.awscdklib.network.CidrBlock
import io.cloudshiftdev.awscdklib.network.routable.Routable
import java.nio.charset.StandardCharsets

public interface SubnetResolver {
    public fun resolve(subnetGroupName: String, availabilityZone: String): Subnet
}

internal data class SubnetGroupProps(
    val name: String,
    val subnetType: SubnetType,
    val cidrMask: Int?,
    val reserved: Boolean,
    val routes: List<SubnetRouteSpec>,
    val allowCrossAzNaclFlows: Boolean
)

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
