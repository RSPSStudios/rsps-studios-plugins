package com.javatar.plugin.map.ui

import com.javatar.plugin.map.ui.model.MapInformationModel
import com.javatar.plugin.map.ui.model.MapPackerModel
import tornadofx.*

class MapTableViewFragment : Fragment() {

    val model: MapPackerModel by inject()

    override val root = tableview(model.maps) {
        bindSelected(model.selectedMap)
        column("Name", MapInformationModel::nameProperty) {
            makeEditable()
            setOnEditCommit {
                val item = it.rowValue.regionId
                model.namedRegions[item] = it.newValue
                model.save()
            }
        }
        column("Region ID", MapInformationModel::regionIdProperty)
        column("Region X", MapInformationModel::regionXProperty)
        column("Region Y", MapInformationModel::regionYProperty)
        column("Floors ID", MapInformationModel::floorsIdProperty)
        column("Objects ID", MapInformationModel::objectsIdProperty)
        column("Xtea Keys", MapInformationModel::keysProperty) {
            cellFormat {
                graphic = find<XteaKeyColumnFragment>("mapInfo" to this.rowItem).root
            }
        }
        smartResize()
    }


}