/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Integer', function () {
    describe('New', function () {
        it('should be within int32_t type', function () {
            var two31 = Math.pow(2, 31);
            assert.strictEqual(module.Integer_New(-1), -1);
            assert.strictEqual(module.Integer_New(0), 0);
            assert.strictEqual(module.Integer_New(1), 1);
            assert.strictEqual(module.Integer_New(two31 - 1), two31 - 1);
            assert.strictEqual(module.Integer_New(two31), -two31);
        });
    });
    describe('NewFromUnsigned', function () {
        it('should be within uint32_t type', function () {
            var two32 = Math.pow(2, 32);
            assert.strictEqual(module.Integer_NewFromUnsigned(-1), two32 - 1);
            assert.strictEqual(module.Integer_NewFromUnsigned(0), 0);
            assert.strictEqual(module.Integer_NewFromUnsigned(1), 1);
            assert.strictEqual(module.Integer_NewFromUnsigned(two32 - 1), two32 - 1);
            assert.strictEqual(module.Integer_NewFromUnsigned(two32), 0);
        });
    });
});
