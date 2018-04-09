/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

"use strict";

(function(){

var PROMISE_STATE = Internal.GetHiddenKey("PromiseState");
var PROMISE_RESULT = Internal.GetHiddenKey("PromiseResult");
var PROMISE_FULFILL_REACTIONS = Internal.HiddenKey("PromiseFulfillReactions");
var PROMISE_REJECT_REACTIONS = Internal.HiddenKey("PromiseRejectReactions");
var PROMISE_IS_HANDLED = Internal.GetHiddenKey("PromiseIsHandled");
var PROMISE_ON_FINALLY = Internal.GetHiddenKey("OnFinally");
var PROMISE_FINALLY_CONSTRUCTOR = Internal.GetHiddenKey("PromiseFinallyConstructor");

/**
 * 25.4 ECMA 6 Promise states.
 */
var PENDING = 0;
var FULFILLED = 1;
var REJECTED = 2;

/**
 * 25.4.1.9 HostPromiseRejectionTracker operations
 */
var OPERATION_REJECT = 0;
var OPERATION_HANDLE = 1;

/**
 * 25.4.1.1 PromiseCapability Records
 */
function PromiseCapabilityRecord(promise, resolve, reject) {
    return { promise, resolve, reject };
}

/**
 * 25.4.1.2 PromiseReaction Records
 */
function PromiseReactionRecord(capabilities, handler) {
    return { capabilities, handler };
}

function Identity(arg) {
    return arg;
}

function Thrower(arg) {
    throw new TypeError(arg);
}

/**
 * 7.3.20 SpeciesConstructor
 */
function SpeciesConstructor(O, defaultConstructor) {
    var C = O.constructor;
    if (C === undefined) {
        return defaultConstructor;
    }
    if (!Internal.IsObject(C)) {
        throw new TypeError('constructor expected ' + typeof C);
    }
    var S = C[Symbol.species];
    if (S == undefined || S == null) {
        return defaultConstructor;
    }
    if (Internal.IsConstructor(S)) {
        return S;
    }
    throw new TypeError('cannot construct');
}

/**
 * 25.4.1.3 CreateResolvingFunctions
 */
function CreateResolvingFunctions(promise) {
    var alreadyResolved = {value:false};
    var resolve = CreatePromiseResolveFunction(promise, alreadyResolved);
    var reject = CreatePromiseRejectFunction(promise, alreadyResolved);
    return {resolve: resolve, reject: reject};
}

/**
 * 25.4.1.3.1 Promise Reject Functions
 */
function CreatePromiseRejectFunction(promise, alreadyResolved) {
    return function(reason) {
        if (alreadyResolved.value === true) {
            return undefined;
        }
        alreadyResolved.value = true;
        return RejectPromise(promise, reason);
    };
}

/**
 * 25.4.1.3.2 Promise Resolve Functions
 */
function CreatePromiseResolveFunction(promise, alreadyResolved) {
    return function(resolution) {
        if (alreadyResolved.value === true) {
            return undefined;
        }
        alreadyResolved.value = true;
        Internal.PromiseHook(1 /* PromiseHook.TYPE_RESOLVE */, promise);
        if (resolution === promise) {
            var selfResolutionError = new TypeError("self resolution!");
            return RejectPromise(promise, selfResolutionError);
        }
        if (!Internal.IsObject(resolution)) {
            return FulfillPromise(promise, resolution);
        }
        try {
            var then = resolution.then;
        } catch (error) {
            return RejectPromise(promise, error);
        }
        if (!then) {
            return FulfillPromise(promise, resolution);
        }
        var thenAction = then;
        if (!Internal.IsCallable(thenAction)) {
            return FulfillPromise(promise, resolution);
        }
        EnqueueJob(PromiseResolveThenableJob(promise, resolution, thenAction));
        return undefined;
    };
}

/**
 * 25.4.1.4 FulfillPromise
 */
function FulfillPromise(promise, value) {
    Internal.Assert(promise[PROMISE_STATE] === PENDING);
    var reactions = promise[PROMISE_FULFILL_REACTIONS];
    promise[PROMISE_RESULT] = value;
    promise[PROMISE_FULFILL_REACTIONS] = undefined;
    promise[PROMISE_REJECT_REACTIONS] = undefined;
    promise[PROMISE_STATE] = FULFILLED;
    return TriggerPromiseReactions(reactions, value);
}

/**
 * 25.4.1.5 NewPromiseCapability
 */
function NewPromiseCapability(C) {
    if (!Internal.IsConstructor(C)) {
        throw new TypeError('Not a valid constructor ' + C);
    }
    var promiseCapability = PromiseCapabilityRecord(undefined, undefined, undefined);
    var executor = GetCapabilitiesExecutor(promiseCapability);
    var promise = new C(executor);
    if (!Internal.IsCallable(promiseCapability.resolve) || !Internal.IsCallable(promiseCapability.reject)) {
        throw new TypeError('cannot create promise');
    }
    promiseCapability.promise = promise;
    return promiseCapability;
}

/**
 * 25.4.1.5.1 GetCapabilitiesExecutor Functions
 */
function GetCapabilitiesExecutor(capability) {
    return function(resolve, reject) {
        if (capability.resolve !== undefined || capability.reject !== undefined) {
            throw new TypeError("error while creating capability!");
        }
        capability.resolve = resolve;
        capability.reject = reject;
        return undefined;
    };
}

function IsPromiseCapabilityRecord(cap) {
    return (cap.promise != undefined &&
            cap.resolve != undefined &&
            cap.reject != undefined);
}

/**
 * 25.4.1.7 RejectPromise
 */
function RejectPromise(promise, reason) {
    Internal.Assert(promise[PROMISE_STATE] === PENDING);
    var reactions = promise[PROMISE_REJECT_REACTIONS];
    promise[PROMISE_RESULT] = reason;
    promise[PROMISE_FULFILL_REACTIONS] = undefined;
    promise[PROMISE_REJECT_REACTIONS] = undefined;
    promise[PROMISE_STATE] = REJECTED;
    if (!promise[PROMISE_IS_HANDLED]) {
        HostPromiseRejectionTracker(promise, OPERATION_REJECT);
    }
    return TriggerPromiseReactions(reactions, reason);
}

/**
 * 25.4.1.8 TriggerPromiseReactions
 */
function TriggerPromiseReactions(reactions, arg) {
    for (var r = 0; r < reactions.length; r++) {
        EnqueueJob(PromiseReactionJob(reactions[r], arg));
    }
    return undefined;
}

/**
 * 25.4.1.9 HostPromiseRejectionTracker(promise, operation)
 * The default implementation of HostPromiseRejectionTracker is to unconditionally return an empty normal completion.
 */
function HostPromiseRejectionTracker(promise, operation) {
    Internal.PromiseRejectionTracker(promise, operation);
}

/**
 * 25.4.2.1 PromiseReactionJob
 */
function PromiseReactionJob(reaction, argument) {
    return function() {
        // Assert: reaction is a PromiseReaction Record.
        var promiseCapability = reaction.capabilities;
        var handler = reaction.handler;

        Internal.PromiseHook(2 /* PromiseHook.TYPE_BEFORE */, promiseCapability.promise);

        var status;
        if (handler === Identity) {
            status = Internal.CallFunction(promiseCapability.resolve, undefined, argument);
        } else if (handler === Thrower) {
            status = Internal.CallFunction(promiseCapability.reject, undefined, argument);
        } else {
            var handlerResult;
            var resolutionFn;
            try {
                handlerResult = Internal.CallFunction(handler, undefined, argument);
                resolutionFn = promiseCapability.resolve;
            } catch (error) {
                handlerResult = error;
                resolutionFn = promiseCapability.reject;
            }
            status = Internal.CallFunction(resolutionFn, undefined, handlerResult);
        }

        Internal.PromiseHook(3 /* PromiseHook.TYPE_AFTER */, promiseCapability.promise);

        return status;
    }
}

/**
 * 25.4.2.2 PromiseResolveThenableJob
 */
function PromiseResolveThenableJob(promiseToResolve, thenable, then) {
    return function() {
        var resolvingFunctions = CreateResolvingFunctions(promiseToResolve);
        try {
            return Internal.CallFunction(then, thenable, resolvingFunctions.resolve, resolvingFunctions.reject);
        } catch (error) {
            return Internal.CallFunction(resolvingFunctions.reject, undefined, error);
        }
    }
}

/**
 * 25.4.3.1 Promise Constructor
 */
var Promise = function Promise(executor) {
    if (new.target === undefined) {
        throw new TypeError("Constructor Promise requires 'new'");
    }
    if (!Internal.IsCallable(executor)) {
        throw new TypeError("cannot create promise: executor not callable");
    }
    var promise = Internal.CreatePromiseFromConstructor(new.target);
    promise[PROMISE_STATE] = PENDING;
    promise[PROMISE_FULFILL_REACTIONS] = [];
    promise[PROMISE_REJECT_REACTIONS] = [];
    promise[PROMISE_IS_HANDLED] = false;
    var resolvingFunctions = CreateResolvingFunctions(promise);
    Internal.PromiseHook(0 /* PromiseHook.TYPE_INIT */, promise);
    try {
        Internal.CallFunction(executor, undefined, resolvingFunctions.resolve, resolvingFunctions.reject);
    } catch (e) {
        Internal.CallFunction(resolvingFunctions.reject, undefined, e);
    }
    return promise;
};

/**
 * 25.4.4 Properties of the Promise Constructor
 */
Internal.ObjectDefineProperty(Promise, "prototype", {value: {}, configurable: false, enumerable: false, writable: false});

/**
 * 25.4.4.1 Promise.all
 */
var promAll = function all(iterable) {
    var C = this;
    if (!Internal.IsObject(C)) {
        throw new TypeError("cannot create promise from type '" + typeof C + "'");
    }
    var promiseCapability = NewPromiseCapability(C);
    try {
        var iterator = Internal.GetIterator(iterable);
    } catch (e) {
        Internal.CallFunction(promiseCapability.reject, undefined, e);
        return promiseCapability.promise;
    }

    var iteratorRecord = {iterator:iterator, done:false};
    try {
        return PerformPromiseAll(iteratorRecord, C, promiseCapability);
    } catch (result) {
        if (!iteratorRecord.done && iteratorRecord.iterator.return) {
            iteratorRecord.iterator.return();
        }
        promiseCapability.reject(result);
        return promiseCapability.promise;
    }
};

/**
 * 25.4.4.1.1 Runtime Semantics: PerformPromiseAll( iteratorRecord, constructor,
 * resultCapability)
 */
function PerformPromiseAll(iteratorRecord, constructor, resultCapability) {
    // TODO assertions using intrinsics
    var values = [];
    var remainingElementsCount = {value:1};
    var index = 0;

    while (true) {
        try {
            var next = iteratorRecord.iterator.next();
            var done = next.done;
        } catch (err) {
            iteratorRecord.done = true;
            throw err;
        }
        if (done) {
            iteratorRecord.done = true;
            remainingElementsCount.value--;
            if (remainingElementsCount.value === 0) {
                var valuesArray = values;
                Internal.CallFunction(resultCapability.resolve, undefined, valuesArray);
            }
            return resultCapability.promise;
        }
        try {
            var nextValue = next.value;
        } catch (err) {
            iteratorRecord.done = true;
            throw err;
        }
        Internal.ArrayPush(values, undefined);
        var nextPromise = constructor.resolve(nextValue);
        var resolveElement = PromiseAllResolveElementFunctions(index, values, resultCapability, remainingElementsCount, {value:false});
        remainingElementsCount.value++;
        nextPromise.then(resolveElement, resultCapability.reject);
        index++;
    }
}

/**
 * 25.4.4.1.2 Promise.all Resolve Element Functions
 */
function PromiseAllResolveElementFunctions(index, values, capabilities, remainingElements, alreadyCalled) {
    return function(x) {
        if (alreadyCalled.value === true) {
            return undefined;
        }
        alreadyCalled.value = true;
        var promiseCapability = capabilities;
        var remainingElementsCount = remainingElements;

        values[index] = x;

        remainingElementsCount.value = remainingElementsCount.value - 1;
        if (remainingElementsCount.value == 0) {
            var valuesArray = CreateArrayFromList(values);
            return Internal.CallFunction(promiseCapability.resolve, undefined, valuesArray);
        }
        return undefined;
    }
}

function CreateArrayFromList(elements) {
    return elements.slice();
}

/**
 * 25.4.4.3 Promise.race
 */
var promRace = function race(iterable) {
    var C = this;
    if (!Internal.IsObject(C)) {
        throw new TypeError("cannot race over this promise");
    }
    var promiseCapability = NewPromiseCapability(C);
    try {
        var iterator = Internal.GetIterator(iterable);
    } catch (e) {
        Internal.CallFunction(promiseCapability.reject, undefined, e);
        return promiseCapability.promise;
    }

    var iteratorRecord = {iterator:iterator, done:false};
    try {
        return PerformPromiseRaceLoop(iteratorRecord, promiseCapability, C);
    } catch (result) {
        if (!iteratorRecord.done && iteratorRecord.iterator.return) {
            iteratorRecord.iterator.return();
        }
        promiseCapability.reject(result);
        return promiseCapability.promise;
    }
};

/**
 * 25.4.4.3.1 PerformPromiseRaceLoop
 */
function PerformPromiseRaceLoop(iteratorRecord, promiseCapability, C) {
    while (true) {
        try {
            var next = iteratorRecord.iterator.next();
            var done = next.done
        } catch (err) {
            iteratorRecord.done = true;
            throw err;
        }
        if (done) {
            iteratorRecord.done = true;
            return promiseCapability.promise;
        }
        try {
            var nextValue = next.value;
        } catch (err) {
            iteratorRecord.done = true;
            throw err;
        }
        var nextPromise = C.resolve(nextValue);
        nextPromise.then(promiseCapability.resolve, promiseCapability.reject);
    }
}

/**
 * 25.4.4.4 Promise.reject
 */
var promReject = function reject(r) {
    var C = this;
    if (!Internal.IsObject(C)) {
        throw new TypeError("expect an object to reject");
    }
    var promiseCapability = NewPromiseCapability(C);
    Internal.CallFunction(promiseCapability.reject, undefined, r);
    return promiseCapability.promise;
};

/**
 * 25.4.4.5 Promise.resolve
 */
var promResolve = function resolve(x) {
    var C = this;
    if (!Internal.IsObject(C)) {
        throw new TypeError("expect an object to resolve");
    }
    if (Internal.IsPromise(x) && x.constructor === C) {
        return x;
    }
    var promiseCapability = NewPromiseCapability(C);
    Internal.CallFunction(promiseCapability.resolve, undefined, x);
    return promiseCapability.promise;
};

/**
 * 25.4.5.1 Promise.prototype.catch
 */
var promProtoCatch = function catchFn(onRejected) {
    var promise = this;
    return promise.then(undefined, onRejected);
};

/**
 * 25.4.5.3 Promise.prototype.then ( onFulfilled , onRejected )
 */
var promProtoThen = function then(onFulfilled, onRejected) {
    var promise = this;
    if (!Internal.IsPromise(promise)) {
        throw new TypeError("cannot call 'then' on a non-promise");
    }
    var C = SpeciesConstructor(promise, Promise);
    Internal.PromiseHook(-1 /* parent info */, promise);
    var resultCapability = NewPromiseCapability(C);
    return PerformPromiseThen(promise, onFulfilled, onRejected, resultCapability);
};

/**
 * 25.4.5.3.1 PerformPromiseThen
 */
function PerformPromiseThen(promise, onFulfilled, onRejected, resultCapability) {
    Internal.Assert(Internal.IsPromise(promise));
    Internal.Assert(IsPromiseCapabilityRecord(resultCapability));
    if (!Internal.IsCallable(onFulfilled)) {
        onFulfilled = Identity;
    }
    if (!Internal.IsCallable(onRejected)) {
        onRejected = Thrower;
    }

    var fulfillReaction = PromiseReactionRecord(resultCapability, onFulfilled);
    var rejectReaction = PromiseReactionRecord(resultCapability, onRejected);
    if (promise[PROMISE_STATE] === PENDING) {
        Internal.ArrayPush(promise[PROMISE_FULFILL_REACTIONS], fulfillReaction);
        Internal.ArrayPush(promise[PROMISE_REJECT_REACTIONS], rejectReaction);
    } else if (promise[PROMISE_STATE] === FULFILLED) {
        EnqueueJob(PromiseReactionJob(fulfillReaction, promise[PROMISE_RESULT]));
    } else if (promise[PROMISE_STATE] === REJECTED) {
        if (!promise[PROMISE_IS_HANDLED]) {
            HostPromiseRejectionTracker(promise, OPERATION_HANDLE);
        }
        EnqueueJob(PromiseReactionJob(rejectReaction, promise[PROMISE_RESULT]));
    }
    promise[PROMISE_IS_HANDLED] = true;
    return resultCapability.promise;
}

/**
 * 8.4.1 EnqueueJob
 */
function EnqueueJob(job) {
    Internal.NextTick(job);
}

/**
 * Promise.prototype.finally ( onFinally )
 */
var promProtoFinally = function _finally(onFinally) {
    var promise = this;
    if (!Internal.IsObject(promise)) {
        throw new TypeError("cannot call 'finally' on a non-promise");
    }
    var C = SpeciesConstructor(promise, Promise);
    Internal.Assert(Internal.IsConstructor(C));
    var thenFinally, catchFinally;
    if (!Internal.IsCallable(onFinally)) {
        thenFinally = onFinally;
        catchFinally = onFinally;
    } else {
        thenFinally = promThenFinally();
        catchFinally = promCatchFinally();
        thenFinally[PROMISE_FINALLY_CONSTRUCTOR] = C;
        catchFinally[PROMISE_FINALLY_CONSTRUCTOR] = C;
        thenFinally[PROMISE_ON_FINALLY] = onFinally;
        catchFinally[PROMISE_ON_FINALLY] = onFinally;
    }
    return Internal.CallFunction(promise.then, promise, thenFinally, catchFinally);
};

function promThenFinally() {
    var fun = (0, function(value) {
        var onFinally = fun[PROMISE_ON_FINALLY];
        Internal.Assert(Internal.IsCallable(onFinally));
        var result = Internal.CallFunction(onFinally,undefined);
        var C = fun[PROMISE_FINALLY_CONSTRUCTOR];
        Internal.Assert(Internal.IsConstructor(C));
        var promise = PromiseResolve(C, result);
        var valueThunk = function() { return value; }
        return Internal.CallFunction(promise.then, promise, valueThunk);
    });
    return fun;
}

function promCatchFinally() {
    var fun = (0, function(reason) {
        var onFinally = fun[PROMISE_ON_FINALLY];
        Internal.Assert(Internal.IsCallable(onFinally));
        var result = Internal.CallFunction(onFinally,undefined);
        var C = fun[PROMISE_FINALLY_CONSTRUCTOR];
        Internal.Assert(Internal.IsConstructor(C));
        var promise = PromiseResolve(C, result);
        return Internal.CallFunction(promise.then, promise, () => { throw reason; });
    });
    return fun;
}

function PromiseResolve(C, x) {
    Internal.Assert(Internal.IsObject(C));
    if (Internal.IsPromise(x)) {
        if (x.constructor === C) { //SameValue
            return x;
        }
    }
    var promiseCapability = NewPromiseCapability(C);
    Internal.CallFunction(promiseCapability.resolve, undefined, x);
    return promiseCapability.promise;
}

/**
 * 25.4.5.2 Promise.prototype.constructor
 */
// Register promise constructor in the Global object.
Internal.ObjectDefineProperty(Internal.GetGlobalObject(), 'Promise', {value: Promise, configurable: true, enumerable: false, writable: true});

function speciesGet() {return this;};
Internal.SetFunctionName(speciesGet, "get [Symbol.species]");
Internal.SetFunctionName(promProtoCatch, "catch");
Internal.SetFunctionName(promProtoFinally, "finally");

Internal.CreateMethodProperty(Promise.prototype, 'constructor', Promise);
Internal.ObjectDefineProperty(Promise.prototype, Symbol.toStringTag, {value: "Promise", configurable: true, enumerable: false, writable: false});
Internal.CreateMethodProperty(Promise.prototype, "catch", promProtoCatch);
Internal.CreateMethodProperty(Promise.prototype, "then", promProtoThen);
Internal.CreateMethodProperty(Promise.prototype, "finally", promProtoFinally);
Internal.CreateMethodProperty(Promise, "all", promAll);
Internal.CreateMethodProperty(Promise, "reject", promReject);
Internal.CreateMethodProperty(Promise, "resolve", promResolve);
Internal.CreateMethodProperty(Promise, "race", promRace);
Internal.ObjectDefineProperty(Promise, Symbol.species, {set: undefined, enumerable: false, configurable: true, get: speciesGet});

if (Internal.V8CompatibilityMode) {
    var promDefer = function() {
        var deferred = {};
        deferred.promise = new this(function(resolve, reject) {
            deferred.resolve = resolve;
            deferred.reject = reject;
        });
        return deferred;
    };

    Internal.CreateMethodProperty(Promise.prototype, "chain", promProtoThen);
    Internal.CreateMethodProperty(Promise, "accept", promResolve);
    Internal.CreateMethodProperty(Promise, "defer", promDefer);
}


// ##### Internal semantics of ECMA2017 async functions that is based on promises

/**
 * Used in 14.6.11 EvaluateBody and 25.5.5.2 AsyncFunctionStart
 */
var NewDefaultCapability = function() {
    return NewPromiseCapability(Promise);
}

Internal.RegisterAsyncFunctionBuiltins(NewDefaultCapability, PerformPromiseThen);

})();
