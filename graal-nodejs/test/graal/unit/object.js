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

var assert = require('assert');
var module = require('./_unit');

describe('Object', function () {
    describe('Get by name', function () {
        it('should return simple properties by name', function () {
            var o = {a: 42, b: 100};
            assert.strictEqual(module.Object_GetByName(o, "a"), 42);
            assert.strictEqual(module.Object_GetByName(o, "b"), 100);
        });
        it('should return protoype properties by name', function () {
            var oParent = {a: 42, b: 100};
            var oChild = Object.create(oParent);
            assert.strictEqual(module.Object_GetByName(oChild, "a"), 42);
            assert.strictEqual(module.Object_GetByName(oChild, "b"), 100);
        });
        it('should return value when given a non-string key', function () {
            var o = {123: 456};
            assert.strictEqual(module.Object_GetByName(o, 123), 456);
        });
        it('should throw an error when the getter throws an error', function () {
            var thrownError = new Error('some error');
            var o = {};
            Object.defineProperty(o, 'foo', {
                get: function() {
                    throw thrownError;
                }
            });
            var caughtError;
            try {
                module.Object_GetByName(o, "foo");
            } catch (err) {
                caughtError = err;
            }
            assert.strictEqual(caughtError, thrownError);
        });
    });
    describe('Get by index', function () {
        it('should return simple properties by index', function () {
            var o = {};
            o[0] = 1000;
            o[1] = 1001;
            assert.strictEqual(module.Object_GetByIndex(o, 0), 1000);
            assert.strictEqual(module.Object_GetByIndex(o, 1), 1001);
        });
    });
    describe('Combine indexed and named properties', function () {
        it('should return simple properties by index and name', function () {
            var o = {a: 42, b: 43};
            o[0] = 1000;
            o[1] = 1001;
            assert.strictEqual(module.Object_GetByIndex(o, 0), 1000);
            assert.strictEqual(module.Object_GetByIndex(o, 1), 1001);
            assert.strictEqual(module.Object_GetByName(o, "a"), 42);
            assert.strictEqual(module.Object_GetByName(o, "b"), 43);
        });
    });
    describe('Set by name', function () {
        it('should set simple properties by name', function () {
            var o = {a: 42, b: 100};
            assert.strictEqual(module.Object_SetByName(o, "c", 200), true);
            assert.strictEqual(module.Object_SetByName(o, "d", 300), true);
            assert.strictEqual(o.c, 200);
            assert.strictEqual(o.d, 300);
        });
        it('should set a property when given a non-string key', function () {
            var o = {};
            assert.strictEqual(module.Object_SetByName(o, 654, 321), true);
            assert.strictEqual(o[654], 321);
        });
    });
    describe('Set by index', function () {
        it('should set simple properties by name', function () {
            var o = [0, 1, 2, 3, 4, 5];
            assert.strictEqual(module.Object_SetByIndex(o, 0, 66), true);
            assert.strictEqual(module.Object_SetByIndex(o, 5, 67), true);
            assert.strictEqual(o[0], 66);
            assert.strictEqual(o[1], 1);
            assert.strictEqual(o[2], 2);
            assert.strictEqual(o[3], 3);
            assert.strictEqual(o[4], 4);
            assert.strictEqual(o[5], 67);
        });
    });
    describe('GetOwnPropertyNames', function () {
        it('should return an empty array for {}', function () {
            var names = module.Object_GetOwnPropertyNames({});
            assert.strictEqual(names.length, 0);
        });
        it('should not return Symbol properties', function () {
            var o = {};
            o[Symbol('foo')] = 'bar';
            var names = module.Object_GetOwnPropertyNames(o);
            assert.strictEqual(names.length, 0);
        });
        it('should return ["a", "b"] for {a:undefined, b:null}', function () {
            var names = module.Object_GetOwnPropertyNames({a: undefined, b: null});
            assert.strictEqual(names.length, 2);
            assert.notStrictEqual(names.indexOf("a"), -1);
            assert.notStrictEqual(names.indexOf("b"), -1);
        });
        it('should return [1, 2] for {1:false, 2:0}', function () {
            var names = module.Object_GetOwnPropertyNames({1: false, 2: 0});
            assert.strictEqual(names.length, 2);
            assert.strictEqual(names.indexOf("1"), -1);
            assert.strictEqual(names.indexOf("2"), -1);
            assert.notStrictEqual(names.indexOf(1), -1);
            assert.notStrictEqual(names.indexOf(2), -1);
        });
        it('should not return the prototype\'s properties', function () {
            var oProto = {a: 123};
            var oChild = Object.create(oProto);
            oProto.b = 42;
            oChild.c = 666;
            var names = module.Object_GetOwnPropertyNames(oChild);
            assert.strictEqual(names.length, 1);
            assert.strictEqual(names[0], "c");
        });
        it('should not return non-enumerable properties', function () {
            var o = {};
            Object.defineProperty(o, 'x', {enumerable: false});
            var names = module.Object_GetOwnPropertyNames(o);
            assert.strictEqual(names.length, 0);
        });
        if (module.hasJavaInterop()) {
            it('should work for foreign objects', function () {
                var point = new java.awt.Point();
                var names = module.Object_GetOwnPropertyNames(point);
                assert.ok(names.includes('x'));
                assert.ok(names.includes('y'));
                assert.ok(!names.includes('z'));
            });
            it('should work for foreign arrays', function () {
                var intArrayClass = Java.type('int[]');
                var intArray = new intArrayClass(3);
                var names = module.Object_GetOwnPropertyNames(intArray);
                assert.deepEqual(names, [0, 1, 2, 'length', 'clone']);
            });
        }
    });
    describe('GetPropertyNames', function () {
        it('should return an empty array for {}', function () {
            var names = module.Object_GetPropertyNames({});
            assert.strictEqual(names.length, 0);
        });
        it('should not return Symbol properties', function () {
            var o = {};
            o[Symbol('foo')] = 'bar';
            var names = module.Object_GetPropertyNames(o);
            assert.strictEqual(names.length, 0);
        });
        it('should return ["a", "b"] for {a:undefined, b:null}', function () {
            var names = module.Object_GetPropertyNames({a: undefined, b: null});
            assert.strictEqual(names.length, 2);
            assert.notStrictEqual(names.indexOf("a"), -1);
            assert.notStrictEqual(names.indexOf("b"), -1);
        });
        it('should return [1, 2] for {1:false, 2:0}', function () {
            var names = module.Object_GetPropertyNames({1: false, 2: 0});
            assert.strictEqual(names.length, 2);
            assert.strictEqual(names.indexOf("1"), -1);
            assert.strictEqual(names.indexOf("2"), -1);
            assert.notStrictEqual(names.indexOf(1), -1);
            assert.notStrictEqual(names.indexOf(2), -1);
        });
        it('should return also the prototype\'s properties', function () {
            var oProto = {a: 123};
            var oChild = Object.create(oProto);
            oProto.b = 42;
            oChild.c = 666;
            var names = module.Object_GetPropertyNames(oChild);
            assert.strictEqual(names.length, 3);
            assert.strictEqual(names.indexOf("a") >= 0, true);
            assert.strictEqual(names.indexOf("b") >= 0, true);
            assert.strictEqual(names.indexOf("c") >= 0, true);
        });
        it('should not return duplicate entries', function () {
            var oProto = {a: 'aa', c: 'cc'};
            var o = Object.create(oProto);
            o.a = 'aaa';
            o.b = 'bbb';
            var names = module.Object_GetPropertyNames(o);
            assert.strictEqual(names.length, 3);
            assert.strictEqual(names[0], 'a');
            assert.strictEqual(names[1], 'b');
            assert.strictEqual(names[2], 'c');
        });
        if (module.hasJavaInterop()) {
            it('should work for foreign objects', function () {
                var point = new java.awt.Point();
                var names = module.Object_GetPropertyNames(point);
                assert.ok(names.includes('x'));
                assert.ok(names.includes('y'));
                assert.ok(!names.includes('z'));
            });
            it('should work for foreign arrays', function () {
                var intArrayClass = Java.type('int[]');
                var intArray = new intArrayClass(3);
                var names = module.Object_GetPropertyNames(intArray);
                assert.deepEqual(names, [0, 1, 2, 'length', 'clone']);
            });
        }
    });
    describe('Has by name', function () {
        it('querying simple properties by name', function () {
            var o = {a: 42, b: 100};
            assert.strictEqual(module.Object_HasByName(o, "a"), true);
            assert.strictEqual(module.Object_HasByName(o, "b"), true);
            assert.strictEqual(module.Object_HasByName(o, "c"), false);
        });
        it('should accept non-string keys', function () {
            var o = {111: 222};
            assert.strictEqual(module.Object_HasByName(o, 111), true);
            assert.strictEqual(module.Object_HasByName(o, 222), false);
        });
    });
    describe('HasOwnProperty', function () {
        it('should return true for an existing own property', function () {
            var o = {a: 42, b: 100};
            assert.strictEqual(module.Object_HasOwnProperty(o, "a"), true);
            assert.strictEqual(module.Object_HasOwnProperty(o, "b"), true);
        });
        it('should return false for a non-existing property', function () {
            var o = {a: 42, b: 100};
            assert.strictEqual(module.Object_HasOwnProperty(o, "c"), false);
            assert.strictEqual(module.Object_HasOwnProperty(o, "d"), false);
        });
        it('should return false for an existing prototype property', function () {
            var oParent = {a: 123};
            var oChild = Object.create(oParent);
            oParent.b = 42;
            oChild.c = 666;
            assert.strictEqual(module.Object_HasOwnProperty(oChild, "a"), false);
            assert.strictEqual(module.Object_HasOwnProperty(oChild, "b"), false);
            assert.strictEqual(module.Object_HasOwnProperty(oChild, "c"), true);
            assert.strictEqual(module.Object_HasOwnProperty(oParent, "a"), true);
            assert.strictEqual(module.Object_HasOwnProperty(oParent, "b"), true);
            assert.strictEqual(module.Object_HasOwnProperty(oParent, "c"), false);
        });
    });
    describe('HasRealNamedProperty', function () {
        it('should return true for an existing own property', function () {
            var o = {a: 42, b: 100};
            assert.strictEqual(module.Object_HasRealNamedProperty(o, "a"), true);
            assert.strictEqual(module.Object_HasRealNamedProperty(o, "b"), true);
        });
        it('should return false for a non-existing property', function () {
            var o = {a: 42, b: 100};
            assert.strictEqual(module.Object_HasRealNamedProperty(o, "c"), false);
            assert.strictEqual(module.Object_HasRealNamedProperty(o, "d"), false);
        });
        it('should return false for an existing prototype property', function () {
            var oParent = {a: 123};
            var oChild = Object.create(oParent);
            oParent.b = 42;
            oChild.c = 666;
            assert.strictEqual(module.Object_HasRealNamedProperty(oChild, "a"), false);
            assert.strictEqual(module.Object_HasRealNamedProperty(oChild, "b"), false);
            assert.strictEqual(module.Object_HasRealNamedProperty(oChild, "c"), true);
            assert.strictEqual(module.Object_HasRealNamedProperty(oParent, "a"), true);
            assert.strictEqual(module.Object_HasRealNamedProperty(oParent, "b"), true);
            assert.strictEqual(module.Object_HasRealNamedProperty(oParent, "c"), false);
        });
        it('should return true for an accessor property', function() {
            var o = {};
            Object.defineProperty(o, "accessor", { get: function() {}, set: function() {} });
            assert.strictEqual(module.Object_HasRealNamedProperty(o, "accessor"), true);
        });
    });
    describe('HasRealIndexedProperty', function () {
        it('should report correct indexed properties', function () {
            var o = {a: 42, b: 100};
            o[20] = "test";
            assert.strictEqual(module.Object_HasRealIndexedProperty(o, 0), false);
            assert.strictEqual(module.Object_HasRealIndexedProperty(o, 19), false);
            assert.strictEqual(module.Object_HasRealIndexedProperty(o, 20), true);
            assert.strictEqual(module.Object_HasRealIndexedProperty(o, 21), false);
        });
    });
    describe('Delete by name', function () {
        it('deleting simple properties by name', function () {
            var o = {a: 42, b: 100};
            assert.strictEqual(o.hasOwnProperty("a"), true);
            assert.strictEqual(o.hasOwnProperty("b"), true);
            assert.strictEqual(module.Object_DeleteByName(o, "a"), true);
            assert.strictEqual(module.Object_DeleteByName(o, "b"), true);
            assert.strictEqual(module.Object_DeleteByName(o, "c"), true); //that's what Node 0.10 reports
            assert.strictEqual(o.hasOwnProperty("a"), false);
            assert.strictEqual(o.hasOwnProperty("b"), false);
        });
        it('should delete a property given a non-string key', function () {
            var o = {111: 222};
            assert.strictEqual(o.hasOwnProperty(111), true);
            assert.strictEqual(module.Object_DeleteByName(o, 111), true);
            assert.strictEqual(o.hasOwnProperty(111), false);
        });
    });
    describe('InternalFieldCount', function () {
        it('simple object without internal fields', function () {
            var o = {a: 42, b: 100};
            assert.strictEqual(module.Object_InternalFieldCount(o), 0);
        });
    });
    describe('GetConstructorName', function () {
        it('should return the constructor name', function () {
            var o = {a: 42, b: 100};
            assert.strictEqual(module.Object_GetConstructorName(o), "Object");
            assert.strictEqual(module.Object_GetConstructorName(Array.prototype), "Array");
        });
    });
    describe('GetPrototype', function () {
        it('should return the prototype', function () {
            var oParent = {a: 42, b: 100};
            var oChild = Object.create(oParent);
            assert.strictEqual(module.Object_GetPrototype(oChild), oParent);
        });
        it('should return the prototype of prototype of array', function () {
            var arr = [1, 2, 3];

            var proto1 = module.Object_GetPrototype(arr);
            var proto2 = module.Object_GetPrototype(proto1);

            assert.strictEqual(proto1, Array.prototype);
            assert.strictEqual(proto2, Object.prototype);
        });
        it('should return null for no prototype', function () {
            var proto = module.Object_GetPrototype(Object.prototype);
            assert.strictEqual(proto, null);
        });
    });
    describe('CreationContext', function () {
        it('should be the current context for simple objects', function () {
            var o = {a: 42, b: 100};
            assert.strictEqual(module.Object_CreationContextIsCurrent(o), true);
        });
        it('should be the newly created (and entered) context', function () {
            assert.strictEqual(module.Object_CreationContextNewContext(), true);
        });
    });
    describe('Clone', function () {
        it('should be able to clone a simple object', function () {
            var o1 = {a: 42, b: 100};
            var o2 = {c: "test", d: 3.1415, child: o1};

            var clone1 = module.Object_Clone(o2);
            assert.strictEqual(clone1 instanceof Object, true);
            assert.strictEqual(typeof clone1, "object");
            assert.strictEqual(clone1 !== o2, true);
            assert.strictEqual(clone1.hasOwnProperty("c"), true);
            assert.strictEqual(clone1.hasOwnProperty("d"), true);
            assert.strictEqual(clone1.hasOwnProperty("child"), true);
            assert.strictEqual(clone1.hasOwnProperty("a"), false);
            assert.strictEqual(clone1.hasOwnProperty("b"), false);

            assert.strictEqual(clone1.c, "test");
            assert.strictEqual(clone1.d, 3.1415);
            assert.strictEqual(clone1.child.a, 42);

            assert.strictEqual(clone1.child === o2.child, true); //it's a shallow copy
        });
    });
    describe('SetAccessor', function () {
        it('should create simple getter', function () {
            var o = {a: 42, b: 100};
            assert.strictEqual(module.Object_SetAccessor(o, "myAccess"), true);
            var gotValue = o.myAccess;
            assert.strictEqual(gotValue, "accessor getter called: myAccess");
        });
        it('should create simple setter', function () {
            var o = {a: 42, b: 100};
            assert.strictEqual(o.mySetValue, undefined);
            assert.strictEqual(module.Object_SetAccessor(o, "myAccess"), true);
            o.myAccess = 1000;
            assert.strictEqual(o.mySetValue, 1000);
            assert.strictEqual(o.hasOwnProperty("mySetValue"), true);
        });
        it('should create a getter with a no-op setter (when setter is not provided and the property is writable)', function () {
            "use strict";
            var o = {mySetValue : 42};
            assert.strictEqual(module.Object_SetAccessorNoSetterWritable(o, "myAccess"), true);
            o.myAccess = 211; // No TypeError here
            assert.strictEqual(o.mySetValue, 42);
        });
        it('should create a getter with no setter (when setter is not provided and the property is read-only)', function () {
            "use strict";
            var o = {mySetValue : 42};
            assert.strictEqual(module.Object_SetAccessorNoSetterReadOnly(o, "myAccess"), true);
            assert.throws(function() { o.myAccess = 211; }, TypeError);
            assert.strictEqual(o.mySetValue, 42);
        });
    });
    describe('GetRealNamedPropertyAttributes', function() {
        it('should return correct attributes for data properties', function () {
            var o = {};
            Object.defineProperty(o, "nonWritable", { enumerable: true, configurable: true });
            Object.defineProperty(o, "nonEnumerable", { configurable: true, writable: true });
            Object.defineProperty(o, "nonConfigurable", { enumerable: true, writable: true });
            Object.defineProperty(o, "full", { enumerable: true, configurable: true, writable: true });
            Object.defineProperty(o, "empty", { });
            assert.strictEqual(module.Object_GetRealNamedPropertyAttributes(o, "nonWritable"), 1 /* ReadOnly */);
            assert.strictEqual(module.Object_GetRealNamedPropertyAttributes(o, "nonEnumerable"), 2 /* DontEnum */);
            assert.strictEqual(module.Object_GetRealNamedPropertyAttributes(o, "nonConfigurable"), 4 /* DontDelete */);
            assert.strictEqual(module.Object_GetRealNamedPropertyAttributes(o, "full"), 0);
            assert.strictEqual(module.Object_GetRealNamedPropertyAttributes(o, "empty"), 7 /* ReadOnly | DontEnum | DontDelete */);
        });
        it('should return correct attributes for accessor properties', function () {
            var o = {};
            Object.defineProperty(o, "setter", { enumerable: true, set: function() {} });
            Object.defineProperty(o, "getter", { configurable: true, get: function() {} });
            Object.defineProperty(o, "accessor", { get: function() {}, set: function() {} });
            // accessor property is never ReadOnly
            assert.strictEqual(module.Object_GetRealNamedPropertyAttributes(o, "setter"), 4 /* DontDelete */);
            assert.strictEqual(module.Object_GetRealNamedPropertyAttributes(o, "getter"), 2 /* DontEnum */);
            assert.strictEqual(module.Object_GetRealNamedPropertyAttributes(o, "accessor"), 6 /* DontEnum | DontDelete */);
        });
    });
    describe('SetIntegrityLevel', function () {
        it('should freeze the object', function () {
            var o = {
                bar: 42
            };
            assert.strictEqual(module.Object_SetIntegrityLevel(o, true), true);
            assert.throws(function () {
                "use strict";
                o.bar = 211;
            }, TypeError);
            assert.throws(function () {
                "use strict";
                o.foo = 42;
            }, TypeError);
        });
        it('should seal the object', function () {
            var o = {
                bar: 42
            };
            assert.strictEqual(module.Object_SetIntegrityLevel(o, false), true);
            o.bar = 211; // value of an existing property can be modified
            assert.strictEqual(o.bar, 211);
            assert.throws(function () {
                "use strict";
                o.foo = 42;
            }, TypeError);
        });
        var proxy = new Proxy({}, {
            preventExtensions: function () {
                return false;
            }
        });
        it('should throw for a Proxy refusing to prevent extensions (freeze)', function () {
            assert.throws(function () {
                module.Object_SetIntegrityLevel(proxy, true);
            }, TypeError);
        });
        it('should throw for a Proxy refusing to prevent extensions (seal)', function () {
            assert.throws(function () {
                module.Object_SetIntegrityLevel(proxy, false);
            }, TypeError);
        });
        if (module.hasJavaInterop()) {
            var point = new java.awt.Point();
            it('should not crash for foreign objects (freeze)', function () {
                assert.strictEqual(module.Object_SetIntegrityLevel(point, true), true);
            });
            it('should not crash for foreign objects (seal)', function () {
                assert.strictEqual(module.Object_SetIntegrityLevel(point, false), true);
            });
        }
    });
    describe('CreateDataProperty', function () {
        describe('name version', function () {
            it('should create data property', function () {
                var o = {};
                var key = 'foo';
                assert.strictEqual(module.Object_CreateDataProperty(o, key, 42), true);
                var desc = Object.getOwnPropertyDescriptor(o, key);
                assert.strictEqual(desc.value, 42);
                assert.strictEqual(desc.configurable, true);
                assert.strictEqual(desc.enumerable, true);
                assert.strictEqual(desc.writable, true);
            });
            it('should not override existing non-configurable property', function () {
                var o = {};
                var key = 'foo';
                Object.defineProperty(o, key, { value: 211 });
                assert.strictEqual(module.Object_CreateDataProperty(o, key, 42), false);
                var desc = Object.getOwnPropertyDescriptor(o, key);
                assert.strictEqual(desc.value, 211);
                assert.strictEqual(desc.configurable, false);
                assert.strictEqual(desc.enumerable, false);
                assert.strictEqual(desc.writable, false);
            });
            it('should not create data property on non-extensible object', function () {
                var o = Object.freeze({});
                var key = 'foo';
                assert.strictEqual(module.Object_CreateDataProperty(o, key, 42), false);
                assert.strictEqual(Object.getOwnPropertyDescriptor(o, key), undefined);
            });
        });
        describe('index version', function () {
            it('should create data property', function () {
                var o = {};
                var key = 123;
                assert.strictEqual(module.Object_CreateDataPropertyIndex(o, key, 42), true);
                var desc = Object.getOwnPropertyDescriptor(o, key);
                assert.strictEqual(desc.value, 42);
                assert.strictEqual(desc.configurable, true);
                assert.strictEqual(desc.enumerable, true);
                assert.strictEqual(desc.writable, true);
            });
            it('should not override existing non-configurable property', function () {
                var o = {};
                var key = 123;
                Object.defineProperty(o, key, { value: 211 });
                assert.strictEqual(module.Object_CreateDataPropertyIndex(o, key, 42), false);
                var desc = Object.getOwnPropertyDescriptor(o, key);
                assert.strictEqual(desc.value, 211);
                assert.strictEqual(desc.configurable, false);
                assert.strictEqual(desc.enumerable, false);
                assert.strictEqual(desc.writable, false);
            });
            it('should not create data property on non-extensible object', function () {
                var o = Object.freeze({});
                var key = 123;
                assert.strictEqual(module.Object_CreateDataPropertyIndex(o, key, 42), false);
                assert.strictEqual(Object.getOwnPropertyDescriptor(o, key), undefined);
            });
            it('should work for indices that do not fit into int32', function () {
                var o = {};
                var key = 4294967295;
                assert.strictEqual(module.Object_CreateDataPropertyIndex(o, key, 42), true);
                var desc = Object.getOwnPropertyDescriptor(o, key);
                assert.strictEqual(desc.value, 42);
                assert.strictEqual(desc.configurable, true);
                assert.strictEqual(desc.enumerable, true);
                assert.strictEqual(desc.writable, true);
            });
        });
    });
    describe('CallAsConstructor', function () {
        it('should create an instance of a function', function () {
            var f = function() {};
            var result = module.Object_CallAsConstructor(f);
            assert.ok(result instanceof f);
        });
        it('should throw for an ordinary object', function () {
            assert.throws(() => module.Object_CallAsConstructor({}), TypeError);
        });
        it('should throw for a non-constructor function', function () {
            assert.throws(() => module.Object_CallAsConstructor(Math.abs), TypeError);
        });
    });
});
