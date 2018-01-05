/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
    describe('SetHiddenPrototype', function () {
        it('simple check on SetHiddenPrototype', function () {
            assert.strictEqual(module.FunctionTemplate_CheckSetHiddenPrototype(), true);
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

