package io.cloudshiftdev.awscdklib.network.routable

import io.cloudshiftdev.awscdk.services.ec2.RouterType

public class RouteTarget(
    public val routerId: String,
    public val routerType: RouterType,
    public val enablesInternetConnectivity: Boolean
)
