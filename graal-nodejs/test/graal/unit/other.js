/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');

describe('Other', function () {
    it('should be possible to redefine process.env.FOO', function () {
        // inspired by a test of 'sinon' Node.js package
        process.env.FOO = 'bar';
        Object.defineProperty(process.env, 'FOO', {value: 'baz', enumerable: true, configurable: true});
        assert.strictEqual(process.env.FOO, 'baz');
    });
});
