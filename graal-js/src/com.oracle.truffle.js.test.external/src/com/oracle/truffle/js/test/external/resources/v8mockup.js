/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
/**
 * This script provides compatibility functions so we (GraalJS, but also
 * Nashorn, ...) can execute the V8 test suite. Note that the functions have
 * been renamed from e.g. %DeoptimizeFunction to v8DeoptimizeFunction, as "%" is
 * no valid part of a function name.
 */

 "use strict";

// v8IgnoreResult is a special value returned by methods that we cannot
// mock-up properly (like v8HasFastProperties()). This value is accepted
// by both assertTrue() and assertFalse()
var v8IgnoreResult = {};

assertEquals = (function (originalAssertEquals) {
    return function (expected, found, user_message) {
        if (expected !== v8IgnoreResult && found !== v8IgnoreResult) {
            originalAssertEquals(expected, found, user_message);
        }
    };
})(assertEquals);

assertSame = (function (originalAssertSame) {
    return function (expected, found, user_message) {
        if (expected !== v8IgnoreResult && found !== v8IgnoreResult) {
            originalAssertSame(expected, found, user_message);
        }
    };
})(assertSame);

// ---------------------- V8 Compiler ---------------------- //
// let compiler tests pass by overriding following functions
// that are originally defined in `mjsunit.js` file
assertUnoptimized = function() {
}
assertOptimized = function() {
}
isNeverOptimize = function() {
    return v8IgnoreResult;
}
isAlwaysOptimize = function() {
    return v8IgnoreResult;
}
isInterpreted = function() {
    return v8IgnoreResult;
}
isOptimized = function() {
    return v8IgnoreResult;
}
isCrankshafted = function() {
    return v8IgnoreResult;
}
isTurboFanned = function() {
    return v8IgnoreResult;
}
// ---------------------- V8 Compiler ---------------------- //


function v8OptimizeFunctionOnNextCall() {
    return undefined;
}

function v8DeoptimizeFunction() {
    return undefined;
}

function v8NeverOptimizeFunction() {
    return undefined;
}

function v8ClearFunctionTypeFeedback() {
    return undefined;
}

function v8RunMicrotasks() {
    return undefined;
}

function v8EnqueueMicrotask(a) {
    return undefined;
}

function v8DebugPrint() {
    for (var i in arguments) {
        print(arguments[i]);
    }
    return undefined;
}

function v8DebugPrintScopes() {
    return undefined;
}

function v8ArgumentsLength(arg) {
    return arg.length;
}

function v8Arguments(arg,i) {
    return arg[i];
}

function v8NotifyContextDisposed() {
    return true;
}

function v8IsConcurrentRecompilationSupported() {
    return true;
}

function v8GetOptimizationStatus() {
    return 0;
}

function v8ToFastProperties(obj) {
    return obj;
}

function v8FlattenString(str) {
    return str;
}

function v8IsValidSmi(value) {
    return TestV8.class(value) === "java.lang.Integer";
}

function v8HasFastObjectElements() {
    return v8IgnoreResult;
}

function v8HasFastSmiOrObjectElements() {
    return v8IgnoreResult;
}

function v8HasFastSmiElements(array) {
    return v8IgnoreResult;
}

function v8HasFastDoubleElements(array) {
    return v8IgnoreResult;
}

function v8HasFastHoleyElements(obj) {
    return v8IgnoreResult;
}

function v8HasFastProperties() {
    return v8IgnoreResult;
}

function v8HasDictionaryElements() {
    return v8IgnoreResult;
}

function v8HaveSameMap(obj1, obj2) {
    return v8IgnoreResult;
}

function v8HasExternalArrayElements() {
    return true;
}

function v8HasExternalByteElements() {
    return true;
}

function v8HasExternalUnsignedByteElements() {
    return true;
}

function v8HasFixedUint8Elements(ob) {
    return true;
}

function v8HasFixedInt16Elements(ob) {
    return true;
}

function v8HasFixedUint16Elements(ob) {
    return true;
}

function v8HasFixedInt32Elements(ob) {
    return true;
}

function v8HasFixedUint32Elements(ob) {
    return true;
}

function v8HasFixedFloat32Elements(ob) {
    return true;
}

function v8HasFixedFloat64Elements(ob) {
    return true;
}

function v8HasFixedUint8ClampedElements(ob) {
    return true;
}

function gc() {
    return undefined;
}

function v8IsMinusZero(a) {
    if (a === 0 && typeof(a) === "number") {
        return (1/a) === -Infinity;
    } else {
        return false;
    }
}

function v8RunningInSimulator() {
    return false;
}

function v8StringCharCode(st, idx) {
    return st.charAt(idx);
}

function v8StringCharCodeAt(st, idx) {
    return st.charCodeAt(idx);
}

function v8StringFromCharCode(code) {
    return String.fromCharCode(code);
}

function v8UnblockConcurrentRecompilation() {
    return undefined;
}

