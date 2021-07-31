package com.javatar.plugin.map.ui.model

import com.displee.cache.CacheLibrary
import com.javatar.plugin.map.data.MapInformation
import com.javatar.plugin.map.data.XteaManager
import javafx.beans.property.*
import javafx.collections.FXCollections
import tornadofx.ViewModel
import tornadofx.onChange

class MapPackerModel : ViewModel() {

    val cache = bind { SimpleObjectProperty<CacheLibrary>(this, "cache") }
    val maps = bind { SimpleListProperty<MapInformation>(this, "maps", FXCollections.observableArrayList()) }
    val regionCount = bind { SimpleIntegerProperty(this, "region_count", config.int("region_count") ?: Short.MAX_VALUE.toInt()) }
    val selectedMap = bind { SimpleObjectProperty<MapInformation>(this, "selected_map") }

    val namedRegions = bind { SimpleMapProperty<Int, String>(this, "named_regions", FXCollections.observableHashMap()) }

    val xteaLocation = bind { SimpleStringProperty(this, "xtea_location", config.string("xteas")) }

    val xteaManager = XteaManager()

    val regionId = bind { SimpleStringProperty(this, "region_id") }
    val regionX = bind { SimpleStringProperty(this, "region_x") }
    val regionY = bind { SimpleStringProperty(this, "region_y") }
    val objectsId = bind { SimpleStringProperty(this, "objects_id") }
    val floorsId = bind { SimpleStringProperty(this, "floors_id") }

    val zeroXteas = bind { SimpleBooleanProperty(this, "zero_xteas", false) }

    init {

        config.forEach { t, u ->
            if(t is String && t.startsWith("region-")) {
                val regionId = t.split("-")[1].toInt()
                namedRegions[regionId] = u as String
            }
        }

        selectedMap.onChange {
            if(it != null) {
                regionId.set("${it.regionId}")
                regionX.set("${it.regionX}")
                regionY.set("${it.regionY}")
                objectsId.set("${it.objectsId}")
                floorsId.set("${it.floorsId}")
            }
        }

        cache.onChange {
            if (it != null) {
                if(xteaManager.regionKeys.isNotEmpty()) {
                    val mapIndex = it.index(5)
                    xteaManager.regionKeys.values.forEach { region ->
                        val regionX = region.mapsquare shr 8
                        val regionY = region.mapsquare and 255
                        val objects = mapIndex.archive("l${regionX}_$regionY")
                        val floors = mapIndex.archive("m${regionX}_$regionY")
                        if (objects != null && floors != null) {
                            maps.add(
                                MapInformation(
                                    region.mapsquare,
                                    regionX,
                                    regionY,
                                    objects.id,
                                    floors.id
                                )
                            )
                            maps.sortBy { it.regionId }
                        }
                    }
                } else {
                    scanForRegions(it)
                }
            }
        }
    }

    private fun scanForRegions(it: CacheLibrary) {
        maps.clear()
        val mapIndex = it.index(5)
        repeat(config.int("region_count") ?: regionCount.get()) { regionId ->
            val regionX = regionId shr 8
            val regionY = regionId and 255
            val objects = mapIndex.archive("l${regionX}_$regionY")
            val floors = mapIndex.archive("m${regionX}_$regionY")
            if (objects != null && floors != null) {
                maps.add(
                    MapInformation(
                        regionId,
                        regionX,
                        regionY,
                        objects.id,
                        floors.id
                    )
                )
            }
        }
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

}