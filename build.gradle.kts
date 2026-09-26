import com.android.build.api.artifact.SingleArtifact
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec

plugins {
    alias(libs.plugins.android.application)
}

val applicationIdValue = "com.penguin.dialer"

val androidBp = file("Android.bp").readText()

fun bpList(property: String): List<String> {
    val body = Regex("""(?s)\n(\s+)$property: \[(.*?)\n\1]""").find(androidBp)?.groupValues?.get(2)
        ?: error("Android.bp: '$property' not found")
    return Regex("\"([^\"]+)\"").findAll(body).map { it.groupValues[1] }.toList()
}

val bpResourceDirs = bpList("resource_dirs")
val extraPackages = bpList("aaptflags")
    .filter { it.startsWith("--extra-packages ") }
    .map { it.removePrefix("--extra-packages ").trim() }
val bpProguardFiles = bpList("proguard_flags_files")

val gradleManifest = layout.buildDirectory.file("gradle-manifest/AndroidManifest.xml").get().asFile
gradleManifest.parentFile.mkdirs()
file("AndroidManifest.xml").readText()
    .replaceFirst(Regex("""\s+package="[^"]+""""), "")
    .let { if (!gradleManifest.exists() || gradleManifest.readText() != it) gradleManifest.writeText(it) }

val keysDir = file(providers.gradleProperty("penguin.keysDir").getOrElse("../priv-keys"))
val keyName = providers.gradleProperty("penguin.keyName").getOrElse("shared")
val pk8 = keysDir.resolve("$keyName.pk8")
val x509 = keysDir.resolve("$keyName.x509.pem")
val signingStore = layout.buildDirectory.file("signing/$keyName.p12").get().asFile
val signingStorePassword = "penguin-build-local"

val hasReleaseKey = pk8.isFile && x509.isFile
if (hasReleaseKey) {
    val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(pk8.readBytes()))
    val cert = x509.inputStream().use { CertificateFactory.getInstance("X.509").generateCertificate(it) }
    val store = KeyStore.getInstance("PKCS12").apply { load(null, null) }
    store.setKeyEntry(keyName, key, signingStorePassword.toCharArray(), arrayOf(cert))
    signingStore.parentFile.mkdirs()
    signingStore.outputStream().use { store.store(it, signingStorePassword.toCharArray()) }
} else {
    logger.warn("Dialer: $pk8 not found, release builds fall back to the debug key")
}

android {
    namespace = "com.android.dialer"
    compileSdk = 36

    defaultConfig {
        applicationId = applicationIdValue
        minSdk = 30
        targetSdk = 36
    }

    signingConfigs {
        if (hasReleaseKey) {
            create("penguin") {
                storeFile = signingStore
                storePassword = signingStorePassword
                keyAlias = keyName
                keyPassword = signingStorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = false
            proguardFiles(*bpProguardFiles.toTypedArray())
            signingConfig = signingConfigs.findByName("penguin") ?: signingConfigs.getByName("debug")
        }
        debug {
            signingConfig = signingConfigs.findByName("penguin") ?: signingConfigs.getByName("debug")
        }
    }

    sourceSets {
        getByName("main") {
            manifest.srcFile(gradleManifest)
            java.directories.run { clear(); add("java") }
            aidl.directories.run { clear(); add("java") }
            res.directories.clear()
            assets.directories.clear()
            resources.directories.clear()
        }
    }

    buildFeatures {
        aidl = true
        buildConfig = false
    }

    androidResources {
        additionalParameters += "--auto-add-overlay"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    packaging {
        jniLibs.useLegacyPackaging = false
        resources.excludes += listOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE*",
            "META-INF/NOTICE*",
            "META-INF/*.kotlin_module",
        )
    }

    useLibrary("org.apache.http.legacy")
}

abstract class GenerateExtraRClasses : DefaultTask() {
    @get:InputFile
    abstract val symbolList: RegularFileProperty

    @get:Input
    abstract val packages: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val byType = sortedMapOf<String, MutableList<String>>()
        symbolList.get().asFile.forEachLine { line ->
            val parts = line.split(' ', limit = 4)
            if (parts.size < 4) return@forEachLine
            val (kind, type, name, value) = parts
            val decl = if (kind == "int[]") {
                "public static final int[] $name = new int[] $value;"
            } else {
                "public static final int $name = $value;"
            }
            byType.getOrPut(type) { mutableListOf() }.add(decl)
        }

        val out = outputDir.get().asFile
        out.deleteRecursively()
        for (pkg in packages.get()) {
            val file = out.resolve(pkg.replace('.', '/') + "/R.java")
            file.parentFile.mkdirs()
            file.bufferedWriter().use { w ->
                w.write("/* Generated from R.txt by GenerateExtraRClasses. */\n")
                w.write("package $pkg;\n\npublic final class R {\n")
                for ((type, decls) in byType) {
                    w.write("  public static final class $type {\n")
                    decls.forEach { w.write("    "); w.write(it); w.write("\n") }
                    w.write("  }\n")
                }
                w.write("}\n")
            }
        }
    }
}

abstract class OverlayResourceDirs : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val resourceDirs: ListProperty<Directory>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun overlay() {
        val out = outputDir.get().asFile
        out.deleteRecursively()
        val dirs = resourceDirs.get().map { it.asFile }

        fun baseName(f: File) = f.name.substringBefore('.')
        fun entryKey(qualifier: String, el: org.w3c.dom.Element): String {
            val type = when (el.tagName) {
                "item" -> el.getAttribute("type")
                "string-array", "integer-array" -> "array"
                else -> el.tagName
            }
            return "$qualifier/$type/${el.getAttribute("name")}"
        }

        val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val parsed = dirs.map { dir ->
            dir.listFiles().orEmpty().filter { it.isDirectory && it.name.startsWith("values") }
                .flatMap { q -> q.listFiles().orEmpty().filter { it.extension == "xml" }.sortedBy { it.name } }
                .associateWith { dbf.newDocumentBuilder().parse(it) }
        }

        fun isAttrDefinition(el: org.w3c.dom.Element) =
            el.tagName == "attr" && (el.hasAttribute("format") || el.childElements().isNotEmpty())

        val winner = HashMap<String, org.w3c.dom.Element>()
        val styleableAttrs = HashMap<String, LinkedHashSet<String>>()
        parsed.forEach { docs ->
            docs.forEach { (file, doc) ->
                val q = file.parentFile.name
                doc.documentElement.childElements().forEach { el ->
                    winner[entryKey(q, el)] = el
                    if (el.tagName == "declare-styleable") {
                        val attrs = styleableAttrs.getOrPut(entryKey(q, el)) { LinkedHashSet() }
                        el.childElements().forEach { child ->
                            attrs += child.getAttribute("name")
                            if (isAttrDefinition(child)) winner[entryKey(q, child)] = child
                        }
                    }
                }
            }
        }

        val fileWinner = HashMap<String, Int>()
        dirs.forEachIndexed { index, dir ->
            dir.listFiles().orEmpty().filter { it.isDirectory && !it.name.startsWith("values") }
                .forEach { q -> q.listFiles().orEmpty().forEach { fileWinner["${q.name}/${baseName(it)}"] = index } }
        }

        val transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer()
        dirs.forEachIndexed { index, dir ->
            val prefix = "%03d_".format(index)
            dir.listFiles().orEmpty().filter { it.isDirectory }.forEach { q ->
                val target = out.resolve(q.name)
                if (!q.name.startsWith("values")) {
                    q.listFiles().orEmpty()
                        .filter { fileWinner["${q.name}/${baseName(it)}"] == index }
                        .forEach { it.copyTo(target.resolve(it.name)) }
                }
            }
            parsed[index].forEach { (file, doc) ->
                val q = file.parentFile.name
                doc.documentElement.childElements().forEach { el ->
                    val key = entryKey(q, el)
                    if (winner[key] !== el) {
                        el.parentNode.removeChild(el)
                    } else if (el.tagName == "declare-styleable") {
                        el.childElements().filter { isAttrDefinition(it) && winner[entryKey(q, it)] !== it }
                            .forEach { attr ->
                                attr.removeAttribute("format")
                                attr.childElements().forEach { attr.removeChild(it) }
                            }
                        val present = el.childElements().map { it.getAttribute("name") }.toSet()
                        styleableAttrs.getValue(key).filter { it !in present }.forEach { name ->
                            el.appendChild(doc.createElement("attr").apply { setAttribute("name", name) })
                        }
                    }
                }
                val target = out.resolve("$q/$prefix${file.name}")
                target.parentFile.mkdirs()
                transformer.transform(
                    javax.xml.transform.dom.DOMSource(doc),
                    javax.xml.transform.stream.StreamResult(target),
                )
            }
        }
    }

    private fun org.w3c.dom.Node.childElements(): List<org.w3c.dom.Element> {
        val list = ArrayList<org.w3c.dom.Element>()
        val nodes = childNodes
        for (i in 0 until nodes.length) {
            (nodes.item(i) as? org.w3c.dom.Element)?.let(list::add)
        }
        return list
    }
}

