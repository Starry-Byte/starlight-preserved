package studio.starrybyte.starlightperserved.configs
data class Config(
    val backupIntervalMinutes: Int = 30,
    val maxBackups: Int = 10,
    val backupDirName: String = "backups",
    val notifyPlayers : Boolean = true,
    val notificationMode: NotificationMode = NotificationMode.ACTION_BAR
)