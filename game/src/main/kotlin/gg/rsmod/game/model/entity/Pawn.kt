package gg.rsmod.game.model.entity

import gg.rsmod.game.action.NpcDeathAction
import gg.rsmod.game.action.PlayerDeathAction
import gg.rsmod.game.event.Event
import gg.rsmod.game.message.impl.SetMapFlagMessage
import gg.rsmod.game.model.*
import gg.rsmod.game.model.attr.*
import gg.rsmod.game.model.bits.INFINITE_VARS_STORAGE
import gg.rsmod.game.model.bits.InfiniteVarsType
import gg.rsmod.game.model.collision.CollisionManager
import gg.rsmod.game.model.combat.DamageMap
import gg.rsmod.game.model.path.FutureRoute
import gg.rsmod.game.model.path.PathFindingStrategy
import gg.rsmod.game.model.path.PathRequest
import gg.rsmod.game.model.path.Route
import gg.rsmod.game.model.path.strategy.BFSPathFindingStrategy
import gg.rsmod.game.model.path.strategy.SimplePathFindingStrategy
import gg.rsmod.game.model.queue.QueueTask
import gg.rsmod.game.model.queue.QueueTaskSet
import gg.rsmod.game.model.queue.TaskPriority
import gg.rsmod.game.model.queue.impl.PawnQueueTaskSet
import gg.rsmod.game.model.region.Chunk
import gg.rsmod.game.model.timer.*
import gg.rsmod.game.plugin.Plugin
import gg.rsmod.game.service.log.LoggerService
import gg.rsmod.game.sync.block.UpdateBlockBuffer
import gg.rsmod.game.sync.block.UpdateBlockType
import kotlinx.coroutines.CoroutineScope
import java.lang.System.currentTimeMillis
import java.lang.ref.WeakReference
import java.util.*

/**
 * A controllable character in the world that is used by something, or someone,
 * for their own purpose.
 *
 * @author Tom <rspsmods@gmail.com>
 */
