pluginManagement {
    repositories {
        maven("https://files.minecraftforge.net/maven/")
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        val mdgVersion: String by settings
        id("net.neoforged.moddev").version(mdgVersion)
    }
}