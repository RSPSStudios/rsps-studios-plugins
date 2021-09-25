dependencies {
    plugin("com.javatar:osrs-definitions:1.0-SNAPSHOT")
    plugin("com.google.code.gson:gson:2.8.6")
}

rsdeplugin {
    pluginId.set("osrs-definition-editor")
    pluginClass.set("com.javatar.plugin.definition.editor.OsrsDefinitionEditor")
    pluginProvider.set("Javatar")
    pluginVersion.set("0.0.6")
    pluginDescription.set("Definition Editor for Old School RuneScape")
}