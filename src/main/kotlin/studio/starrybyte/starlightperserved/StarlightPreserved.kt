package studio.starrybyte.starlightperserved

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import studio.starrybyte.starlightperserved.backups.BackupTrigger
import studio.starrybyte.starlightperserved.configs.Config
import studio.starrybyte.starlightperserved.configs.ConfigManager
import kotlin.io.path.absolute

object StarlightPreserved : ModInitializer {
    private var lastBackupTimeMs = System.currentTimeMillis()
    private var server: MinecraftServer? = null
    private lateinit var config: Config
    private var backupIntervalMs: Long = Long.MAX_VALUE
    private var backupTrigger: BackupTrigger? = null
    override fun onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register { server ->
            this.server = server
            val worldDir = server.getWorldPath(LevelResource.ROOT).absolute().normalize()
            config = ConfigManager(worldDir.parent).loadConfig()
            backupTrigger = BackupTrigger(server, config)
            backupIntervalMs = config.backupIntervalMinutes * 60 * 1000L
        }
        ServerLifecycleEvents.SERVER_STOPPED.register {
            this.server = null
            this.backupTrigger = null
        }

        ServerTickEvents.END_SERVER_TICK.register { _ ->
            if (System.currentTimeMillis() - lastBackupTimeMs < backupIntervalMs
            ) {
                return@register
            }
            backupTrigger?.triggerBackup() { lastBackupTimeMs = System.currentTimeMillis() }
        }
    }
}