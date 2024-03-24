package io.cloudshiftdev.awscdklib.network.securevpc

import io.cloudshiftdev.awscdklib.testing.filterByType
import io.cloudshiftdev.awscdklib.testing.shouldEqualJsonResource
import io.cloudshiftdev.awscdklib.testing.shouldEqualJsonValueAtPath
import io.cloudshiftdev.awscdklib.testing.testStack
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize

internal class SecureVpcTest : FunSpec() {
    init {
        test("Network created successfully") {
            val ctx = testStack { stack ->
                val secureNetwork =
                    SecureNetwork(stack, "MySecureVpc") {
                        cidrBlock("10.200.0.0/20")
                        maxAzs(2)
                        reservedAzs(1)

                        publicPrivateIsolatedNetwork()
                    }
            }

            val resources = ctx.stack.resources
            ctx.shouldEqualJsonResource("/cloudshift/awscdk/securevpc/SecureNetworkTest-1.json")
            assertSoftly {
                resources.filterByType("AWS::EC2::FlowLog").shouldBeSingleton {
                    it.json.shouldEqualJsonValueAtPath("\$.Properties.MaxAggregationInterval", "60")
                }

                resources.filterByType("AWS::EC2::EIP").shouldHaveSize(3)
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

                ctx.stack.outputs
                    .map { it.exportedName }
                    .shouldContainExactly(listOf("natPublicIp1", "natPublicIp2", "natPublicIp3"))
            }
        }
        //
        //        test("Network with firewall created successfully") {
        //            val ctx = testStack { stack ->
        //                val nfw = NetworkFirewallRoutable { vpc, _ ->
        //                    NetworkFirewall(vpc, "NetworkFirewall") {
        //                        vpc(vpc)
        //                        subnetPlacement("NetworkFirewall")
        //                    }
        //                }
        //                SecureVpc(stack, "Network") {
        //                    cidrBlock("10.200.0.0/20")
        //                    maxAzs(2)
        //                    reservedAzs(1)
        //                    publicPrivateIsolatedNetworkWithFirewall(nfw)
        //                }
        //            }
        //
        //
        // ctx.shouldEqualJsonResource("/cloudshift/awscdk/securevpc/SecureNetworkTest-2.json")
        //        }
    }
}
