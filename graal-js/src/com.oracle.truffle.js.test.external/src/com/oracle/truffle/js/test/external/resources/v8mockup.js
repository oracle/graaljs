/*
 * Copyright (c) 2018, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * This script provides functions for the execution of the V8 test suite.
 */

// v8IgnoreResult is a special value returned by methods that we cannot
// mock-up properly (like %HasFastProperties()). This value is accepted
// by both assertTrue() and assertFalse()
var v8IgnoreResult = {};

var assertEquals = (function (originalAssertEquals) {
    return function (expected, found, user_message) {
        if (expected !== v8IgnoreResult && found !== v8IgnoreResult) {
            originalAssertEquals(expected, found, user_message);
        }
    };
})(assertEquals);

var assertSame = (function (originalAssertSame) {
    return function (expected, found, user_message) {
        if (expected !== v8IgnoreResult && found !== v8IgnoreResult) {
            originalAssertSame(expected, found, user_message);
        }
    };
})(assertSame);

// ---------------------- V8 Compiler ---------------------- //
// let compiler tests pass by overriding following functions
// that are originally defined in `mjsunit.js` file
var assertUnoptimized = function() {
};
var assertOptimized = function() {
};
var isNeverOptimize = function() {
    return v8IgnoreResult;
};
var isNeverOptimizeLiteMode = function() {
    return v8IgnoreResult;
};
var isAlwaysOptimize = function() {
    return v8IgnoreResult;
};
var isInterpreted = function() {
    return v8IgnoreResult;
};
var isOptimized = function() {
    return v8IgnoreResult;
};
var isMaglevved = function() {
    return v8IgnoreResult;
};
var isTurboFanned = function() {
    return v8IgnoreResult;
};
var isBaseline = function () { //used by mjsunit/baseline/* tests
    return v8IgnoreResult;
};
var topFrameIsInterpreted = function() {
    return v8IgnoreResult;
};
var topFrameIsBaseline = function() {
    return v8IgnoreResult;
};
var topFrameIsMaglevved = function() {
    return v8IgnoreResult;
};
var topFrameIsTurboFanned = function() {
    return v8IgnoreResult;
};

// ---------------------- d8 global object ---------------------- //

load = (function() {
    let originalLoad = load;
    return path => originalLoad(path.startsWith('test/') ? ('lib/testv8/' + path) : path);
})();

readbuffer = (function() {
    let originalReadbuffer = readbuffer;
    return path => originalReadbuffer(path.startsWith('test/') ? ('lib/testv8/' + path) : path);
})();

Worker = (function() {
    let originalWorker = Worker;
    return function() {
        if (typeof arguments[0] === 'string' && arguments[0].startsWith('test/')) {
            arguments[0] = 'lib/testv8/' + arguments[0];
        }
        return (new.target === undefined) ? originalWorker(...arguments) : new originalWorker(...arguments);
    };
})();

// Save `load` function in another binding so that tests that re-write ``load` binding can still
// use d8.file.execute.
let d8_file_execute_load = load;

var d8 = {
    file: {
        execute: function(path) {
            d8_file_execute_load(path);
            // Ensures that assertTraps() checks just the error type
            // (WebAssembly.RuntimeError) and not the exact error message
            if (path.endsWith('wasm-module-builder.js')) {
                kTrapMsgs = [];
            }
        }
    }
};

// The following stuff should be enabled by --expose-externalize-string,
// but it does not seem to hurt to have it enabled always

var createExternalizableString = function(str) {
    return str;
};

var externalizeString = function(str) {
    if (str.length === 1 && str.codePointAt(0) < 256) {
        throw new Error("string does not support externalization.");
    }
};

function isOneByteString(str) {
    return v8IgnoreResult;
}

let kExternalStringMinOneByteLength = 1;
let kExternalStringMinTwoByteLength = 1;
let kExternalStringMinOneByteCachedLength = 5;
let kExternalStringMinTwoByteCachedLength = 3;

// ---------------------- other mockup functions ---------------- //

