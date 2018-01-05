/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Boolean', function () {
    describe('IsUndefined', function () {
        it('should return false for True', function () {
            assert.strictEqual(module.Boolean_IsUndefinedForTrue(), false);
        });
        it('should return false for False', function () {
            assert.strictEqual(module.Boolean_IsUndefinedForFalse(), false);
        });
    });
    describe('IsNull', function () {
        it('should return false for True', function () {
            assert.strictEqual(module.Boolean_IsNullForTrue(), false);
        });
        it('should return false for False', function () {
            assert.strictEqual(module.Boolean_IsNullForFalse(), false);
        });
    });
    describe('IsTrue', function () {
        it('should return true for True', function () {
            assert.strictEqual(module.Boolean_IsTrueForTrue(), true);
        });
        it('should return false for False', function () {
            assert.strictEqual(module.Boolean_IsTrueForFalse(), false);
        });
    });
    describe('IsFalse', function () {
        it('should return false for True', function () {
            assert.strictEqual(module.Boolean_IsFalseForTrue(), false);
        });
        it('should return true for False', function () {
            assert.strictEqual(module.Boolean_IsFalseForFalse(), true);
        });
    });
    describe('IsNativeError', function () {
        it('should return false for True', function () {
            assert.strictEqual(module.Value_IsNativeError(true), false);
        });
        it('should return false for False', function () {
            assert.strictEqual(module.Value_IsNativeError(false), false);
        });
    });
});
