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
import com.github.burrunan.gradle.cache.DefaultCache
import com.github.burrunan.gradle.github.event.ActionsTrigger

fun localBuildCache(jobId: String, trigger: ActionsTrigger, gradleVersion: String, treeId: String): Cache {
    val prefixChars = "0123456789abcdef"
    val buildCacheLocation = "~/.gradle/caches/build-cache-*"
    val caches = mutableListOf<Cache>()
    val defaultBranch = trigger.event.repository.default_branch

    val pkPrefix = when(trigger) {
        is ActionsTrigger.PullRequest -> "PR-${trigger.event.pull_request.number}"
        is ActionsTrigger.BranchPush -> trigger.event.ref.removePrefix("refs/heads/")
        is ActionsTrigger.Other -> throw IllegalStateException("Unknown event ${trigger.name}")
    }
    val restoreKeys = when(trigger) {
        is ActionsTrigger.PullRequest -> arrayOf(
            pkPrefix,
            trigger.event.pull_request.base.ref.removePrefix("refs/heads/"),
            defaultBranch
        )
        is ActionsTrigger.BranchPush -> arrayOf(
            pkPrefix,
            defaultBranch
        )
        is ActionsTrigger.Other -> throw IllegalStateException("Unknown event ${trigger.name}")
    }
    for (char in prefixChars) {
        val prefix = "gradle-build-cache-$jobId-$gradleVersion-$char"
        caches.add(
            DefaultCache(
                name = "local-build-cache-$char",
                primaryKey = "$prefix-$pkPrefix-$treeId",
                restoreKeys = restoreKeys.map { "$prefix-$it" },
                paths = if (char != '0') {
                    listOf(
                        "$buildCacheLocation/$char*",
                        "!$buildCacheLocation/*.lock"
                    )
                } else {
                    // All the files from build-cache except the ones covered in prefixChars
                    listOf(
                        "$buildCacheLocation/",
                        "!$buildCacheLocation/*.lock"
                    ) +
                    prefixChars.filter { it != '0' }.map {
                        "!$buildCacheLocation/$it*"
                    }
                }
            )
        )
    }
    return CompositeCache("build-cache", caches)
}
