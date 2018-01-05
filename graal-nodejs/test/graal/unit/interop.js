/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var vm = require('vm');

describe('Interop', function () {
    if (typeof Interop === 'object') {
        it('eval should work in a new context', function () {
            assert.strictEqual(require('vm').runInNewContext("Interop.eval('application/javascript', '7*6')"), 42);
        });
        it('export/import should work in a new context', function () {
            assert.strictEqual(require('vm').runInNewContext("Interop.export('obj', { foo: 'bar' }); Interop.import('obj').foo"), 'bar');
        });
    }
});
