import java.util.zip.ZipFile

plugins {
    application
}

repositories {
    mavenCentral()
}

// JavaFX 26.0.2：25 修掉了 Marlin 里 sun.misc.Unsafe 的弃用调用（JDK-8334137），
// 运行时不再刷 Unsafe 警告；26 线与工具链 JDK 26 对应。
val fxVersion = "26.0.2"
// javafx-graphics / controls / fxml 带了各平台的本地库，必须挑对应平台的 classifier。
val fxPlatform = when {
    System.getProperty("os.name").contains("win", ignoreCase = true) -> "win"
    System.getProperty("os.name").contains("mac", ignoreCase = true) -> "mac"
    else -> "linux"
}

dependencies {
    implementation("org.jflac:jflac-codec:1.5.2")
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    implementation("com.googlecode.soundlibs:tritonus-share:0.3.7.4")
    // jlayer 的 pom 把 junit 声明成了运行期依赖，这里用不上
    implementation("com.googlecode.soundlibs:jlayer:1.0.1.4") {
        exclude(group = "junit")
    }

    // 不带 classifier 的 javafx-base / controls 只是 300 字节的空壳，真正的类在带平台的那个 jar 里
    implementation("org.openjfx:javafx-base:$fxVersion:$fxPlatform")
    implementation("org.openjfx:javafx-graphics:$fxVersion:$fxPlatform")
    implementation("org.openjfx:javafx-controls:$fxVersion:$fxPlatform")
    // materialfx-all 11.x 只拆出了 6 个基础控件；21.x 才有 MFXTextField / MFXRadioButton /
    // MFXSlider / MFXComboBox 这些，做完整界面得用它。
    implementation("io.github.palexdev:materialfx:21.18.0-alpha")
    implementation("org.glavo:MonetFX:0.1.0")
    // Material 图标：字体随 jar 走，不需要额外的资源文件或联网
    implementation("org.kordamp.ikonli:ikonli-javafx:12.4.0")
    implementation("org.kordamp.ikonli:ikonli-material2-pack:12.4.0")
}

// jpackage 的 --app-version 进的是 MSI 的版本字段，只认数字点分（r1092 这种会被拒），
// 发版时由 CI 用 -PappVersion 传 1.{提交数/100}.{提交数%100} 进来。
val appVersion = (findProperty("appVersion") as String?) ?: "1.0.0"

version = "1.0.0"

base {
    archivesName = "match3"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}

// 源码直接挂在 src/<package> 下，没有 main/java 这一层。
// jlayer 的 pom 把 junit 挂成了运行期依赖，游戏用不上它。
configurations.all {
    exclude(group = "junit")
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("."))
        resources.setIncludes(listOf("resource/**"))
    }
}

application {
    mainClass = "Main"
    // javafx.graphics 经 NativeLibLoader 走 System::load（受限方法），不加这条运行时会刷警告
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

// 薄 jar 只给 packageInput 用，加个 classifier 免得和 fatJar 抢 build/libs/match3.jar；
// 进 package-input 时再改回 match3.jar，因为 --main-jar 要写死这个名字。
tasks.jar {
    archiveVersion = ""
    archiveClassifier = "slim"
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    testLogging.showStandardStreams = true
}

// 打胖 jar 时每个音频 provider 都有自己的 META-INF/services 文件，
// 直接去重会丢掉其中几个，SPI 就发现不到解码器了，所以先把它们按文件名合并。
val mergeServiceFiles = tasks.register("mergeServiceFiles") {
    val runtime = configurations.runtimeClasspath
    inputs.files(runtime)
    val outDir = layout.buildDirectory.dir("merged-services")
    outputs.dir(outDir)
    doLast {
        val merged = LinkedHashMap<String, LinkedHashSet<String>>()
        for (jar in runtime.get().files) {
            if (!jar.name.endsWith(".jar")) continue
            ZipFile(jar).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name
                    if (entry.isDirectory || !name.startsWith("META-INF/services/")) continue
                    zip.getInputStream(entry).reader(Charsets.UTF_8).buffered().use { reader ->
                        while (true) {
                            val raw = reader.readLine() ?: break
                            val provider = raw.trim()
                            if (provider.isEmpty() || provider.startsWith("#")) continue
                            val bucket = merged.getOrPut(name.substringAfterLast('/')) { LinkedHashSet() }
                            bucket.add(provider)
                        }
                    }
                }
            }
        }
        val target = outDir.get().asFile.resolve("META-INF/services")
        target.deleteRecursively()
        target.mkdirs()
        for ((name, providers) in merged) {
            target.resolve(name).writeText(providers.joinToString("\n", postfix = "\n"), Charsets.UTF_8)
        }
    }
}

val fatJar = tasks.register<Jar>("fatJar") {
    group = "build"
    description = "把所有依赖和合并后的 SPI 注册打进一个可直接运行的 jar"
    archiveBaseName = "match3"
    // 产物名字要稳定，CI 按 build/libs/match3.jar 上传
    archiveVersion = ""
    archiveClassifier = ""
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "Main"
    }
    from(sourceSets.main.get().output)
    dependsOn(mergeServiceFiles)
    from(mergeServiceFiles.map { layout.buildDirectory.dir("merged-services") })
    from({
        configurations.runtimeClasspath.get().files
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    }) {
        exclude("META-INF/services/**")
    }
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/MANIFEST.MF")
}

