/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('Isolate', function () {
    describe('Basic test', function () {
        it('should pass the basic test', function () {
            assert.strictEqual(module.Isolate_BasicTest(), true);
        });
    });
});
