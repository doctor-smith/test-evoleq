/**
 * Copyright (c) 2019 Dr. Florian Schmidt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.drx.evoleq.test

import junit.framework.AssertionFailedError
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import org.drx.evoleq.evolving.Evolving
import org.drx.evoleq.evolving.Parallel
import org.drx.evoleq.time.Change
import org.drx.evoleq.time.happen


val testRunner: SendChannel<Change<suspend CoroutineScope.() -> Unit>> by lazy {
    GlobalScope.actor<Change<suspend CoroutineScope.() -> Unit>> {
        var blocked = false
        for (f in channel) {
            while (blocked) {
                delay(1)
            }
            var error: Throwable? = null
            Parallel<Unit> {
                blocked = true
                var job: Job? = null
                try {
                    withTimeout(10_000) {
                        job = launch { f.value(scope) }
                        job!!.join()
                    }
                } catch (exception: Exception) {
                    job!!.cancel()
                    error = exception
                } catch (exception: java.lang.Exception) {
                    job!!.cancel()
                    error = exception
                }
                catch (assertionError: AssertionError) {
                    job!!.cancel()
                    error = assertionError
                }
                catch (assertionError: java.lang.AssertionError) {
                    job!!.cancel()
                    error = assertionError
                }

                blocked = false
            }.get()
            if (error == null) {
                f.value = { Unit }
            } else {
                f.value = { throw error!! }
            }
        }

    }
}
suspend fun performTest(test: suspend CoroutineScope.()->Unit): Evolving<suspend CoroutineScope.()->Unit> {
    val change = Change(test)
    val changing = change.happen()
    testRunner.send(change)
    return changing
}

fun runTest(test: suspend CoroutineScope.()->Unit): Unit = runBlocking {
    performTest(test).get()()
}