package gg.rsmod.plugins.content.quests.impl

import gg.rsmod.game.model.entity.Player
import gg.rsmod.plugins.api.Skills
import gg.rsmod.plugins.api.cfg.Items
import gg.rsmod.plugins.api.ext.getVarp
import gg.rsmod.plugins.api.ext.setVarp
import gg.rsmod.plugins.content.quests.*

/**
 * @author Ilwyd <https://github.com/ilwyd>
 */

object TheBloodPact : Quest( // Adds Quest Info
    name = "The Blood Pact",
    startPoint = "Speak to Xenia in the graveyard of Lumbridge Chruch.",
    requirements = emptyList(),
    requiredItems = "None.",
    combat = "Must be able to defeat three low-level enemies.",
    rewards =
        "1 Quest Point; a sword, chargebow, and magic staff; 100 Attack, Strength, Defence, Ranged and Magic " +
            "XP; access to the Lumbridge Catacombs dungeon",
    pointReward = 1,
    questId = 7238,
    spriteId = 3176,
    slot = 170,
    stages = 60,
) {
    init { // inits the quest to the server
        addQuest(this)
    }

    override fun getObjective(
        player: Player,
        stage: Int,
    ): QuestStage =
        when (stage) {
            1 ->
                QuestStage( // Quest Journal Stage 1
                    objectives =
                        listOf(),
                )
            else -> TODO()
        }

    override fun finishQuest(player: Player) {
        player.advanceToNextStage(this)
        player.addXp(Skills.ATTACK, 100.0)
        player.addXp(Skills.STRENGTH, 100.0)
        player.addXp(Skills.DEFENCE, 100.0)
        player.addXp(Skills.RANGED, 100.0)
        player.addXp(Skills.MAGIC, 100.0)
        player.setVarp(QUEST_POINT_VARP, player.getVarp(QUEST_POINT_VARP).plus(pointReward))
        player.buildQuestFinish(
            this,
            item = Items.REESES_SWORD,
            rewards =
                arrayOf(
                    "1 Quest Point",
                    "Kayle's chargebow, Caitlin's staff, and",
                    "Reese's sword.",
                    "100 Attack, Strength, Defence, Ranged and",
                    "Magic XP",
                    "Access to the Lumbridge Catacombs",
                    "1 Quest Point",
                ),
        )
    }
}
