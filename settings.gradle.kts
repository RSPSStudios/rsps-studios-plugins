pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("http://legionkt.com:8085/repository/maven-snapshots/") {
            isAllowInsecureProtocol = true
        }
    }
}
rootProject.name = "RspsStudiosPlugins"

include("osrs-plugin")
include("osrs-map-packer")

