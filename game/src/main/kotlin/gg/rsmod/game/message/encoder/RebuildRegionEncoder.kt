package gg.rsmod.game.message.encoder

import gg.rsmod.game.message.MessageEncoder
import gg.rsmod.game.message.impl.RebuildRegionMessage
import gg.rsmod.net.packet.GamePacketBuilder
import io.netty.buffer.Unpooled

/**
 * @author Tom <rspsmods@gmail.com>
 */
class RebuildRegionEncoder : MessageEncoder<RebuildRegionMessage>() {

    override fun extract(message: RebuildRegionMessage, key: String): Number = when (key) {
        "chunkX" -> message.chunkX
        "chunkZ" -> message.chunkZ
        "map_size" -> message.mapSize
        "force_reload" -> message.forceReload
        "unknown_byte" -> 3
        else -> throw Exception("Unhandled value key.")
    }

    override fun extractBytes(message: RebuildRegionMessage, key: String): ByteArray = when (key) {
        "player_regions" -> {
            val bitBuffer = GamePacketBuilder()
            if (message.playerRegions != null && message.playerTile != null && message.playerIndex != null) {
                bitBuffer.switchToBitAccess()
                bitBuffer.putBits(30, message.playerTile)
                message.playerRegions.forEachIndexed { index, region ->
                    if (index != message.playerIndex) {
                        bitBuffer.putBits(18, region)
                    }
                }
                bitBuffer.switchToByteAccess()
            }
            ByteArray(bitBuffer.readableBytes)
        }
        "chunks" -> {
            val bitBuffer = GamePacketBuilder()
            bitBuffer.switchToBitAccess()
            message.chunks.forEach { chunk ->
                bitBuffer.putBits(1, if (chunk != null) 1 else 0)
                if(chunk != null) {
                    bitBuffer.putBits(26, chunk)
                }
            }

            ByteArray(bitBuffer.readableBytes)
        }
        "xteas" -> {
            val buffer = Unpooled.buffer(message.chunks.size * (Int.SIZE_BYTES * 4))

            message.xteas.forEach { xtea ->
                xtea.forEach { key ->
                    buffer.writeInt(key)
                }
            }

            ByteArray(buffer.readableBytes())
        }
        else -> throw Exception("Unhandled value key.")

//        "data" -> {
//            val xteaBuffer = Unpooled.buffer(message.coordinates.size * (Int.SIZE_BYTES * 4))
//
//            val regions = hashSetOf<Int>()
//            var xteaCount = 0
//            message.coordinates.forEach { copiedCoords ->
//                if (copiedCoords == -1) {
//                    return@forEach
//                }
//                val rx = copiedCoords shr 14 and 0x3FF
//                val rz = copiedCoords shr 3 and 0x7FF
//                val region = rz / 8 + (rx / 8 shl 8)
//                if (regions.add(region)) {
//                    val keys = message.xteaKeyService!!.get(region)
//                    for (xteaKey in keys) {
//                        xteaBuffer.writeInt(xteaKey) // Client always reads as int
//                    }
//                    xteaCount++
//                }
//            }
//
//            val bitBuf = GamePacketBuilder()
//            bitBuf.switchToBitAccess()
//            var index = 0
//            message.coordinates.forEach { copiedCoords ->
//                bitBuf.putBit(copiedCoords != -1)
//                if (copiedCoords != -1) {
//                    bitBuf.putBits(26, copiedCoords)
//                }
//                index++
//            }
//            bitBuf.switchToByteAccess()
//
//            val buf = Unpooled.buffer(Short.SIZE_BYTES + bitBuf.readableBytes + xteaBuffer.readableBytes())
//
//            /*
//             * Write the XTEA key count.
//             */
//            buf.writeShort(xteaCount)
//            /*
//             * Write the bit data.
//             */
//            buf.writeBytes(bitBuf.byteBuf)
//            /*
//             * Write the XTEA keys.
//             */
//            buf.writeBytes(xteaBuffer)
//
//            val data = ByteArray(buf.readableBytes())
//            buf.readBytes(data)
//            data
//        }
//
    }
}