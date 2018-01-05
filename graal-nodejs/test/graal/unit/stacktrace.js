/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('StackTrace', function () {
    describe('GetFrameCount', function () {
        it('should return a number', function () {
            var frameCount = module.StackTrace_GetFrameCount();
            assert.strictEqual(typeof frameCount, "number");
            assert.strictEqual(frameCount > 0, true);
        });
    });
    describe('GetFrame', function () {
        it('can access method', function () {
            assert.strictEqual(module.StackTrace_CanGetFrame(), true);
        });
    });
});

describe('StackFrame', function () {
    describe('GetLineNumber', function () {
        it('should return a number', function () {
            var column = module.StackTrace_FrameGetLineNumber();
            assert.strictEqual(typeof column, "number");
            assert.strictEqual(column > 0, true);
        });
    });
    describe('GetColumn', function () {
        it('should return a number', function () {
            var column = module.StackTrace_FrameGetColumn();
            assert.strictEqual(typeof column, "number");
            assert.strictEqual(column > 0, true);
        });
    });
    describe('GetFunctionName', function () {
        it('should return a string', function thisNameReturned() {
            var name = module.StackTrace_FrameGetFunctionName();
            assert.strictEqual(typeof name, "string");
            assert.strictEqual(name, "thisNameReturned");
        });
    });
    describe('GetScriptName', function () {
        it('should return a string', function () {
            var name = module.StackTrace_FrameGetScriptName();
            assert.strictEqual(typeof name, "string");
            assert.strictEqual(name.length > 0, true);
            assert.strictEqual(name.indexOf("stacktrace.js") >= 0, true);
        });
    });
    describe('IsEval', function () {
        it('should not be inside eval', function () {
            var isEval = module.StackTrace_FrameIsEval();
            assert.strictEqual(isEval, false);
        });
        it.skip('should be inside eval', function () { //never returns true for any frame, on Node.js
            var isEval = eval("function test() { return module.StackTrace_FrameIsEval(); }; test();");
            assert.strictEqual(isEval, true);
        });
    });
    describe.skip('GetScriptId', function () { //not supported by our v8.h
        it('should return an integer', function () {
            var id = module.StackTrace_FrameGetScriptId();
            console.log(id);
            assert.strictEqual(typeof id, "number");
            assert.strictEqual(id > 0, true);
        });
    });
});

