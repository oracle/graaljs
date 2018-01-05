/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
    describe.skip('IsOneByte', function () {
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
    });
    describe('WriteUtf8', function () {
        it('should copy content to buffer', function () {
            var str = "abcABC123!$@";
            var result = module.String_CheckWriteUtf8(str);
            assert.strictEqual(result, true);
        });
    });
});
