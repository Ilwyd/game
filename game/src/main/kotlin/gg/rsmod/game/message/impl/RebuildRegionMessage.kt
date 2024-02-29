package gg.rsmod.game.message.impl

import gg.rsmod.game.message.Message
import gg.rsmod.game.model.region.Chunk
import gg.rsmod.game.model.region.ChunkSet
import gg.rsmod.game.service.xtea.XteaKeyService
import net.runelite.cache.definitions.MapDefinition.Tile

/**
 * @author Tom <rspsmods@gmail.com>
 */
data class RebuildRegionMessage(
    val chunkX: Int,
    val chunkZ: Int,
    val forceReload: Int,
    val mapSize: Int,
    val chunks: IntArray,
    val xteas: Array<IntArray>,
    val playerIndex: Int? = null,
    val playerTile: Int? = null,
    val playerRegions: IntArray? = null
) : Message {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RebuildRegionMessage

        if (chunkX != other.chunkX) return false
        if (chunkZ != other.chunkZ) return false
        if (forceReload != other.forceReload) return false
        if (mapSize != other.mapSize) return false
        if (!chunks.contentEquals(other.chunks)) return false
        if (!xteas.contentDeepEquals(other.xteas)) return false
        if (playerIndex != other.playerIndex) return false
        if (playerTile != other.playerTile) return false
        if (playerRegions != null) {
            if (other.playerRegions == null) return false
            if (!playerRegions.contentEquals(other.playerRegions)) return false
        } else if (other.playerRegions != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = chunkX
        result = 31 * result + chunkZ
        result = 31 * result + forceReload
        result = 31 * result + mapSize
        result = 31 * result + chunks.hashCode()
        result = 31 * result + xteas.contentDeepHashCode()
        result = 31 * result + (playerIndex ?: 0)
        result = 31 * result + (playerTile ?: 0)
        result = 31 * result + (playerRegions?.contentHashCode() ?: 0)
        return result
    }
}