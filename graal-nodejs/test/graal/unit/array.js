/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Array', function () {
    describe('New', function () {
        it('should create an empty array', function () {
            var arr = module.Array_New(0);
            assert.strictEqual(arr instanceof Array, true);
            assert.strictEqual(arr.length, 0);
        });
        it('should create an empty array of certain length', function () {
            var arr = module.Array_New(123);
            assert.strictEqual(arr instanceof Array, true);
            assert.strictEqual(arr.length, 123);
        });
    });
    describe('Length', function () {
        it('should return 0 for []', function () {
            assert.strictEqual(module.Array_Length([]), 0);
        });
        it('should return 3 for [0,1,2]', function () {
            assert.strictEqual(module.Array_Length([0, 1, 2]), 3);
        });
        it('should return 9999 for new Array(9999)', function () {
            assert.strictEqual(module.Array_Length(new Array(9999)), 9999);
        });
    });
});
