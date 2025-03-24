import gg.rsmod.plugins.content.quests.finishedQuest
import gg.rsmod.plugins.content.quests.impl.TheBloodPact

val FADE_IN_INTERFACE = 170
val FADE_OUT_INTERFACE = 115

on_obj_option(Objs.CATACOMB_ENTRANCE, "Climb-down") {
    if (player.finishedQuest(TheBloodPact)) {
        player.moveTo(3877, 5526, 1)
    } else {
        initialCutscene(player)
    }
}

fun initialCutscene(player: Player) {
    player.queue {
        player.openInterface(FADE_OUT_INTERFACE, InterfaceDestination.MAIN_SCREEN_OVERLAY)
        wait(3)

        val instance = world.createTemporaryInstance(player, 15446)!!
        val bottomLeftX = instance.area.bottomLeftX
        val bottomLeftZ = instance.area.bottomLeftZ
        player.moveTo(Tile(bottomLeftX + 37, bottomLeftZ + 20, 1))

        val kayle = Npc(Npcs.KAYLE, Tile(bottomLeftX + 36, bottomLeftZ + 28, 1), player.world)
        val reese = Npc(Npcs.REESE, Tile(bottomLeftX + 37, bottomLeftZ + 27, 1), player.world)
        val caitlin = Npc(Npcs.CAITLIN, Tile(bottomLeftX + 38, bottomLeftZ + 28, 1), player.world)
        val ilona = Npc(Npcs.ILONA, Tile(bottomLeftX + 37, bottomLeftZ + 29, 1), player.world)

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
        val reeseRoute = reese.walkTo(this, Tile(bottomLeftX + 37, bottomLeftZ + 37, 1))
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
        reese.walkTo(this, Tile(bottomLeftX + 37, bottomLeftZ + 41, 1))
        player.openInterface(FADE_OUT_INTERFACE, InterfaceDestination.MAIN_SCREEN_OVERLAY)
    }
}
