/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Function', function () {
    describe('NewInstance', function () {
        it('without arguments', function () {
            var source = Object;
            var func = module.Function_NewInstance(source);
            assert.strictEqual(func instanceof source, true);
            assert.strictEqual(func === source, false);
        });
        it('with arguments', function () {
            var source = Object;
            var func = module.Function_NewInstance(source, 7, [0, 1, 2, 3, 4, 5, 6]);
            assert.strictEqual(func instanceof source, true);
            assert.strictEqual(func === source, false);
            //TODO check the set arguments
        });
    });
    describe('SetName', function () {
        it('simple name', function () {
            function SetNameTestFunc() {
                return 42;
            }
            ;
            var source = SetNameTestFunc;
            var newName = "NewNameTestFunc";
            var func = module.Function_SetName(source, newName);
            assert.strictEqual(func === source, true);
            assert.strictEqual(func.name === newName, true);
            assert.strictEqual(SetNameTestFunc.name === newName, true);
        });
    });
    describe('Call', function () {
        it('should call a simple function', function () {
            function SimpleFunction(a, b, c) {
                return a + b + c;
            }
            var rec = {};
            var result = module.Function_Call(SimpleFunction, rec, 3, 10, 11, 12);
            assert.strictEqual(typeof result, "number");
            assert.strictEqual(result, 33);
        });
        it('should call a simple function via proxy', function () {
            function identity(a) {
                return a;
            }
            var proxy = new Proxy(identity, { apply: function(f,thiz,args) {return  6 * f.apply(thiz, args)}});
            var target = {};
            var result = module.Function_Call(proxy, target, 1, 7);
            assert.strictEqual(typeof result, "number");
            assert.strictEqual(result, 42);
        });
    });
});