globalThis['%OptimizeFunctionOnNextCall'] = function(f) {
    if (typeof f === 'function') {
        Object.defineProperty(f, '_optimized', { value: true });
    }
    return undefined;
};

globalThis['%DeoptimizeFunction'] = function() {
    return undefined;
};

globalThis['%NeverOptimizeFunction'] = function() {
    return undefined;
};

globalThis['%ClearFunctionTypeFeedback'] = function() {
    return undefined;
};

globalThis['%PerformMicrotaskCheckpoint'] = function() {
    return TestV8.runMicrotasks();
};

globalThis['%EnqueueMicrotask'] = function(a) {
    return undefined;
};

globalThis['%DebugPrint'] = function() {
    for (var i in arguments) {
        print(arguments[i]);
    }
    return undefined;
};

globalThis['%DebugPrintScopes'] = function() {
    return undefined;
};

globalThis['%ArgumentsLength'] = function(arg) {
    return arg.length;
};

globalThis['%Arguments'] = function(arg,i) {
    return arg[i];
};

globalThis['%NotifyContextDisposed'] = function() {
    return true;
};

globalThis['%IsConcurrentRecompilationSupported'] = function() {
    return true;
};

globalThis['%GetOptimizationStatus'] = function(f) {
    var all_flags = 0xFFFFF;
    return f._optimized ? all_flags : (all_flags & ~V8OptimizationStatus.kTopmostFrameIsTurboFanned);
};

globalThis['%ToFastProperties'] = function(obj) {
    return obj;
};

globalThis['%FlattenString'] = function(str) {
    return str;
};

globalThis['%IsValidSmi'] = function(value) {
    return TestV8.class(value) === "java.lang.Integer";
};

globalThis['%HasFastElements'] = function() {
    return v8IgnoreResult;
};

globalThis['%HasFastPackedElements'] = function() {
    return v8IgnoreResult;
};

globalThis['%HasFastObjectElements'] = function() {
    return v8IgnoreResult;
};

globalThis['%HasFastSmiOrObjectElements'] = function() {
    return v8IgnoreResult;
};

globalThis['%HasFastSmiElements'] = function(array) {
    return v8IgnoreResult;
};

globalThis['%HasFastDoubleElements'] = function(array) {
    return v8IgnoreResult;
};

globalThis['%HasFastHoleyElements'] = function(obj) {
    return v8IgnoreResult;
};

globalThis['%HasFastProperties'] = function() {
    return v8IgnoreResult;
};

globalThis['%HasDictionaryElements'] = function() {
    return v8IgnoreResult;
};

globalThis['%HaveSameMap'] = function(obj1, obj2) {
    return v8IgnoreResult;
};

globalThis['%HasFixedUint8Elements'] = function(ob) {
    return true;
};

globalThis['%HasFixedInt16Elements'] = function(ob) {
    return true;
};

globalThis['%HasFixedUint16Elements'] = function(ob) {
    return true;
};

globalThis['%HasFixedInt32Elements'] = function(ob) {
    return true;
};

globalThis['%HasFixedUint32Elements'] = function(ob) {
    return true;
};

globalThis['%HasFixedFloat32Elements'] = function(ob) {
    return true;
};

globalThis['%HasFixedFloat64Elements'] = function(ob) {
    return true;
};

globalThis['%HasFixedUint8ClampedElements'] = function(ob) {
    return true;
};

//watch out: this might be modified by TestV8Runnable, see GR-29754.
function gc() {
    TestV8.gc();
}

globalThis['%IsMinusZero'] = function(a) {
    if (a === 0 && typeof(a) === "number") {
        return (1/a) === -Infinity;
    } else {
        return false;
    }
};

globalThis['%RunningInSimulator'] = function() {
    return false;
};

globalThis['%UnblockConcurrentRecompilation'] = function() {
    return undefined;
};

globalThis['%IsObject'] = function(obj) {
    return typeof(obj) === "object" || typeof(obj) === "undefined";
};

globalThis['%GetOptimizationCount'] = function(obj) {
    return 1;
};

globalThis['%MaxSmi'] = function() {
    return 2147483647;
};

globalThis['%DebugDisassembleFunction'] = function() {
    return undefined;
};