val overlayResources = tasks.register<OverlayResourceDirs>("overlayResourceDirs") {
    resourceDirs.set(bpResourceDirs.map { layout.projectDirectory.dir(it) })
    outputDir.set(layout.buildDirectory.dir("generated/res/overlay"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(overlayResources, OverlayResourceDirs::outputDir)

        val name = variant.name.replaceFirstChar { it.uppercase() }
        val task = tasks.register<GenerateExtraRClasses>("generate${name}ExtraRClasses") {
            symbolList.set(variant.artifacts.get(SingleArtifact.RUNTIME_SYMBOL_LIST))
            packages.set(extraPackages.filter { it != "com.android.dialer" })
        }
        variant.sources.java?.addGeneratedSourceDirectory(task, GenerateExtraRClasses::outputDir)
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.BELLSOFT
    }
}

tasks.withType<JavaCompile>().configureEach {
    exclude("com/android/dialer/rootcomponentgenerator/**")
    options.compilerArgs.addAll(listOf("-Xlint:-deprecation", "-Xlint:-unchecked", "-Xmaxerrs", "2000"))
    options.isFork = true
    options.forkOptions.memoryMaximumSize = "4g"
}

dependencies {
    implementation(project(":protos"))
    compileOnly(project(":systemstubs"))

    implementation(files("prebuilts/apache-mime4j-core-0.8.9.jar"))
    implementation(files("prebuilts/apache-mime4j-dom-0.8.9.jar"))
    implementation(files("prebuilts/commons-io-2.13.0.jar"))
    implementation(files("prebuilts/BlurView/BlurView-version-3.1.0.aar"))

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.collection)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.dynamicanimation)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.localbroadcastmanager)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.recyclerview)
    implementation(libs.material)
    implementation(libs.dagger)
    implementation(libs.javax.inject)
    implementation(libs.glide)
    implementation(libs.guava)
    implementation(libs.jsr305)
    implementation(libs.error.prone.annotations)
    implementation(libs.libphonenumber)
    implementation(libs.libphonenumber.geocoder)
    implementation(libs.volley)
    compileOnly(libs.auto.value.annotations)

    annotationProcessor(project(":rootcomponentprocessor"))
    annotationProcessor(libs.auto.value)
    annotationProcessor(libs.dagger.compiler)
    annotationProcessor(libs.glide.compiler)
}
