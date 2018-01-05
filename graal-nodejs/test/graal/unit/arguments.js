/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Arguments', function () {
    describe('IsConstructCall', function () {
        it('should return false for a regular call', function () {
            var func = module.Arguments_FunctionWithArguments();
            var obj = {};
            func(obj);
            assert.strictEqual(obj.isConstructCall, false);
        });
        it('should return true for a construct call', function () {
            var Func = module.Arguments_FunctionWithArguments();
            var obj = {};
            new Func(obj);
            assert.strictEqual(obj.isConstructCall, true);
        });
    });
    describe('This', function () {
        it('should return correct this value', function () {
            var func = module.Arguments_FunctionWithArguments();
            var obj = {};
            var expectedThis = {a: 123};
            func.call(expectedThis, obj);
            var actualThis = obj.thisValue;
            assert.strictEqual(actualThis, expectedThis);
        });
    });
    describe('Holder', function () {
        //holder is usually identical to This, except for some corner cases e.g., around prototypes
        //https://groups.google.com/forum/#!topic/v8-users/Axf4hF_RfZo
        it('should return correct holder value', function () {
            var func = module.Arguments_FunctionWithArguments();
            var obj = {};
            var expectedThis = {a: 123, b: "test"};
            func.call(expectedThis, obj);
            var actualHolder = obj.holderValue;
            assert.strictEqual(actualHolder, expectedThis);
        });
    });
});
