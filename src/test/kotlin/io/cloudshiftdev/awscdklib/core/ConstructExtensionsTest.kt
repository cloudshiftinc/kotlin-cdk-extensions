package io.cloudshiftdev.awscdklib.core

import io.cloudshiftdev.awscdk.services.s3.Bucket
import io.cloudshiftdev.awscdk.services.s3.CfnBucket
import io.cloudshiftdev.awscdklib.testing.shouldEqualJsonValueAtPath
import io.cloudshiftdev.awscdklib.testing.testStack
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe

class ConstructExtensionsTest : FunSpec() {
    init {
        test("adds tag") {
            val ctx = testStack {
                val s3Bucket = Bucket(it, "MyBucket")
                s3Bucket.tag("MyTagKey", "MyTagValue")
            }

            ctx.stack.resources.single().should {
                it.type.shouldBe("AWS::S3::Bucket")
                it.tags.shouldBe(mapOf("MyTagKey" to "MyTagValue"))
            }
        }

        test("adds comment") {
            val ctx = testStack {
                val s3Bucket = Bucket(it, "MyBucket")
                s3Bucket.addComment("Some comment")
            }

            ctx.stack.resources.single().should {
                it.type.shouldBe("AWS::S3::Bucket")
                it.metadata.shouldBe(mapOf("cloudshift:comment" to "Some comment"))
            }
        }

        test("adds property override") {
            val ctx = testStack {
                val s3Bucket = Bucket(it, "MyBucket")
                s3Bucket.addPropertyOverride<CfnBucket>("OverriddenProperty", "Value")
            }

            ctx.stack.resources.single().should {
                it.type.shouldBe("AWS::S3::Bucket")
                it.json.shouldEqualJsonValueAtPath("\$.Properties.OverriddenProperty", "Value")
            }
        }
    }
}
