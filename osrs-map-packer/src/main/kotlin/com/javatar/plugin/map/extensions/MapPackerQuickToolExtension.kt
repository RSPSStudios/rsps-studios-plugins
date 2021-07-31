package com.javatar.plugin.map.extensions

import com.displee.cache.CacheLibrary
import com.javatar.api.ui.fs.QuickToolExtension
import com.javatar.plugin.map.ui.MapPackerFragment
import com.javatar.plugin.map.ui.model.MapPackerModel
import javafx.scene.control.Menu
import javafx.stage.FileChooser
import org.pf4j.Extension
import tornadofx.Fragment
import tornadofx.action
import tornadofx.find
import tornadofx.item

@Extension
class MapPackerQuickToolExtension : QuickToolExtension {
    override fun applyQuickTool(menu: Menu, cachePath: String) {
        menu.item("Map Packer").action {
            find<MapPackerFragment>().apply {
                findXteas(model, this)
                model.cache.set(CacheLibrary.create(cachePath))
            }.openModal(block = true, escapeClosesWindow = false)
        }
    }

    private fun findXteas(model: MapPackerModel, fragment: Fragment) {
        if(model.xteaLocation.get() == null || !model.xteaManager.load(model.xteaLocation.get())) {
            val fileChooser = FileChooser()
            val file = fileChooser.showOpenDialog(fragment.currentWindow)
            model.xteaLocation.set(file.absolutePath)
            if(!model.xteaManager.load(file.absolutePath)) {
                findXteas(model, fragment)
            } else {
                model.save()
            }
        }
    }
}