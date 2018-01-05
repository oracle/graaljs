/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var module = require('./_unit');

describe('External', function () {
    describe('New', function () {
        it('should be able to get value', function () {
            var objWithExt = module.External_New();
        });
        it('value should be of type external', function () {
            var objWithExt = module.External_New();
            assert.strictEqual(module.External_CheckInternalFieldIsExtern(objWithExt, 0), true);
        });
    });
});
