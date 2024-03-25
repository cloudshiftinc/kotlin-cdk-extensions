import com.gradle.scan.agent.serialization.scan.serializer.kryo.ja
import com.ncorti.ktfmt.gradle.tasks.KtfmtCheckTask
import com.ncorti.ktfmt.gradle.tasks.KtfmtFormatTask
import com.vanniktech.maven.publish.SonatypeHost
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktfmt)
    application
}

application {
    mainClass = "io.cloudshifttdev.cdktest.CdkMainKt"
}


dependencies {
    implementation(projects.kotlinCdkExtensions)
}

ktfmt {
    kotlinLangStyle()
}

java {
    consistentResolution { useCompileClasspathVersions() }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

kotlin {
    explicitApi()
    jvmToolchain(17)
}

tasks.withType<KtfmtFormatTask>().configureEach {
    enabled = false
}

