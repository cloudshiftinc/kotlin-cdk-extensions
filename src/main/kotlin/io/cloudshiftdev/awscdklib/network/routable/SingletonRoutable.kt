package io.cloudshiftdev.awscdklib.network.routable

import io.cloudshiftdev.awscdk.services.ec2.Vpc
import io.cloudshiftdev.awscdklib.network.securevpc.NetworkDefinition

public abstract class SingletonRoutable<T>(private val initBlock: (Vpc, NetworkDefinition) -> T) :
    Routable {
    private var state: T? = null

    final override fun routeTarget(context: RoutableContext): RouteTarget {
        val s =
            when (state) {
                null -> initBlock(context.vpc, context.networkDefinition)
                else -> state!!
            }
        state = s

        return routeTarget(s, context)
    }

    public abstract fun routeTarget(state: T, context: RoutableContext): RouteTarget
}
