plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.BELLSOFT
    }
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf(rootDir.resolve("java")))
            include(
                "com/android/dialer/rootcomponentgenerator/*.java",
                "com/android/dialer/inject/HasRootComponent.java",
                "com/android/dialer/inject/IncludeInDialerRoot.java",
                "com/android/dialer/inject/RootComponentGeneratorMetadata.java",
            )
        }
    }
}

dependencies {
    implementation(libs.auto.common)
    implementation(libs.auto.service.annotations)
    implementation(libs.dagger)
    implementation(libs.guava.jre)
    implementation(libs.javapoet)
    implementation(libs.javax.inject)
    annotationProcessor(libs.auto.service)
}