function v8IsObject(obj) {
    return typeof(obj) === "object" || typeof(obj) === "undefined";
}

function v8GetOptimizationCount(obj) {
    return 1;
}

function v8MaxSmi() {
    return 2147483647;
}

function v8DebugDisassembleFunction() {
    return undefined;
}

function v8SetProperty(obj, name, value) {
    obj[name] = value;
}

function v8SetAllocationTimeout() {
    return undefined;
}

function v8OptimizeObjectForAddingMultipleProperties(obj) {
    return obj;
}

function v8TryMigrateInstance(obj) {
    return obj;
}

function v8StringParseInt(str, rad) {
    return parseInt(str,rad);
}

function externalizeString(str) {
    return str;
}

function v8CallFunction() {
    var thisObj = arguments[0];
    var func = arguments[arguments.length-1];
    var newArgs = [];
    for (var i=1;i<arguments.length-1;i++) {
        newArgs[i-1] = arguments[i];
    }
    return func.call(thisObj,...newArgs);
}

function v8Call() {
    var func = arguments[0];
    var thisObj = arguments[1];
    var newArgs = [];
    for (var i=2;i<arguments.length;i++) {
        newArgs[i-2] = arguments[i];
    }
    return func.call(thisObj,...newArgs);
}

function v8Apply() {
    var thisObj = arguments[0];
    var func = arguments[1];
    var newArgs = [];
    for (var i=2;i<arguments.length;i++) {
        newArgs[i-2] = arguments[i];
    }
    return func.apply(thisObj,...newArgs);
}


function v8GlobalParseInt(value, radix) {
    return parseInt(value, radix);
}

function v8SetFlags(flags) {
    return undefined;
}

function v8Break() {
    return undefined;
}

function v8DebugBreak() {
    return undefined;
}

function v8NewString(len, flag) {
    return new String(len);
}

function v8ValueOf(obj) {
    return obj.valueOf();
}

function v8ClassOf(obj) {
    return TestV8.className(obj);
}

function v8ObjectFreeze(obj) {
    return obj.freeze();
}

function v8StringCharFromCode(str) {
    return str;
}

function v8SmiLexicographicCompare(a, b) {
    return TestV8.stringCompare(a+"",b+"");
}

function v8StringCompare(a, b) {
    return TestV8.stringCompare(a+"",b+"");
}

function v8StringCompareRT(a, b) {
    return v8StringCompare(a, b);
}

function v8HasFixedInt8Elements() {
    return false;
}

function v8MakeReferenceError(message) {
    return new ReferenceError(message);
}

function v8AbortJS(message) {
    exit(message);
}

function v8HomeObjectSymbol() {
    return Symbol("__home__");
}

function v8PreventExtensions(obj) {
    return Object.preventExtensions(obj);
}

function v8GetPropertyNames(obj) {
    return Object.keys(obj);
}

function v8NormalizeElements(arr) {
    return arr;
}

function v8SymbolIsPrivate(sym) {
    return false;
}

function v8CreatePrivateSymbol(sym) {
    return Symbol(sym);
}

function v8CreatePrivateOwnSymbol(sym) {
    return Symbol(sym);
}

function v8ArrayBufferNeuter(arr) {
    TestV8.typedArrayDetachBuffer(arr);
    return arr;
}

function v8GetScript(name) {
    return undefined;
}

function v8AddNamedProperty(object, name, valueParam, prop) {
    var isWritable = !(prop & 1);
    var isEnumerable = !(prop & 2);
    var isConfigurable = !(prop & 4); 
    
    var propDesc = { value: valueParam, writable: isWritable, enumerable: isEnumerable, configurable: isConfigurable };
    
    return Object.defineProperty(object, name, propDesc);
}

function v8SetValueOf(a,b) {
    return a;
}

function v8OneByteSeqStringSetChar() {
}

function v8TwoByteSeqStringSetChar() {
}

function v8IsRegExp(obj) {
    return Object.prototype.toString.call(obj) == "[object RegExp]";
}

function v8IsArray(obj) {
    return Object.prototype.toString.call(obj) == "[object Array]";
}

function v8IsFunction(obj) {
    return Object.prototype.toString.call(obj) == "[object Function]";
}

function v8IsSpecObject(obj) {
    return Object.prototype.toString.call(obj) == "[object Date]";
}

function v8FunctionGetScript(scr) {
}

function v8ConstructDouble(hi,lo) {
    return TestV8.constructDouble(hi,lo);
}

function v8DoubleHi(value) {
    return TestV8.doubleHi(value);
}

function v8DoubleLo(value) {
    return TestV8.doubleLo(value);
}

function v8HasSloppyArgumentsElements(a) {
    return true;
}

function v8MakeError(i,msg) {
    return new Error(msg);
}

function v8AllocateHeapNumber() {
    return 0;
}

function v8DeoptimizeNow() {
    TestV8.deoptimize();
}

