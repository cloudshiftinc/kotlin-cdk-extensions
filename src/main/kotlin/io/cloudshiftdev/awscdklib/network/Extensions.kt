package io.cloudshiftdev.awscdklib.network

import io.cloudshiftdev.awscdk.services.ec2.CfnInternetGateway
import io.cloudshiftdev.awscdk.services.ec2.CfnVPCGatewayAttachment
import io.cloudshiftdev.awscdk.services.ec2.ISubnet
import io.cloudshiftdev.awscdk.services.ec2.IVpc
import io.cloudshiftdev.awscdk.services.ec2.PrivateSubnet
import io.cloudshiftdev.awscdk.services.ec2.PublicSubnet
import io.cloudshiftdev.awscdk.services.ec2.Vpc

/**
 * Attaches internet gateway to this VPC. Does not add it to routing tables. Useful for minimal VPCs
 * that have only ISOLATED subnets but require IGW for GlobalAccelerator or other purposes.
 */
public fun Vpc.attachInternetGateway(): CfnInternetGateway {
    val igw = CfnInternetGateway(this, "IGW")
    CfnVPCGatewayAttachment(this, "VPCGW") {
        internetGatewayId(igw.ref())
        vpcId(vpcId())
    }
    return igw
}

/*

public fun Vpc.deleteDefaultNetworkAcls() {
    val vpc = this
    DispatchingCustomResource.create(vpc, "DeleteDefaultNetworkAcls") {
        codeSource(FunctionCodeSource.fromResource("lambda-custom-resource-functions"))
        customResourceId("DeleteDefaultNetworkAclEntries")
        properties("VpcId" to vpc.vpcId(), "DefaultNaclId" to vpc.vpcDefaultNetworkAcl())
        policyStatement {
            allow()
            action("ec2:DeleteNetworkAclEntry")
            resourceArn(vpc) {
                service("ec2")
                resource("network-acl")
                resourceName(vpc.vpcDefaultNetworkAcl())
            }
        }
        policyStatement {
            allow()
            action("ec2:DescribeNetworkAcls")
            anyResource()
        }
    }
}

public fun Subnet.replaceLocalRouteWithVpcEndpoint(destinationCidrBlock: CidrBlock, vpcEndpointId: String) {
    DispatchingCustomResource.create(this, "ReplaceLocalRouteWithVpcEndpoint") {
        codeSource(FunctionCodeSource.fromResource("lambda-custom-resource-functions"))
        customResourceId("ReplaceLocalRouteWithVpcEndpoint")
        properties(
            "RouteTableId" to routeTable().routeTableId(),
            "DestinationCidrBlock" to destinationCidrBlock.toString(),
            "VpcEndpointId" to vpcEndpointId
        )
        policyStatement {
            allow()
            action("ec2:ReplaceRoute")
            resourceArn(this@replaceLocalRouteWithVpcEndpoint) {
                service("ec2")
                resource("route-table")
                resourceName(routeTable.routeTableId)
            }
        }
    }
}
*/

public fun ISubnet.isPublic(): Boolean {
    return this is PublicSubnet
}

public fun ISubnet.isPrivate(): Boolean {
    return this is PrivateSubnet
}

public fun ISubnet.isIsolated(): Boolean {
    return !(isPublic() || isPrivate())
}

public val ISubnet.cidrBlock: CidrBlock
    get() = CidrBlock.of(ipv4CidrBlock())

public val IVpc.cidrBlock: CidrBlock
    get() = CidrBlock.of(vpcCidrBlock())
