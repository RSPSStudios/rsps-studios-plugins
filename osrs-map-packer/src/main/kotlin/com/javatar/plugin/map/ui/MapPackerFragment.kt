package com.javatar.plugin.map.ui

import com.displee.cache.CacheLibrary
import com.javatar.api.ui.models.EventLogModel
import com.javatar.plugin.map.ui.model.MapInformationModel
import com.javatar.plugin.map.data.PackMapData
import com.javatar.plugin.map.data.RegionKey
import com.javatar.plugin.map.ui.model.MapPackerModel
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import javafx.stage.StageStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import tornadofx.*
import java.io.File
import java.nio.file.Files

class MapPackerFragment : Fragment("Map Packer") {

    val model: MapPackerModel by inject()
    val events: EventLogModel by di()

    init {
        val cache = params["cache"] as CacheLibrary
        model.cache.set(cache)
    }

    override val root = borderpane {
        minWidth = 1200.0
        minHeight = 600.0
        disableWhen(model.xteaManager.regionKeys.emptyProperty())
        style {
            baseColor = c("#3f474f")
        }
        top {
            toolbar(
                button("Replace Map") {
                    disableWhen(model.selectedMap.isNull)
                    action {
                        val selected = model.selectedMap.get()
                        replaceMap(selected)
                    }
                },
                button("Import Map") {
                    action {
                        addMapOrRSPSiPack()
                    }
                },
                button("Export Maps") {
                    action {
                        val directory = chooseDirectory("Choose Directory", File(System.getProperty("user.home")))
                        if (directory != null && directory.exists() && directory.isDirectory) {
                            events log "Started Extracting Maps."
                            GlobalScope.launch(Dispatchers.IO) {
                                model.maps.forEach { extractMap(directory, it) }
                                GlobalScope.launch(Dispatchers.JavaFx) {
                                    events log "Finished Extracting ${model.maps.size} Maps."
                                }
                            }
                        }
                    }
                },
                button("Import Maps") {
                    action {
                        importMapsFromDirectory()
                    }
                },
                separator(),
                button("Export Map") {
                    disableWhen(model.selectedMap.isNull)
                    action {
                        if (model.cache.get() == null)
                            return@action
                        val cache = model.cache.get()
                        val dir = DirectoryChooser()
                        dir.initialDirectory = File(System.getProperty("user.home"))
                        val directory = dir.showDialog(currentWindow)
                        if (directory.exists() && directory.isDirectory) {
                            val objectsFile = if (model.decryptMap.get()) {
                                val key = model.xteaManager.regionKeys[model.regionId.get().toInt()]
                                if (key?.key != null) {
                                    cache.data(5, model.objectsId.get().toInt(), xtea = key.key)
                                } else null
                            } else {
                                cache.data(5, model.objectsId.get().toInt())
                            }
                            val floorsFile = cache.data(5, model.floorsId.get().toInt())
                            if (objectsFile != null && floorsFile != null) {
                                val region = if(model.decryptMap.get()) {
                                    null
                                } else model.xteaManager.regionKeys[model.regionId.get().toInt()]
                                Files.write(
                                    directory.toPath().resolve("${model.regionId.get()}.map"),
                                    PackMapData.pack(
                                        model.namedRegions[model.regionId.get().toInt()] ?: "",
                                        region ?: RegionKey(model.regionId.get().toInt()),
                                        model.objectsId.get().toInt(),
                                        model.floorsId.get().toInt(),
                                        objectsFile,
                                        floorsFile
                                    )
                                )
                            } else {
                                events xlog "Objects ${model.objectsId.get()} for region ${model.regionId.get()} does not exist or no xteas exist."
                            }
                        }
                    }
                },
                checkbox("Decrypt Map", property = model.decryptMap),
                separator(),
                button("Generate Xtea File") {
                    action {
                        val directory = chooseDirectory("Choose Directory To Save") {
                            initialDirectory = File(System.getProperty("user.home"))
                        }
                        if(directory != null && directory.exists() && directory.isDirectory) {
                            model.xteaManager.generateXteaFileTo(directory.absolutePath)
                        }
                    }
                }
            )
        }
        center {
            add<MapTableViewFragment> {
                find<MapLoadingFragment>().openModal(
                        stageStyle = StageStyle.UNDECORATED,
                        block = true,
                        escapeClosesWindow = false
                    )
            }
        }
        right<ModifiedRegionsFragment>()
    }

    private fun importMapsFromDirectory() {
        val directory = chooseDirectory("Choose Directory With Map Files") {
            initialDirectory = File(System.getProperty("user.home"))
        }
        if(directory != null && directory.exists() && directory.isDirectory) {
            readMassMapFiles(directory)
        }
    }

    private fun extractMap(location: File, info: MapInformationModel) {
        val cache = model.cache.get()
        if (cache != null) {
            val name = model.namedRegions[info.regionId]
            val regionKey = model.xteaManager.regionKeys[info.regionId]
            if (regionKey != null) {
                val regionX = regionKey.mapsquare shr 8
                val regionY = regionKey.mapsquare and 255
                val objectData = cache.data(5, "l${regionX}_$regionY", regionKey.key)
                val floorData = cache.data(5, "m${regionX}_$regionY")
                if (objectData != null && floorData != null) {
                    Files.write(
                        location.toPath().resolve("region-${regionKey.mapsquare}.rsmap"),
                        PackMapData.pack(name ?: "", regionKey, info.objectsId, info.floorsId, objectData, floorData)
                    )
                }
            }
        }
    }

