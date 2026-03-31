package studio.starrybyte.starlightperserved.backups

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import org.slf4j.LoggerFactory
import studio.starrybyte.starlightperserved.compression.Zipper
import studio.starrybyte.starlightperserved.configs.Config
import studio.starrybyte.starlightperserved.logs.Broadcaster
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolute
import kotlin.io.path.deleteIfExists
import kotlin.io.path.name

class BackupTrigger(val server: MinecraftServer, val config: Config) {
    private val logger = LoggerFactory.getLogger("StarlightPreserved")
    private var isBackupInProgress: AtomicBoolean = AtomicBoolean(false)
    private val broadcaster = Broadcaster(server, config)
    private val zipper = Zipper()
    fun triggerBackup(onDoneCallBack: () -> Unit) {
        val worldDir = server.getWorldPath(LevelResource.ROOT).absolute().normalize()

        if (isBackupInProgress.get()) {
            return
        }
        isBackupInProgress.set(true)
        val timestamp = LocalDateTime
            .now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val noSaveStates = server.allLevels.associateWith { it.noSave }
        server.isAutoSave = false
        server.saveEverything(false, true, true)
        val backupDir = worldDir.parent.resolve(config.backupDirName)
        val outFile = backupDir.resolve("backup-$timestamp.zip")
        logger.info(backupDir.toString())
        Thread {
            try {
                broadcaster.info("Starting backup...")
                zipper.zipDirectory(worldDir, outFile)
                server.execute {
                    noSaveStates.forEach { (level, noSave) ->
                        level.noSave = noSave
                    }
                    val message = Component.empty()
                        .append(Component.literal("[Starlight Preserved] Backup complete!").withStyle(ChatFormatting.GREEN))
                        .append(Component.literal("\n"))
                        .append(Component.literal("Saved to ${outFile.name}").withStyle(ChatFormatting.GRAY))
                    broadcaster.info(message)
                }
            } catch (e: Exception) {
                broadcaster.error("Backup failed: ${e.message}")
                outFile.deleteIfExists()
            } finally {
                onDoneCallBack()
                isBackupInProgress.set(false)
            }
        }.also { it.isDaemon = false }.start()
    }
}