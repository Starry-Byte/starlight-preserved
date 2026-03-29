package studio.starrybyte.starlightperserved

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.absolute

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
        val backupDir = worldDir.parent.resolve("backups").toFile()
        val outFile = File(backupDir, "backup-$timestamp.zip")
        logger.info(backupDir.toString())
        Thread {
            try {
                broadcastInfo("Starting backup...")
                zipDirectory(worldDir.toFile(), outFile)
                server.execute {
                    noSaveStates.forEach { (level, noSave) ->
                        level.noSave = noSave
                    }
                    broadcastInfo("Backup completed, saved to ${outFile.name}!")
                }
            } catch (e: Exception) {
                broadcastError("Backup failed: ${e.message}")
                outFile.delete()
            } finally {
                lastBackupTimeMs = System.currentTimeMillis()
                isBackupInProgress.set(false)
            }
        }.also { it.isDaemon = false }.start()
    }

    private fun zipDirectory(worldDir: File, outDir: File) {
        outDir.parentFile.mkdirs()
        ZipOutputStream(outDir.outputStream().buffered()).use { stream ->
            val sourcePath = worldDir.toPath()
            worldDir.walkTopDown().forEach { file ->
                if (file.name == "session.lock"
                    || !file.isFile
                ) return@forEach
                val entryName = sourcePath.relativize(file.toPath()).toString()
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