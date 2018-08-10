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

var assert = require('assert');
var module = require('./_unit');
var thrower = function () {
    throw new Error();
};

describe('StackTrace', function () {
    describe('GetFrameCount', function () {
        it('should return a number', function () {
            var frameCount = module.StackTrace_GetFrameCount(thrower);
            assert.strictEqual(typeof frameCount, "number");
            assert.strictEqual(frameCount > 0, true);
        });
    });
    describe('GetFrame', function () {
        it('can access method', function () {
            assert.strictEqual(module.StackTrace_CanGetFrame(thrower), true);
        });
    });
});

describe('StackFrame', function () {
    describe('GetLineNumber', function () {
        it('should return a number', function () {
            var column = module.StackTrace_FrameGetLineNumber(thrower);
            assert.strictEqual(typeof column, "number");
            assert.strictEqual(column > 0, true);
        });
    });
    describe('GetColumn', function () {
        it('should return a number', function () {
            var column = module.StackTrace_FrameGetColumn(thrower);
            assert.strictEqual(typeof column, "number");
            assert.strictEqual(column > 0, true);
        });
    });
    describe('GetFunctionName', function () {
        it('should return a string', function () {
            var name = module.StackTrace_FrameGetFunctionName(function thisNameReturned() {
                throw new Error();
            });
            assert.strictEqual(typeof name, "string");
            assert.strictEqual(name, "thisNameReturned");
        });
    });
    describe('GetScriptName', function () {
        it('should return a string', function () {
            var name = module.StackTrace_FrameGetScriptName(thrower);
            assert.strictEqual(typeof name, "string");
            assert.strictEqual(name.length > 0, true);
            assert.strictEqual(name.indexOf("stacktrace.js") >= 0, true);
        });
    });
    describe('IsEval', function () {
        it('should not be inside eval', function () {
            var isEval = module.StackTrace_FrameIsEval(thrower);
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

