package studio.starrybyte.starlightperserved
data class Config(
    val backupIntervalMinutes: Int = 30,
    val maxBackups: Int = 10,
    val backupDirName: String = "backups",
    val notifyPlayers: Boolean = true,
)