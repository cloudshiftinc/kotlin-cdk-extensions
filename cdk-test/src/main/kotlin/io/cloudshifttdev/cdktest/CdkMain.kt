package io.cloudshifttdev.cdktest

import io.cloudshiftdev.awscdk.App
import io.cloudshiftdev.awscdk.Stack
import io.cloudshiftdev.awscdklib.network.securenetwork.SecureNetwork
import io.cloudshiftdev.awscdklib.network.securenetwork.publicPrivateIsolatedNetworkWithFirewall

public fun main(args: Array<String>) {
    val app = App()

    val stack = Stack(app, "CdkExtensionsTestStack") {
        env {
            account("615821722087")
            region("us-east-1")
        }
    }

    val secureNetwork =
        SecureNetwork(stack, "MySecureNetwork") {
            cidrBlock("10.200.0.0/20")
            maxAzs(3)
            reservedAzs(0)

            publicPrivateIsolatedNetworkWithFirewall()
        }

    app.synth()
}
