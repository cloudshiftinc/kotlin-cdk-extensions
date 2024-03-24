package io.cloudshiftdev.awscdklib.network.routable

import io.cloudshiftdev.awscdk.services.ec2.Vpc
import io.cloudshiftdev.awscdklib.network.securevpc.SecureNetworkProps
import io.cloudshiftdev.awscdklib.network.securevpc.SubnetResolver
import io.cloudshiftdev.constructs.Construct

public class RoutableContext internal constructor(
    public val subnetScope: Construct,
    public val subnetResolver: SubnetResolver,
    public val availabilityZone: String,
    public val vpc: Vpc,
    internal val networkDefinition: SecureNetworkProps
)
