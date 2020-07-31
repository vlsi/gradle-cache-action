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
package com.github.burrunan.gradle

import com.github.burrunan.gradle.cache.Cache
import com.github.burrunan.gradle.cache.CompositeCache
import com.github.burrunan.gradle.github.event.ActionsTrigger
import com.github.burrunan.gradle.github.stateVariable
import com.github.burrunan.gradle.github.suspendingStateVariable
import com.github.burrunan.gradle.github.toBoolean
import github.actions.core.debug
import github.actions.exec.exec

class GradleCacheAction(val trigger: ActionsTrigger, val params: Parameters) {
    suspend fun run() {
        val gradleVersion = suspendingStateVariable("gradleVersion") {
            hashFiles("gradle/wrapper/gradle-wrapper.properties").hash
        }

        val caches = mutableListOf<Cache>()

        if (params.generatedGradleJars) {
            caches.add(gradleGeneratedJarsCache(gradleVersion.get()))
        }

        if (params.localBuildCache) {
            val treeId = when(trigger) {
                is ActionsTrigger.BranchPush -> trigger.event.head_commit.tree_id
                else -> getTreeId(trigger.event.after)
            }

            if (params.debug) {
                debug("Using tree id of $treeId")
            }

            caches.add(localBuildCache(params.jobId, trigger, gradleVersion.get(), treeId))
        }

        val cache = CompositeCache("all-caches", caches)
        val post = stateVariable("POST").toBoolean()
        if (post.get()) {
            cache.save()
        } else {
            post.set(true)
            cache.restore()
        }
    }

    private suspend fun getTreeId(commitId: String): String {
        val treeId = exec("git", "--show", "--quiet", "--format=%T", commitId).stdout
        if (params.debug) {
            debug("Commit $commitId points to tree $treeId")
        }
        return treeId
    }
}
