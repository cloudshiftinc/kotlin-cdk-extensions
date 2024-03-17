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

public data class SubnetGroupSpec(
    val name: String,
    val subnetGroupType: SubnetGroupType,
    val cidrMask: Int?,
    val reserved: Boolean,
    val routes: List<SubnetRouteSpec>,
    val allowCrossAzNaclFlows: Boolean
)

public sealed class RouteDestination {
    public open fun name(): String {
        return javaClass.simpleName
    }

    public object Internet : RouteDestination()

    public object Vpc : RouteDestination()

    public data class Cidr(val cidr: CidrBlock) : RouteDestination() {
        override fun name(): String {
            return super.name() + Hashing.md5().hashString(cidr.toString(), StandardCharsets.UTF_8)
        }
    }
}

public data class SubnetRouteSpec(val destination: RouteDestination, val routable: Routable)
