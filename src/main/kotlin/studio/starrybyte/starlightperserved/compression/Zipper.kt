package studio.starrybyte.starlightperserved.compression

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream

class Zipper {
    fun zipDirectory(worldDir: Path, outDir: Path) {
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
}