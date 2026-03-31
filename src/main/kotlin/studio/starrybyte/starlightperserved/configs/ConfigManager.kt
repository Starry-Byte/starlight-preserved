package studio.starrybyte.starlightperserved.configs

import org.slf4j.LoggerFactory
import org.tomlj.Toml
import studio.starrybyte.starlightperserved.StarlightPreserved
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

class ConfigManager(val basePath: Path) {
    private val logger = LoggerFactory.getLogger("Starlight Preserved")
    private val configFilePath = Path("configs/starlight-preserved.toml")

    fun loadConfig(): Config {
        val path = basePath.resolve(configFilePath)
        assureExists(path)
        try {
            val toml = Toml.parse(path)
            val rawMode = toml.getString("notification_mode")
            val config = Config(
                backupIntervalMinutes = toml.getLong("backup_interval_minutes")?.toInt() ?: 30,
                maxBackups = toml.getLong("max_backups")?.toInt() ?: 10,
                backupDirName = toml.getString("backup_dir_name") ?: "backups",
                notifyPlayers = toml.getBoolean("notify_players") ?: true,
                notificationMode = if (rawMode != null) NotificationMode.valueOf(rawMode) else NotificationMode.ACTION_BAR,
            )
            return config
        } catch (e: Exception) {
            logger.error("Could not read config file", e)
            return Config()
        }
    }

    private fun assureExists(path: Path) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path.parent)
                StarlightPreserved::class.java
                    .getResourceAsStream("/configs/starlight-preserved.toml")!!
                    .use { Files.copy(it, path) }
            } catch (e: Exception) {
                logger.error("Could not create config file", e)
            }
        }
    }
}