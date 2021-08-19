package com.javatar.plugin.map.data

import java.io.*
import java.nio.ByteBuffer

object PackMapData {

    fun pack(
        name: String = "",
        regionKey: RegionKey,
        objectsId: Int,
        floorsId: Int,
        objectsData: ByteArray,
        floorsData: ByteArray
    ): ByteArray {
        val byteOut = ByteArrayOutputStream()
        val out = DataOutputStream(byteOut)
        out.writeUTF(name)
        out.writeInt(regionKey.mapsquare)
        if (regionKey.key.isEmpty()) {
            out.writeInt(0)
        } else {
            regionKey.key.forEach { out.writeInt(it) }
        }
        out.writeInt(objectsId)
        out.writeInt(floorsId)
        out.writeInt(objectsData.size)
        out.write(objectsData)
        out.write(floorsData)
        return byteOut.toByteArray()
    }

    fun unpack(file: File): MapData {
        val byteInput = ByteArrayInputStream(file.readBytes())
        val input = DataInputStream(byteInput)
        val regionKey = IntArray(4)

        val name = input.readUTF()
        val regionId = input.readInt()
        val keyPart = input.readInt()
        if (keyPart != 0) {
            regionKey[0] = keyPart
            regionKey[1] = input.readInt()
            regionKey[2] = input.readInt()
            regionKey[3] = input.readInt()
        } else {
            regionKey.fill(0)
        }
        val objectsId = input.readInt()
        val floorsId = input.readInt()
        val offset = input.readInt()
        val objectsData = ByteArray(offset)
        val floorData = ByteArray(input.available() - offset)
        input.read(objectsData)
        input.read(floorData)
        return MapData(name, regionId, regionKey, objectsId, floorsId, objectsData, floorData)
    }

    fun unpackRSPSiPackFile(file: File): List<MapData> {
        val list = mutableListOf<MapData>()
        val buffer = ByteBuffer.wrap(file.readBytes())
        val size = buffer.int
        for (i in 0 until size) {
            val objectMapId = buffer.int
            val tileMapId = buffer.int
            val positionX = buffer.int
            val positionY = buffer.int
            val cX = (0 + 64 * positionX) / 64
            val cY = (0 + 64 * positionY) / 64
            val hash = (cX shl 8) + cY
            val objLen = buffer.int
            val objData = ByteArray(objLen)
            buffer[objData]
            val landscapeLen = buffer.int
            val landscapeData = ByteArray(landscapeLen)
            buffer[landscapeData]
            val offsetX = 64 * positionX
            val offsetY = 64 * positionY
            val objectMapData = objData
            val tileMapData = landscapeData
            list.add(
                MapData(
                    "",
                    hash,
                    intArrayOf(0, 0, 0, 0),
                    objectMapId,
                    tileMapId,
                    objectMapData,
                    tileMapData
                )
            )
        }
        return list
    }

}