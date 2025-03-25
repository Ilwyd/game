package gg.rsmod.plugins.content.npcs.definitions.humanoids

import gg.rsmod.plugins.content.combat.CombatAnimation

/**
 * Handles Kayle's definitions
 * @author Ilwyd <https://github.com/ilwyd>
 */

val NPC_ID = Npcs.KAYLE

on_npc_pre_death(NPC_ID) {
    val p = npc.damageMap.getMostDamage()!! as Player
    p.playSound(Sfx.HUMAN_DEATH)
}

on_npc_death(NPC_ID) {
    world.spawn(
        Npc(
            Npcs.KAYLE_9629,
            npc.tile,
            world,
        ),
    )
}

set_combat_def(npc = NPC_ID) {
    configs {
        attackSpeed = 4
        respawnDelay = 0
    }
    stats {
        hitpoints = 150
        attack = 1
        ranged = 1
        magic = 1
        strength = 1
        defence = 1
    }
    anims {
        attack =
            CombatAnimation.SLING.combatstyle[CombatStyle.FIRST.id]
                .attackAnimation.id
        block = CombatAnimation.SLING.blockAnimation.id
        death = 12855
    }
}
