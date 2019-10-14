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
import org.drx.evoleq.dsl.parallel
import org.drx.evoleq.evolving.Parallel
import org.junit.Test

class RunnerTest {

    @Test fun testRunner() = runBlocking {
        var done1 = false
        var done2 = false
        val startTime = System.currentTimeMillis()
        Parallel<Unit> {
            runTest {
                delay(1_000)
                done1 = true
            }
        }
        parallel<Unit> {
            runTest {
                delay(1_000)
                done2 = true
            }
        }
        while(!(done1 && done2)){delay(1)}
        val time = System.currentTimeMillis() - startTime
        assert( time >= 2_000)
    }

    @Test fun testRunnersOrder() = runBlocking {
        var current = 0
        IntRange(1,100).forEach {
            runTest {
                assert(it > current)
                current = it
                delay(1)
            }
        }
    }

    @Test fun testRunnerTimeout()  {
        var time: Long = 10_000

        val startTime = System.currentTimeMillis()

        try{
            runTest(10){
                delay(10_000)
            }
        }
        catch(exception : Exception) {
            time = System.currentTimeMillis() - startTime
        }
        assert(time < 9_000)
    }

    @Test fun  cancellationOfJob() = runBlocking {
        var job: Job? = null
        try {
            runTest(10) {
                job = launch {
                    delay(10_000)
                }
                job!!.join()
            }
        }catch(exception : Exception) {
            println(exception.message)
        }

        while(job == null){
            delay(10)
        }
        assert(job!!.isCancelled)

    }
    @Test fun  cancellationOfParallel() = runBlocking {
        var job: Parallel<Unit>? = null
        try {
            runTest(100) {
                parallel {
                    job = parallel {
                        delay(10_000)
                    }
                }

            }
        }catch(exception : Exception) {
            println(exception.message)
        }

        while(job == null){
            delay(10)
        }
        assert(job!!.job.isCancelled)

    }

    @Test fun  self() = runTest{
        val x = Parallel {
            assert(true)
            1
        }
        assert(x.get() == 1)

    }
}