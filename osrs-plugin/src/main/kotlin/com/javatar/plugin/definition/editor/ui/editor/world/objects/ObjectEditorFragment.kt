package com.javatar.plugin.definition.editor.ui.editor.world.objects

import com.javatar.api.http.Client
import com.javatar.api.http.StringBody
import com.javatar.api.ui.models.AccountModel
import com.javatar.osrs.definitions.impl.ObjectDefinition
import com.javatar.osrs.definitions.loaders.ObjectLoader
import com.javatar.plugin.definition.editor.OsrsDefinitionEditor
import com.javatar.plugin.definition.editor.managers.ConfigDefinitionManager
import com.javatar.plugin.definition.editor.ui.editor.world.objects.models.ObjectEditorModel
import com.javatar.plugin.definition.editor.ui.editor.world.objects.tabs.ObjectActionsFragment
import com.javatar.plugin.definition.editor.ui.editor.world.objects.tabs.ObjectConfigsFragment
import com.javatar.plugin.definition.editor.ui.editor.world.objects.tabs.ObjectVariablesFragment
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.javafx.JavaFx
import tornadofx.*

class ObjectEditorFragment : Fragment("World Object Editor") {

    val model: ObjectEditorModel by inject()

    val accountModel: AccountModel by di()
    val client: Client by di()

    val objects = ConfigDefinitionManager(ObjectLoader())

    init {
        model.cache.onChange {
            if (it != null) {
                loadObjects()
            }
        }
        model.searchText.onChange {
            if (it == null || it.isEmpty()) {
                model.objects.setAll(objects.definitions.values)
            }
        }
    }

    override val root = hbox {
        prefWidth = 800.0
        fitToParentHeight()
        style {
            baseColor = c("#3f474f")
        }
        vbox {
            fitToParentHeight()
            hbox {
                button("Pack Object") {
                    disableWhen(model.selected.isNull)
                    action {
                        val creds = accountModel.activeCredentials.get()
                        val cache = model.cache.get()
                        if (creds != null && cache != null) {
                            model.commitObject()
                            val obj = model.selected.get()
                            val json = OsrsDefinitionEditor.gson.toJson(obj)
                            client.post<ByteArray>("tools/osrs/objects", StringBody(json), creds)
                                .catch {
                                    alert(Alert.AlertType.ERROR, "Error packing Object", it.message)
                                    emit(byteArrayOf())
                                }
                                .onEach {
                                    if (it.isNotEmpty()) {
                                        cache.put(2, 6, obj.id, it)
                                        cache.index(2).update()
                                        println(cache.path)
                                        alert(
                                            Alert.AlertType.INFORMATION,
                                            "Packing Object ${obj.id}",
                                            "Successfully packed Object ${obj.id}."
                                        )
                                    }
                                }.launchIn(CoroutineScope(Dispatchers.JavaFx))
                        }
                    }
                }
                button("Add Object") {
                    action {
                        val newId = objects.nextId
                        val def = ObjectDefinition()
                        def.id = newId
                        objects.add(def)
                        model.objects.add(def)
                    }
                }
                button("Delete Object") {
                    disableWhen(model.selected.isNull)
                    action {
                        val obj = model.selected.get()
                        if(obj != null) {
                            val alert = alert(
                                Alert.AlertType.CONFIRMATION,
                                "Delete ${obj.name ?: obj.id}",
                                "Are you sure you want to delete ${obj.name ?: obj.id}",
                                buttons = arrayOf(ButtonType.CANCEL, ButtonType.YES)
                            )
                            if(alert.result === ButtonType.YES) {
                                model.objects.remove(obj)
                                objects.remove(obj)
                                val cache = model.cache.get()
                                if(cache != null) {
                                    cache.remove(2, 6, obj.id)
                                    cache.index(2).update()
                                }
                            }
                        }
                    }
                }
            }
            textfield(model.searchText) {
                action {
                    searchObjects(text)
                }
            }
            listview(model.objects) {
                prefHeightProperty().bind(this@vbox.heightProperty())
                model.selected.bind(selectionModel.selectedItemProperty())
                cellFormat {
                    text = if (it.name == null || it.name == "null") {
                        "Object ${it.id}"
                    } else {
                        "${it.name} - ${it.id}"
                    }
                }
            }
        }
        tabpane {
            disableWhen(model.selected.isNull)
            tab<ObjectConfigsFragment> {
                closableProperty().bind(false.toProperty())
            }
            tab<ObjectActionsFragment> {
                closableProperty().bind(false.toProperty())
            }
            tab<ObjectVariablesFragment> {
                closableProperty().bind(false.toProperty())
            }
        }
    }

    private fun loadObjects() {
        val cache = model.cache.get()
        if (cache != null) {
            val objectIds = cache.index(2).archive(6)?.fileIds() ?: intArrayOf()
            val list = mutableListOf<ObjectDefinition>()
            for (objectId in objectIds) {
                val data = cache.data(2, 6, objectId)
                if (data != null) {
                    list.add(objects.load(objectId, data))
                }
            }
            model.objects.setAll(list)
        }
    }

    private fun searchObjects(text: String) {
        if (text.isEmpty()) {
            model.objects.setAll(objects.definitions.values)
            return
        }
        when {
            text.endsWith(":var") -> {
                val identifier = text.split(":")[0]
                val list = if (identifier.isNotEmpty()) {
                    val id = text.split(":")[0].toInt()
                    objects.definitions.values.filter {
                        it.varbitID == id || it.varpID == id
                    }
                } else {
                    objects.definitions.values.filter {
                        it.varbitID != -1 || it.varpID != -1
                    }
                }
                model.objects.setAll(list)
            }
            text.endsWith(":anim") -> {
                val identifier = text.split(":")[0]
                val list = if (identifier.isNotEmpty()) {
                    val id = text.split(":")[0].toInt()
                    objects.definitions.values.filter {
                        it.animationID == id
                    }
                } else {
                    objects.definitions.values.filter {
                        it.animationID != -1
                    }
                }
                model.objects.setAll(list)
            }
            text.endsWith(":sound") -> {
                val identifier = text.split(":")[0]
                val list = if (identifier.isNotEmpty()) {
                    val id = text.split(":")[0].toInt()
                    objects.definitions.values.filter {
                        it.ambientSoundId == id || (it.soundEffectIds != null && id in it.soundEffectIds)
                    }
                } else {
                    objects.definitions.values.filter {
                        it.ambientSoundId != -1 || (it.soundEffectIds != null && it.soundEffectIds.isNotEmpty())
                    }
                }
                model.objects.setAll(list)
            }
            text.isInt() -> {
                val id = text.toInt()
                model.objects.setAll(objects.definitions[id])
            }
            else -> {
                val list = objects.definitions.values.filter {
                    it.name != null && it.name.lowercase().contains(text.lowercase())
                }
                model.objects.setAll(list)
            }
        }
    }

}