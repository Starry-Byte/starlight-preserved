package studio.starrybyte.starlightperserved.logs

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import studio.starrybyte.starlightperserved.configs.Config
import studio.starrybyte.starlightperserved.configs.NotificationMode

class Broadcaster(val server: MinecraftServer, val config: Config) {
    fun info(message: String) {
        if (!config.notifyPlayers) {
            return
        }
        server.playerList
            .broadcastSystemMessage(
                Component.literal("[Starlight Preserved] $message")
                    .withStyle(ChatFormatting.GREEN),
                config.notificationMode == NotificationMode.ACTION_BAR
            )
    }

    fun info(component: Component) {
        if (!config.notifyPlayers) {
            return
        }
        server.playerList
            .broadcastSystemMessage(
                component,
                config.notificationMode == NotificationMode.ACTION_BAR
            )
    }

    fun error(message: String) {
        if (!config.notifyPlayers) {
            return
        }
        server.playerList
            .broadcastSystemMessage(
                Component.literal("[Starlight Preserved] $message").withStyle(
                    ChatFormatting.RED
                ),
                config.notificationMode == NotificationMode.ACTION_BAR
            )
    }
}