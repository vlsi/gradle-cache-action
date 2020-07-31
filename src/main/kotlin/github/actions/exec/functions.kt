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
package github.actions.exec

import com.github.burrunan.gradle.jsObject
import kotlinx.coroutines.await

class ExecResult(
    val exitCode: Int,
    val stdout: String,
)

suspend fun exec(
    commandLine: String, vararg args: String,
    options: ExecOptions.() -> Unit = {}
): ExecResult {
    val stdout = mutableListOf<String>()
    val exitCode = exec(
        commandLine,
        args.copyOf(),
        jsObject(options).apply {
            listeners = jsObject {
                this.stdout = {
                    stdout.add(it.toString())
                }
            }
        }
    ).await()
    return ExecResult(
        exitCode = exitCode.toInt(),
        stdout = stdout.joinToString("\n")
    )
}
