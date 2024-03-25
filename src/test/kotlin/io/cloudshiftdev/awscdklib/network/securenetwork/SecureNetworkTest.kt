package io.cloudshiftdev.awscdklib.network.securenetwork

import io.cloudshiftdev.awscdklib.testing.filterByType
import io.cloudshiftdev.awscdklib.testing.shouldEqualJsonResource
import io.cloudshiftdev.awscdklib.testing.shouldEqualJsonValueAtPath
import io.cloudshiftdev.awscdklib.testing.testStack
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveSize

internal class SecureNetworkTest : FunSpec() {
    init {
        test("Simple network created") {
            val ctx = testStack { stack ->
                val secureNetwork =
                    SecureNetwork(stack, "MySecureNetwork") {
                        cidrBlock("10.200.0.0/20")
                        maxAzs(2)
                        reservedAzs(1)

                        publicPrivateIsolatedNetwork()
                    }
            }

            val resources = ctx.stack.resources

            assertSoftly {
                resources.filterByType("AWS::EC2::FlowLog").shouldBeSingleton {
                    it.json.shouldEqualJsonValueAtPath("\$.Properties.MaxAggregationInterval", "60")
                }

                resources.filterByType("AWS::EC2::EIP").shouldHaveSize(2)
                resources.filterByType("AWS::EC2::VPC").shouldBeSingleton {
                    it.json.shouldEqualJsonValueAtPath("\$.Properties.CidrBlock", "10.200.0.0/20")
                }
                resources.filterByType("AWS::EC2::InternetGateway").shouldBeSingleton()
                resources
                    .filterByType("AWS::EC2::Subnet")
                    .filter { it.tags["aws-cdk:subnet-type"] == "Public" }
                    .shouldHaveSize(2)
                resources
                    .filterByType("AWS::EC2::Subnet")
                    .filter { it.tags["aws-cdk:subnet-type"] == "Private" }
                    .shouldHaveSize(2)
                resources
                    .filterByType("AWS::EC2::Subnet")
                    .filter { it.tags["aws-cdk:subnet-type"] == "Isolated" }
                    .shouldHaveSize(2)
                resources.filterByType("AWS::EC2::Subnet").shouldHaveSize(6)
                resources.filterByType("AWS::EC2::NatGateway").shouldHaveSize(2)

                ctx.shouldEqualJsonResource("/cloudshift/awscdk/securevpc/SecureNetworkTest-simple.json")
            }
        }

        test("NetworkFirewall egress network") {
            val ctx = testStack { stack ->
                val secureNetwork =
                    SecureNetwork(stack, "MySecureNetwork") {
                        cidrBlock("10.200.0.0/20")
                        maxAzs(2)
                        reservedAzs(1)

                        publicPrivateIsolatedNetworkWithFirewall()
                    }
            }

            val resources = ctx.stack.resources

            assertSoftly {
                resources.filterByType("AWS::EC2::FlowLog").shouldBeSingleton {
                    it.json.shouldEqualJsonValueAtPath("\$.Properties.MaxAggregationInterval", "60")
                }

                resources.filterByType("AWS::EC2::EIP").shouldHaveSize(2)
                resources.filterByType("AWS::EC2::VPC").shouldBeSingleton {
                    it.json.shouldEqualJsonValueAtPath("\$.Properties.CidrBlock", "10.200.0.0/20")
                }
                resources.filterByType("AWS::EC2::InternetGateway").shouldBeSingleton()
                resources
                    .filterByType("AWS::EC2::Subnet")
                    .filter { it.tags["aws-cdk:subnet-type"] == "Public" }
                    .shouldHaveSize(2)
                resources
                    .filterByType("AWS::EC2::Subnet")
                    .filter { it.tags["aws-cdk:subnet-type"] == "Private" }
                    .shouldHaveSize(2)
                resources
                    .filterByType("AWS::EC2::Subnet")
                    .filter { it.tags["aws-cdk:subnet-type"] == "Isolated" }
                    .shouldHaveSize(4)
                resources.filterByType("AWS::EC2::Subnet").shouldHaveSize(8)
                resources.filterByType("AWS::EC2::NatGateway").shouldHaveSize(2)

                ctx.shouldEqualJsonResource("/cloudshift/awscdk/securevpc/SecureNetworkTest-NetworkFirewall.json")
            }
        }
    }
}