abstract class Pawn(
    val world: World,
) : Entity() {
    /**
     * The index assigned when this [Pawn] is successfully added to a [PawnList].
     */
    var index = -1

    /**
     * @see UpdateBlockBuffer
     */
    internal var blockBuffer = UpdateBlockBuffer()

    /**
     * The 3D [Tile] that this pawn was standing on, in the last game cycle.
     */
    internal var lastTile: Tile? = null

    /**
     * The last tile that was set for the pawn's [gg.rsmod.game.model.region.Chunk].
     */
    internal var lastChunkTile: Tile? = null

    /**
     * Whether or not this pawn can be teleported this game cycle.
     */
    internal var moved = false

    /**
     * @see [MovementQueue]
     */
    val movementQueue by lazy { MovementQueue(this) }

    /**
     * The current directions that this pawn is moving.
     */
    internal var steps: MovementQueue.StepDirection? = null

    /**
     * The last [Direction] this pawn was facing.
     */
    internal var lastFacingDirection: Direction = Direction.SOUTH

    /**
     * A public getter property for [lastFacingDirection].
     */
    val faceDirection: Direction
        get() = lastFacingDirection

    /**
     * The current [LockState] which filters what actions this pawn can perform.
     */
    var lock = LockState.NONE

    /**
     * The attributes attached to the pawn.
     *
     * @see AttributeMap
     */
    val attr = AttributeMap()

    /**
     * The timers attached to the pawn.
     *
     * @see TimerMap
     */
    val timers = TimerMap()

    internal val queues: QueueTaskSet = PawnQueueTaskSet()

    /**
     * The equipment bonus for the pawn.
     */
    val equipmentBonuses = IntArray(18)

    /**
     * The current prayer icon that the pawn has active.
     */
    var prayerIcon = -1

    /**
     * Transmog is the action of turning into an npc. This value is equal to the
     * npc id of the npc you want to turn into, visually.
     */
    private var transmogId = -1

    /**
     * A list of pending [Hit]s.
     */
    private val pendingHits = mutableListOf<Hit>()

    /**
     * A [DamageMap] to keep track of who has dealt damage to this pawn.
     */
    val damageMap = DamageMap()

    /**
     * A flag which indicates if this pawn is visible to players in the world.
     */
    var invisible = false

    /**
     * A flag which indicates if this pawn has teleported.
     */
    var teleported = false

    /**
     * The [FutureRoute] for the pawn, if any.
     * @see createPathFindingStrategy
     */
    private var futureRoute: FutureRoute? = null

    var walkMask = 0

    internal var lastAnimation = 0L

    /**
     * Handles logic before any synchronization tasks are executed.
     */
    abstract fun cycle()

    fun isDead(): Boolean = getCurrentLifepoints() == 0

    fun isAlive(): Boolean = !isDead()

    abstract fun isRunning(): Boolean

    abstract fun getSize(): Int

    abstract fun getCurrentLifepoints(): Int

    abstract fun getMaximumLifepoints(): Int

    abstract fun setCurrentLifepoints(level: Int)

    abstract fun addBlock(block: UpdateBlockType)

    abstract fun hasBlock(block: UpdateBlockType): Boolean

    /**
     * Lock the pawn to the default [LockState.FULL] state.
     */
    fun lock() {
        lock = LockState.FULL
    }

    /**
     * Unlock the pawn and set it to [LockState.NONE] state.
     */
    fun unlock() {
        lock = LockState.NONE
    }

    /**
     * Checks if the pawn has any lock state set.
     */
    fun isLocked(): Boolean = lock != LockState.NONE

    fun getTransmogId(): Int = transmogId

    fun setTransmogId(transmogId: Int) {
        this.transmogId = transmogId
        addBlock(UpdateBlockType.APPEARANCE)
    }

    fun hasMoveDestination(): Boolean = futureRoute != null || movementQueue.hasDestination()

    fun stopMovement() {
        if (this is Player) {
            write(SetMapFlagMessage(255, 255))
        }
        movementQueue.clear()
    }

    fun getCentreTile(): Tile = tile.transform(getSize() shr 1, getSize() shr 1)

    /**
     * Gets the tile the pawn is currently facing towards.
     */
    // Credits: Kris#1337
    fun getFrontFacingTile(
        target: Tile,
        offset: Int = 0,
    ): Tile {
        val size = (getSize() shr 1)
        val centre = getCentreTile()

        val granularity = 2048
        val lutFactor = (granularity / (Math.PI * 2)) // Lookup table factor

        val theta = Math.atan2((target.z - centre.z).toDouble(), (target.x - centre.x).toDouble())
        var angle = Math.toDegrees((((theta * lutFactor).toInt() + offset) and (granularity - 1)) / lutFactor)
        if (angle < 0) {
            angle += 360
        }
        angle = Math.toRadians(angle)

        val tx = Math.round(centre.x + (size * Math.cos(angle))).toInt()
        val tz = Math.round(centre.z + (size * Math.sin(angle))).toInt()
        return Tile(tx, tz, tile.height)
    }

    /**
     * Alias for [getFrontFacingTile] using a [Pawn] as the target tile.
     */
    fun getFrontFacingTile(
        target: Pawn,
        offset: Int = 0,
    ): Tile = getFrontFacingTile(target.getCentreTile(), offset)

    /**
     * Initiate combat with [target].
     */
    fun attack(target: Pawn) {
        if (isAlive() && !invisible) {
            resetInteractions()
            interruptQueues()

            attr[COMBAT_TARGET_FOCUS_ATTR] = WeakReference(target)

            /*
             * Players always have the default combat, and npcs will use default
             * combat <strong>unless</strong> they have a custom npc combat plugin
             * bound to their npc id.
             */
            if (entityType.isPlayer || this is Npc && !world.plugins.executeNpcCombat(this)) {
                world.plugins.executeCombat(this)
            }
        }
    }

    fun addHit(hit: Hit) {
        pendingHits.add(hit)
    }

    fun clearHits() {
        pendingHits.clear()
    }

    /**
     * Handle a single cycle for [timers].
     */
    fun timerCycle() {
        val iterator = timers.getTimers().iterator()

        while (iterator.hasNext()) {
            val entry = iterator.next()
            val key = entry.key
            val time = entry.value
            val updatedTime = if (key.tickForward) time + 1 else time - 1
            entry.setValue(updatedTime)
            if (updatedTime <= 0 && !key.tickForward) {
                if (key == RESET_PAWN_FACING_TIMER) {
                    resetFacePawn()
                } else {
                    world.plugins.executeTimer(this, key)
                }
                if (!timers.has(key) && key.removeOnZero) {
                    iterator.remove()
                }
            }
        }
    }

    /**
     * Handle a single cycle for [pendingHits].
     */
    fun hitsCycle() {
        val hitIterator = pendingHits.iterator()
        iterator@ while (hitIterator.hasNext()) {
            if (isDead()) {
                break
            }
            val hit = hitIterator.next()

            if (lock.delaysDamage()) {
                hit.damageDelay = Math.max(0, hit.damageDelay - 1)
                continue
            }

            if (hit.damageDelay-- == 0) {
                if (!hit.cancelCondition()) {
                    blockBuffer.hits.add(hit)
                    addBlock(UpdateBlockType.HITMARK)

                    for (hitmark in hit.hitmarks) {
                        val hp = getCurrentLifepoints()
                        if (hitmark.damage > hp) {
                            hitmark.damage = hp
                        }
                        /*
                         * Only lower the pawn's hp if they do not have infinite
                         * health enabled.
                         */
                        if (INFINITE_VARS_STORAGE.get(this, InfiniteVarsType.HP) == 0) {
                            setCurrentLifepoints(hp - hitmark.damage)
                        }
                        /*
                         * If the pawn has less than or equal to 0 health,
                         * terminate all queues and begin the death logic.
                         */
                        if (getCurrentLifepoints() <= 0) {
                            hit.actions.forEach { action -> action(hit) }
                            if (entityType.isPlayer) {
                                executePlugin(PlayerDeathAction.deathPlugin)
                            } else {
                                executePlugin(NpcDeathAction.deathPlugin)
                            }
                            hitIterator.remove()
                            break@iterator
                        }
                    }
                    hit.actions.forEach { action -> action(hit) }
                }
                hitIterator.remove()
            }
        }
        if (isDead() && pendingHits.isNotEmpty()) {
            pendingHits.clear()
        }
    }

    /**
     * Handle the [futureRoute] if necessary.
     */
    fun handleFutureRoute() {
        if (futureRoute?.completed == true && futureRoute?.strategy?.cancel == false) {
            val futureRoute = futureRoute!!
            walkPath(futureRoute.route.path, futureRoute.stepType, futureRoute.detectCollision)
            this.futureRoute = null
        }
    }

    /**
     * Walk to all the tiles specified in our [path] queue, using [stepType] as
     * the [MovementQueue.StepType].
     */
    fun walkPath(
        path: Queue<Tile>,
        stepType: MovementQueue.StepType,
        detectCollision: Boolean,
    ) {
        if (path.isEmpty()) {
            if (this is Player) {
                write(SetMapFlagMessage(255, 255))
            }
            return
        }

        if (timers.has(FROZEN_TIMER)) {
            if (this is Player) {
                writeMessage(MAGIC_STOPS_YOU_FROM_MOVING)
            }
            return
        }

        if (timers.has(STUN_TIMER)) {
            return
        }

        movementQueue.clear()
        var tail: Tile? = null
        var next = path.poll()
        while (next != null) {
            movementQueue.addStep(next, stepType, detectCollision)
            val poll = path.poll()
            if (poll == null) {
                tail = next
            }
            next = poll
        }

        /*
         * If the tail is null (should never be unless we mess with code above), or
         * if the tail is the tile we're standing on, then we don't have to move at all!
         */
        if (tail == null || tail.sameAs(tile)) {
            if (this is Player) {
                write(SetMapFlagMessage(255, 255))
            }
            movementQueue.clear()
            return
        }

        if (this is Player && lastKnownRegionBase != null) {
            write(SetMapFlagMessage(tail.x - lastKnownRegionBase!!.x, tail.z - lastKnownRegionBase!!.z))
        }
    }

    fun walkTo(
        tile: Tile,
        stepType: MovementQueue.StepType = MovementQueue.StepType.NORMAL,
        detectCollision: Boolean = true,
    ) = walkTo(tile.x, tile.z, stepType, detectCollision)

    fun walkTo(
        x: Int,
        z: Int,
        stepType: MovementQueue.StepType = MovementQueue.StepType.NORMAL,
        detectCollision: Boolean = true,
    ) {
        /*
         * Already standing on requested destination.
         */
        if (tile.x == x && tile.z == z) {
            return
        }

        if (timers.has(FROZEN_TIMER)) {
            if (this is Player) {
                writeMessage(MAGIC_STOPS_YOU_FROM_MOVING)
            }
            return
        }

        if (timers.has(STUN_TIMER)) {
            return
        }

        val multiThread = world.multiThreadPathFinding
        val request = PathRequest.createWalkRequest(this, x, z, projectile = false, detectCollision = detectCollision)
        val strategy = createPathFindingStrategy(copyChunks = multiThread)

        /*
         * When using multi-thread path-finding, the [PathRequest.createWalkRequest]
         * must have the [tile] in sync with the game-thread, so we need to make sure
         * that in this cycle, the pawn's [tile] does not change. The easiest way to
         * do this is by clearing their movement queue. Though it can cause weird
         */
        if (multiThread) {
            movementQueue.clear()
        }
        futureRoute?.strategy?.cancel = true

        if (multiThread) {
            futureRoute = FutureRoute.of(strategy, request, stepType, detectCollision)
        } else {
            val route = strategy.calculateRoute(request)
            walkPath(route.path, stepType, detectCollision)
        }
    }

    suspend fun walkTo(
        it: QueueTask,
        tile: Tile,
        stepType: MovementQueue.StepType = MovementQueue.StepType.NORMAL,
        detectCollision: Boolean = true,
    ) = walkTo(it, tile.x, tile.z, stepType, detectCollision)

    suspend fun walkTo(
        it: QueueTask,
        x: Int,
        z: Int,
        stepType: MovementQueue.StepType = MovementQueue.StepType.NORMAL,
        detectCollision: Boolean = true,
    ): Route {
        /*
         * Already standing on requested destination.
         */
        if (tile.x == x && tile.z == z) {
            return Route(EMPTY_TILE_DEQUE, success = true, tail = Tile(tile))
        }
        val multiThread = world.multiThreadPathFinding
        val request = PathRequest.createWalkRequest(this, x, z, projectile = false, detectCollision = detectCollision)
        val strategy = createPathFindingStrategy(copyChunks = multiThread)

        movementQueue.clear()
        futureRoute?.strategy?.cancel = true

        if (multiThread) {
            futureRoute = FutureRoute.of(strategy, request, stepType, detectCollision)
            while (!futureRoute!!.completed) {
                it.wait(1)
            }
            return futureRoute!!.route
        }

        val route = strategy.calculateRoute(request)
        walkPath(route.path, stepType, detectCollision)
        return route
    }

    fun teleportTo(
        x: Int,
        z: Int,
        height: Int = 0,
    ) {
        moved = true
        blockBuffer.teleport = true
        tile = Tile(x, z, height)
        movementQueue.clear()
        addBlock(UpdateBlockType.MOVEMENT_TYPE)
        addBlock(UpdateBlockType.MOVEMENT)
    }

    fun teleportTo(tile: Tile) {
        teleportTo(tile.x, tile.z, tile.height)
    }

    fun teleportNpc(
        x: Int,
        z: Int,
        height: Int = 0,
    ) {
        moved = true
        teleported = true
        invisible = true
        blockBuffer.teleport = true
        tile = Tile(x, z, height)
        movementQueue.clear()
    }

    fun teleportNpc(tile: Tile) {
        teleportNpc(tile.x, tile.z, tile.height)
    }

    fun moveTo(
        x: Int,
        z: Int,
        height: Int = 0,
    ) {
        moved = true
        blockBuffer.teleport = !tile.isWithinRadius(x, z, height, Player.NORMAL_VIEW_DISTANCE)
        tile = Tile(x, z, height)
        movementQueue.clear()
        addBlock(UpdateBlockType.MOVEMENT_TYPE)
        addBlock(UpdateBlockType.MOVEMENT)
    }

    fun moveTo(tile: Tile) {
        moveTo(tile.x, tile.z, tile.height)
    }

    fun animate(
        id: Int,
        delay: Int = 0,
        idleOnly: Boolean = false,
        priority: Boolean = true,
    ) {
        if (!priority && lastAnimation > currentTimeMillis()) {
            return
        }
        if (id != -1) {
            lastAnimation = currentTimeMillis() + (world.getAnimationDelay(id) + 3)
        }
        blockBuffer.animation = id
        blockBuffer.animationDelay = delay
        blockBuffer.idleOnly = idleOnly
        addBlock(UpdateBlockType.ANIMATION)
    }

    fun resetAnimation() {
        blockBuffer.animation = -1
        blockBuffer.animationDelay = 0
        blockBuffer.idleOnly = false
        addBlock(UpdateBlockType.ANIMATION)
    }

    fun graphic(
        id: Int,
        height: Int = 0,
        delay: Int = 0,
        rotation: Int = 0,
    ) {
        blockBuffer.graphicId = id
        blockBuffer.graphicHeight = height
        blockBuffer.graphicDelay = delay
        blockBuffer.graphicRotation = rotation
        addBlock(UpdateBlockType.GFX)
    }

    fun graphic(graphic: Graphic) {
        graphic(graphic.id, graphic.height, graphic.delay, graphic.rotation)
    }

    fun forceChat(message: String) {
        blockBuffer.forceChat = message
        addBlock(UpdateBlockType.FORCE_CHAT)
    }

    @Suppress("UNUSED_PARAMETER")
    fun faceTile(
        face: Tile,
        width: Int = 1,
        length: Int = 1,
    ) {
        if (entityType.isPlayer) {
            val srcX = tile.x
            val srcZ = tile.z
            val dstX = face.x
            val dstZ = face.z

            var degreesX = (srcX - dstX).toDouble()
            var degreesZ = (srcZ - dstZ).toDouble()

            blockBuffer.faceDegrees = (Math.atan2(degreesX, degreesZ) * 2607.5945876176133).toInt() and 0x3fff
        } else if (entityType.isNpc) {
            val faceX = (face.x * 2) + 1
            val faceZ = (face.z * 2) + 1
            blockBuffer.faceDegrees = (faceX shl 16) or faceZ
        }

        blockBuffer.facePawnIndex = -1
        addBlock(UpdateBlockType.FACE_TILE)
    }

    fun facePawn(pawn: Pawn) {
        blockBuffer.faceDegrees = 0

        val index = if (pawn.entityType.isPlayer) pawn.index + 32768 else pawn.index
        if (blockBuffer.facePawnIndex != index) {
            blockBuffer.faceDegrees = 0
            blockBuffer.facePawnIndex = index
            addBlock(UpdateBlockType.FACE_PAWN)
        }

        attr[FACING_PAWN_ATTR] = WeakReference(pawn)
    }

    fun resetFacePawn() {
        blockBuffer.faceDegrees = 0

        val index = -1
        if (blockBuffer.facePawnIndex != index) {
            blockBuffer.faceDegrees = 0
            blockBuffer.facePawnIndex = index
            addBlock(UpdateBlockType.FACE_PAWN)
        }

        attr.remove(FACING_PAWN_ATTR)
    }

    /**
     * Resets any interaction this pawn had with another pawn.
     */
    fun resetInteractions() {
        attr.remove(COMBAT_TARGET_FOCUS_ATTR)
        attr.remove(INTERACTING_NPC_ATTR)
        attr.remove(INTERACTING_PLAYER_ATTR)
        resetFacePawn()
    }

    fun queue(
        priority: TaskPriority = TaskPriority.STANDARD,
        logic: suspend QueueTask.(CoroutineScope) -> Unit,
    ) {
        if (this is Player && priority == TaskPriority.STRONG) {
            this.closeInterfaceModal()
        }
        queues.queue(this, world.coroutineDispatcher, priority, logic)
    }

    /**
     * Adds a queue to the pawn that may lock
     * them while the queue executes, and upon completion
     * unlock the pawn
     */
    fun lockingQueue(
        priority: TaskPriority = TaskPriority.STANDARD,
        lockState: LockState = LockState.FULL,
        logic: suspend QueueTask.(CoroutineScope) -> Unit,
    ) {
        // set the lockstate
        lock = lockState

        if (this is Player && priority == TaskPriority.STRONG) {
            this.closeInterfaceModal()
        }
        queues.queue(this, world.coroutineDispatcher, priority, logic, lock = true)
    }

    /**
     * Terminates any on-going [QueueTask]s that are being executed by this [Pawn].
     */
    fun interruptQueues() {
        if (this is Player) {
            if (isResting()) {
                varps.setState(173, attr[LAST_KNOWN_RUN_STATE]!!.toInt())
            }
        }
        unlock()
        queues.terminateTasks()
    }

    /**
     * Terminates specific interactions/queues
     * based on parameters given
     */
    fun fullInterruption(
        movement: Boolean = false,
        interactions: Boolean = false,
        animations: Boolean = false,
        queue: Boolean = false,
    ) {
        if (this is Player) {
            if (isResting()) {
                varps.setState(173, attr[LAST_KNOWN_RUN_STATE]!!.toInt())
            }
        }
        unlock()
        if (movement) {
            stopMovement()
        }
        if (interactions) {
            resetInteractions()
        }
        if (animations) {
            animate(-1)
        }
        if (queue) {
            queues.terminateTasks()
        }
    }

    /**
     * Executes a plugin with this [Pawn] as its context.
     */
    fun <T> executePlugin(logic: Plugin.() -> T): T {
        val plugin = Plugin(this)
        return logic(plugin)
    }

    fun triggerEvent(event: Event) {
        world.plugins.executeEvent(this, event)
        world.getService(LoggerService::class.java, searchSubclasses = true)?.logEvent(this, event)
    }

    fun hasLineOfSightTo(
        other: Pawn,
        projectile: Boolean,
        maximumDistance: Int = 12,
    ): Boolean {
        if (this.tile.height != other.tile.height) {
            return false
        }

        if (this.tile.sameAs(other.tile)) {
            return true
        }

        if (this.tile.getDistance(other.tile) > maximumDistance) {
            return false
        }

        return this.world.collision.raycast(this.tile, other.tile, projectile)
    }

    fun faces(
        other: Pawn,
        maximumDistance: Int = 12,
    ): Boolean {
        if (this.tile.height != other.tile.height) {
            return false
        }

        if (this.tile.sameAs(other.tile)) {
            return true
        }

        if (this.tile.getDistance(other.tile) > maximumDistance) {
            return false
        }

        val deltaX = other.tile.x - this.tile.x
        val deltaZ = other.tile.z - this.tile.z

        return when (this.faceDirection) {
            Direction.NORTH_WEST -> deltaZ >= deltaX
            Direction.NORTH -> deltaZ >= 0
            Direction.NORTH_EAST -> deltaZ >= deltaX * -1
            Direction.WEST -> deltaX <= 0
            Direction.EAST -> deltaX >= 0
            Direction.SOUTH_WEST -> deltaZ <= deltaX * -1
            Direction.SOUTH -> deltaZ <= 0
            Direction.SOUTH_EAST -> deltaX >= deltaZ
            else -> false
        }
    }

    fun sees(
        other: Pawn,
        maximumDistance: Int,
    ) = faces(other, maximumDistance) && hasLineOfSightTo(other, true, maximumDistance)

    internal fun createPathFindingStrategy(copyChunks: Boolean = false): PathFindingStrategy {
        val collision: CollisionManager =
            if (copyChunks) {
                val chunks =
                    world.chunks.copyChunksWithinRadius(
                        tile.chunkCoords,
                        height = tile.height,
                        radius = Chunk.CHUNK_VIEW_RADIUS,
                    )
                CollisionManager(chunks, createChunksIfNeeded = false)
            } else {
                world.collision
            }
        return if (entityType.isPlayer) {
            BFSPathFindingStrategy(
                collision,
            )
        } else {
            SimplePathFindingStrategy(collision, this)
        }
    }

    companion object {
        private val EMPTY_TILE_DEQUE = ArrayDeque<Tile>()
    }
}
