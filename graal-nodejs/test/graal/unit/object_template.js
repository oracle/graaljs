/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

describe('ObjectTemplate', function () {
    describe('NewInstance', function () {
        it('create an object from template', function () {
            var obj = module.ObjectTemplate_NewInstance();
            assert.strictEqual(obj instanceof Object, true);
            assert.strictEqual(Object.keys(obj).length, 0);
        });
    });
    describe('InternalFieldCount', function () {
        it('check default field count', function () {
            assert.strictEqual(module.ObjectTemplate_DefaultInternalFieldCount(), 0);
        });
        it('set and check InternalFieldCount', function () {
            assert.strictEqual(module.ObjectTemplate_SetAndCheckInternalFieldCount(), true);
        });
    });
    describe('Set', function () {
        it('should set properties on a new Object instance', function () {
            var obj = module.ObjectTemplate_Set("foo", "bar");
            assert.strictEqual(obj instanceof Object, true);
            assert.strictEqual(obj.foo, "bar");
        });
    });
    describe('SetAccessor', function () {
        it('should create simple getter', function () {
            var obj = module.ObjectTemplate_CreateWithAccessor("myAccess");
            var gotValue = obj.myAccess;
            assert.strictEqual(gotValue, "accessor getter called: myAccess");
        });
        it('should create simple setter', function () {
            var obj = module.ObjectTemplate_CreateWithAccessor("myAccess");
            assert.strictEqual(obj.mySetValue, undefined);
            obj.myAccess = 1000;
            assert.strictEqual(obj.mySetValue, 1000);
            assert.strictEqual(obj.hasOwnProperty("mySetValue"), true);
        });
    });
    describe('SetHandler', function () {
        it('should create object with the specified internal field count', function () {
            var result = module.ObjectTemplate_CheckNamedHandlerWithInternalFields();
            assert.ok(result);
        });
        it('should not crash with an empty named enumerator', function () {
            var obj = module.ObjectTemplate_CreateWithEmptyNamedEnumerator();
            assert.strictEqual(Object.keys(obj).length, 0);
        });
        it('should not crash with an empty indexed enumerator', function () {
            var obj = module.ObjectTemplate_CreateWithEmptyIndexedEnumerator();
            assert.strictEqual(Object.keys(obj).length, 0);
        });
    });
});
