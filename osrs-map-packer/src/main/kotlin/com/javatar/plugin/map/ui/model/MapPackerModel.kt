package com.javatar.plugin.map.ui.model

import com.displee.cache.CacheLibrary
import com.javatar.plugin.map.data.XteaManager
import javafx.beans.property.*
import javafx.collections.FXCollections
import javafx.stage.FileChooser
import tornadofx.Fragment
import tornadofx.ViewModel
import tornadofx.onChange

class MapPackerModel : ViewModel() {

    val cache = bind { SimpleObjectProperty<CacheLibrary>(this, "cache") }
    val maps = bind { SimpleListProperty<MapInformationModel>(this, "maps", FXCollections.observableArrayList()) }
    val regionCount =
        bind { SimpleIntegerProperty(this, "region_count", config.int("region_count") ?: Short.MAX_VALUE.toInt()) }
    val selectedMap = bind { SimpleObjectProperty<MapInformationModel>(this, "selected_map") }

    val namedRegions = bind { SimpleMapProperty<Int, String>(this, "named_regions", FXCollections.observableHashMap()) }

    val xteaLocation = bind { SimpleStringProperty(this, "xtea_location", config.string("xteas")) }

    val xteaManager = XteaManager()

    val regionId = bind { SimpleStringProperty(this, "region_id") }
    val regionX = bind { SimpleStringProperty(this, "region_x") }
    val regionY = bind { SimpleStringProperty(this, "region_y") }
    val objectsId = bind { SimpleStringProperty(this, "objects_id") }
    val floorsId = bind { SimpleStringProperty(this, "floors_id") }

    val decryptMap = bind { SimpleBooleanProperty(this, "zero_xteas", false) }

    val loadingProgress = bind { SimpleDoubleProperty(this, "loading_progress", 0.0) }

    init {
        selectedMap.onChange {
            if (it != null) {
                regionId.set("${it.regionId}")
                regionX.set("${it.regionX}")
                regionY.set("${it.regionY}")
                objectsId.set("${it.objectsId}")
                floorsId.set("${it.floorsId}")
            }
        }
    }

    fun findRegions(cache: CacheLibrary? = this.cache.get()) {
        if (cache != null) {
            config.forEach { t, u ->
                if (t is String && t.startsWith("region-")) {
                    val regionId = t.split("-")[1].toInt()
                    namedRegions[regionId] = u as String
                }
            }
            if (xteaManager.regionKeys.isNotEmpty()) {
                val mapIndex = cache.index(5)
                xteaManager.regionKeys.values.forEachIndexed { index, region ->
                    val regionX = region.mapsquare shr 8
                    val regionY = region.mapsquare and 255
                    val objects = mapIndex.archive("l${regionX}_$regionY")
                    val floors = mapIndex.archive("m${regionX}_$regionY")
                    if (objects != null && floors != null) {
                        maps.add(
                            MapInformationModel(
                                region.mapsquare,
                                regionX,
                                regionY,
                                objects.id,
                                floors.id,
                                namedRegions[region.mapsquare] ?: "Unknown",
                                xteaManager.regionKeys[region.mapsquare]?.key ?: intArrayOf(0, 0, 0, 0)
                            )
                        )
                    }
                    val t = (index / xteaManager.regionKeys.size)
                    println(t)
                    loadingProgress.set(t.toDouble())
                }
            } else {
                println("Scanning regions.")
                scanForRegions(cache)
            }
            maps.sortBy { it.regionId }
        } else {
            println("Cache is null. ffs")
        }
    }

    private fun scanForRegions(it: CacheLibrary) {
        maps.clear()
        val mapIndex = it.index(5)
        val size = config.int("region_count") ?: regionCount.get()
        repeat(size) { regionId ->
            val regionX = regionId shr 8
            val regionY = regionId and 255
            val objects = mapIndex.archive("l${regionX}_$regionY")
            val floors = mapIndex.archive("m${regionX}_$regionY")
            if (objects != null && floors != null) {
                maps.add(
                    MapInformationModel(
                        regionId,
                        regionX,
                        regionY,
                        objects.id,
                        floors.id,
                        "",
                        intArrayOf(0, 0, 0, 0)
                    )
                )
            }
            val value = regionId / size.toDouble()
            println(value)
            loadingProgress.set(value)
        }
        loadingProgress.set(1.0)
        mapIndex.clear()
    }

    fun save() {
        with(config) {
            set("xteas", xteaLocation.get())
            set("region_count", regionCount.get())
            if (namedRegions.isNotEmpty()) {
                namedRegions.forEach {
                    if (it.value != "unknown") {
                        set("region-${it.key}", it.value)
                    }
                }
            }
            save()
        }
    }

    fun findXteas(fragment: Fragment) {
        if (xteaLocation.get() == null || !xteaManager.load(xteaLocation.get())) {
            val fileChooser = FileChooser()
            val file = fileChooser.showOpenDialog(fragment.currentWindow)
            xteaLocation.set(file.absolutePath)
            if (!xteaManager.load(file.absolutePath)) {
                findXteas(fragment)
            } else {
                save()
            }
        }
    }

}