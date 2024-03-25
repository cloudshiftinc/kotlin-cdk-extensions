package io.cloudshiftdev.awscdklib.network.securenetwork

import io.cloudshiftdev.awscdk.services.ec2.IpAddresses
import io.cloudshiftdev.awscdk.services.ec2.Vpc
import io.cloudshiftdev.awscdklib.network.SubnetPredicates
import io.cloudshiftdev.awscdklib.network.firewall.FirewallPolicy
import io.cloudshiftdev.awscdklib.network.firewall.NetworkFirewall
import io.cloudshiftdev.awscdklib.testing.filterByType
import io.cloudshiftdev.awscdklib.testing.shouldEqualJson
import io.cloudshiftdev.awscdklib.testing.shouldEqualJsonResource
import io.cloudshiftdev.awscdklib.testing.testStack
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeSingleton
import io.kotest.matchers.collections.shouldHaveSize

class NetworkFirewallTest :
    FunSpec({
        test("NetworkFirewall test") {
            val ctx = testStack { stack ->
                val vpc = Vpc(stack, "MyVpc") { ipAddresses(IpAddresses.cidr("10.200.0.0/20")) }

                val firewallPolicy =
                    FirewallPolicy(
                        scope = vpc,
                        id = "FirewallPolicy",
                    )

                val firewall =
                    NetworkFirewall(
                        stack,
                        "MyFirewall",
                        vpc = vpc,
                        subnetMappings = SubnetPredicates.publicSubnets(),
                        firewallName = "MyFirewall",
                        firewallPolicy = firewallPolicy
                    )
            }

            val resources = ctx.stack.resources

            assertSoftly {
                resources.filterByType("AWS::NetworkFirewall::Firewall").shouldBeSingleton {
                    it.json.shouldEqualJson(
                        """{
      "Type": "AWS::NetworkFirewall::Firewall",
      "Properties": {
        "FirewallName": "MyFirewall",
        "FirewallPolicyArn": {
          "Fn::GetAtt": [
            "MyVpcFirewallPolicyCfnFirewallPolicyCA70F78E",
            "FirewallPolicyArn"
          ]
        },
        "SubnetMappings": [
          {
            "SubnetId": {
              "Ref": "MyVpcPublicSubnet1SubnetF6608456"
            }
          },
          {
            "SubnetId": {
              "Ref": "MyVpcPublicSubnet2Subnet492B6BFB"
            }
          }
        ],
        "Tags": [
          {
            "Key": "Name",
            "Value": "TestStack/MyVpc"
          }
        ],
        "VpcId": {
          "Ref": "MyVpcF9F0CA6F"
        }
      }
               } """
                            .trimIndent()
                    )
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

                resources.filterByType("AWS::EC2::Subnet").shouldHaveSize(4)
                resources.filterByType("AWS::EC2::NatGateway").shouldHaveSize(2)

                ctx.shouldEqualJsonResource(
                    "/cloudshift/awscdk/networkfirewall/NetworkFirewall-simple.json"
                )
            }
        }
    })
