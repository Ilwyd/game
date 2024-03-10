package gg.rsmod.game.sync.task

import gg.rsmod.game.message.impl.RebuildNormalMessage
import gg.rsmod.game.message.impl.RebuildRegionMessage
import gg.rsmod.game.model.Coordinate
import gg.rsmod.game.model.Tile
import gg.rsmod.game.model.entity.Player
import gg.rsmod.game.model.region.Chunk
import gg.rsmod.game.sync.SynchronizationTask
import net.runelite.cache.region.Region

/**
 * @author Tom <rspsmods@gmail.com>
 */
object PlayerPreSynchronizationTask : SynchronizationTask<Player> {

    override fun run(pawn: Player) {
        pawn.handleFutureRoute()
        pawn.movementQueue.cycle()

        val last = pawn.lastKnownRegionBase
        val current = pawn.tile

        if (last == null || shouldRebuildRegion(last, current)) {
            val regionX = ((current.x shr 3) - (Chunk.MAX_VIEWPORT shr 4)) shl 3
            val regionZ = ((current.z shr 3) - (Chunk.MAX_VIEWPORT shr 4)) shl 3

            pawn.lastKnownRegionBase = Coordinate(regionX, regionZ, current.height)
            val instance = pawn.world.instanceAllocator.getMap(current)
            val xteaKeyService = pawn.world.xteaKeyService

            val rebuildMessage = when {
                instance != null -> {
                    val chunkX = pawn.tile.chunkOffsetX
                    val chunkZ = pawn.tile.chunkOffsetZ
                    val forceReload = 1
                    val mapSize = pawn.mapSize
                    val chunks = instance.getCoordinates(pawn.tile)
                    val xteas = mutableListOf<IntArray>()
                    val playerIndex = pawn.index
                    val playerTile = pawn.tile.as30BitInteger

                    val regions = hashSetOf<Int>()
                    chunks.forEach { copiedCoords ->
                        if (copiedCoords == -1) {
                            return@forEach
                        }
                        val rx = copiedCoords shr 14 and 0x3FF
                        val rz = copiedCoords shr 3 and 0x7FF
                        val region = rz / 8 + (rx / 8 shl 8)
                        if (regions.add(region)) {
                            val keys = xteaKeyService!!.get(region)
                            xteas.add(keys)
                        }
                    }

                    val playerRegions = IntArray(2048 - 1)
                    pawn.world.players.forEach {player ->
                        playerRegions[player.index] = (player.tile.asTileHashMultiplier)
                    }

                    println("Sending 128")
                    RebuildRegionMessage(
                        chunkX,
                        chunkZ,
                        forceReload,
                        mapSize,
                        chunks,
                        xteas.toTypedArray(),
                        playerIndex,
                        playerTile,
                        playerRegions

//                    instance.getCoordinates(pawn.tile),
//                    xteaService
                    )
                }
                else -> RebuildNormalMessage(pawn.mapSize, if(pawn.forceMapRefresh) 1 else 0, current.x shr 3, current.z shr 3, xteaKeyService)
            }
            pawn.write(rebuildMessage)
        }
    }

    private fun shouldRebuildRegion(old: Coordinate, new: Tile): Boolean {
        val dx = new.x - old.x
        val dz = new.z - old.z

        return dx <= Player.NORMAL_VIEW_DISTANCE || dx >= Chunk.MAX_VIEWPORT - Player.NORMAL_VIEW_DISTANCE - 1
                || dz <= Player.NORMAL_VIEW_DISTANCE || dz >= Chunk.MAX_VIEWPORT - Player.NORMAL_VIEW_DISTANCE - 1
    }
}