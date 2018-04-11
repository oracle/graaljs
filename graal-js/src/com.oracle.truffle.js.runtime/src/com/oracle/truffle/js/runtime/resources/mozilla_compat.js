/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * This script contains non-standard, Mozilla compatibility functionality on
 * the standard global objects. Please note that it is incomplete. Only the most
 * often used functionality is supported.
 */

// JavaAdapter
Object.defineProperty(this, "JavaAdapter", {
    configurable: true, enumerable: false, writable: true,
    value: function() {
        if (arguments.length < 2) {
            throw new TypeError("JavaAdapter requires at least two arguments");
        }

        var types = Array.prototype.slice.call(arguments, 0, arguments.length - 1);
        var NewType = Java.extend.apply(Java, types);
        return new NewType(arguments[arguments.length - 1]);
    }
});


// importPackage
// avoid unnecessary chaining of __noSuchProperty__ again
// in case user loads this script more than once.
if (typeof importPackage == 'undefined') {

Object.defineProperty(this, "importPackage", {
    configurable: true, enumerable: false, writable: true,
    value: (function() {
        var _packages = [];
        var global = this;
        var oldNoSuchProperty = global.__noSuchProperty__;
        var __noSuchProperty__ = function(name) {
            'use strict';
            for (var i in _packages) {
                try {
                    var type = Java.type(_packages[i] + "." + name);
                    global[name] = type;
                    return type;
                } catch (e) {}
            }

            if (oldNoSuchProperty) {
                return oldNoSuchProperty.call(this, name);
            } else {
                if (this === undefined) {
                    throw new ReferenceError(name + " is not defined");
                } else {
                    return undefined;
                }
            }
        }

        Object.defineProperty(global, "__noSuchProperty__", {
            writable: true, configurable: true, enumerable: false,
            value: __noSuchProperty__
        });

        var prefix = "[JavaPackage ";
        return function() {
            for (var i in arguments) {
                var pkgName = arguments[i];
                if ((typeof pkgName) != 'string') {
                    pkgName = String(pkgName);
                    // extract name from JavaPackage object
                    if (pkgName.startsWith(prefix)) {
                        pkgName = pkgName.substring(prefix.length, pkgName.length - 1);
                    }
                }
                _packages.push(pkgName);
            }
        }
    })()
});

}

// sync
Object.defineProperty(this, "sync", {
    configurable: true, enumerable: false, writable: true,
    value: function(func, syncobj) {
        if (arguments.length < 1 || arguments.length > 2 ) {
            throw "sync(function [,object]) parameter count mismatch";
        }
        return Java.synchronized(func, syncobj);
    }
});

// Object.prototype.toSource
Object.defineProperty(Object.prototype, "toSource", {
    configurable: true, enumerable: false, writable: true,
    value: function toSource(state) {
        if (!state) {
            state = new (Java.type('java.util.HashSet'))();
        }
        if (state.contains(this)) {
            return "{}";
        }
        state.add(this);
        var str = new (Java.type('java.lang.StringBuilder'))('({');
        for (i in this) {
            str.append(i);
            str.append(':');
            if (this[i] instanceof Object && typeof(this[i].toSource) == 'function') {
                str.append(this[i].toSource(state));
            } else {
                str.append(String(this[i]));
            }
            str.append(', ');
        }
        // delete last extra command and space
        str = str.deleteCharAt(str.length() - 1);
        str = str.deleteCharAt(str.length() - 1);
        str.append('})');
        return str.toString();
    }
});

// Boolean.prototype.toSource
Object.defineProperty(Boolean.prototype, "toSource", {
    configurable: true, enumerable: false, writable: true,
    value: function() {
        return '(new Boolean(' + this.toString() + '))';
    }
});

// Date.prototype.toSource
Object.defineProperty(Date.prototype, "toSource", {
    configurable: true, enumerable: false, writable: true,
    value: function() {
        return '(new Date(' + this.valueOf() + '))';
    }
});

// Number.prototype.toSource
Object.defineProperty(Number.prototype, "toSource", {
    configurable: true, enumerable: false, writable: true,
    value: function() {
        return '(new Number(' + this.toString() + '))';
    }
});

// RegExp.prototype.toSource
Object.defineProperty(RegExp.prototype, "toSource", {
    configurable: true, enumerable: false, writable: true,
    value: function() {
        return this.toString();
    }
});

// String.prototype.toSource
Object.defineProperty(String.prototype, "toSource", {
    configurable: true, enumerable: false, writable: true,
    value: function() {
        return '(new String(' + this.quote() + '))';
    }
});

// Error.prototype.toSource
Object.defineProperty(Error.prototype, "toSource", {
    configurable: true, enumerable: false, writable: true,
    value: function() {
        var msg = this.message? String(this.message).quote() : "''";
        return '(new ' + this.name + '(' + msg + '))';
    }
});

// Function.prototype.arity
Object.defineProperty(Function.prototype, "arity", {
    configurable: true, enumerable: false,
    get: function() { return this.length; },
    set: function(x) {
        throw new TypeError("Function arity can not be modified");
    }
});

// String.prototype.quote
Object.defineProperty(String.prototype, "quote", {
    configurable: true, enumerable: false, writable: true,
    value: function() {
        return JSON.stringify(this);
    }
});

// Rhino: global.importClass
Object.defineProperty(this, "importClass", {
    configurable: true, enumerable: false, writable: true,
    value: function() {
        for (var arg in arguments) {
            var clazz = arguments[arg];
            if (Java.isType(clazz)) {
                var className = Java.typeName(clazz);
                var simpleName = className.substring(className.lastIndexOf('.') + 1);
                this[simpleName] = clazz;
            } else {
                throw new TypeError(clazz + " is not a Java class");
            }
        }
    }
});
