import com.matthewprenger.cursegradle.CurseArtifact
import com.matthewprenger.cursegradle.CurseProject
import com.matthewprenger.cursegradle.CurseRelation
import com.matthewprenger.cursegradle.Options
import org.gradle.api.internal.artifacts.dependencies.DefaultImmutableVersionConstraint.strictly
import java.net.URI

plugins {
    id("net.neoforged.moddev")
    id("com.modrinth.minotaur") version "2.+"
    id("com.matthewprenger.cursegradle") version "1.4.0"
}

base {
    val archivesBaseName: String by project
    archivesName.set(archivesBaseName)
}
val log: File = file("changelog.md")
val modVersion: String by project
val minecraftVersion: String by project
version = "$modVersion-fleet.1+$minecraftVersion+neoforge"
val mavenGroup: String by project
group = mavenGroup
println("## Changelog for ${base.archivesName.get()} $modVersion \n\n" + log.readText())
println(base.archivesName.get().replace('_','-'))

val loaderVersion: String by project
neoForge {
    version = loaderVersion

    runs {
        register("client") {
            client()
        }
    }

    mods {
        register("testProject") {
            sourceSet(sourceSets.main.get())
        }
    }
}

repositories {
    maven {
        name = "FallenBreath"
        url = uri("https://maven.fallenbreath.me/releases")
    }
    maven {
        name = "Modrinth"
        url = uri("https://api.modrinth.com/maven")
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven {
        url = URI("https://maven.neoforged.net/releases")
    }
    maven {
        url = URI("https://thedarkcolour.github.io/KotlinForForge/")
    }
    maven {
        name = "Jitpack"
        url = uri("https://jitpack.io")
    }
    maven {
        name = "FzzyMaven"
        url = uri("https://maven.fzzyhmstrs.me/")
    }
    mavenLocal()
    mavenCentral()
}
dependencies {
    val guavaVersion: String by project
    implementation("com.google.guava:guava:$guavaVersion")
    /*val minecraftVersion: String by project
    minecraft("com.mojang:minecraft:$minecraftVersion")
    val yarnMappings: String by project
    val yarnMappingsPatchVersion: String by project
    mappings( loom.layered {
        mappings("net.fabricmc:yarn:$yarnMappings:v2")
        mappings("dev.architectury:yarn-mappings-patch-neoforge:$yarnMappingsPatchVersion")
    })
    val loaderVersion: String by project
    neoForge("net.neoforged:neoforge:$loaderVersion")*/

    // fleet: Kotlin for Forge and Fzzy Config removed (config ported to Java + ModConfigSpec)

    val cmVersion: String by project
    implementation("com.github.Fallen-Breath.conditional-mixin:conditional-mixin-neoforge:$cmVersion")
    jarJar("com.github.Fallen-Breath.conditional-mixin:conditional-mixin-neoforge:$cmVersion") {
        version {
            strictly(cmVersion)
            prefer(cmVersion)
        }
    }

}

tasks {
    val javaVersion = JavaVersion.VERSION_25
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.release.set(javaVersion.toString().toInt())
    }

    jar {
        from("LICENSE") { rename { "${base.archivesName.get()}_${it}" } }
    }
    jar {
        from( "credits.txt") { rename { "${base.archivesName.get()}_${it}" } }
    }

    processResources {
        inputs.property("version", project.version)
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(mutableMapOf(
                "version" to project.version)
            )
        }
    }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        //withSourcesJar()
    }

    modrinth.get().group = "upload"
    modrinthSyncBody.get().group = "upload"
}

if (System.getenv("MODRINTH_TOKEN") != null) {
    modrinth {
        val releaseType: String by project
        val mcVersions: String by project
        val uploadDebugMode: String by project
        val modrinthSlugName: String by project

        token.set(System.getenv("MODRINTH_TOKEN"))
        projectId.set(modrinthSlugName)
        versionNumber.set("$modVersion+$minecraftVersion+neoforge")
        versionName.set("${base.archivesName.get()}-$modVersion+$minecraftVersion+neoforge")
        versionType.set(releaseType)
        uploadFile.set(tasks.jar.get())
        gameVersions.addAll(mcVersions.split(","))
        loaders.addAll("neoforge")
        detectLoaders.set(false)
        changelog.set(log.readText())
        dependencies {
        }
        debugMode.set(uploadDebugMode.toBooleanStrictOrNull() ?: true)
    }
}

if (System.getenv("CURSEFORGE_TOKEN") != null) {
    curseforge {
        val releaseType: String by project
        val mcVersions: String by project
        val uploadDebugMode: String by project

        apiKey = System.getenv("CURSEFORGE_TOKEN")
        project(closureOf<CurseProject> {
            id = "985426"
            changelog = log
            changelogType = "markdown"
            this.releaseType = releaseType
            for (ver in mcVersions.split(",")) {
                addGameVersion(ver)
            }
            addGameVersion("NeoForge")
            mainArtifact(tasks.jar.get().archiveFile.get(), closureOf<CurseArtifact> {
                displayName = "${base.archivesName.get()}-$modVersion+$minecraftVersion+neoforge"
                relations(closureOf<CurseRelation> {
                })
            })
            relations(closureOf<CurseRelation> {
            })
        })
        options(closureOf<Options> {
            javaIntegration = false
            forgeGradleIntegration = false
            javaVersionAutoDetect = false
            debug = uploadDebugMode.toBooleanStrictOrNull() ?: true
        })
    }
}

tasks.register("uploadAll") {
    group = "upload"
    dependsOn(tasks.modrinth.get())
    dependsOn(tasks.curseforge.get())
}