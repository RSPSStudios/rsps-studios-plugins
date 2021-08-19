package com.javatar.plugin.map.ui

import com.javatar.plugin.map.ui.model.MapInformationModel
import com.javatar.plugin.map.data.RegionKey
import com.javatar.plugin.map.ui.model.MapPackerModel
import javafx.beans.property.SimpleStringProperty
import tornadofx.*

class KeyEditorFragment : Fragment("Xtea Key Editor") {

    val model: MapPackerModel by inject()

    val info = params["info"] as MapInformationModel

    private val part1 = SimpleStringProperty("${info.keysProperty[0]}")
    private val part2 = SimpleStringProperty("${info.keysProperty[1]}")
    private val part3 = SimpleStringProperty("${info.keysProperty[2]}")
    private val part4 = SimpleStringProperty("${info.keysProperty[3]}")

    override val root = form {
        style {
            baseColor = c("#3f474f")
        }
        fieldset(info.name ?: "Region ${info.regionId} Keys") {
            field {
                textfield(part1) {
                    stripNonNumeric("-")
                    action {
                        info.keysProperty[0] = text.toInt()
                        model.xteaManager.replacedKeys[info.regionId] = RegionKey(info.regionId, info.keysProperty.toIntArray())
                    }
                }
            }
            field {
                textfield(part2) {
                    stripNonNumeric("-")
                    action {
                        info.keysProperty[1] = text.toInt()
                        model.xteaManager.replacedKeys[info.regionId] = RegionKey(info.regionId, info.keysProperty.toIntArray())
                    }
                }
            }
            field {
                textfield(part3) {
                    stripNonNumeric("-")
                    action {
                        info.keysProperty[2] = text.toInt()
                        model.xteaManager.replacedKeys[info.regionId] = RegionKey(info.regionId, info.keysProperty.toIntArray())
                    }
                }
            }
            field {
                textfield(part4) {
                    stripNonNumeric("-")
                    action {
                        info.keysProperty[3] = text.toInt()
                        model.xteaManager.replacedKeys[info.regionId] = RegionKey(info.regionId, info.keysProperty.toIntArray())
                    }
                }
            }
            button("Save and Close").action {
                info.keysProperty[0] = part1.get().toInt()
                info.keysProperty[1] = part2.get().toInt()
                info.keysProperty[2] = part3.get().toInt()
                info.keysProperty[3] = part4.get().toInt()
                model.xteaManager.replacedKeys[info.regionId] = RegionKey(info.regionId, info.keysProperty.toIntArray())
                this@KeyEditorFragment.close()
            }
        }
    }

}