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

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.test.runBlockingTest
import org.drx.evoleq.dsl.parallel
import org.drx.evoleq.evolving.Evolving
import org.drx.evoleq.time.Change
import org.drx.evoleq.time.change
import org.drx.evoleq.time.happen

data class TestData(val timeout: Long = 10_000, val test: Change<suspend CoroutineScope.() -> Unit>)

var runner: SendChannel<TestData>? = null


fun testRunner(): SendChannel<TestData>  {
    if(runner == null || (runner != null && runner!!.isClosedForSend)){
    runner = GlobalScope.actor<TestData>(capacity = 1_000_000) {
        var blocked = false
        for (test in channel) {

            while (blocked) {
                delay(1)
            }

            blocked = true
            val change = test.test
            val testFunction =  change.value
                var error: Throwable? = null
                val run = GlobalScope.parallel {
                    this@actor + this.coroutineContext
                    var job: Job? = null
                    try {
                        withTimeout( test.timeout ) {

                            job = launch { testFunction() }

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
                    finally{
                        Unit
                    }

                }
                run.get()
                if (error == null) {
                    change.value = { Unit }
                } else {
                    run.cancel(Unit).get()
                    change.value = { throw error!! }

                }
                blocked = false
            }
        }

    }
    return runner!!
}

suspend fun performTest(timeout: Long, test: suspend CoroutineScope.()->Unit): Evolving<suspend CoroutineScope.()->Unit> {
    val change = GlobalScope.change(test)
    val changing = change.happen()
    testRunner().send(TestData(timeout,change))
    return changing
}

fun runTest(timeout: Long = 10_000, test: suspend CoroutineScope.()->Unit): Unit = runBlocking {
    performTest(timeout){test()}.get()()
}