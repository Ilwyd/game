import gg.rsmod.plugins.content.quests.canStartQuest
import gg.rsmod.plugins.content.quests.impl.TheBloodPact
import gg.rsmod.plugins.content.quests.startQuest
import gg.rsmod.plugins.content.quests.startedQuest

on_npc_option(Npcs.XENIA, "Talk-to") {
    player.queue {
        if (!player.startedQuest(TheBloodPact) && player.canStartQuest(TheBloodPact)) {
            this.chatNpc("I'm glad you've come by. I need some help.")
            preQuestDialogue(this)
        }
    }
}

suspend fun preQuestDialogue(task: QueueTask) {
    when (
        task.options(
            "What do you need help with?",
            "Who are you?",
            "How did you know who I am?",
            "Sorry, I've got to go.",
        )
    ) {
        1 -> {
            help(task)
        }
        2 -> {
            task.chatNpc("My name's Xenia. I'm an adventurer.")
            task.chatNpc(
                "I'm one of the old guard, I suppose. I helped found the Champions' Guild, and I've done a " +
                    "fair few quests in my time.",
                wrap = true,
            )
            task.chatNpc("Now I'm starting to get a bit old for action, which is why I need your help.", wrap = true)
            preQuestDialogue(task)
        }
        3 -> {
            task.chatNpc(
                " Oh, I have my ways. I get the feeling that you're one to watch; you could be quite the " +
                    "hero some day.",
                wrap = true,
            )
            preQuestDialogue(task)
        }
        4 -> {}
    }
}

suspend fun help(task: QueueTask) {
    task.chatPlayer("What do you need help with?", facialExpression = FacialExpression.CONFUSED)
    task.chatNpc(
        "Some cultists of Zamorak have gone into the catacombs with a prisoner. I don't know what they're " +
            "planning, but I'm pretty sure it's not a tea party.",
        wrap = true,
    )
    task.chatNpc(
        "There are three of them, and I'm not as young as I was the last time I was here. I don't want to go" +
            " down there without backup.",
        wrap = true,
    )
    choice(task)
}

suspend fun choice(task: QueueTask) {
    when (
        task.options(
            "I'll help you.",
            "I need to know more before I help you.",
        )
    ) {
        1 -> {
            task.player.startQuest(TheBloodPact)
            task.chatNpc("I knew you would!")
            task.chatNpc("We've got no time to lose. You head down the stairs, and I'll follow.", wrap = true)
        }
        2 -> {
            task.chatNpc(
                "Very wise. I got into a lot of trouble in my youth by rushing in without knowing a " +
                    "situation.",
                wrap = true,
            )
            knowMore(task)
        }
    }
}

suspend fun knowMore(task: QueueTask) {
    when (
        task.options(
            "Tell me more about these cultists.",
            "Who did they kidnap?",
            "What's down there?",
            "Is there a reward if I help you?",
            "Enough questions.",
        )
    ) {
        1 -> {
            task.chatNpc(
                "Lumbridge is a Saradominist town, but there will always be some people drawn to worship " +
                    "Zamorak. They must have found some ritual that they think will give them power over other people.",
                wrap = true,
            )
            knowMore(task)
        }
        2 -> {
            task.chatNpc(
                "A young woman named Ilona. She had just left Lumbridge to start training at the Wizards' " +
                    "Tower.",
                wrap = true,
            )
            task.chatNpc(
                "They just grabbed her on the road. If she'd been a full wizard then she'd have been able " +
                    "to fight them off, but without training she didn't have a chance.",
                wrap = true,
            )
            knowMore(task)
        }
        3 -> {
            task.chatNpc(
                "The catacombs of Lumbridge Church. The dead of Lumbridge have been buried there since.." +
                    ".well, for about forty years now.",
                wrap = true,
            )
            knowMore(task)
        }
        4 -> {
            task.chatNpc(
                "I can't offer anything myself, but I know the cultists all have weapons, and you'll be " +
                    "able to keep them if we succeed. This adventure would also help to train your combat skills.",
                wrap = true,
            )
            knowMore(task)
        }
        5 -> {
            task.chatNpc("So, will you help me, ${task.player.username}?")
            choice(task)
        }
    }
}
