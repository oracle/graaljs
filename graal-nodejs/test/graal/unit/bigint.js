/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

describe('BigInt', function () {
    var minInt64 = -(2n**63n);
    var maxInt64 = 2n**63n-1n;
    var minUint64 = 0n;
    var maxUint64 = 2n**64n-1n;

    describe('New', function () {
        it('should work for extreme values', function () {
            assert.strictEqual(module.BigInt_NewMinValue(), minInt64);
            assert.strictEqual(module.BigInt_NewMaxValue(), maxInt64);
        });
    });
    describe('NewFromUnsigned', function () {
        it('should work for extreme values', function () {
            assert.strictEqual(module.BigInt_NewFromUnsignedMinValue(), minUint64);
            assert.strictEqual(module.BigInt_NewFromUnsignedMaxValue(), maxUint64);
        });
    });
    describe('NewFromWords', function () {
        it('should respect sign bit', function () {
            assert.strictEqual(module.BigInt_NewFromWords(0, 1, [42]), 42n);
            assert.strictEqual(module.BigInt_NewFromWords(1, 1, [42]), -42n);
        });
        it('should work for a basic case', function () {
            var words = [211, 42, 3];
            var expected = 0n;
            for (var i = 0; i < words.length; i++) {
                expected += (BigInt(words[i]) << BigInt(i)*64n);
            }
            assert.strictEqual(module.BigInt_NewFromWords(0, words.length, words), expected);
        });
        it('should work for a large word count', function () {
            var words = new Array(501);
            words.fill(0);
            words[500] = 42;
            var expected = -(42n << (500n*64n));
            assert.strictEqual(module.BigInt_NewFromWords(1, words.length, words), expected);
        });
    });
    describe('Int64Value', function () {
        it('should return correct result for extreme values', function () {
            assert.strictEqual(module.BigInt_Int64Value(minInt64), minInt64);
            assert.strictEqual(module.BigInt_Int64Value(maxInt64), maxInt64);
            assert.strictEqual(module.BigInt_Int64Value(minInt64-1n), maxInt64);
            assert.strictEqual(module.BigInt_Int64Value(maxInt64+1n), minInt64);
        });
        it('should return correct lossless flag for extreme values', function () {
            assert.strictEqual(module.BigInt_Int64ValueLossLess(minInt64), true);
            assert.strictEqual(module.BigInt_Int64ValueLossLess(maxInt64), true);
            assert.strictEqual(module.BigInt_Int64ValueLossLess(minInt64-1n), false);
            assert.strictEqual(module.BigInt_Int64ValueLossLess(maxInt64+1n), false);
        });
    });
    describe('Uint64Value', function () {
        it('should return correct result for extreme values', function () {
            assert.strictEqual(module.BigInt_Uint64Value(minUint64), minUint64);
            assert.strictEqual(module.BigInt_Uint64Value(maxUint64), maxUint64);
            assert.strictEqual(module.BigInt_Uint64Value(minUint64-1n), maxUint64);
            assert.strictEqual(module.BigInt_Uint64Value(maxUint64+1n), minUint64);
        });
        it('should return correct lossless flag for extreme values', function () {
            assert.strictEqual(module.BigInt_Uint64ValueLossLess(minUint64), true);
            assert.strictEqual(module.BigInt_Uint64ValueLossLess(maxUint64), true);
            assert.strictEqual(module.BigInt_Uint64ValueLossLess(minUint64-1n), false);
            assert.strictEqual(module.BigInt_Uint64ValueLossLess(maxUint64+1n), false);
        });
    });
    describe('WordCount', function () {
        it('should return correct value for zero', function () {
            assert.strictEqual(module.BigInt_WordCount(0n), 0);
        });
        it('should return correct value for positive numbers', function () {
            var value = 1n;
            for (var i = 1; i <= 1000; i++) {
                assert.strictEqual(module.BigInt_WordCount(value), Math.ceil(i/64));
                value <<= 1n;
            }
        });
        it('should return correct value for negative numbers', function () {
            var value = -1n;
            for (var i = 1; i <= 1000; i++) {
                assert.strictEqual(module.BigInt_WordCount(value), Math.ceil(i/64));
                value <<= 1n;
            }
        });
    });
    describe('ToWordsArray', function () {
        it('should return the correct sign bit', function () {
            assert.deepStrictEqual(module.BigInt_ToWordsArray(42n, 1), [0, 1, 42]);
            assert.deepStrictEqual(module.BigInt_ToWordsArray(-42n, 1), [1, 1, 42]);
        });
        it('should respect word count', function () {
            var bigInt = (211n << 128n) + (42n << 64n) + 7n;
            assert.deepStrictEqual(module.BigInt_ToWordsArray(bigInt, 0), [0, 3]);
            assert.deepStrictEqual(module.BigInt_ToWordsArray(bigInt, 1), [0, 3, 7]);
            assert.deepStrictEqual(module.BigInt_ToWordsArray(bigInt, 2), [0, 3, 7, 42]);
            assert.deepStrictEqual(module.BigInt_ToWordsArray(bigInt, 3), [0, 3, 7, 42, 211]);
            assert.deepStrictEqual(module.BigInt_ToWordsArray(bigInt, 4), [0, 3, 7, 42, 211]);
            assert.deepStrictEqual(module.BigInt_ToWordsArray(bigInt, 5), [0, 3, 7, 42, 211]);
        });
        it('should work for a large word count', function () {
            var bigInt = -(42n << (500n*64n));
            var expected = new Array(503);
            expected.fill(0);
            expected[0] = 1; // signBit
            expected[1] = 501; // wordCount
            expected[502] = 42; // the most significant word
            assert.deepStrictEqual(module.BigInt_ToWordsArray(bigInt, 501), expected);
        });
    });
});
