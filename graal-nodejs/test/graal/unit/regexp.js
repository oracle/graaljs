/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
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

const assert = require('assert');
var module = require('./_unit');

describe('RegExp', function () {
    describe('Exec', function () {
        it('should return matches, captures, groups, indices and input', function () {
            const regexp = /(?<word>alpha)(z)?/d;
            const input = ' alpha';
            const expected = regexp.exec(input);
            assert.deepStrictEqual(module.RegExp_Exec(regexp, input), expected);
        });
        it('should return null when there is no match', function () {
            assert.strictEqual(module.RegExp_Exec(/a/, 'b'), null);
        });
        it('should update and reset lastIndex for global regexps', function () {
            const regexp = /a/g;
            assert.strictEqual(module.RegExp_Exec(regexp, 'ba ba').index, 1);
            assert.strictEqual(regexp.lastIndex, 2);
            assert.strictEqual(module.RegExp_Exec(regexp, 'ba ba').index, 4);
            assert.strictEqual(regexp.lastIndex, 5);
            assert.strictEqual(module.RegExp_Exec(regexp, 'ba ba'), null);
            assert.strictEqual(regexp.lastIndex, 0);
        });
        it('should respect lastIndex for sticky regexps', function () {
            const regexp = /a/y;
            assert.strictEqual(module.RegExp_Exec(regexp, 'ba'), null);
            regexp.lastIndex = 1;
            assert.strictEqual(module.RegExp_Exec(regexp, 'ba').index, 1);
            assert.strictEqual(regexp.lastIndex, 2);
        });
        it('should preserve lastIndex for non-global regexps', function () {
            const regexp = /a/;
            regexp.lastIndex = 5;
            assert.strictEqual(module.RegExp_Exec(regexp, 'a')[0], 'a');
            assert.strictEqual(regexp.lastIndex, 5);
        });
        it('should honor an overridden exec method', function () {
            const regexp = /a/;
            for (const result of [null, [], {}]) {
                regexp.exec = function (input) {
                    assert.strictEqual(this, regexp);
                    assert.strictEqual(input, 'test');
                    return result;
                };
                assert.strictEqual(module.RegExp_Exec(regexp, 'test'), result);
            }
        });
        it('should use the builtin when exec is not callable', function () {
            const regexp = /a/;
            regexp.exec = null;
            assert.strictEqual(module.RegExp_Exec(regexp, 'a')[0], 'a');
        });
        it('should reject primitive results from an overridden exec', function () {
            const regexp = /a/;
            for (const result of [undefined, false, 1, 'a', Symbol('a'), 1n]) {
                regexp.exec = () => result;
                assert.throws(() => module.RegExp_Exec(regexp, 'a'), TypeError);
            }
        });
        it('should preserve exceptions from exec getters and methods', function () {
            const sentinel = new Error('exec');
            const regexp = /a/;
            Object.defineProperty(regexp, 'exec', {
                configurable: true,
                get() { throw sentinel; },
            });
            assert.throws(() => module.RegExp_Exec(regexp, 'a'), (err) => err === sentinel);
            Object.defineProperty(regexp, 'exec', {
                value() { throw sentinel; },
            });
            assert.throws(() => module.RegExp_Exec(regexp, 'a'), (err) => err === sentinel);
        });
        it('should preserve exceptions from lastIndex conversion', function () {
            const sentinel = new Error('lastIndex');
            const regexp = /a/g;
            regexp.lastIndex = { valueOf() { throw sentinel; } };
            assert.throws(() => module.RegExp_Exec(regexp, 'a'), (err) => err === sentinel);
        });
        it('should throw when lastIndex cannot be updated', function () {
            const regexp = /a/g;
            Object.defineProperty(regexp, 'lastIndex', { writable: false });
            assert.throws(() => module.RegExp_Exec(regexp, 'a'), TypeError);
        });
        it('should update legacy RegExp static properties', function () {
            module.RegExp_Exec(/(a)/, 'ba');
            assert.strictEqual(RegExp.input, 'ba');
            assert.strictEqual(RegExp.$1, 'a');
        });
    });
});
