import java.util.zip.ZipFile

plugins {
    application
}

repositories {
    mavenCentral()
}

// 音频解码全部走 javax.sound.sampled 的 SPI：
// jflac 提供 FLAC 的 AudioFileReader，mp3spi 提供 MP3 的，jlayer 是 mp3spi 的解码后端。
dependencies {
    implementation("org.jflac:jflac-codec:1.5.2")
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    implementation("com.googlecode.soundlibs:tritonus-share:0.3.7.4")
    implementation("com.googlecode.soundlibs:jlayer:1.0.1.4")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(27)
    }
}

// 源码直接挂在 src/<package> 下，没有 main/java 这一层。
sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("."))
        resources.setIncludes(listOf("resource/**"))
    }
}

application {
    mainClass = "view.MenuFrame"
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
    archiveClassifier = ""
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "view.MenuFrame"
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
