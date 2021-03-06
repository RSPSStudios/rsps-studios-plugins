package com.javatar.plugin.map.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import javafx.beans.property.SimpleMapProperty
import javafx.collections.FXCollections
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class XteaManager {

    val regionKeys = SimpleMapProperty<Int, RegionKey>(FXCollections.observableHashMap())
    val replacedKeys  = SimpleMapProperty<Int, RegionKey>(FXCollections.observableHashMap())

    val gson = Gson()

    fun load(location: String) : Boolean {
        val path = Path.of(location)
        try {
            val map = gson.fromJson<List<RegionKey>>(path.readText(), object : TypeToken<List<RegionKey>>(){}.type)
            regionKeys.putAll(map.associateBy { it.mapsquare })
            println("Loaded ${regionKeys.size} xteas.")
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
        return true
    }

    fun generateXteaFile(location: String) {
        val map = HashMap(regionKeys)
        map.putAll(replacedKeys)
        if(map.isNotEmpty()) {
            val list = map.values
            Files.write(Path.of(location.replace("xteas.json", "newXteas.json")), gson.toJson(list).toByteArray())
        }
    }

    fun generateXteaFileTo(location: String) {
        val map = HashMap(regionKeys)
        map.putAll(replacedKeys)
        if(map.isNotEmpty()) {
            val list = map.values
            Files.write(Path.of(location, "newxteas.json"), gson.toJson(list).toByteArray())
        }
    }

}