globalThis['%SetProperty'] = function(obj, name, value) {
    obj[name] = value;
};

globalThis['%SetAllocationTimeout'] = function() {
    return undefined;
};

globalThis['%OptimizeObjectForAddingMultipleProperties'] = function(obj) {
    return obj;
};

globalThis['%TryMigrateInstance'] = function(obj) {
    return obj;
};

globalThis['%StringParseInt'] = function(str, rad) {
    return parseInt(str,rad);
};

globalThis['%CallFunction'] = function() {
    var thisObj = arguments[0];
    var func = arguments[arguments.length-1];
    var newArgs = [];
    for (var i=1;i<arguments.length-1;i++) {
        newArgs[i-1] = arguments[i];
    }
    return func.call(thisObj,...newArgs);
};

globalThis['%Call'] = function() {
    var func = arguments[0];
    var thisObj = arguments[1];
    var newArgs = [];
    for (var i=2;i<arguments.length;i++) {
        newArgs[i-2] = arguments[i];
    }
    return func.call(thisObj,...newArgs);
};

globalThis['%Apply'] = function() {
    var thisObj = arguments[0];
    var func = arguments[1];
    var newArgs = [];
    for (var i=2;i<arguments.length;i++) {
        newArgs[i-2] = arguments[i];
    }
    return func.apply(thisObj,...newArgs);
};


globalThis['%GlobalParseInt'] = function(value, radix) {
    return parseInt(value, radix);
};

globalThis['%SetFlags'] = function(flags) {
    return undefined;
};

globalThis['%Break'] = function() {
    return undefined;
};

globalThis['%DebugBreak'] = function() {
    return undefined;
};

globalThis['%NewString'] = function(len, flag) {
    return new String(len);
};

globalThis['%ValueOf'] = function(obj) {
    return obj.valueOf();
};

globalThis['%ClassOf'] = function(obj) {
    return TestV8.className(obj);
};

globalThis['%ObjectFreeze'] = function(obj) {
    return obj.freeze();
};

globalThis['%SmiLexicographicCompare'] = function(a, b) {
    return TestV8.stringCompare(a+"",b+"");
};

globalThis['%StringCompare'] = function(a, b) {
    return TestV8.stringCompare(a+"",b+"");
};

globalThis['%HasFixedInt8Elements'] = function() {
    return false;
};

globalThis['%MakeReferenceError'] = function(message) {
    return new ReferenceError(message);
};

globalThis['%AbortJS'] = function(message) {
    printErr(message);
    quit(1);
};

globalThis['%HomeObjectSymbol'] = function() {
    return Symbol("__home__");
};

globalThis['%PreventExtensions'] = function(obj) {
    return Object.preventExtensions(obj);
};

globalThis['%GetPropertyNames'] = function(obj) {
    return Object.keys(obj);
};

globalThis['%NormalizeElements'] = function(arr) {
    return arr;
};

globalThis['%SymbolIsPrivate'] = function(sym) {
    return TestV8.symbolIsPrivate(sym);
};

globalThis['%CreatePrivateSymbol'] = function(sym) {
    return TestV8.createPrivateSymbol(sym);
};

globalThis['%ArrayBufferDetach'] = function(arr) {
    TestV8.typedArrayDetachBuffer(arr);
};

globalThis['%GetScript'] = function(name) {
    return undefined;
};

globalThis['%AddNamedProperty'] = function(object, name, valueParam, prop) {
    var isWritable = !(prop & 1);
    var isEnumerable = !(prop & 2);
    var isConfigurable = !(prop & 4); 
    
    var propDesc = { value: valueParam, writable: isWritable, enumerable: isEnumerable, configurable: isConfigurable };
    
    return Object.defineProperty(object, name, propDesc);
};

globalThis['%SetValueOf'] = function(a,b) {
    return a;
};

globalThis['%OneByteSeqStringSetChar'] = function() {
};

globalThis['%TwoByteSeqStringSetChar'] = function() {
};

globalThis['%IsRegExp'] = function(obj) {
    return Object.prototype.toString.call(obj) === "[object RegExp]";
};

