import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermissions

import scala.collection.JavaConverters.*
import scala.util.Properties

import sbt.*
import sbt.internal.util.ManagedLogger
import java.nio.file.StandardCopyOption

/** Starting point:
  * https://github.com/randomcoder/sbt-git-hooks/blob/master/src/main/scala/uk/co/randomcoding/sbt/GitHooks.scala
  */
// format: off
object GitHooks {
  def apply(hooksSourceDir: File, hooksTargetDir: File, log: ManagedLogger): Unit =
    if (hooksSourceDir.isDirectory && hooksTargetDir.exists()) {
      IO.listFiles(hooksSourceDir)
        .map(hook => (hook, hooksTargetDir / hook.name))
        .foreach { case (originalHook, targetHook) =>
          log.info(s"Copying ${originalHook.name} hook to $targetHook")
          Files.copy(originalHook.asPath, targetHook.asPath, StandardCopyOption.REPLACE_EXISTING)
          if (!Properties.isWin)
            targetHook.setPermissions(PosixFilePermissions.fromString("rwxr-xr-x").asScala.toSet)
        }
    }
}
// format: on
