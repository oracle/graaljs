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

describe('ObjectNew', function () {
    describe('RegExp::New', function () {
        it('should return a RegExp object', function () {
            var re = module.ObjectNew_RegExp("beer");
            assert(re instanceof RegExp);
            assert(re.global);
        });
    });
    describe('Date::New', function () {
        const time = new Date(2018,1,1).valueOf();
        it('should return a Date object', function () {
            var date = module.ObjectNew_Date(time);
            assert(date instanceof Date);
            assert.strictEqual(date.valueOf(), time);
        });
        it('should return a Date object (maybe version)', function () {
            var date = module.ObjectNew_DateMaybe(time);
            assert(date instanceof Date);
            assert.strictEqual(date.valueOf(), time);
        });
    });
    describe('BooleanObject::New', function () {
        it('should return a Boolean object', function () {
            assert(module.ObjectNew_BooleanObject(false) instanceof Boolean);
            assert.strictEqual(module.ObjectNew_BooleanObject(false).valueOf(), false);
            assert.strictEqual(module.ObjectNew_BooleanObject(true).valueOf(), true);
        });
    });
    describe('NumberObject::New', function () {
        it('should return a Number object', function () {
            var number = module.ObjectNew_NumberObject(42);
            assert(number instanceof Number);
            assert.strictEqual(number.valueOf(), 42);
        });
    });
    describe('StringObject::New', function () {
        it('should return a String object', function () {
            var string = module.ObjectNew_StringObject("beer");
            assert(string instanceof String);
            assert.strictEqual(string.valueOf(), "beer");
        });
    });
});