globalThis['%IsArray'] = function(obj) {
    return Object.prototype.toString.call(obj) === "[object Array]";
};

globalThis['%IsFunction'] = function(obj) {
    return Object.prototype.toString.call(obj) === "[object Function]";
};

globalThis['%IsSpecObject'] = function(obj) {
    return Object.prototype.toString.call(obj) === "[object Date]";
};

globalThis['%FunctionGetScript'] = function(scr) {
};

globalThis['%ConstructDouble'] = function(hi,lo) {
    return TestV8.constructDouble(hi,lo);
};

globalThis['%DoubleHi'] = function(value) {
    return TestV8.doubleHi(value);
};

globalThis['%DoubleLo'] = function(value) {
    return TestV8.doubleLo(value);
};

globalThis['%HasSloppyArgumentsElements'] = function(a) {
    return true;
};

globalThis['%MakeError'] = function(i,msg) {
    return new Error(msg);
};

globalThis['%AllocateHeapNumber'] = function() {
    return 0;
};

globalThis['%DeoptimizeNow'] = globalThis['%_DeoptimizeNow'] = function() {
    TestV8.deoptimize();
};

globalThis['%OptimizeOsr'] = function() {
    return undefined;
};

globalThis['%DisassembleFunction'] = function(f) {
    TestV8.deoptimize(); // not what is expected
    return undefined;
};

globalThis['%GetUndetectable'] = function() {
    return undefined;
};

globalThis['%FixedArrayGet'] = function(vector, slot) {
    return vector[slot];
};

globalThis['%GetTypeFeedbackVector'] = function(vector) {
    return undefined;
};

globalThis['%DebugGetLoadedScripts'] = function() {
    return undefined;
};

globalThis['%IsSmi'] = function(value){
    return typeof(value) === "number" && Math.floor(value) === value && !globalThis['%IsMinusZero'](value) && TestV8.class(value) === "java.lang.Integer" && value !== 2147483648;
};

globalThis['%FunctionGetInferredName'] = function(a) {
    return a.name;
};

globalThis['%TruncateString'] = function(string, index) {
    return string.substring(0,index);
};

globalThis['%MathClz32'] = function(a) {
    return Math.clz32(a);
};

globalThis['%ScheduleBreak'] = function() {
    return undefined;
};

globalThis['%FormatMessageString'] = function(i, m, o, p) {
    if (i < 0) { throw new TypeError("out of bounds"); }
    return new Error(m+o+p);
};

globalThis['%ToMethod'] = function(f, o) {
    return f.toMethod(o);
};

globalThis['%RegExpConstructResult'] = function() {
    return undefined;
};

globalThis['%DefineAccessorPropertyUnchecked'] = function() {
    throw new Error("illegal access");
};

globalThis['%DefineDataPropertyUnchecked'] = function() {
    throw new Error("illegal access");
};

globalThis['%NewObjectFromBound'] = function(obj) {
    return undefined;
};

globalThis['%IsConstructCall'] = function(obj) {
    return false;
};

globalThis['%DeoptimizeNow'] = function() {
    return undefined;
};

globalThis['%AtomicsFutexNumWaitersForTesting'] = function(a, b) {
    return 0;
};

globalThis['%ToLength'] = function(a) {
    return TestV8.toLength(a);
};

globalThis['%ToName'] = function(a) {
    return TestV8.toName(a);
};

globalThis['%ToStringRT'] = function(a) {
    return TestV8.toStringConv(a);
};

globalThis['%ToPrimitive'] = function(a) {
    return TestV8.toPrimitive(a);
};

globalThis['%ToPrimitive_Number'] = function(a) {
    return TestV8.toPrimitiveNumber(a);
};

globalThis['%ToPrimitive_String'] = function(a) {
    return TestV8.toPrimitiveString(a);
};

globalThis['%ToNumber'] = function(a) {
    return TestV8.toNumber(a);
};

globalThis['%GetHoleNaNUpper'] = function() {
    return 0;
};

globalThis['%GetHoleNaNLower'] = function() {
    return 2146959360;
};

