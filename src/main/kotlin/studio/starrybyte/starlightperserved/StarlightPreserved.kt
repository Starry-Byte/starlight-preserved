package studio.starrybyte.starlightperserved

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.absolute
import kotlin.io.path.deleteIfExists
import kotlin.io.path.name
import kotlin.io.path.outputStream

object StarlightPreserved : ModInitializer {
    private val logger = LoggerFactory.getLogger("StarlightPreserved")
    private const val BACKUP_INTERVAL_MS = 30 * 60 * 1000L
    private var lastBackupTimeMs = System.currentTimeMillis()
    private var server: MinecraftServer? = null
    private var isBackupInProgress: AtomicBoolean = AtomicBoolean(false)
    override fun onInitialize() {

        ServerLifecycleEvents.SERVER_STARTING.register { server -> this.server = server }
        ServerLifecycleEvents.SERVER_STOPPED.register { this.server = null }
        ServerTickEvents.END_SERVER_TICK.register { server ->
            if (System.currentTimeMillis() - lastBackupTimeMs < BACKUP_INTERVAL_MS
            ) {
                return@register
            }
            triggerBackup(server)
        }
    }

    private fun triggerBackup(server: MinecraftServer) {
        if(isBackupInProgress.get()){
            return
        }
        isBackupInProgress.set(true)
        val timestamp = LocalDateTime
            .now()
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val noSaveStates = server.allLevels.associateWith { it.noSave }
        server.isAutoSave = false
        server.saveEverything(false, true, true)
        val worldDir = server.getWorldPath(LevelResource.ROOT).absolute().normalize()
        val backupDir = worldDir.parent.resolve("backups")
        val outFile = backupDir.resolve("backup-$timestamp.zip")
        logger.info(backupDir.toString())
        Thread {
            try {
                broadcastInfo("Starting backup...")
                zipDirectory(worldDir, outFile)
                server.execute {
                    noSaveStates.forEach { (level, noSave) ->
                        level.noSave = noSave
                    }
                    broadcastInfo("Backup completed, saved to ${outFile.name}!")
                }
            } catch (e: Exception) {
                broadcastError("Backup failed: ${e.message}")
                outFile.deleteIfExists()
            } finally {
                lastBackupTimeMs = System.currentTimeMillis()
                isBackupInProgress.set(false)
            }
        }.also { it.isDaemon = false }.start()
    }

    private fun zipDirectory(worldDir: Path, outDir: Path) {
        Files.createDirectories(worldDir)
        ZipOutputStream(outDir.outputStream().buffered()).use { stream ->
            worldDir.toFile().walkTopDown().forEach { file ->
                if (file.name == "session.lock"
                    || !file.isFile
                ) return@forEach
                val entryName = worldDir.relativize(file.toPath()).toString()
                stream.putNextEntry(ZipEntry(entryName))
                file.inputStream().use { it.copyTo(stream) }
                stream.closeEntry()
            }
        }
    }

    private fun broadcastInfo(message: String) {
        server?.playerList
            ?.broadcastSystemMessage(
                Component.literal("[Starlight Preserved] $message")
                    .withStyle(ChatFormatting.GREEN)
                    .withStyle(ChatFormatting.BOLD), true
            )
    }

    private fun broadcastError(message: String) {
        server?.playerList
            ?.broadcastSystemMessage(
                Component.literal("[Starlight Preserved] $message").withStyle(
                    ChatFormatting.RED
                ), false
            )
    }
}