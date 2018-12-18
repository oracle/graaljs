/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

function expectFail(fn, msg) {
  try {
    fn();
  } catch (ex) {
    if (!ex.message.includes(msg)) {
      throw ex;
    }
    return true;
  }
  throw TypeError("error expected for method: "+fn);
}

function check(fn) {
    //add extra objects to avoid caches
    var a = [{}, {a:1}, {a:true, b:"hallo"}, [1,2,3]];
    for (var i in a) {
        fn(a[i]);
    }
}

function checkCall(fn, ...args) {
    //add extra objects to avoid caches
    var a = [{}, {a:1}, {a:true, b:"hallo"}, [1,2,3]];
    for (var i in a) {
        try {
            fn.call(a[i], ...args);
        } catch (e) {
            if (e instanceof TypeError && e.message.includes('Message not supported')) {
                continue;
            }
            throw e;
        }
    }
}

//Global
check(print);
check(console.log);
check(console.debug);
check(console.info);
check(console.dir);
check(console.error);
check(console.warn);
check(console.assert);
//check(console.clear);
check(console.count);
check(console.countReset);
check(console.group);
check(console.groupCollapsed);
check(console.groupEnd);
check(console.time);
check(console.timeEnd);
check(console.timeLog);

check(decodeURI);
check(decodeURIComponent);
check(encodeURI);
check(encodeURIComponent);
check(escape);
check(eval);
check(isFinite);
check(isNaN);
check(parseFloat);
check(parseInt);
check(unescape);

//Array
check(Array.from);
check(Array.isArray);
check(Array.of);

checkCall(Array.prototype.concat);
checkCall(Array.prototype.copyWithin);
checkCall(Array.prototype.fill);
checkCall(Array.prototype.includes);
checkCall(Array.prototype.indexOf);
checkCall(Array.prototype.join);
checkCall(Array.prototype.keys);
checkCall(Array.prototype.lastIndexOf);
checkCall(Array.prototype.pop);
checkCall(Array.prototype.push);
checkCall(Array.prototype.reverse);
checkCall(Array.prototype.shift);
checkCall(Array.prototype.slice);
checkCall(Array.prototype.splice);
checkCall(Array.prototype.toLocaleString);
checkCall(Array.prototype.toString);
checkCall(Array.prototype.unshift);

function callbackFn () {};

checkCall(Array.prototype.filter, callbackFn);
checkCall(Array.prototype.find, callbackFn);
checkCall(Array.prototype.findIndex, callbackFn);
checkCall(Array.prototype.reduce, callbackFn, 0);
checkCall(Array.prototype.reduceRight, callbackFn, 0);

checkCall(Array.prototype.every, callbackFn);
checkCall(Array.prototype.forEach, callbackFn);
checkCall(Array.prototype.map, callbackFn);
checkCall(Array.prototype.some, callbackFn);


checkCall(Array.prototype.sort);

//Boolean
expectFail( o => { Boolean.prototype.toString.call({}) }, "not a Boolean object");
expectFail( o => { Boolean.prototype.valueOf.call({}) }, "not a Boolean object");

//Date
check(Date.parse);

expectFail( o => { Date.prototype.getDate.call({}) }, "not a Date object");
expectFail( o => { Date.prototype.getFullYear.call({}) }, "not a Date object");
Date.prototype.toString.call(new Date());

//Error

//Function

//JSON
checkCall(JSON.stringify);

//Map
expectFail( o => { Map.prototype.clear.call({}) }, "Map expected");

//Math
check(Math.abs);
check(Math.acos);
check(Math.acosh);
check(Math.asin);
check(Math.asinh);
check(Math.atan);
check(Math.atan2);
check(Math.atanh);
check(Math.cbrt);
check(Math.ceil);
check(Math.clz32);
check(Math.cos);
check(Math.cosh);
check(Math.exp);
check(Math.expm1);
check(Math.floor);
check(Math.fround);
check(Math.hypot);
check(Math.imul);
check(Math.log);
check(Math.log10);
check(Math.log1p);
check(Math.log2);
check(Math.max);
check(Math.min);
check(Math.pow);
check(Math.round);
check(Math.sign);
check(Math.sin);
check(Math.sinh);
check(Math.sqrt);
check(Math.tan);
check(Math.tanh);
check(Math.trunc);

//Number
check(Number.isFinite);
check(Number.isInteger);
check(Number.isNaN);
check(Number.isSafeInteger);
check(Number.parseFloat);
check(Number.parseInt);

//Object
check(Object.assign);
check(Object.create);
//check(Object.defineProperties);
//check(Object.defineProperty);
check(Object.entries);
check(Object.freeze);
check(Object.getOwnPropertyDescriptor);
check(Object.getOwnPropertyDescriptors);
check(Object.getOwnPropertyNames);
check(Object.getOwnPropertySymbols);
check(Object.getPrototypeOf);
check(Object.is);
check(Object.isExtensible);
check(Object.isFrozen);
check(Object.isSealed);
check(Object.keys);
check(Object.preventExtensions);
check(Object.seal);
//check(Object.setPrototypeOf);
check(Object.values);

checkCall(Object.prototype.hasOwnProperty);
checkCall(Object.prototype.isPrototypeOf);
checkCall(Object.prototype.propertyIsEnumerable);
checkCall(Object.prototype.toString);
checkCall(Object.prototype.valueOf);

//Promise

//Proxy

//RegExp

//Set
expectFail( o => { Set.prototype.clear.call({}) }, "Set expected");

//String

//WeakMap
expectFail( o => { WeakMap.prototype.has.call({}, {}) }, "WeakMap expected");

//WeakSet
expectFail( o => { WeakSet.prototype.has.call({}, {}) }, "WeakSet expected");

true;