globalThis['%TailCall'] = function() {
    return undefined;
};

globalThis['%IsJSReceiver'] = function(a) {
    return typeof(a) === "object";
};

globalThis['%FunctionGetSourceCode'] = function() {
};

globalThis['%SetForceInlineFlag'] = function() {
};

globalThis['%MathSqrt'] = function(a) {
    return Math.sqrt(a);
};

globalThis['%IsAsmWasmCode'] = function() {
    return v8IgnoreResult;
};

globalThis['%IsNotAsmWasmCode'] = function() {
    return v8IgnoreResult;
};

globalThis['%ExecuteInDebugContext'] = function() {
    return undefined;
};

globalThis['%SpeciesProtector'] = function() {
    return v8IgnoreResult;
};

globalThis['%GeneratorGetFunction'] = function() {
    return undefined;
};

globalThis['%BaselineFunctionOnNextCall'] = function() {
    return undefined;
};

globalThis['%InterpretFunctionOnNextCall'] = function() {
    return undefined;
};

globalThis['%CreateDataProperty'] = globalThis['%_CreateDataProperty'] = function(obj, key, val) {
    "use strict";
    obj[key] = val;
};

globalThis['%ClearFunctionFeedback'] = function(obj) {
};

globalThis['%HasDoubleElements'] = function(obj) {
    return v8IgnoreResult;
};

globalThis['%HasSmiElements'] = function(obj) {
    return v8IgnoreResult;
};

globalThis['%HasSmiOrObjectElements'] = function(obj) {
    return v8IgnoreResult;
};

globalThis['%HasObjectElements'] = function(obj) {
    return v8IgnoreResult;
};

globalThis['%HasHoleyElements'] = function(obj) {
    return v8IgnoreResult;
};

globalThis['%HeapObjectVerify'] = function(obj) {
    return v8IgnoreResult;
};

globalThis['%DebugCollectCoverage'] = function(obj) {
    return [];
};

globalThis['%GetDeoptCount'] = function() {
    return v8IgnoreResult;
};

globalThis['%CreateAsyncFromSyncIterator'] = function(obj) {
    return TestV8.createAsyncFromSyncIterator(obj);
};

globalThis['%InNewSpace'] = function(obj) {
    return obj;
};

globalThis['%WasmNumInterpretedCalls'] = function() {
    return undefined;
};

globalThis['%RedirectToWasmInterpreter'] = function() {
};

globalThis['%SetWasmCompileControls'] = function() {
};

globalThis['%TypeProfile'] = function() {
};

globalThis['%IsWasmCode'] = function() {
    return v8IgnoreResult;
};

globalThis['%CollectGarbage'] = function() {
};

globalThis['%SetWasmInstantiateControls'] = function() {
};

globalThis['%ConstructConsString'] = function(s1, s2) {
    return s1 + s2;
};

globalThis['%IsJSReceiver'] = function() {
    return v8IgnoreResult;
};

globalThis['%InternalizeString'] = function(str) {
    return str;
};

globalThis['%DebugToggleBlockCoverage'] = function() {
};

globalThis['%StringMaxLength'] = function() {
  return TestV8.stringMaxLength;
};

globalThis['%GetCallable'] = function(target_function_name) {
    if (target_function_name) {
        // This is the upcoming behavior of the native.
        return globalThis[target_function_name];
    } else {
        // This is the current behavior of the native.
        return (a, b) => a - b;
    }
};

globalThis['%GetDefaultICULocale'] = function() {
    throw new Error("v8 internal method not implemented");
};

globalThis['%SetForceSlowPath'] = function() {
};

globalThis['%SetKeyedProperty'] = function(object, key, value, language_mode) {
    object[key] = value;
};

globalThis['%DebugGetLoadedScriptIds'] = function() {
};

globalThis['%DebugTogglePreciseCoverage'] = function() {
};

globalThis['%DebugTrace'] = function() {
    throw new Error("v8 internal method not implemented");
};

globalThis['%DisallowCodegenFromStrings'] = function() {
};

globalThis['%DisallowWasmCodegen'] = function() {
};

