package com.javatar.plugin.map.ui.model

import com.javatar.plugin.map.data.RegionKey
import javafx.beans.property.*
import javafx.collections.FXCollections
import tornadofx.ViewModel
import tornadofx.getValue
import tornadofx.listProperty
import tornadofx.onChange

class MapInformationModel(
    regionId: Int,
    regionX: Int,
    regionY: Int,
    objectsId: Int,
    floorsId: Int,
    name: String,
    keys: IntArray
) : ViewModel() {

    val packModel: MapPackerModel by inject()

    val regionIdProperty = bind { SimpleIntegerProperty(this, "region_id", regionId) }
    val regionId by regionIdProperty

    val regionXProperty = bind { SimpleIntegerProperty(this, "region_x", regionX) }
    val regionX by regionXProperty

    val regionYProperty = bind { SimpleIntegerProperty(this, "region_y", regionY) }
    val regionY by regionYProperty

    val objectsIdProperty = bind { SimpleIntegerProperty(this, "objects_id", objectsId) }
    val objectsId by objectsIdProperty

    val floorsIdProperty = bind { SimpleIntegerProperty(this, "floors_id", floorsId) }
    val floorsId by floorsIdProperty

    val nameProperty = bind { SimpleStringProperty(this, "name", name) }
    val name by nameProperty

    val keysProperty =
        SimpleListProperty(this, "keys_property", FXCollections.observableArrayList(*keys.toTypedArray()))

    val originalKeysProperty: ReadOnlyListProperty<Int> = SimpleListProperty(this, "original_keys", FXCollections.observableArrayList(*keys.toTypedArray()))

    val useZeroKeys = bind { SimpleBooleanProperty(this, "use_zero_keys", false) }

    init {
        useZeroKeys.onChange {
            if(it) {
                keysProperty.setAll(0, 0, 0, 0)
                packModel.xteaManager.replacedKeys[regionId] = RegionKey(regionId, intArrayOf(0, 0, 0, 0))
            } else {
                keysProperty.setAll(originalKeysProperty)
                packModel.xteaManager.replacedKeys.remove(regionId)
            }
        }
    }

    companion object {
        val ZERO_KEYS = listProperty(0, 0, 0, 0)
    }

}