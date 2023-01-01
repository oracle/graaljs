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

describe('FunctionTemplate', function () {
    describe('HasInstance', function () {
        it('should return true for an instance of the function template', function () {
            assert.strictEqual(module.FunctionTemplate_HasInstanceIsInstance(), true);
        });
        it('should return false for an unrelated object', function () {
            assert.strictEqual(module.FunctionTemplate_HasInstanceIsNotInstance(), false);
        });
        it('should return false for an object (not created by the template) with the correct prototype', function () {
            assert.strictEqual(module.FunctionTemplate_HasInstanceSamePrototype(), false);
        });
        it('should return true for an instance of the inherited function template', function () {
            assert.strictEqual(module.FunctionTemplate_HasInstanceInherits(), true);
        });
    });
    describe('SetClassName', function () {
        it('can call SetClassName', function () {
            assert.strictEqual(module.FunctionTemplate_SetClassName("myClassName"), true);
        });
    });
    describe('GetFunction', function () {
        it('GetFunction returns a Function that returns the right value', function () {
            var func = module.FunctionTemplate_GetFunction();
            assert.strictEqual(func instanceof Function, true);
            var thiz = {};
            assert.strictEqual(func.call(thiz), thiz);
        });
    });
    describe('ReadOnlyPrototype', function () {
        it('ReadOnlyPrototype makes the prototype non-writable', function () {
            var functionWithDefaults = module.FunctionTemplate_GetFunction();
            var defualtDesc = Object.getOwnPropertyDescriptor(functionWithDefaults, 'prototype');
            assert.strictEqual(defualtDesc.writable, true);
            assert.strictEqual(defualtDesc.enumerable, false);
            assert.strictEqual(defualtDesc.configurable, false);

            var functionWithReadOnlyPrototype = module.FunctionTemplate_GetFunctionWithReadOnlyPrototype();
            var readOnlyDesc = Object.getOwnPropertyDescriptor(functionWithReadOnlyPrototype, 'prototype');
            assert.strictEqual(readOnlyDesc.writable, false);
            assert.strictEqual(defualtDesc.enumerable, false);
            assert.strictEqual(defualtDesc.configurable, false);
        });
    });
    describe('SetLength', function () {
        it('SetLength should set the value of "length" property', function () {
            var functionWithDefaults = module.FunctionTemplate_GetFunction();
            var defualtDesc = Object.getOwnPropertyDescriptor(functionWithDefaults, 'length');
            assert.strictEqual(defualtDesc.value, 0);
            assert.strictEqual(defualtDesc.writable, false);
            assert.strictEqual(defualtDesc.enumerable, false);
            assert.strictEqual(defualtDesc.configurable, true);

            var functionWithLength = module.FunctionTemplate_GetFunctionWithLength(42);
            var descWithLength = Object.getOwnPropertyDescriptor(functionWithLength, 'length');
            assert.strictEqual(descWithLength.value, 42);
            assert.strictEqual(descWithLength.writable, false);
            assert.strictEqual(descWithLength.enumerable, false);
            assert.strictEqual(descWithLength.configurable, true);
        });
    });
    describe('InstanceTemplate', function () {
        it('simple check on InstanceTemplate', function () {
            assert.strictEqual(module.FunctionTemplate_CheckInstanceTemplate(), true);
        });
    });
    describe('PrototypeTemplate', function () {
        it('simple check on PrototypeTemplate', function () {
            assert.strictEqual(module.FunctionTemplate_CheckPrototypeTemplate(), true);
        });
    });
    describe('Set', function () {
        it('should set properties on a new Function object instance', function () {
            var obj = module.FunctionTemplate_SetOnInstanceTemplate("foo", "bar");
            assert.strictEqual(obj instanceof Object, true);
            assert.strictEqual(obj.foo, "bar");

        });
        it('should set properties on a new instance, not its prototype', function () {
            var obj = module.FunctionTemplate_SetOnInstanceTemplate("foo", "bar");
            assert.strictEqual(obj instanceof Object, true);
            assert.strictEqual(obj.foo, "bar");
            assert.strictEqual(Object.keys(obj).length, 1);
        });
    });
    describe('SetAccessor', function () {
        it('should create simple getter', function () {
            var obj = module.FunctionTemplate_CreateWithAccessor("myAccess");
            var gotValue = obj.myAccess;
            assert.strictEqual(gotValue, "accessor getter called: myAccess");
        });
        it('should create simple setter', function () {
            var obj = module.FunctionTemplate_CreateWithAccessor("myAccess");
            assert.strictEqual(obj.mySetValue, undefined);
            obj.myAccess = 1000;
            assert.strictEqual(obj.mySetValue, 1000);
            assert.strictEqual(obj.hasOwnProperty("mySetValue"), true);
        });
    });
});
