/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Exception', function () {
    describe('Error', function () {
        it('create an Error object', function () {
            var err = module.Exception_Error("TestMessage");
            assert.strictEqual(err instanceof Error, true);
            assert.strictEqual(err.hasOwnProperty("message"), true);
        });
    });
    describe('RangeError', function () {
        it('create a RangeError object', function () {
            var err = module.Exception_RangeError("TestMessage");
            assert.strictEqual(err instanceof RangeError, true);
            assert.strictEqual(err.hasOwnProperty("message"), true);
        });
    });
    describe('ReferenceError', function () {
        it('create a ReferenceError object', function () {
            var err = module.Exception_ReferenceError("TestMessage");
            assert.strictEqual(err instanceof ReferenceError, true);
            assert.strictEqual(err.hasOwnProperty("message"), true);
        });
    });
    describe('SyntaxError', function () {
        it('create a TypeError object', function () {
            var err = module.Exception_SyntaxError("TestMessage");
            assert.strictEqual(err instanceof SyntaxError, true);
            assert.strictEqual(err.hasOwnProperty("message"), true);
        });
    });
    describe('TypeError', function () {
        it('create a TypeError object', function () {
            var err = module.Exception_TypeError("TestMessage");
            assert.strictEqual(err instanceof TypeError, true);
            assert.strictEqual(err.hasOwnProperty("message"), true);
        });
    });
});
