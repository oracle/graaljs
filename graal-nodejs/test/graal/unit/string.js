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

describe('String', function () {
    describe('Length', function () {
        it('should return 0 for ""', function () {
            assert.strictEqual(module.String_Length(""), 0);
        });
        it('should return 10 for "0123456789"', function () {
            assert.strictEqual(module.String_Length("0123456789"), 10);
        });
    });
    describe('Concat', function () {
        it('should return "" for concat("","")', function () {
            assert.strictEqual(module.String_Concat("", ""), "");
        });
        it('should return "a1b2" for concat("a1","b2")', function () {
            assert.strictEqual(module.String_Concat("a1", "b2"), "a1b2");
        });
    });
    describe('Utf8Length', function () {
        it('should return 0 for ""', function () {
            assert.strictEqual(module.String_Utf8Length(""), 0);
        });
        it('should return 10 for "0123456789"', function () {
            assert.strictEqual(module.String_Utf8Length("0123456789"), 10);
        });
        it('should return 2 for "\\u0061\\u0041"', function () {
            assert.strictEqual(module.String_Utf8Length("\u0061\u0041"), 2);
        });
    });
    describe('IsExternal', function () {
        it('should return false for ""', function () {
            assert.strictEqual(module.String_IsExternal(""), false);
        });
        it('should return false for "0123456789"', function () {
            assert.strictEqual(module.String_IsExternal("0123456789"), false);
        });
        it('should return false for "\\u0061\\u0041"', function () {
            assert.strictEqual(module.String_IsExternal("\u0061\u0041"), false);
        });
    });
    describe('Utf8Value', function () {
        it('create simple string', function () {
            assert.strictEqual(module.String_Utf8Value("H\u0065llo"), 5);
        });
        it('when toString() throws an exception', function () {
            var o = {
                toString: function () {
                    throw new Error('Do not stringify me!');
                }
            };
            assert.strictEqual(module.String_Utf8Value(o), 0);
        });
        it('when the string contains code-points 0x0000 or 0x007F', function () {
            assert.strictEqual(module.String_Utf8Value(String.fromCodePoint(0x0000)), 1);
            assert.strictEqual(module.String_Utf8Value(String.fromCodePoint(0x007F)), 1);
        });
        it('when the string contains code-points 0x080 or 0x07FF', function () {
            assert.strictEqual(module.String_Utf8Value(String.fromCodePoint(0x0080)), 2);
            assert.strictEqual(module.String_Utf8Value(String.fromCodePoint(0x07FF)), 2);
        });
        it('when the string contains code-points 0x0800 or 0xFFFF', function () {
            assert.strictEqual(module.String_Utf8Value(String.fromCodePoint(0x0800)), 3);
            assert.strictEqual(module.String_Utf8Value(String.fromCodePoint(0xFFFF)), 3);
        });
        it('when the string contains code-points 0x10000 or 0x10FFFF', function () {
            assert.strictEqual(module.String_Utf8Value(String.fromCodePoint(0x10000)), 4);
            assert.strictEqual(module.String_Utf8Value(String.fromCodePoint(0x10FFFF)), 4);
        });
        it('for an empty handle', function () {
            assert.strictEqual(module.String_Utf8ValueEmpty(), true);
        });
    });
    describe('IsExternalOneByte', function () {
        it('should return false for ""', function () {
            assert.strictEqual(module.String_IsExternalOneByte(""), false);
        });
    });
    describe('IsOneByte', function () {
        it('should return true for ""', function () {
            assert.strictEqual(module.String_IsOneByte(""), true);
        });
    });
    describe('Write', function () {
        it('should copy content to buffer', function () {
            var str = "abcABC123!$@";
            var result = module.String_CheckWrite(str);
            assert.strictEqual(result, true);
        });
    });
    describe('WriteOneByte', function () {
        it('should copy content to buffer', function () {
            var str = "abcABC123!$@";
            var result = module.String_CheckWriteOneByte(str);
            assert.strictEqual(result, true);
        });
        it('should use just lower byte', function () {
            assert.deepEqual([0x61], Array.from(Buffer.from('\u0161', 'latin1')));
        });
        it('should process each surrogate separately', function () {
            assert.deepEqual([0x3D, 0xA9], Array.from(Buffer.from('\uD83D\uDCA9', 'latin1')));
        });
    });
    describe('WriteUtf8', function () {
        it('should copy content to buffer', function () {
            var str = "abcABC123!$@";
            var result = module.String_CheckWriteUtf8(str);
            assert.strictEqual(result, true);
        });
    });
});
