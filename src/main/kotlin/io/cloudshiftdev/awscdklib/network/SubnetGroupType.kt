package io.cloudshiftdev.awscdklib.network

import io.cloudshiftdev.awscdk.services.ec2.SubnetType

// cleaner than using CDK enumeration which has multiple overlapping deprecated types
public sealed class SubnetGroupType(public val subnetType: SubnetType) {
    public data object Public : SubnetGroupType(SubnetType.PUBLIC)

    public data object Private : SubnetGroupType(SubnetType.PRIVATE_WITH_EGRESS)

    public data object Isolated : SubnetGroupType(SubnetType.PRIVATE_ISOLATED)
}
