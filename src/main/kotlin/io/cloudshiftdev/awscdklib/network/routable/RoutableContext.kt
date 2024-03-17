package io.cloudshiftdev.awscdklib.network.routable

import io.cloudshiftdev.awscdk.services.ec2.Vpc
import io.cloudshiftdev.awscdklib.network.securevpc.NetworkDefinition
import io.cloudshiftdev.awscdklib.network.securevpc.SubnetResolver
import io.cloudshiftdev.constructs.Construct

public class RoutableContext(
    public val subnetScope: Construct,
    public val subnetResolver: SubnetResolver,
    public val availabilityZone: String,
    public val vpc: Vpc,
    public val networkDefinition: NetworkDefinition
)
