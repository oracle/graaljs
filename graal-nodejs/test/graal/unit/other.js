/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

var async_hooks = require('async_hooks');
var assert = require('assert');
var fs = require('fs');
var module = require('./_unit');
var util = require('util');
var vm = require('vm');

describe('Other', function () {
    it('should be possible to redefine process.env.FOO', function () {
        // inspired by a test of 'sinon' Node.js package
        process.env.FOO = 'bar';
        Object.defineProperty(process.env, 'FOO', {value: 'baz', enumerable: true, configurable: true, writable: true});
        assert.strictEqual(process.env.FOO, 'baz');
    });
    if (module.hasJavaInterop()) {
        it('util.inspect should work for JavaObjects', function() {
            var Point = Java.type('java.awt.Point');
            var point = new Point();
            // just make sure that it does not throw an error
            util.inspect(Point);
            assert.match(util.inspect(point), /getX/);
        });
        it('util.inspect should work for java.math.BigInteger', function() {
            const BigInteger = Java.type('java.math.BigInteger');
            const Long = Java.type('java.lang.Long');
            assert.strictEqual(String(2n ** 64n - 2n), util.inspect(BigInteger.valueOf(Long.MAX_VALUE).shiftLeft(1)));
            assert.strictEqual(String(2n ** 63n - 1n), util.inspect(BigInteger.valueOf(Long.MAX_VALUE)));
            assert.strictEqual(String(2n ** 63n - 1n), util.inspect(Long.MAX_VALUE));
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
    it('should refuse null and undefined (in a template with a signature check) properly', function () {
        var nativeMethodWithASignatureCheck = Object.getPrototypeOf(vm.Script.prototype).runInContext;
        [null, undefined].forEach(function (thiz) {
            assert.throws(function() {
                nativeMethodWithASignatureCheck.call(thiz);
            }, TypeError, "Illegal invocation");
        });
    });
    it('should throw the right error from vm.runInNewContext() (GR-9592)', function () {
        Error.prepareStackTrace = function () {
            fs.existsSync("."); // a call that invokes a native method needed here
        };
        var caught = null;
        try {
            vm.runInNewContext("'");
        } catch (e) {
            caught = e;
        } finally {
            Error.prepareStackTrace = null;
        }
        assert.notStrictEqual(caught, null, "Error not thrown");
        assert.strictEqual(caught.name, "SyntaxError");
    });
    it('should be possible to use vm.compileFunction()', function () {
        var fn = vm.compileFunction('return a + b + c + d;', ['a', 'b'], {contextExtensions: [{c: 42}, {d: 211}]});
        assert.strictEqual(fn(1000, 20000), 21253);
    });
    it('should be possible to use TextDecoder("utf-8", { fatal: true })', function () {
        // should not fail (fails when Node.js is built without intl support)
        new util.TextDecoder('utf-8', { fatal: true });
    });
    it('should not define FinalizationGroup', function () {
        assert.strictEqual(global.FinalizationGroup, undefined);
    });
    it('should keep local storage after await', function(done) {
        const { AsyncLocalStorage } = async_hooks;
        const asyncLocalStorage = new AsyncLocalStorage();

        asyncLocalStorage.run({id: 42}, async () => {
            try {
                assert.strictEqual(asyncLocalStorage.getStore().id, 42);
                await 211;
                assert.strictEqual(asyncLocalStorage.getStore().id, 42);
                done();
            } catch (err) {
                done(err);
            }
        });
    });
});