globalThis['%ICsAreEnabled'] = function() {
    return true;
};

globalThis['%IsLiftoffFunction'] = function() {
    return v8IgnoreResult;
};

globalThis['%regexp_internal_match'] = function(regexp, string) {
    return regexp.exec(string);
};

globalThis['%Typeof'] = function(object) {
    return typeof(object);
};

globalThis['%CompleteInobjectSlackTracking'] = function(object) {
};

globalThis['%ArraySpeciesProtector'] = function() {
    return v8IgnoreResult;
};

globalThis['%MapIteratorProtector'] = function() {
    return v8IgnoreResult;
};

globalThis['%SetIteratorProtector'] = function() {
    return v8IgnoreResult;
};

globalThis['%StringIteratorProtector'] = function() {
    return v8IgnoreResult;
};

globalThis['%CreateIterResultObject'] = function(value, done) {
    return { value: value, done: !!done };
};

globalThis['%ConstructSlicedString'] = function(string, index) {
    return string.substring(index);
};

globalThis['%StrictEqual'] = function(x, y) {
    return x === y;
};

globalThis['%StrictNotEqual'] = function(x, y) {
    return x !== y;
};

globalThis['%Equal'] = function(x, y) {
    return x == y;
};

globalThis['%NotEqual'] = function(x, y) {
    return x != y;
};

globalThis['%LessThan'] = function(x, y) {
    return x < y;
};

globalThis['%LessThanOrEqual'] = function(x, y) {
    return x <= y;
};

globalThis['%GreaterThan'] = function(x, y) {
    return x > y;
};

globalThis['%GreaterThanOrEqual'] = function(x, y) {
    return x >= y;
};

globalThis['%StringLessThan'] = function(x, y) {
    return x < y;
};

globalThis['%SetAllowAtomicsWait'] = function(allow) {
    TestV8.setAllowAtomicsWait(allow);
};

globalThis['%AtomicsNumWaitersForTesting'] = function(array, index) {
    return TestV8.atomicsNumWaitersForTesting(array, index);
};

globalThis['%AtomicsNumUnresolvedAsyncPromisesForTesting'] = function(array, index) {
    return TestV8.atomicsNumUnresolvedAsyncPromisesForTesting(array, index);
};

globalThis['%SerializeWasmModule'] = function() {
    throw new Error("v8 internal method not implemented");
};

globalThis['%DeserializeWasmModule'] = function() {
    throw new Error("v8 internal method not implemented");
};

globalThis['%WasmGetNumberOfInstances'] = function() {
    return v8IgnoreResult;
};

globalThis['%GetWasmExceptionId'] = function() {
    throw new Error("v8 internal method not implemented");
};

globalThis['%GetWasmExceptionValues'] = function() {
    throw new Error("v8 internal method not implemented");
};

globalThis['%FreezeWasmLazyCompilation'] = function() {
};

globalThis['%GetWasmRecoveredTrapCount'] = function() {
    return v8IgnoreResult;
};

globalThis['%IsWasmTrapHandlerEnabled'] = function() {
    return false;
};

globalThis['%WasmMemoryHasFullGuardRegion'] = function() {
    return v8IgnoreResult;
};

globalThis['%SetWasmThreadsEnabled'] = function() {
    throw new Error("v8 internal method not implemented");
};

globalThis['%WasmTierUpFunction'] = function() {
};

globalThis['%HandleDebuggerStatement'] = function() {
};

globalThis['%IsThreadInWasm'] = function() {
    return v8IgnoreResult;
};

globalThis['%EnsureFeedbackVectorForFunction'] = function() {
};

globalThis['%PrepareFunctionForOptimization'] = function() {
};

globalThis['%HasPackedElements'] = function() {
    return v8IgnoreResult;
};

globalThis['%GetProperty'] = function(receiver, key) {
    return receiver[key];
};

globalThis['%EnableCodeLoggingForTesting'] = function() {
};

globalThis['%TurbofanStaticAssert'] = function() {
};

function setTimeout(callback) {
    TestV8.setTimeout(callback);
}

