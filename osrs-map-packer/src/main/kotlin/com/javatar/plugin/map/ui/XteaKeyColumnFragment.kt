package com.javatar.plugin.map.ui

import com.javatar.plugin.map.ui.model.MapInformationModel
import com.javatar.plugin.map.ui.model.MapInformationModel.Companion.ZERO_KEYS
import javafx.geometry.Pos
import tornadofx.*

class XteaKeyColumnFragment : Fragment() {

    val rowItem: MapInformationModel = params["mapInfo"] as MapInformationModel

    override val root = hbox {
        alignment = Pos.CENTER
        spacing = 10.0
        checkbox("Remove Keys", rowItem.useZeroKeys) {
            enableWhen(rowItem.keysProperty.isEqualTo(ZERO_KEYS).or(rowItem.keysProperty.isEqualTo(rowItem.originalKeysProperty))
                .and(rowItem.packModel.decryptMap))
        }
        button("Edit Keys") {
            disableWhen(rowItem.packModel.decryptMap.not())
            action {
                find<KeyEditorFragment>(params = mapOf(KeyEditorFragment::info to rowItem))
                    .openModal(
                        block = true,
                        escapeClosesWindow = false,
                    )
            }
        }
    }

}