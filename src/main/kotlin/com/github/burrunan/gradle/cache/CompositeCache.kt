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

import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class CompositeCache(
    override val name: String,
    private val caches: List<Cache>,
) : Cache {
    override suspend fun save() = supervisorScope {
        for (cache in caches) {
            launch {
                cache.save()
            }
        }
    }

    override suspend fun restore(): RestoreType = supervisorScope {
        for (cache in caches) {
            launch {
                cache.restore()
            }
        }
        RestoreType.Unknown
    }
}
