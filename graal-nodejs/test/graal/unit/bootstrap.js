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

// Tests basic functionality that other tests depend on (like whether
// false/true/undefined/null passed from the native code is really
// false/true/undefined/null when it gets to JavaScript.
describe('Bootstrap', function () {
    it('should return undefined', function () {
        assert.strictEqual(module.Bootstrap_Undefined(), undefined);
    });
    it('should return null', function () {
        assert.strictEqual(module.Bootstrap_Null(), null);
    });
    it('should return true', function () {
        assert.strictEqual(module.Bootstrap_True(), true);
    });
    it('should return false', function () {
        assert.strictEqual(module.Bootstrap_False(), false);
    });
    it('should return int32', function () {
        assert.strictEqual(module.Bootstrap_Int32(), 211);
    });
    it('should return uint32', function () {
        assert.strictEqual(module.Bootstrap_Uint32(), 3000000000);
    });
    it('should return double', function () {
        assert.strictEqual(module.Bootstrap_Double(), 3.14);
    });
    describe('ReturnValue::Get()', function() {
        it('should return undefined when no value was set', function() {
            assert.strictEqual(module.Bootstrap_GetNotSet(), undefined);
        });
        it('should work for int32 values', function() {
            assert.strictEqual(module.Bootstrap_GetInt32(), true);
        });
        it('should work for uint32 values', function() {
            assert.strictEqual(module.Bootstrap_GetUint32(), true);
        });
        it('should work for double values', function() {
            assert.strictEqual(module.Bootstrap_GetDouble(), true);
        });
    });
});
