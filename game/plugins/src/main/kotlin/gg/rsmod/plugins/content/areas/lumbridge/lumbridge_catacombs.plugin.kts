import gg.rsmod.game.model.instance.InstancedMap
import gg.rsmod.plugins.content.combat.CombatAnimation
import gg.rsmod.plugins.content.quests.advanceToNextStage
import gg.rsmod.plugins.content.quests.finishedQuest
import gg.rsmod.plugins.content.quests.getCurrentStage
import gg.rsmod.plugins.content.quests.impl.TheBloodPact

val FADE_IN_INTERFACE = 170
val FADE_OUT_INTERFACE = 115

on_obj_option(Objs.CATACOMB_ENTRANCE, "Climb-down") {
    if (player.finishedQuest(TheBloodPact)) {
        player.moveTo(3877, 5526, 1)
    } else if (player.getCurrentStage(TheBloodPact) == 1) {
        val instance = buildInstance(player)
        initialCutscene(player, instance)
    } else if (player.getCurrentStage(TheBloodPact) == 2) {
        val instance = buildInstance(player)
        fightKayle(player, instance)
    }
}

fun buildInstance(player: Player): InstancedMap {
    return world.createTemporaryInstance(player, 15446)!!
}

fun initialCutscene(
    player: Player,
    instance: InstancedMap,
) {
    player.lockingQueue { // TODO: Block player input during this sequence. Clicking softlocks the player
        player.openInterface(FADE_OUT_INTERFACE, InterfaceDestination.MAIN_SCREEN_OVERLAY)
        wait(3)

        val bottomLeftX = instance.area.bottomLeftX
        val bottomLeftZ = instance.area.bottomLeftZ
        player.moveTo(Tile(bottomLeftX + 37, bottomLeftZ + 20, 1))

        val kayle = Npc(Npcs.KAYLE_9639, Tile(bottomLeftX + 36, bottomLeftZ + 28, 1), player.world)
        val reese = Npc(Npcs.REESE_9637, Tile(bottomLeftX + 37, bottomLeftZ + 27, 1), player.world)
        val caitlin = Npc(Npcs.CAITLIN_9638, Tile(bottomLeftX + 38, bottomLeftZ + 28, 1), player.world)
        val ilona = Npc(Npcs.ILONA_9640, Tile(bottomLeftX + 37, bottomLeftZ + 29, 1), player.world)

        world.spawn(kayle)
        world.spawn(reese)
        world.spawn(caitlin)
        world.spawn(ilona)

        wait(3)
        player.moveCameraTo(100, 52, 73, 300, 100)
        player.cameraLookAt(53, 60, 50, 100, 100)
        player.moveTo(bottomLeftX + 43, bottomLeftZ + 34, 1)
        wait(1)
        kayle.walkTo(this, Tile(bottomLeftX + 36, bottomLeftZ + 32, 1))
        var reeseRoute = reese.walkTo(this, Tile(bottomLeftX + 37, bottomLeftZ + 37, 1))
        caitlin.walkTo(this, Tile(bottomLeftX + 38, bottomLeftZ + 37, 1))
        ilona.walkTo(this, Tile(bottomLeftX + 37, bottomLeftZ + 38, 1))
        player.openInterface(FADE_IN_INTERFACE, InterfaceDestination.MAIN_SCREEN_OVERLAY)
        wait {
            reese.tile == reeseRoute.tail
        }
        reese.faceTile(Tile(bottomLeftX + 36, bottomLeftZ + 32, 1)) // Have to use faceTile here, too far
        this.chatNpc(
            "Come on, Kayle! We don't have forever.",
            npc = Npcs.REESE,
        )
        kayle.walkTo(this, Tile(bottomLeftX + 36, bottomLeftZ + 36, 1))
        this.chatNpc(
            "Look, Reese; are you sure about this? There must be some other way we can...",
            npc = Npcs.KAYLE,
            wrap = true,
        )
        reese.facePawn(kayle)
        this.chatNpc(
            "We made a blood pact, Kayle! The three of us are in this all the way.",
            npc = Npcs.REESE,
            wrap = true,
        )
        kayle.facePawn(reese)
        this.chatNpc(
            "Yes, but...",
            npc = Npcs.KAYLE,
        )
        caitlin.facePawn(reese)
        this.chatNpc(
            "Do we have to take this idiot?",
            npc = Npcs.CAITLIN,
        )
        reese.facePawn(caitlin)
        this.chatNpc(
            "Yes! The blood pact! You read the book!",
            npc = Npcs.REESE,
        )
        this.chatNpc(
            "Let me go! I didn't make any blood pact with-",
            npc = Npcs.ILONA,
        )
        reese.facePawn(ilona)
        this.chatNpc(
            "Shut up!",
            npc = Npcs.REESE,
        )
        reese.facePawn(kayle)
        this.chatNpc(
            "Kayle, you stay here. Guard the door.",
            npc = Npcs.REESE,
        )
        reese.facePawn(ilona)
        this.chatNpc(
            "You, come on.",
            npc = Npcs.REESE,
        )
        reese.resetFacePawn()
        caitlin.resetFacePawn()
        caitlin.walkTo(this, Tile(bottomLeftX + 38, bottomLeftZ + 42, 1))
        ilona.walkTo(this, Tile(bottomLeftX + 37, bottomLeftZ + 42, 1))
        wait(1)
        reeseRoute = reese.walkTo(this, Tile(bottomLeftX + 37, bottomLeftZ + 41, 1))
        player.openInterface(FADE_OUT_INTERFACE, InterfaceDestination.MAIN_SCREEN_OVERLAY)
        wait { reese.tile == reeseRoute.tail }

        world.remove(reese)
        world.remove(kayle)
        world.remove(caitlin)
        world.remove(ilona)

        player.advanceToNextStage(TheBloodPact)
        player.resetCamera()
        fightKayle(player, instance)
    }
}

fun fightKayle(
    player: Player,
    instance: InstancedMap,
) {
    player.openInterface(FADE_IN_INTERFACE, InterfaceDestination.MAIN_SCREEN_OVERLAY)
    val bottomLeftX = instance.area.bottomLeftX
    val bottomLeftZ = instance.area.bottomLeftZ

    player.moveTo(bottomLeftX + 37, bottomLeftZ + 23, 1)

    val xenia = Npc(Npcs.XENIA, Tile(bottomLeftX + 37, bottomLeftZ + 23, 1), player.world)
    val kayle = Npc(Npcs.KAYLE, Tile(bottomLeftX + 37, bottomLeftZ + 40, 1), player.world)
    xenia.facePawn(player)
    kayle.faceTile(Tile(bottomLeftX + 37, bottomLeftZ + 22, 1))
    world.spawn(xenia)
    world.spawn(kayle)

    xenia.queue {
        while (true) {
            xenia.walkTo(player.tile, MovementQueue.StepType.FORCED_RUN)
            wait(1)
        }
    }

    kayle.queue {
        wait { kayle.sees(xenia, 13) }
        xenia.hit(1, HitType.RANGE)
        xenia.animate(CombatAnimation.UNARMED.blockAnimation.id)
        xenia.setCurrentLifepoints(2)
    }
}
