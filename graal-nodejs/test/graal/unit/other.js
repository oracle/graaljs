/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

var assert = require('assert');
var util = require('util');
var vm = require('vm');

describe('Other', function () {
    it('should be possible to redefine process.env.FOO', function () {
        // inspired by a test of 'sinon' Node.js package
        process.env.FOO = 'bar';
        Object.defineProperty(process.env, 'FOO', {value: 'baz', enumerable: true, configurable: true});
        assert.strictEqual(process.env.FOO, 'baz');
    });
    if (typeof Java !== 'undefined') {
        it('util.inspect should work for JavaObjects', function() {
            var Point = Java.type('java.awt.Point');
            var point = new Point();
            var skip = false;
            try {
                Object.getOwnPropertyDescriptor(point, 'x');
            } catch (e) {
                skip = true; // NashornJavaInterop
            }
            if (!skip) {
                // just make sure that it does not throw an error
                util.inspect(Point);
                util.inspect(point);
            }
        });
    }
    it('should not regress in ExecuteNativeFunctionNode', function () {
        // inspired by a wrong rewrite of ExecuteNativeFunctionNode
        var script = new vm.Script('');
        script.runInThisContext(); // fine
        assert.throws(function() {
            Object.create(script).runInThisContext();
        }, TypeError, "Illegal invocation");
    });
});
