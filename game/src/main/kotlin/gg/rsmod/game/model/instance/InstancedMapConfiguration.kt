package gg.rsmod.game.model.instance

import gg.rsmod.game.model.PlayerUID
import gg.rsmod.game.model.Tile
import gg.rsmod.game.model.World
import gg.rsmod.game.model.entity.Npc
import java.util.*

/**
 * Configurations required to construct a new [InstancedMap].
 *
 * @see InstancedMap
 *
 * @param bypassObjectChunkBounds
 * If true, objects that are found to exceed the bounds of its [Chunk] will
 * not throw an error - however the object will not be applied to the world's
 * [gg.rsmod.game.model.region.ChunkSet], so this flag should be used with
 * that caveat in mind.
 *
 * Explanation:
 * In certain scenarios, an object's tile can overextend its original [Chunk]
 * where it would be placed in the [InstancedMap]; this can occur in any object
 * who's width or length is greater than 1 (one).
 *
 * Example:
 * - 2x2 object is in the local tile of 2,7 (in respect to its [Chunk])
 * - The [InstancedChunk.rot] is set to 2 (two)
 * - The outcome local tile would be 2,-1
 *
 * The outcome local tile would be out-of-bounds in its [Chunk] and would
 * lead to undesired behaviour.
 *
 * @author Tom <rspsmods@gmail.com>
 */
class InstancedMapConfiguration private constructor(
    val exitTile: Tile,
    val owner: PlayerUID?,
    val attributes: EnumSet<InstancedMapAttribute>,
    val bypassObjectChunkBounds: Boolean,
    val npcs: List<Npc>,
) {
    class Builder {
        private var exitTile: Tile? = null

        private var owner: PlayerUID? = null

        private val attributes = EnumSet.noneOf(InstancedMapAttribute::class.java)

        private var bypassObjectChunkBounds: Boolean = false

        private val npcs = mutableListOf<Npc>()

        fun build(): InstancedMapConfiguration {
            val ownerRequired =
                EnumSet.of(
                    InstancedMapAttribute.DEALLOCATE_ON_LOGOUT,
                    InstancedMapAttribute.DEALLOCATE_ON_DEATH,
                )

            checkNotNull(exitTile) { "Exit tile must be set." }
            check(
                owner != null || attributes.none { it in ownerRequired },
            ) { "One or more attributes require an owner to be set." }

            return InstancedMapConfiguration(exitTile!!, owner, attributes, bypassObjectChunkBounds, npcs)
        }

        fun setExitTile(tile: Tile): Builder {
            this.exitTile = tile
            return this
        }

        fun setOwner(owner: PlayerUID): Builder {
            this.owner = owner
            return this
        }

        fun addAttribute(
            attribute: InstancedMapAttribute,
            vararg others: InstancedMapAttribute,
        ): Builder {
            attributes.add(attribute)
            attributes.addAll(others)
            return this
        }

        /**
         * Adds an NPC to a list that will be spawned when the instance is allocated.
         * Tile positions for the NPCs should be the relative coordinates based on the
         * bottom left tile of the instance.
         */
        fun addNpc(npc: Npc) {
            npcs.add(npc)
        }

        fun addNpc(
            id: Int,
            relativeX: Int,
            relativeZ: Int,
            height: Int,
            world: World,
        ) {
            addNpc(Npc(id, Tile(relativeX, relativeZ, height), world))
        }

        /**
         * @see InstancedMapConfiguration.bypassObjectChunkBounds
         */
        fun setBypassObjectChunkBounds(bypassObjectChunkBounds: Boolean): Builder {
            this.bypassObjectChunkBounds = bypassObjectChunkBounds
            return this
        }
    }
}
