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
package com.github.burrunan.gradle.github

import github.actions.core.getState
import github.actions.core.saveState

interface MutableStateVariable<T : Any> {
    fun set(value: T)
}

open class BaseStateVariable<T : Any>(protected val name: String) : MutableStateVariable<T> {
    protected lateinit var value: T

    override fun set(value: T) {
        this.value = value
        saveState(name, value)
    }
}

interface SuspendingStateVariable<T : Any> : MutableStateVariable<T> {
    suspend fun get(): T
}

class DefaultSuspendingStateVariable(name: String, val default: suspend () -> String) :
    BaseStateVariable<String>(name), SuspendingStateVariable<String> {
    override suspend fun get(): String {
        if (::value.isInitialized) {
            return value
        }
        return getState(name).ifBlank { defaultValue() }.also { value = it }
    }

    private suspend fun defaultValue() = default().also {
        if (it.isNotBlank()) {
            set(it)
        }
    }
}

interface StateVariable<T : Any> : MutableStateVariable<T> {
    fun get(): T
}

class DefaultStateVariable(name: String, private val default: () -> String) :
    BaseStateVariable<String>(name), StateVariable<String> {
    override fun get(): String {
        if (::value.isInitialized) {
            return value
        }
        return getState(name).ifBlank { defaultValue() }.also { value = it }
    }

    private fun defaultValue() = default().also {
        if (it.isNotBlank()) {
            set(it)
        }
    }
}

fun suspendingStateVariable(name: String, default: suspend () -> String): SuspendingStateVariable<String> =
    DefaultSuspendingStateVariable(name, default)

fun stateVariable(name: String, default: () -> String = { "" }): StateVariable<String> =
    DefaultStateVariable(name, default)

fun <T : Any, R : Any> StateVariable<T>.transform(decode: (T) -> R, encode: (R) -> T) = object : StateVariable<R> {
    override fun get(): R = decode(this@transform.get())

    override fun set(value: R) {
        this@transform.set(encode(value))
    }
}

fun StateVariable<String>.toBoolean() = transform({ it == "Y" }, { if (it) "Y" else "N" })
fun StateVariable<String>.toInt() = transform({ it.toInt() }, { it.toString() })
