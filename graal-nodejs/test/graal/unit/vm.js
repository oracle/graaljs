/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

var assert = require('assert');
var vm = require('vm');

describe('vm', function () {
    it('should use the right Error.prepareStackTrace', function() {
        Error.prepareStackTrace = function() { return 'outer'; };
        // The following test-case passes on the original Node.js but fails
        // on graal-node.js because we use the current (instead of the originating)
        // realm in the implementation of error.stack.
        //var error = vm.runInNewContext('Error.prepareStackTrace = function() { return "inner"; }; new Error();');
        //assert.strictEqual(error.stack, 'inner');
        var stack = vm.runInNewContext('Error.prepareStackTrace = function() { return "inner"; }; new Error().stack;');
        assert.strictEqual(stack, 'inner');
        delete Error.prepareStackTrace;
    });
    it('should handle Reflect.getOwnPropertyDescriptor(this,"Function")', function () {
        var desc = vm.runInNewContext('Reflect.getOwnPropertyDescriptor(this,"Function")');
        assert.strictEqual(typeof desc.value, 'function');
        assert.strictEqual(desc.value.name, 'Function');
        assert.strictEqual(desc.writable, true);
        assert.strictEqual(desc.enumerable, false);
        assert.strictEqual(desc.configurable, true);
    });
    it('should handle vm.runInNewContext("toString")', function() {
        var result = vm.runInNewContext('toString');
        assert.strictEqual(typeof result, 'function');
    });
    it('should handle non-configurable properties of sandbox', function () {
        var sandbox = {};
        var value = 42;
        Object.defineProperty(sandbox, 'foo', {value: value});
        var context = vm.createContext(sandbox);
        var desc = vm.runInContext('Object.getOwnPropertyDescriptor(this, "foo")', context);
        assert.strictEqual(desc.value, value);
        assert.strictEqual(desc.writable, false);
        assert.strictEqual(desc.enumerable, false);
        assert.strictEqual(desc.configurable, false);
    });
    it('should handle the pattern used in filestack-js', function () {
        // regression test motivated by filestack-js (npm package), it used to throw TypeError
        var result = vm.runInNewContext('Object.defineProperty(this, "console", { value: 42 }); Object.keys(this)', { console: console });
        assert.ok(result.includes('console'));
    });
    it('should handle a script whose name clashes with the name of a core module', function () {
        assert.throws(function() {
            new vm.Script('!', { filename: 'vm.js' });
        }, SyntaxError);
        assert.strictEqual(new vm.Script('6*7', { filename: 'vm.js' }).runInThisContext(), 42);
    });
    it('should honor parsingContext option of compileFunction()', function () {
        // Extracted from parallel/test-vm-basic.js.
        // Similar pattern is used by jest testing framework.
        assert.strictEqual(
            vm.compileFunction('return varInContext', [], { parsingContext: vm.createContext({varInContext: 'abc'}) })(),
            'abc'
        );
    });
    it('should have correct globalThis', function () {
        var sandbox = {};
        var fooValue = 42;
        Object.defineProperty(sandbox, 'foo', {value: fooValue});
        Object.defineProperty(sandbox, 'setTimeout', {value: setTimeout});
        var context = vm.createContext(sandbox);
        assert.strictEqual(vm.runInContext('foo', context), fooValue);
        assert.strictEqual(vm.runInContext('globalThis.foo', context), fooValue);
        var desc = vm.runInContext('Reflect.getOwnPropertyDescriptor(globalThis, "setTimeout")', context);
        assert.ok(desc);
        assert.strictEqual(desc.value, setTimeout);
        assert.strictEqual(desc.writable, false);
        assert.strictEqual(desc.enumerable, false);
        assert.strictEqual(desc.configurable, false);
    });
    it('should allow overriding globalThis', function () {
        var sandbox = {};
        var fooValue = 42;
        var globalThisOverride = {foo: 43};
        Object.defineProperty(sandbox, 'foo', {value: fooValue});
        Object.defineProperty(sandbox, 'globalThis', {value: globalThisOverride, configurable: true, writable: true});
        var context = vm.createContext(sandbox);
        assert.strictEqual(vm.runInContext('foo', context), fooValue);
        assert.strictEqual(vm.runInContext('globalThis', context), globalThisOverride);
        assert.strictEqual(vm.runInContext('globalThis.foo', context), globalThisOverride.foo);
        assert.strictEqual(sandbox.globalThis, globalThisOverride);
    });
    it('should not have "global" property', function () {
        var context = vm.createContext();
        assert.strictEqual(vm.runInContext('typeof global', context), 'undefined');
        assert.strictEqual(vm.runInContext('typeof globalThis.global', context), 'undefined');
    });
    it('should include global own keys', function () {
        var emptyContext = vm.createContext();
        var context = vm.createContext({1: 'one', 3: 'three', extra: 42, [Symbol.unscopables]: 43});

        for (let query of ['Object.getOwnPropertyNames(globalThis)', 'Reflect.ownKeys(globalThis)']) {
            var globalBuiltins = vm.runInContext(query, emptyContext);
            assert(globalBuiltins.includes('Object'), globalBuiltins);

            var globalPropertyNames = vm.runInContext('globalThis[0] = "zero"; globalThis[2] = "two"; ' + query, context);
            assert(globalPropertyNames.includes('Object'), globalPropertyNames);

            // Make sure properties are in the correct order, too.
            assert.strictEqual(globalPropertyNames[0], '0');
            assert.strictEqual(globalPropertyNames[1], '1');
            assert.strictEqual(globalPropertyNames[2], '2');
            assert.strictEqual(globalPropertyNames[3], '3');
            assert.strictEqual(globalPropertyNames[4], 'extra');
            assert.strictEqual(globalPropertyNames[5], globalBuiltins[0]);

            // Symbols of the context object are not included.
            assert(!globalPropertyNames.includes(Symbol.unscopables), globalPropertyNames);
        }

        var globalPropertySymbols = vm.runInContext('Object.getOwnPropertySymbols(globalThis)', emptyContext);
        // Symbols of the context object are not included.
        assert(!globalPropertySymbols.includes(Symbol.unscopables), globalPropertySymbols);
        assert.strictEqual(43, vm.runInContext('globalThis[Symbol.unscopables]', context));
    });
});
