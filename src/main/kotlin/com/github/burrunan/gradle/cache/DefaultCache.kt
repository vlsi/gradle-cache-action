/*
 * Copyright 2020 Vladimir Sitnikov <sitnikov.vladimir@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.github.burrunan.gradle.cache

import com.github.burrunan.gradle.HashResult
import com.github.burrunan.gradle.github.formatBytes
import com.github.burrunan.gradle.github.stateVariable
import com.github.burrunan.gradle.github.toBoolean
import com.github.burrunan.gradle.github.toInt
import com.github.burrunan.gradle.hashFiles
import github.actions.cache.restoreAndLog
import github.actions.cache.saveAndLog
import github.actions.core.info
import kotlin.math.absoluteValue

class DefaultCache(
    name: String,
    private val primaryKey: String,
    private val restoreKeys: List<String> = listOf(),
    private val paths: List<String>,
) : Cache {
    @Suppress("CanBePrimaryConstructorProperty")
    override val name: String = name

    private val isExactMatch = stateVariable("${name}_exact").toBoolean()
    private val restoredContentsHash = stateVariable("${name}_hash")
    private val restoredContentsBytes = stateVariable("${name}_bytes").toInt()

    var contents: HashResult? = null

    override suspend fun restore(): RestoreType {
        info("Restoring $name")
        return restoreAndLog(paths, primaryKey, restoreKeys).also {
            isExactMatch.set(it is RestoreType.Exact)
            // If we know the cache was not restored, no need to hash the restored contents
            if (!isExactMatch.get() && it !is RestoreType.None) {
                val hash = hashFiles(*paths.toTypedArray())
                contents = hash
                restoredContentsHash.set(hash.hash)
                restoredContentsBytes.set(hash.totalBytes)
            }
        }
    }

    override suspend fun save() {
        if (isExactMatch.get()) {
            info("$name loaded from exact match, no need to update the cache entry")
            return
        }
        val newHash = hashFiles(*paths.toTypedArray())
        contents = newHash
        if (newHash.numFiles == 0) {
            info("$name: no files to cache => won't upload empty cache")
            return
        }
        if (restoredContentsHash.get().isNotBlank()) {
            info("$name: comparing modifications of the cache contents")
            if (newHash.hash == restoredContentsHash.get()) {
                info("$name: contents did not change => no need to upload it")
                return
            }
            val delta = restoredContentsBytes.get() - newHash.totalBytes
            info("$name: hash content differs (${delta.absoluteValue} bytes ${if (delta > 0) "increase" else "decrease"})")
        }
        info("$name: uploading ${newHash.totalBytes.toLong().formatBytes()}, ${newHash.numFiles} files")
        saveAndLog(paths, primaryKey)
    }
}
