package com.javatar.plugin.map.data

import java.io.*

object PackMapData {

    fun pack(name: String = "", regionKey: RegionKey, objectsData: ByteArray, floorsData: ByteArray) : ByteArray {
        val byteOut = ByteArrayOutputStream()
        val out = DataOutputStream(byteOut)
        out.writeUTF(name)
        out.writeInt(regionKey.mapsquare)
        if(regionKey.key.isEmpty()) {
            out.writeInt(0)
        } else {
            regionKey.key.forEach { out.writeInt(it) }
        }
        out.writeInt(objectsData.size)
        out.write(objectsData)
        out.write(floorsData)
        return byteOut.toByteArray()
    }

    fun unpack(file: File) : MapData {
        val byteInput = ByteArrayInputStream(file.readBytes())
        val input = DataInputStream(byteInput)
        val regionKey = IntArray(4)

        val name = input.readUTF()
        val regionId = input.readInt()
        val keyPart = input.readInt()
        if(keyPart != 0) {
            regionKey[0] = keyPart
            regionKey[1] = input.readInt()
            regionKey[2] = input.readInt()
            regionKey[3] = input.readInt()
        } else {
            regionKey.fill(0)
        }
        val offset = input.readInt()
        val objectsData = ByteArray(offset)
        val floorData = ByteArray(input.available() - offset)
        input.read(objectsData)
        input.read(floorData)
        return MapData(name, regionId, regionKey, objectsData, floorData)
    }

}