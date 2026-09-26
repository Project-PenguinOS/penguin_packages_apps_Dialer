plugins {
    `java-library`
    alias(libs.plugins.protobuf)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.BELLSOFT
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

val stageProtos by tasks.registering(Sync::class) {
    from(rootDir) { include("java/**/*.proto") }
    into(layout.buildDirectory.dir("proto-src"))
}

sourceSets {
    main {
        proto {
            srcDir(stageProtos)
        }
    }
}

dependencies {
    api(libs.protobuf.javalite)
}

protobuf {
    protoc { artifact = libs.protoc.get().toString() }
    generateProtoTasks {
        all().configureEach {
            builtins {
                named("java") { option("lite") }
            }
        }
    }
}
