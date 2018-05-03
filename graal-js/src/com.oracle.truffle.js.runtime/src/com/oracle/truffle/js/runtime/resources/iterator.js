/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

"use strict";

(function(){

const ITERATED_OBJECT_ID = Internal.GetHiddenKey("IteratedObject");
const ITERATOR_NEXT_INDEX_ID = Internal.GetHiddenKey("IteratorNextIndex");

const ITERATION_KIND_KEY = 1 << 0;
const ITERATION_KIND_VALUE = 1 << 1;
const ITERATION_KIND_KEY_PLUS_VALUE = ITERATION_KIND_KEY | ITERATION_KIND_VALUE;

var TypedArray = Internal.TypedArray; // == Object.getPrototypeOf(Int8Array)

function CreateIterResultObject(value, done) {
  return { value, done };
}


// String Iterator
const ITERATED_STRING_ID = Internal.HiddenKey("IteratedString");
const STRING_ITERATOR_NEXT_INDEX_ID = Internal.HiddenKey("StringIteratorNextIndex");
const charCodeAt = String.prototype.charCodeAt;
const fromCharCode = String.fromCharCode;

var StringIterator = Internal.MakeConstructor(function StringIterator() {});
StringIterator.prototype = Object.create(Internal.GetIteratorPrototype());

function CreateStringIterator(string) {
  Internal.Assert(typeof string === 'string');
  var iterator = new StringIterator();
  iterator[ITERATED_STRING_ID] = string;
  iterator[STRING_ITERATOR_NEXT_INDEX_ID] = 0;
  return iterator;
}

function IsStringIterator(iterator) {
  // Returns true if iterator has all of the internal slots of a String Iterator Instance (ES6 21.1.5.3).
  if (Internal.HasHiddenKey(iterator, ITERATED_STRING_ID)) {
    // If one of the String Iterator internal slots is present, the other must be as well.
    Internal.Assert(Internal.HasHiddenKey(iterator, STRING_ITERATOR_NEXT_INDEX_ID));
    return true;
  }
  return false;
}

function StringIteratorNext() {
  var iterator = this;
  if (!IsStringIterator(iterator)) {
    throw new TypeError("not a String Iterator");
  }
  var string = iterator[ITERATED_STRING_ID];
  if (string === undefined) {
    return CreateIterResultObject(undefined, true);
  }
  var position = iterator[STRING_ITERATOR_NEXT_INDEX_ID];
  var length = string.length;
  if (position >= length) {
    iterator[ITERATED_STRING_ID] = undefined;
    return CreateIterResultObject(undefined, true);
  }
  var first = Internal.CallFunction(charCodeAt, string, position);
  var resultString;
  if (first < 0xD800 || first > 0xDBFF || position + 1 === length) {
    resultString = fromCharCode(first);
  } else {
    var second = Internal.CallFunction(charCodeAt, string, position + 1);
    if (second < 0xDC00 || second > 0xDFFF) {
      resultString = fromCharCode(first);
    } else {
      resultString = fromCharCode(first) + fromCharCode(second);
    }
  }
  iterator[STRING_ITERATOR_NEXT_INDEX_ID] = position + resultString.length;
  return CreateIterResultObject(resultString, false);
}

Internal.CreateMethodProperty(StringIterator.prototype, "next", StringIteratorNext);
Internal.SetFunctionName(StringIteratorNext, "next");
Internal.ObjectDefineProperty(StringIterator.prototype, Symbol.toStringTag, {value: "String Iterator", writable: false, enumerable: false, configurable: true});

function StringPrototypeIterator() {
  Internal.RequireObjectCoercible(this);
  return CreateStringIterator(Internal.ToString(this));
}

Internal.CreateMethodProperty(String.prototype, Symbol.iterator, StringPrototypeIterator);
Internal.SetFunctionName(StringPrototypeIterator, "[Symbol.iterator]");

})();
