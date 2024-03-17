package io.cloudshiftdev.awscdklib.network.routable

import io.cloudshiftdev.awscdk.services.ec2.RouterType
import io.cloudshiftdev.awscdk.services.ec2.Vpc
import io.cloudshiftdev.awscdklib.network.firewall.NetworkFirewall
import io.cloudshiftdev.awscdklib.network.securevpc.NetworkDefinition

public class NetworkFirewallRoutable(initBlock: (Vpc, NetworkDefinition) -> NetworkFirewall) :
    SingletonRoutable<NetworkFirewall>(initBlock) {

    override fun routeTarget(nfw: NetworkFirewall, context: RoutableContext): RouteTarget {
        val vpcEndpointId =
            nfw.azEndpointMap[context.availabilityZone]
                ?: error(
                    "Network Firewall does not have an endpoint in AZ ${context.availabilityZone}"
                )
        return RouteTarget(
            routerId = vpcEndpointId,
            routerType = RouterType.VPC_ENDPOINT,
            enablesInternetConnectivity = true
        )
    }
}
