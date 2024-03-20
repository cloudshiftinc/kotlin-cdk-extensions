package io.cloudshiftdev.awscdklib.s3

import io.cloudshiftdev.awscdk.RemovalPolicy
import io.cloudshiftdev.awscdk.services.s3.BlockPublicAccess
import io.cloudshiftdev.awscdk.services.s3.BucketEncryption
import io.cloudshiftdev.awscdk.services.s3.BucketProps

public fun BucketProps.Builder.secureBucket() {
    blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
    encryption(BucketEncryption.S3_MANAGED)
    enforceSsl(true)
    versioned(true)
    removalPolicy(RemovalPolicy.RETAIN)
}
