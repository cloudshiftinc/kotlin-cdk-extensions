package io.cloudshiftdev.awscdklib.s3

import io.cloudshiftdev.awscdk.services.s3.BlockPublicAccess
import io.cloudshiftdev.awscdk.services.s3.Bucket
import io.cloudshiftdev.awscdk.services.s3.BucketEncryption

public fun Bucket.Builder.secureBucket() {
    blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
    encryption(BucketEncryption.S3_MANAGED)
}
