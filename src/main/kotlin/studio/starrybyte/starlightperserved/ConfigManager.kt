package studio.starrybyte.starlightperserved

import com.google.gson.GsonBuilder
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path

class ConfigManager(val basePath: Path) {
    val logger = LoggerFactory.getLogger("Starlight Preserved")
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val configFilePath = Path("configs/starlightpreserved.json")

    fun  loadConfig() : Config
    {
        val path = basePath.resolve(configFilePath)
        if(!Files.exists(path)){
            try {
                Files.createDirectories(path.parent)
                val defaultConfig = Config()
                path.toFile().writeText(gson.toJson(defaultConfig)  )
            }
            catch (e: Exception) {
                logger.error("Could not create config file", e)
            }
        }
        try {
            val config =  gson.fromJson(Files.readString(path), Config::class.java)
           return config
        }
        catch (e: Exception) {
            logger.error("Could not read config file", e)
            return Config()
        }
    }
}