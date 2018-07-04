/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
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
    value: (function(){
        var MySet = typeof Set == 'function' ? Set : (function(){
              function ES5Set() {
                  this.elements = [];
              }
              Object.defineProperties(ES5Set.prototype, {
                  has: {value: function has(e) {return this.elements.indexOf(e) >= 0;}, configurable: true, writable: true},
                  add: {value: function add(e) {return this.elements.push(e);}, configurable: true, writable: true},
              });
              return ES5Set;
        })();
        return function toSource(state) {
            if (!state) {
                state = new MySet();
            }
            if (state.has(this)) {
                return "{}";
            }
            state.add(this);
            var str = '({', sep = '';
            for (i in this) {
                str += sep;
                str += i;
                str += ':';
                if (this[i] instanceof Object && typeof(this[i].toSource) == 'function') {
                    str += this[i].toSource(state);
                } else {
                    str += String(this[i]);
                }
                sep = ', ';
            }
            str += '})';
            return String(str);
        }
    })()
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
        for (var i = 0; i < arguments.length; i++) {
            var clazz = arguments[i];
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
