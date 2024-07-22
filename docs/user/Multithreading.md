---
layout: docs
toc_group: js
link_title: Multithreading
permalink: /reference-manual/js/Multithreading/
---

# Multithreading

Running JavaScript on GraalVM supports multithreading.
Depending on the usage scenario, threads can be used to execute parallel JavaScript code using multiple [`Context`](https://www.graalvm.org/sdk/javadoc/org/graalvm/polyglot/Context.html) objects, or multiple [Worker](https://nodejs.org/api/worker_threads.html) threads.

## Multithreading with Java and JavaScript

Multithreading is supported when running JavaScript in the context of Java interoperability.
The basic model of multithreaded execution supported by GraalVM is a "share-nothing" model that should be familiar to any JavaScript developer:
1. An arbitrary number of JavaScript `Context`s can be created, but they should be used by one thread at a time.
2. Concurrent access to JavaScript objects is not allowed: any JavaScript object cannot be accessed by more than one thread at a time.
3. Concurrent access to Java objects is allowed: any Java object can be accessed by any Java or JavaScript thread, concurrently.

A JavaScript `Context` cannot be accessed by two or more threads, concurrently, but it is possible to access the same `Context` from multiple threads using proper syncronization, to ensure that concurrent access never happens.

### Examples

The GraalJS [unit tests](https://github.com/graalvm/graaljs/tree/master/graal-js/src/com.oracle.truffle.js.test.threading/src/com/oracle/truffle/js/test/threading) contain several examples of multithreaded Java/JavaScript interactions.
The most notable ones describe how:
1. [Multiple `Context` objects can be executed in multiple threads](https://github.com/graalvm/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test.threading/src/com/oracle/truffle/js/test/threading/ConcurrentAccess.java).
2. [JavaScript values created by one thread can be used from another thread when proper synchronization is used](https://github.com/graalvm/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test.threading/src/com/oracle/truffle/js/test/threading/SingleThreadAccess.java).
3. [A `Context` can be accessed from multiple threads when proper synchronization is used](https://github.com/graalvm/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test.threading/src/com/oracle/truffle/js/test/threading/ConcurrentAccess.java).
4. [Java concurrency can be used from JavaScript](https://github.com/graalvm/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test.threading/src/com/oracle/truffle/js/test/threading/ForkJoinTest.java).
5. [Java objects can be accessed by multiple JavaScript threads, concurrently](https://github.com/graalvm/graaljs/blob/master/graal-js/src/com.oracle.truffle.js.test.threading/src/com/oracle/truffle/js/test/threading/SharedJavaObjects.java).

### Related Documentation

* [Java Interoperability](JavaInteroperability.md)