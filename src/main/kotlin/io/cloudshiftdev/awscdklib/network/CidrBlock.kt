package io.cloudshiftdev.awscdklib.network

import inet.ipaddr.IPAddress
import io.cloudshiftdev.awscdk.services.ec2.AclCidr

@JvmInline
public value class CidrBlock private constructor(public val value: String) {
    public companion object {

        public fun of(value: IPAddress): CidrBlock {
            return of(value.toCanonicalString())
        }

        public fun of(value: String): CidrBlock {
            return CidrBlock(value)
        }

        public fun allIPv4(): CidrBlock = _allIPv4

        public fun privateNetworks(): List<CidrBlock> = _privateNetworks

        private val _allIPv4: CidrBlock = of("0.0.0.0/0")
        private val _privateNetworks =
            listOf("10.0.0.0/8", "192.168.0.0/16", "172.16.0.0/12").map(CidrBlock::of)
    }

    public fun toAclCidr(): AclCidr = AclCidr.ipv4(value)

    override fun toString(): String = value
}
