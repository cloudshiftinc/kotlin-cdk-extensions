@file:Suppress("PropertyName")

package io.cloudshiftdev.awscdklib.network

import io.cloudshiftdev.awscdk.services.ec2.SubnetSelection
import io.cloudshiftdev.awscdk.services.ec2.SubnetType

public object SubnetPredicates {

    public fun publicSubnets(): SubnetSelection = type(SubnetType.PUBLIC)

    public fun privateSubnets(): SubnetSelection = type(SubnetType.PRIVATE_WITH_EGRESS)

    public fun isolatedSubnets(): SubnetSelection = type(SubnetType.PRIVATE_ISOLATED)

    public fun groupNamed(name: String): SubnetSelection = SubnetSelection { subnetGroupName(name) }

    public fun type(subnetType: SubnetType): SubnetSelection = SubnetSelection {
        subnetType(subnetType)
    }

    public fun type(subnetGroupType: SubnetGroupType): SubnetSelection =
        type(subnetGroupType.subnetType)
}
