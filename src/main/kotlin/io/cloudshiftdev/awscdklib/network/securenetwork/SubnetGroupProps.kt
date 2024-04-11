package io.cloudshiftdev.awscdklib.network.securenetwork

import io.cloudshiftdev.awscdk.services.ec2.SubnetType
import io.cloudshiftdev.awscdklib.network.SubnetGroupName

internal data class SubnetGroupProps(
    val name: SubnetGroupName,
    val subnetType: SubnetType,
    val cidrMask: Int?,
    val reserved: Boolean,
)
