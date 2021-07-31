package com.javatar.plugin.definition.editor.fs.types.archives

import com.javatar.api.fs.ArchiveType
import com.javatar.api.fs.JFile
import com.javatar.api.fs.directories.RootDirectory
import javafx.scene.control.TabPane
import javafx.scene.image.ImageView

class NpcArchiveType : ArchiveType {
    override val indexId: Int = 2
    override val archiveId: Int = 9

    override fun cache(jfiles: List<JFile>, root: RootDirectory) {
    }

    override fun icon(file: JFile, root: RootDirectory): ImageView? {
        return null
    }

    override fun identifier(file: JFile, root: RootDirectory): String {
        return "Npcs"
    }

    override fun open(file: JFile, root: RootDirectory, editorPane: TabPane) {
    }

    override fun save(json: String, file: JFile, root: RootDirectory) {
    }
}