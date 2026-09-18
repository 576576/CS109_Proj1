import java.util.zip.ZipFile

plugins {
    application
}

repositories {
    mavenCentral()
}

// JavaFX 的稳定版在 Maven Central 上只到 24.0.1；25 只有 EA，26 还没有。
val fxVersion = "24.0.1"
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
    implementation("org.openjfx:javafx-fxml:$fxVersion:$fxPlatform")
    implementation("io.github.palexdev:materialfx-all:11.27.0")
    implementation("org.glavo:MonetFX:0.1.0")
}

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
}

// 主 jar 不带版本号：jpackage 的 --main-jar 要写死这个名字。
tasks.jar {
    archiveVersion = ""
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

// 供 jpackage 使用：主 jar + 依赖 jar 平铺在一个目录里，各自的 SPI 文件保持独立。
val packageInput = tasks.register<Sync>("packageInput") {
    group = "build"
    description = "把主 jar、依赖 jar 和 resource 目录集中到 build/package-input"
    into(layout.buildDirectory.dir("package-input"))
    from(tasks.jar)
    from(configurations.runtimeClasspath)
    from("resource") { into("resource") }
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

// 临时：验证 JavaFX + MaterialFX + MonetFX 能否在本机跑起来，迁移完成后删除。
tasks.register<JavaExec>("fxSpike") {
    group = "verification"
    description = "跑 spike.FxSpike"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass = "spike.FxSpike"
}
