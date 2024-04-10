package io.cloudshiftdev.awscdklib.network

import net.pearx.kasechange.toPascalCase

@JvmInline
public value class SubnetGroupName private constructor(public val value: String) {
    init {
        require(value == value.toPascalCase()) {
            "Subnet group name must match convention; expected '${value.toPascalCase()}', got '$value'"
        }
    }

    public companion object {
        public fun of(value: String): SubnetGroupName = SubnetGroupName(value)

        public fun String.toSubnetGroupName(): SubnetGroupName = of(this)
    }

    override fun toString(): String = value
}
