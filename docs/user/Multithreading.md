# Multithreading

Running JavaScript on GraalVM supports multithreading.
Depending on the usage scenario, threads can be used to execute parallel JavaScript code using multiple [`Context`](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html) objects, or multiple [Worker](https://nodejs.org/api/worker_threads.html) threads.

## Multithreading with Java and JavaScript

Multithreading is supported when running JavaScript in the context of Java interoperability.
The basic model of multi-threaded execution supported by GraalVM is a "share-nothing" model that should be familiar to any JavaScript developer:

1. An arbitrary number of JavaScript `Context`s can be created, but they should be used by one thread at a time.
2. Concurrent access to JavaScript objects is not allowed: any JavaScript object cannot be accessed by more than one thread at a time.
3. Concurrent access to Java objects is allowed: any Java object can be accessed by any Java or JavaScript thread, concurrently.

A JavaScript `Context` cannot be accessed by two or more threads, concurrently, but it is possible to access the same `Context` from multiple threads using proper syncronization, to ensure that concurrent access never happens.

### Examples

The GraalVM JavaScript [unit tests](https://github.com/graalvm/graaljs/tree/master/graal-js/src/com.oracle.truffle.js.test.threading/src/com/oracle/truffle/js/test/threading) contain several examples of multi-threaded Java/JavaScript interactions.
The most notable ones describe how:

1. [Multiple `Context` objects can be executed in multiple threads](https://github.com/graalvm/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test.threading/src/com/oracle/truffle/js/test/threading/ConcurrentAccess.java).
2. [JavaScript values created by one thread can be used from another thread when proper synchronization is used](https://github.com/graalvm/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test.threading/src/com/oracle/truffle/js/test/threading/SingleThreadAccess.java).
3. [A `Context` can be accessed from multiple threads when proper synchronization is used](https://github.com/graalvm/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test.threading/src/com/oracle/truffle/js/test/threading/ConcurrentAccess.java).
4. [Java concurrency can be used from JavaScript](https://github.com/graalvm/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test.threading/src/com/oracle/truffle/js/test/threading/ForkJoinTest.java).
5. [Java objects can be accessed by multiple JavaScript threads, concurrently](https://github.com/graalvm/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test.threading/src/com/oracle/truffle/js/test/threading/SharedJavaObjects.java).

## Multithreading with Node.js

The basic multithreading model of GraalVM JavaScript applies to Node.js applications as well.
In Node.js, a [Worker](https://nodejs.org/api/worker_threads.html#worker_threads_worker_threads) thread can be created to execute JavaScript code in parallel, but JavaScript objects cannot be shared between Workers.
On the contrary, a Java object created with GraalVM Java interoperability (e.g., using `Java.type()`) can be shared between Node.js Workers.
This allows multi-threaded Node.js applications to share Java objects.

### Examples

The GraalVM Node.js [unit tests](https://github.com/graalvm/graaljs/tree/master/graal-nodejs/test/graal/unit) contain several examples of multi-threaded Node.js applications.
The most notable examples show how:

1. [Node.js worker threads can execute Java code](https://github.com/graalvm/graaljs/blob/master/graal-nodejs/test/graal/unit/worker.js).
2. [Java objects can be shared between Node.js worker threads](https://github.com/graalvm/graaljs/blob/master/graal-nodejs/test/graal/unit/javaMessages.js).
3. [JavaScript `Promise` objects can be used to `await` on messages from workers, using Java objects to bind promises to worker messages](https://github.com/graalvm/graaljs/blob/master/graal-nodejs/test/graal/unit/workerInteropPromises.js).