    private fun replaceMap(selected: MapInformationModel?) {
        val cache = model.cache.get()
        if (selected != null && cache != null) {
            val files = chooseFile(
                "Choose Map File",
                arrayOf(
                    FileChooser.ExtensionFilter("RSPS Studios Map format", "*.rsmap"),
                ),
                File(System.getProperty("user.home")),
                mode = FileChooserMode.Single
            )
            if (files.size == 1) {
                val file = files[0]
                val mapData = PackMapData.unpack(file)
                cache.put(5, mapData.objectsId, mapData.objectsData, mapData.regionKey)
                cache.put(5, mapData.floorsId, mapData.floorsData)
                model.xteaManager.regionKeys[mapData.regionId] = RegionKey(mapData.regionId, mapData.regionKey)
                model.xteaManager.generateXteaFile(model.xteaLocation.get())
                cache.index(5).update()
            }
        }
    }

    private fun addMapOrRSPSiPack() {
        val file = chooseFile(
            "Choose Map File or RSPSi Pack File",
            arrayOf(
                FileChooser.ExtensionFilter("RSPSi Pack Files (.pack)", "*.pack"),
                FileChooser.ExtensionFilter("RSPS Stuidos Map Files (.rsmap)", "*.rsmap")
            )
        ) {
            initialDirectory = File(System.getProperty("user.home"))
        }
        if(file.isNotEmpty()) {
            file.forEach { readMapFile(it) }
        }
    }

    private fun readMassMapFiles(dir: File) {
        val files = dir.listFiles()
        if(files != null && files.isNotEmpty()) {
            for (file in files) {
                if (file.extension == "pack") {
                    val mapList = PackMapData.unpackRSPSiPackFile(file)
                    val cache = model.cache.get()
                    if (cache != null) {
                        mapList.forEach { md ->
                            val index = model.maps.indexOfFirst { it.regionId == md.regionId }
                            val regionX = md.regionId shr 8
                            val regionY = md.regionId and 255
                            val mapInfo = MapInformationModel(
                                md.regionId,
                                regionX,
                                regionY,
                                md.objectsId,
                                md.floorsId,
                                md.name,
                                md.regionKey
                            )
                            if (index != -1) {
                                model.maps[index] = mapInfo
                            } else {
                                model.maps.add(mapInfo)
                            }
                            cache.put(5, md.objectsId, md.objectsData, md.regionKey)
                            cache.put(5, md.floorsId, md.floorsData)
                        }
                    } else {
                        events xlog "Could not find cache! (Cache null)"
                    }
                } else if (file.extension == "rsmap") {
                    val info = PackMapData.unpack(file)
                    val index = model.maps.indexOfFirst { it.regionId == info.regionId }
                    val regionX = info.regionId shr 8
                    val regionY = info.regionId and 255
                    val mapInfo = MapInformationModel(
                        info.regionId,
                        regionX,
                        regionY,
                        info.objectsId,
                        info.floorsId,
                        info.name,
                        info.regionKey
                    )
                    if (index != -1) {
                        model.maps[index] = mapInfo
                    } else {
                        model.maps.add(mapInfo)
                    }
                    val cache = model.cache.get()
                    if (cache != null) {
                        cache.put(5, info.objectsId, info.objectsData, info.regionKey)
                        cache.put(5, info.floorsId, info.floorsData)
                    }
                }
            }
            runAsync {
                val cache = model.cache.get()
                if(cache != null) {
                    cache.index(5).update()
                } else false
            } ui {
                if(it) {
                    events log "Finished Packing ${files.size} map files."
                } else {
                    events xlog "Cache could not be found! (Cache null)"
                }
            }
        }
    }

    private fun readMapFile(file: File) {
        if(file.extension == "pack") {
            readRSPSiPackFile(file)
        } else if(file.extension == "rsmap") {
            readRSMapFile(file)
        }
    }

    private fun readRSMapFile(file: File) {
        val info = PackMapData.unpack(file)

        val index = model.maps.indexOfFirst { it.regionId == info.regionId }

        val regionX = info.regionId shr 8
        val regionY = info.regionId and 255

        val mapInfo = MapInformationModel(
            info.regionId,
            regionX,
            regionY,
            info.objectsId,
            info.floorsId,
            info.name,
            info.regionKey
        )

        if(index != -1) {
            model.maps[index] = mapInfo
        } else {
            model.maps.add(mapInfo)
        }
        runAsync {
            val cache = model.cache.get()
            if(cache != null) {
                cache.put(5, info.objectsId, info.objectsData, info.regionKey)
                cache.put(5, info.floorsId, info.floorsData)
                cache.index(5).update()
                true
            } else false
        } ui {
            if (it) {
                events log "Finished Packing Region ${info.name} - ${info.regionId} - ${info.regionKey.contentToString()}"
            } else {
                events xlog "Could not find cache! (Cache null)"
            }
        }
    }

    private fun readRSPSiPackFile(file: File) {
        val mapList = PackMapData.unpackRSPSiPackFile(file)
        val cache = model.cache.get()
        if(cache != null) {
            mapList.forEach {  md ->
                val index = model.maps.indexOfFirst { it.regionId == md.regionId }
                val regionX = md.regionId shr 8
                val regionY = md.regionId and 255
                val mapInfo = MapInformationModel(
                    md.regionId,
                    regionX,
                    regionY,
                    md.objectsId,
                    md.floorsId,
                    md.name,
                    md.regionKey
                )
                if(index != -1) {
                    model.maps[index] = mapInfo
                } else {
                    model.maps.add(mapInfo)
                }
                cache.put(5, md.objectsId, md.objectsData, md.regionKey)
                cache.put(5, md.floorsId, md.floorsData)
            }
            runAsync {
                cache.index(5).update()
            } ui {
                if (it) {
                    events log "Finished Packing ${mapList.size} regions."
                } else {
                    events xlog "Failed to update map index."
                }
            }
        } else {
            events xlog "Could not find cache! (Cache null)"
        }
    }

}