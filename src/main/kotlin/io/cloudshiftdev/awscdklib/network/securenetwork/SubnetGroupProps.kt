package io.cloudshiftdev.awscdklib.network.securenetwork

import io.cloudshiftdev.awscdk.services.ec2.SubnetType


internal data class SubnetGroupProps(
    val name: String,
    val subnetType: SubnetType,
    val cidrMask: Int?,
    val reserved: Boolean,
    val allowCrossAzNaclFlows: Boolean
)