// JavaFX 是模块化的，jlink 要从模块路径上拿它们；这里单独摊一份，别和普通依赖混在一起。
// 只收带平台 classifier 的 jar：不带 classifier 的那两个是空壳，进了模块路径会和平台 jar 抢模块名。
val fxModules = tasks.register<Sync>("fxModules") {
    group = "build"
    description = "把 JavaFX 的模块 jar 集中到 build/fx-modules，给 jlink 和 run 用"
    into(layout.buildDirectory.dir("fx-modules"))
    from(configurations.runtimeClasspath) {
        include {
            it.file.name.startsWith("javafx-")
                    && it.file.name.contains("-$fxPlatform")
                    && it.file.name.endsWith(".jar")
        }
    }
}

// JavaFX 留在 classpath 上会落进未命名模块，NativeLibLoader 走 System::load 就刷 unnamed module 警告。
// 这里摘出 classpath 改挂 --module-path，按命名模块加载（应用没有 module-info，未命名模块照样读得到）。
// IDEA 用自己的 runner 跑 Main 时拿不到这段配置，需在 Run Configuration 的 VM options 里补同样几条。
val fxModuleNames = "javafx.base,javafx.graphics,javafx.controls"

tasks.named<JavaExec>("run") {
    dependsOn(fxModules)
    classpath = sourceSets.main.get().runtimeClasspath.filter { !it.name.startsWith("javafx-") }
    jvmArgs = listOf(
        "--module-path", layout.buildDirectory.dir("fx-modules").get().asFile.absolutePath,
        "--add-modules", fxModuleNames,
        "--enable-native-access=ALL-UNNAMED,$fxModuleNames",
    )
}

// 供 jpackage 使用：主 jar + 依赖 jar 平铺在一个目录里，各自的 SPI 文件保持独立。
// JavaFX 不放进来：它已经在 jlink 裁出来的运行时里了，再放一份就从 classpath 加载、退回未命名模块。
val packageInput = tasks.register<Sync>("packageInput") {
    group = "build"
    description = "把主 jar、依赖 jar 和 resource 目录集中到 build/package-input"
    into(layout.buildDirectory.dir("package-input"))
    from(tasks.jar) { rename { "match3.jar" } }
    from(configurations.runtimeClasspath) {
        exclude { it.file.name.startsWith("javafx-") }
    }
    from("resource") { into("resource") }
}

// jlink 裁运行时 + jpackage 出安装包；CI 里跑的也是这两步。
val runtimeModules = listOf(
    "java.base", "java.desktop", "java.logging", "java.prefs", "java.sql",
    "jdk.localedata", "jdk.unsupported", "jdk.zipfs",
    "javafx.base", "javafx.graphics", "javafx.controls",
)

fun jtool(name: String): String {
    val home = System.getenv("JAVA_HOME")
    return if (home == null) name else "$home/bin/$name"
}

val jlinkRuntime = tasks.register<Exec>("jlinkRuntime") {
    group = "build"
    description = "裁一份带 JavaFX 的运行时到 build/runtime"
    dependsOn(fxModules)
    doFirst {
        delete(layout.buildDirectory.dir("runtime"))
    }
    commandLine(
        jtool("jlink"),
        "--module-path", layout.buildDirectory.dir("fx-modules").get().asFile.absolutePath,
        "--add-modules", runtimeModules.joinToString(","),
        "--strip-debug", "--no-header-files", "--no-man-pages", "--compress=zip-9",
        "--output", layout.buildDirectory.dir("runtime").get().asFile.absolutePath,
    )
}

tasks.register<Exec>("dist") {
    group = "build"
    description = "用 jpackage 把应用和裁出来的运行时打成 Windows 安装包"
    dependsOn(packageInput, jlinkRuntime)
    doFirst {
        delete(layout.buildDirectory.dir("dist"))
    }
    commandLine(
        jtool("jpackage"),
        "--type", "exe",
        "--input", layout.buildDirectory.dir("package-input").get().asFile.absolutePath,
        "--main-jar", "match3.jar", "--main-class", "Main",
        "--runtime-image", layout.buildDirectory.dir("runtime").get().asFile.absolutePath,
        "--name", "Match3", "--app-version", appVersion, "--vendor", "CS109",
        "--icon", "packaging/app.ico",
        // 裁出来的运行时里 JavaFX 是命名模块，ALL-UNNAMED 覆盖不到它们
        "--java-options", "--enable-native-access=ALL-UNNAMED,$fxModuleNames",
        "--dest", layout.buildDirectory.dir("dist").get().asFile.absolutePath,
        "--win-dir-chooser", "--win-menu", "--win-shortcut",
    )
}

tasks.build {
    dependsOn(fatJar)
}

// 纯 main 方法的冒烟测试，没有 JUnit。
tasks.register<JavaExec>("smokeTest") {
    group = "verification"
    description = "跑 smoke.SmokeTest"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "smoke.SmokeTest"
}