function v8OptimizeOsr() {
    return undefined;
}

function v8DisassembleFunction(f) {
    TestV8.deoptimize(); // not what is expected
    return undefined;
}

function v8GetUndetectable() {
    return undefined;
}

function v8FixedArrayGet(vector, slot) {
    return vector[slot];
}

function v8GetTypeFeedbackVector(vector) {
    return undefined;
}

function v8DebugGetLoadedScripts() {
    return undefined;
}

function v8IsSmi(value){
    return typeof(value)==="number" && Math.floor(value) === value && !v8IsMinusZero(value) && TestV8.class(value) === "java.lang.Integer" && value != 2147483648;
}

function v8FunctionGetInferredName(a) {
    return a.name;
}

function v8TruncateString(string, index) {
    return string.substring(0,index);
}

function v8MathClz32(a) {
    return Math.clz32(a);
}

function v8ScheduleBreak() {
    return undefined;
}

function v8FormatMessageString(i, m, o, p) {
    if (i < 0) { throw new TypeError("out of bounds"); }
    return new Error(m+o+p);
}

function v8ToMethod(f, o) {
    return f.toMethod(o);
}

function v8RegExpConstructResult() {
    return undefined;
}

function v8DefineAccessorPropertyUnchecked() {
    throw new Error("illegal access");
}

function v8DefineDataPropertyUnchecked() {
    throw new Error("illegal access");
}

function v8NewObjectFromBound(obj) {
    return undefined;
}

function v8IsConstructCall(obj) {
    return false;
}

function v8DeoptimizeNow() {
    return undefined;
}

function v8AtomicsFutexNumWaitersForTesting(a, b) {
    return 0;
}

function v8ToLength(a) {
    return TestV8.toLength(a);
}

function v8ToName(a) {
    return TestV8.toName(a);
}

function v8ToString(a) {
    return TestV8.toStringConv(a);
}

function v8ToPrimitive(a) {
    return TestV8.toPrimitive(a);
}

function v8ToPrimitive_Number(a) {
    return TestV8.toPrimitiveNumber(a);
}

function v8ToPrimitive_String(a) {
    return TestV8.toPrimitiveString(a);
}

function v8ToNumber(a) {
    return TestV8.toNumber(a);
}

function v8GetHoleNaNUpper() {
    return 0;
}

function v8GetHoleNaNLower() {
    return 2146959360;
}

function v8TailCall() {
    return undefined;
}

function v8IsJSReceiver(a) {
    return typeof(a) === "object";
}

function v8FunctionGetSourceCode() {
}

function v8SetForceInlineFlag() {
}

function v8MathSqrt(a) {
    return Math.sqrt(a);
}

function v8IsAsmWasmCode() {
    return v8IgnoreResult;
}

function v8IsNotAsmWasmCode() {
    return v8IgnoreResult;
}

function v8ExecuteInDebugContext() {
    return undefined;
}

function v8SpeciesProtector() {
    return v8IgnoreResult;
}

function v8GeneratorGetFunction() {
    return undefined;
}

function v8BaselineFunctionOnNextCall() {
    return undefined;
}

function v8InterpretFunctionOnNextCall() {
    return undefined;
}

function v8CreateDataProperty(obj, key, val) {
    obj[key] = val;
}

function v8ClearFunctionFeedback(obj) {
}

function v8HasDoubleElements(obj) {
    return v8IgnoreResult;
}

function v8HasSmiElements(obj) {
    return v8IgnoreResult;
}

function v8HasSmiOrObjectElements(obj) {
    return v8IgnoreResult;
}

function v8HasObjectElements(obj) {
    return v8IgnoreResult;
}

function v8HasHoleyElements(obj) {
    return v8IgnoreResult;
}

function v8HeapObjectVerify(obj) {
    return v8IgnoreResult;
}

function v8DebugCollectCoverage(obj) {
    return [];
}

function v8GetDeoptCount() {
    return v8IgnoreResult;
}

function v8CreateAsyncFromSyncIterator(obj) {
    return obj;
}

function v8InNewSpace(obj) {
    return obj;
}

function v8WasmNumInterpretedCalls() {
    return undefined;
}

function v8RedirectToWasmInterpreter() {
}

function v8SetWasmCompileControls() {
}

function v8TypeProfile() {
}

function v8IsWasmCode() {
    return v8IgnoreResult;
}

function v8CollectGarbage() {
}

function v8SetWasmInstantiateControls() {
}

function v8ConstructConsString(s1, s2) {
    return s1 + s2;
}

function v8MaxSmi() {
    return 0;
}

function v8IsJSReceiver() {
    return v8IgnoreResult;
}

function v8InternalizeString(str) {
    return str;
}

function v8DebugToggleBlockCoverage() {}

function v8StringMaxLength() {
  return TestV8.stringMaxLength;
}

function isOneByteString() {
    return v8IgnoreResult;
}
