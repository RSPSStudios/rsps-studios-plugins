dependencies {
    plugin("com.javatar:osrs-definitions:1.0-SNAPSHOT")
    plugin("com.google.code.gson:gson:2.8.6")
}

rsdeplugin {
    pluginId.set("osrs-map-packer")
    pluginClass.set("com.javatar.plugin.map.MapPackerPlugin")
    pluginProvider.set("Javatar")
    pluginVersion.set("0.0.1")
    pluginDescription.set("Map Packer for old school runescape")
}