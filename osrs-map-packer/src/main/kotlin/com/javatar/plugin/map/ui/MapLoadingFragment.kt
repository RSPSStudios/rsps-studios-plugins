package com.javatar.plugin.map.ui

import com.javatar.plugin.map.ui.model.MapPackerModel
import javafx.geometry.Pos
import tornadofx.*

class MapLoadingFragment : Fragment("Loading Maps") {

    val model: MapPackerModel by inject()

    override fun onDock() {
        model.findXteas(this@MapLoadingFragment)
        runAsync {
            model.findRegions()
        } ui {
            close()
        }
    }

    override val root = vbox {
        style {
            baseColor = c("#3f474f")
        }
        spacing = 10.0
        label("Loading Map Information")
        hbox {
            spacing = 10.0
            progressbar {
                progressProperty().bind(model.loadingProgress)
            }
            progressindicator {
                progressProperty().bind(model.loadingProgress)
            }
        }
        alignment = Pos.CENTER
    }

    override fun onBeforeShow() {

    }
}