var testRunner = (function() {
    var _done;
    var _waitUntilDone = function() {
        if (!_done) {
            setTimeout(_waitUntilDone);
        }
    };
    return {
        notifyDone() {
            _done = true;
        },
        waitUntilDone() {
            _done = false;
            _waitUntilDone();
        },
        quit: quit
    };
})();

globalThis['%HasElementsInALargeObjectSpace'] = function(array) {
    return v8IgnoreResult;
};

globalThis['%WasmNumCodeSpaces'] = function(argument) {
};

globalThis['%SimulateNewspaceFull'] = function() {
};

globalThis['%RegexpHasBytecode'] = function(regexp, is_latin1) {
    return v8IgnoreResult;
};

globalThis['%RegexpHasNativeCode'] =  function(regexp, is_latin1) {
    return v8IgnoreResult;
};

globalThis['%NewRegExpWithBacktrackLimit'] = function(regex, flags, limit) {
    return new RegExp(regex, flags); //limit missing
};

globalThis['%WasmTierDownModule'] = function() {
};

globalThis['%WasmTierUpModule'] = function() {
};

globalThis['%IsBeingInterpreted'] = function() {
    return v8IgnoreResult;
};

globalThis['%ArrayBufferMaxByteLength'] = function() {
    return 0x7fff_fff7;
};

globalThis['%MinSMI'] = function() {
    return -2147483648;
};

globalThis['%ReferenceEqual'] = function(a, b) {
    return TestV8.referenceEqual(a, b);
};

globalThis['%CollectTypeProfile'] = function() {
};

globalThis['%CompileBaseline'] = function() {
};

globalThis['%IsDictPropertyConstTrackingEnabled'] = function() {
    return v8IgnoreResult;
};

globalThis['%HasOwnConstDataProperty'] = function() {
    return v8IgnoreResult;
};

globalThis['%RegexpIsUnmodified'] = function() {
    return v8IgnoreResult;
};

globalThis['%PromiseSpeciesProtector'] = function() {
    return v8IgnoreResult;
};

globalThis['%RegExpSpeciesProtector'] = function() {
    return v8IgnoreResult;
};

globalThis['%TypedArraySpeciesProtector'] = function() {
    return v8IgnoreResult;
};

globalThis['%ArrayIteratorProtector'] = function() {
    return v8IgnoreResult;
};

globalThis['%ToString'] = function(a) {
    return TestV8.toStringConv(a);
};

globalThis['%ScheduleGCInStackCheck'] = function() {
};

globalThis['%DynamicCheckMapsEnabled'] = function() {
    return v8IgnoreResult;
};

globalThis['%TierupFunctionOnNextCall'] = function() {
};

globalThis['%IsTopTierTurboprop'] = function() {
};

globalThis['%WasmTierUp'] = function() {
};

globalThis['%WasmTierDown'] = function() {
};

globalThis['%RegexpTypeTag'] = function() {
    return v8IgnoreResult;
};

globalThis['%BaselineOsr'] = function() {
    globalThis['%OptimizeFunctionOnNextCall'](globalThis['%BaselineOsr'].caller);
};

globalThis['%GetAndResetRuntimeCallStats'] = function() {
    return v8IgnoreResult;
};

globalThis['%IsConcatSpreadableProtector'] = function() {
    return v8IgnoreResult;
};

globalThis['%InLargeObjectSpace'] = function() {
    return v8IgnoreResult;
};

globalThis['%Is64Bit'] = function() {
    return v8IgnoreResult;
};

globalThis['%IsAtomicsWaitAllowed'] = function() {
    return v8IgnoreResult;
};

globalThis['%ThrowStackOverflow'] = function() {
    throw new RangeError("stack exceeded");
};

globalThis['%VerifyType'] = function() {
    return v8IgnoreResult;
};

globalThis['%PretenureAllocationSite'] = function() {
    return v8IgnoreResult;
};

function version() {
    return Graal.versionGraalVM;
};

globalThis['%CreatePrivateNameSymbol'] = function(name) {
    return TestV8.createPrivateSymbol(name);
};

globalThis['%ConstructInternalizedString'] = function(string) {
    return string;
};

