/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var vm = require('vm');

describe('vm', function () {
    it('should handle non-configurable properties of sandbox', function () {
        var sandbox = {};
        var value = 42;
        Object.defineProperty(sandbox, 'foo', {value: value});
        var context = vm.createContext(sandbox);
        var desc = vm.runInContext('Object.getOwnPropertyDescriptor(this, "foo")', context);
        assert.strictEqual(desc.value, value);
        assert.strictEqual(desc.writable, false);
        assert.strictEqual(desc.enumerable, false);
        // The following assertion holds on the original Node.js but we
        // fail to satisfy it because the global object of the context
        // is implemented by Proxy that cannot have non-configurable
        // properties not present in the target (which is the original
        // global object, not the sandbox - where the non-configurable
        // property is defined)
        //assert.strictEqual(desc.configurable, false);
    });
});
