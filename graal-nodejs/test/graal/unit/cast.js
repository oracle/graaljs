/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Casting', function () {
    describe('Array', function () {
        it('should return identical value', function () {
            var arr = [1, 2, 3];
            var result = module.Cast_Array(arr);
            assert.strictEqual(arr.length, result.length);
            assert.strictEqual(arr[0], result[0]);
            assert.strictEqual(arr[1], result[1]);
            assert.strictEqual(arr[2], result[2]);
        });
    });
    describe('Date', function () {
        it('should return identical value', function () {
            var date = new Date();
            assert.strictEqual(module.Cast_Date(date), date);
        });
    });
    describe('Function', function () {
        it('should return identical value', function () {
            var func = function (a) {
                return a + a;
            };
            assert.strictEqual(module.Cast_Function(func), func);
        });
    });
    describe('Integer', function () {
        it('should return identical value', function () {
            var intVal = 1234;
            assert.strictEqual(module.Cast_Integer(intVal), intVal);
        });
    });
    describe('Number', function () {
        it('should return identical value', function () {
            var numVal = 1234.567;
            assert.strictEqual(module.Cast_Number(numVal), numVal);
        });
    });
    describe('Object', function () {
        it('should return identical value', function () {
            var obj = {a: 123, b: "test"};
            assert.strictEqual(module.Cast_Object(obj), obj);
        });
    });
    describe('RegExp', function () {
        it('should return identical value', function () {
            var regExp = /a/g;
            assert.strictEqual(module.Cast_RegExp(regExp), regExp);
        });
    });
    describe('String', function () {
        it('should return identical value', function () {
            var string = "abc123";
            assert.strictEqual(module.Cast_String(string), string);
        });
    });
});