globalThis['%ActiveTierIsMaglev'] = function(fun) {
    return v8IgnoreResult;
};

globalThis['%OptimizeMaglevOnNextCall'] = function(fun) {
};

globalThis['%DisableOptimizationFinalization'] = function() {
};

globalThis['%WaitForBackgroundOptimization'] = function() {
};

globalThis['%FinalizeOptimization'] = function() {
};

globalThis['%SystemBreak'] = function() {
};

globalThis['%IsSameHeapObject'] = function(obj1, obj2) {
    return Object.is(obj1, obj2);
};

globalThis['%IsSharedString'] = function(obj) {
    return typeof(obj) === "string" && v8IgnoreResult;
};

globalThis['%IsInternalizedString'] = function(obj) {
    return typeof(obj) === "string" && v8IgnoreResult;
};

globalThis['%SharedGC'] = function() {
};

globalThis['%GetWasmExceptionTagId'] = function(exception, instance) {
    throw new Error("v8 internal method not implemented");
};

globalThis['%IsTurboFanFunction'] = function(fun) {
    return v8IgnoreResult;
};

globalThis['%ArrayBufferSetDetachKey'] = function(arrayBuffer, key) {
};

globalThis['%IsTurbofanEnabled'] = function() {
    return v8IgnoreResult;
};

globalThis['%ActiveTierIsTurbofan'] = function(fun) {
    return v8IgnoreResult;
};

globalThis['%IsMaglevEnabled'] = function() {
    return v8IgnoreResult;
};

globalThis['%CurrentFrameIsTurbofan'] = function() {
    return v8IgnoreResult;
};

globalThis['%ConstructThinString'] = function(str) {
    return str;
};

globalThis['%IsSparkplugEnabled'] = function() {
    return v8IgnoreResult;
};

globalThis['%ActiveTierIsSparkplug'] = function(fun) {
    return v8IgnoreResult;
};

globalThis['%ForceFlush'] = function(fun) {
};

globalThis['%InYoungGeneration'] = function(o) {
    return v8IgnoreResult;
};

globalThis['%IsInPlaceInternalizableString'] = function(str) {
    return v8IgnoreResult;
};

globalThis['%GetWeakCollectionSize'] = function(weakMapOrSet) {
    return v8IgnoreResult;
};

globalThis['%NoElementsProtector'] = function() {
    return v8IgnoreResult;
};

globalThis['%SetBatterySaverMode'] = function(value) {
    return v8IgnoreResult;
};

globalThis['%NotifyIsolateBackground'] = function() {
};

globalThis['%NotifyIsolateForeground'] = function() {
};

globalThis['%IsEfficiencyModeEnabled'] = function() {
    return v8IgnoreResult;
};

globalThis['%GetFunctionForCurrentFrame'] = function() {
    return v8IgnoreResult;
};

globalThis['%FlushWasmCode'] = function() {
};

globalThis['%IsUncompiledWasmFunction'] = function() {
    return v8IgnoreResult;
};

globalThis['%WasmEnterDebugging'] = function() {
};

globalThis['%WasmLeaveDebugging'] = function() {
};

globalThis['%IsWasmDebugFunction'] = function() {
    return v8IgnoreResult;
};

globalThis['%SetPriorityBestEffort'] = function() {
};

globalThis['%SetPriorityUserBlocking'] = function() {
};

globalThis['%RuntimeEvaluateREPL'] = eval;

globalThis['%StringIsFlat'] = function() {
    return v8IgnoreResult;
};

globalThis['%HasCowElements'] = function() {
    return v8IgnoreResult;
};

globalThis['%StringWrapperToPrimitiveProtector'] = function() {
    return v8IgnoreResult;
};

globalThis['%TypedArrayLengthProtector'] = function() {
    return v8IgnoreResult;
};

globalThis['%GetInitializerFunction'] = function() {
};

globalThis['%DefineObjectOwnProperty'] = function(o, key, value) {
    Reflect.defineProperty(o, key, { value, configurable: true, enumerable: true, writable: true });
    return value;
};

globalThis['%GetFeedback'] = function() {
};
