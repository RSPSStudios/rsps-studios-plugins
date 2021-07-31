package com.javatar.plugin.map.ui

import com.javatar.api.ui.models.EventLogModel
import com.javatar.api.ui.utilities.contextmenu
import com.javatar.plugin.map.data.MapInformation
import com.javatar.plugin.map.data.PackMapData
import com.javatar.plugin.map.data.RegionKey
import com.javatar.plugin.map.ui.model.MapPackerModel
import javafx.beans.binding.Bindings
import javafx.scene.control.TextInputDialog
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import tornadofx.*
import java.io.*
import java.nio.file.Files
import java.nio.file.Path

class MapPackerFragment : Fragment("Map Packer") {

    val model: MapPackerModel by inject()
    val events: EventLogModel by di()

    override val root = borderpane {
        disableWhen(model.xteaManager.regionKeys.emptyProperty())
        style {
            baseColor = c("#3f474f")
        }
        top {
            toolbar(
                button("Replace Map") {
                    action {
                        val selected = model.selectedMap.get()
                        replaceMap(selected)
                    }
                },
                button("Add Map") {

                },
                button("Extract All") {
                    action {
                        val directory = chooseDirectory("Choose Directory", File(System.getProperty("user.home")))
                        if(directory != null && directory.exists() && directory.isDirectory) {
                            events log "Started Extracting Maps."
                            GlobalScope.launch(Dispatchers.IO) {
                                model.maps.forEach { extractMap(directory, it) }
                                GlobalScope.launch(Dispatchers.JavaFx) {
                                    events log "Finished Extracting ${model.maps.size} Maps."
                                }
                            }
                        }
                    }
                }
            )
        }
        left {
            listview(model.maps) {
                model.selectedMap.bind(selectionModel.selectedItemProperty())
                cellFormat {
                    textProperty().bind(Bindings.createStringBinding({
                        if (!model.namedRegions.containsKey(it.regionId)) {
                            "Region ${it.regionId} - Objects(${it.objectsId}) - Floors(${it.floorsId})"
                        } else model.namedRegions[it.regionId]
                    }, model.namedRegions))
                    contextmenu {
                        item("Rename").action {
                            val input = TextInputDialog()
                            input.dialogPane.style {
                                baseColor = c("#3f474f")
                            }
                            val output = input.showAndWait()
                            if (output.isPresent && output.get().isNotEmpty()) {
                                model.namedRegions[it.regionId] = output.get()
                                model.save()
                            }
                        }
                    }
                }
            }
        }
        center {
            form {
                disableWhen(model.selectedMap.isNull)
                fieldset {
                    field("Region ID") {
                        label(model.regionId)
                    }
                    field("Region X") {
                        label(model.regionX)
                    }
                    field("Region Y") {
                        label(model.regionY)
                    }
                    field("Objects ID") {
                        label(model.objectsId)
                    }
                    field("Floors ID") {
                        label(model.floorsId)
                    }
                }
                fieldset("Extract Map Data") {
                    field {
                        checkbox("Decrypt Xteas", property = model.zeroXteas)
                    }
                }
                button("Dump Map") {
                    action {
                        if (model.cache.get() == null)
                            return@action
                        val cache = model.cache.get()
                        val dir = DirectoryChooser()
                        dir.initialDirectory = File(System.getProperty("user.home"))
                        val directory = dir.showDialog(currentWindow)
                        if (directory.exists() && directory.isDirectory) {
                            val objectsFile = if (model.zeroXteas.get()) {
                                val key = model.xteaManager.regionKeys[model.regionId.get().toInt()]
                                if (key?.key != null) {
                                    cache.data(5, model.objectsId.get().toInt(), xtea = key.key)
                                } else null
                            } else {
                                cache.data(5, model.objectsId.get().toInt())
                            }
                            val floorsFile = cache.data(5, model.floorsId.get().toInt())
                            if (objectsFile != null && floorsFile != null) {
                                val region = model.xteaManager.regionKeys[model.regionId.get().toInt()]
                                Files.write(
                                    directory.toPath().resolve("${model.regionId.get()}.map"),
                                    PackMapData.pack(
                                        model.namedRegions[model.regionId.get().toInt()] ?: "",
                                        region ?: RegionKey(model.regionId.get().toInt()),
                                        objectsFile,
                                        floorsFile
                                    )
                                )
                            } else {
                                events xlog "Objects ${model.objectsId.get()} for region ${model.regionId.get()} does not exist or no xteas exist."
                            }
                        }
                    }
                }
            }
        }

    }

    private fun extractMap(location: File, info: MapInformation) {
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
                        location.toPath().resolve("region-${regionKey.mapsquare}.map"),
                        PackMapData.pack(name ?: "", regionKey, objectData, floorData)
                    )
                }
            }
        }
    }

    private fun replaceMap(selected: MapInformation?) {
        val cache = model.cache.get()
        if (selected != null && cache != null) {
            val files = chooseFile(
                "Choose Map File",
                arrayOf(
                    FileChooser.ExtensionFilter("RSPS Studios Map format", "map"),
                ),
                File(System.getProperty("user.home")),
                mode = FileChooserMode.Single
            )
            if (files.size == 1) {
                val file = files[0]
                val mapData = PackMapData.unpack(file)
                val regionX = mapData.regionId shr 8
                val regionY = mapData.regionId and 255
                cache.put(5, "l${regionX}_$regionY", mapData.objectsData, mapData.regionKey)
                cache.put(5, "m${regionX}_$regionY", mapData.objectsData)
                model.xteaManager.regionKeys[mapData.regionId] = RegionKey(mapData.regionId, mapData.regionKey)
                model.xteaManager.generateXteaFile(model.xteaLocation.get())
                cache.index(5).update()
            }
        }
    }

}