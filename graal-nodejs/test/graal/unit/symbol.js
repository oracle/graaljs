/*
 * Copyright (c) 2019, 2023, Oracle and/or its affiliates. All rights reserved.
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

describe('Symbol', function () {
    describe('GetAsyncIterator', function () {
        it('should return Symbol.asyncIterator', function () {
            assert.strictEqual(module.Symbol_GetAsyncIterator(), Symbol.asyncIterator);
        });
    });
    describe('GetHasInstance', function () {
        it('should return Symbol.hasInstance', function () {
            assert.strictEqual(module.Symbol_GetHasInstance(), Symbol.hasInstance);
        });
    });
    describe('GetIsConcatSpreadable', function () {
        it('should return Symbol.isConcatSpreadable', function () {
            assert.strictEqual(module.Symbol_GetIsConcatSpreadable(), Symbol.isConcatSpreadable);
        });
    });
    describe('GetIterator', function () {
        it('should return Symbol.iterator', function () {
            assert.strictEqual(module.Symbol_GetIterator(), Symbol.iterator);
        });
    });
    describe('GetMatch', function () {
        it('should return Symbol.match', function () {
            assert.strictEqual(module.Symbol_GetMatch(), Symbol.match);
        });
    });
    describe('GetReplace', function () {
        it('should return Symbol.replace', function () {
            assert.strictEqual(module.Symbol_GetReplace(), Symbol.replace);
        });
    });
    describe('GetSearch', function () {
        it('should return Symbol.search', function () {
            assert.strictEqual(module.Symbol_GetSearch(), Symbol.search);
        });
    });
    describe('GetSplit', function () {
        it('should return Symbol.split', function () {
            assert.strictEqual(module.Symbol_GetSplit(), Symbol.split);
        });
    });
    describe('GetToPrimitive', function () {
        it('should return Symbol.toPrimitive', function () {
            assert.strictEqual(module.Symbol_GetToPrimitive(), Symbol.toPrimitive);
        });
    });
    describe('GetToStringTag', function () {
        it('should return Symbol.toStringTag', function () {
            assert.strictEqual(module.Symbol_GetToStringTag(), Symbol.toStringTag);
        });
    });
    describe('GetUnscopables', function () {
        it('should return Symbol.unscopables', function () {
            assert.strictEqual(module.Symbol_GetUnscopables(), Symbol.unscopables);
        });
    });
    describe('For', function () {
        it('should return Symbol', function () {
            assert.strictEqual(typeof module.Symbol_For('some_description'), 'symbol');
        });
        it('should return the same symbol when invoked multiple times', function () {
            var description = 'another_description';
            var symbol1 = module.Symbol_For(description);
            var symbol2 = module.Symbol_For(description);
            assert.strictEqual(symbol1, symbol2);
        });
        it('should return the same symbol as Symbol.for()', function () {
            var description = 'yet_another_description';
            var symbol1 = module.Symbol_For(description);
            var symbol2 = Symbol.for(description);
            assert.strictEqual(symbol1, symbol2);
        });
        it('should return a registered symbol', function () {
            var description = 'my_symbol';
            var symbol = module.Symbol_For(description);
            assert.strictEqual(Symbol.keyFor(symbol), description);
        });
    });
    describe('ForApi', function () {
        it('should return Symbol', function () {
            assert.strictEqual(typeof module.Symbol_ForApi('some_description'), 'symbol');
        });
        it('should return the same symbol when invoked multiple times', function () {
            var description = 'another_description';
            var symbol1 = module.Symbol_ForApi(description);
            var symbol2 = module.Symbol_ForApi(description);
            assert.strictEqual(symbol1, symbol2);
        });
        it('should not return the same symbol as Symbol.for()', function () {
            var description = 'yet_another_description';
            var symbol1 = module.Symbol_ForApi(description);
            var symbol2 = Symbol.for(description);
            assert.notStrictEqual(symbol1, symbol2);
        });
        it('should return an unregistered symbol', function () {
            var description = 'my_symbol';
            var symbol = module.Symbol_ForApi(description);
            assert.strictEqual(Symbol.keyFor(symbol), undefined);
        });
    });
});
