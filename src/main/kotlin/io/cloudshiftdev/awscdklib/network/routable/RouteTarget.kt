package io.cloudshiftdev.awscdklib.network.routable

import io.cloudshiftdev.awscdk.services.ec2.RouterType

public data class RouteTarget(
    val routerId: String,
    val routerType: RouterType,
    val enablesInternetConnectivity: Boolean
)
