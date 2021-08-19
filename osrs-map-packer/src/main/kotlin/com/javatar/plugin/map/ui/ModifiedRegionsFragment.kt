package com.javatar.plugin.map.ui

import com.javatar.plugin.map.ui.model.MapPackerModel
import tornadofx.*

class ModifiedRegionsFragment : Fragment() {

    val model: MapPackerModel by inject()

    override val root = scrollpane {
        vbox {
            paddingAll = 25.0
            spacing = 10.0
            label("Modified Regions")
            vbox {
                dynamicContent(model.xteaManager.replacedKeys) {
                    it?.values?.forEach {
                        vbox {
                            spacing = 5.0
                            label("Region ${it.mapsquare}")
                            vbox {
                                paddingLeft = 25.0
                                spacing = 2.0
                                label("${it.key[0]}")
                                label("${it.key[1]}")
                                label("${it.key[2]}")
                                label("${it.key[3]}")
                            }
                            separator()
                        }
                    }
                }
            }
        }

    }

}