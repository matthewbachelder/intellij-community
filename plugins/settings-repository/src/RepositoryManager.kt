package org.jetbrains.settingsRepository

import com.intellij.openapi.progress.ProgressIndicator

import java.io.InputStream
import gnu.trove.THashSet
import java.util.Collections
import com.intellij.openapi.progress.EmptyProgressIndicator

public trait RepositoryManager {
  public fun createRepositoryIfNeed(): Boolean

  /**
   * Think twice before use
   */
  public fun deleteRepository()

  public fun isRepositoryExists(): Boolean

  public fun getUpstream(): String?

  public fun hasUpstream(): Boolean

  /**
   * Return error message if failed
   */
  public fun setUpstream(url: String?, branch: String?)

  public fun read(path: String): InputStream?

  /**
   * @param async Write postpone or immediately
   */
  public fun write(path: String, content: ByteArray, size: Int)

  public fun delete(path: String)

  public fun listSubFileNames(path: String): Collection<String>

  /**
   * Not all implementations support progress indicator (will not be updated on progress)
   */
  public fun commit(indicator: ProgressIndicator): Boolean

  public fun commit(paths: List<String>)

  public fun push(indicator: ProgressIndicator = EmptyProgressIndicator())

  public fun fetch(): Updater

  public fun pull(indicator: ProgressIndicator): UpdateResult?

  public fun has(path: String): Boolean

  public fun resetToTheirs(indicator: ProgressIndicator): UpdateResult?

  public fun resetToMy(indicator: ProgressIndicator, localRepositoryInitializer: (() -> Unit)?): UpdateResult?

  public fun canCommit(): Boolean

  public trait Updater {
    fun merge(): UpdateResult?

    // valid only if merge was called before
    val definitelySkipPush: Boolean
  }
}

public trait UpdateResult {
  val changed: Collection<String>
  val deleted: Collection<String>
}

val EMPTY_UPDATE_RESULT = ImmutableUpdateResult(Collections.emptySet(), Collections.emptySet())

public data class ImmutableUpdateResult(override val changed: Collection<String>, override val deleted: Collection<String>) : UpdateResult {
  public fun toMutable(): MutableUpdateResult = MutableUpdateResult(changed, deleted)
}

public data class MutableUpdateResult(changed: Collection<String>, deleted: Collection<String>) : UpdateResult {
  override val changed = THashSet(changed)
  override val deleted = THashSet(deleted)

  fun add(result: UpdateResult?): MutableUpdateResult {
    if (result != null) {
      add(result.changed, result.deleted)
    }
    return this
  }

  fun add(newChanged: Collection<String>, newDeleted: Collection<String>): MutableUpdateResult {
    changed.removeAll(newDeleted)
    deleted.removeAll(newChanged)

    changed.addAll(newChanged)
    deleted.addAll(newDeleted)
    return this
  }

  fun addChanged(newChanged: Collection<String>): MutableUpdateResult {
    deleted.removeAll(newChanged)
    changed.addAll(newChanged)
    return this
  }
}

public fun UpdateResult?.isEmpty(): Boolean = this == null || (changed.isEmpty() && deleted.isEmpty())

public fun UpdateResult?.concat(result: UpdateResult?): UpdateResult? {
  if (result.isEmpty()) {
    return this
  }
  else if (isEmpty()) {
    return result
  }
  else {
    this!!
    return MutableUpdateResult(changed, deleted).add(result!!)
  }
}