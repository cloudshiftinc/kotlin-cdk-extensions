package io.cloudshiftdev.awscdklib.s3

import io.cloudshiftdev.awscdk.services.s3.Bucket
import io.cloudshiftdev.awscdklib.testing.filterByType
import io.cloudshiftdev.awscdklib.testing.testStack
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class S3BucketTest : FunSpec() {
    init {
        test("s3 buckets are secured") {
            val ctx = testStack { Bucket(it, "SecureBucket") { secureBucket() } }

            ctx.stack.resources.filterByType("AWS::S3::Bucket").single().should {
                it.logicalId.shouldBe("SecureBucket1ED1C5CE")
                it.type.shouldBe("AWS::S3::Bucket")
                it.json.shouldEqualJson(
                    """
                    {
                        "Type": "AWS::S3::Bucket",
                        "Properties": {
                            "BucketEncryption": {
                                "ServerSideEncryptionConfiguration": [
                                    {
                                        "ServerSideEncryptionByDefault": {
                                            "SSEAlgorithm": "AES256"
                                        }
                                    }
                                ]
                            },
                            "PublicAccessBlockConfiguration": {
                                "BlockPublicAcls": "true",
                                "BlockPublicPolicy": "true",
                                "IgnorePublicAcls": "true",
                                "RestrictPublicBuckets": "true"
                            },
                            "VersioningConfiguration": {
                              "Status": "Enabled"
                            }
                        },
                        "UpdateReplacePolicy": "Retain",
                        "DeletionPolicy": "Retain"
                    }
                    """
                        .trimIndent())
            }
        }
    }
}
