package io.cloudshiftdev.awscdklib.network.routable

import io.cloudshiftdev.awscdk.services.ec2.Vpc
import io.cloudshiftdev.awscdklib.network.securevpc.NetworkDefinition
import io.cloudshiftdev.awscdklib.network.securevpc.SubnetResolver
import io.cloudshiftdev.constructs.Construct

public data class RoutableContext(
    val subnetScope: Construct,
    val subnetResolver: SubnetResolver,
    val availabilityZone: String,
    val vpc: Vpc,
    val networkDefinition: NetworkDefinition
)
