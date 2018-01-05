/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
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
});

