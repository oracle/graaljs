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

"use strict";

(function(){

function getFloat32(byteOffset, littleEndian=false){
  return Internal.GetViewValue(this, byteOffset, Internal.ToBoolean(littleEndian), "Float32");
}

function getFloat64(byteOffset, littleEndian=false){
  return Internal.GetViewValue(this, byteOffset, Internal.ToBoolean(littleEndian), "Float64");
}

function getInt8(byteOffset){
  return Internal.GetViewValue(this, byteOffset, true, "Int8");
}

function getInt16(byteOffset, littleEndian=false){
  return Internal.GetViewValue(this, byteOffset, Internal.ToBoolean(littleEndian), "Int16");
}

function getInt32(byteOffset, littleEndian=false){
  return Internal.GetViewValue(this, byteOffset, Internal.ToBoolean(littleEndian), "Int32");
}

function getUint8(byteOffset){
  return Internal.GetViewValue(this, byteOffset, true, "Uint8");
}

function getUint16(byteOffset, littleEndian=false){
  return Internal.GetViewValue(this, byteOffset, Internal.ToBoolean(littleEndian), "Uint16");
}

function getUint32(byteOffset, littleEndian=false){
  return Internal.GetViewValue(this, byteOffset, Internal.ToBoolean(littleEndian), "Uint32");
}

function setFloat32(byteOffset, value, littleEndian=false){
  return Internal.SetViewValue(this, byteOffset, Internal.ToBoolean(littleEndian), "Float32", value);
}

function setFloat64(byteOffset, value, littleEndian=false){
  return Internal.SetViewValue(this, byteOffset, Internal.ToBoolean(littleEndian), "Float64", value);
}

function setInt8(byteOffset, value){
  return Internal.SetViewValue(this, byteOffset, true, "Int8", value);
}

function setInt16(byteOffset, value, littleEndian=false){
  return Internal.SetViewValue(this, byteOffset, Internal.ToBoolean(littleEndian), "Int16", value);
}

function setInt32(byteOffset, value, littleEndian=false){
  return Internal.SetViewValue(this, byteOffset, Internal.ToBoolean(littleEndian), "Int32", value);
}

function setUint8(byteOffset, value){
  return Internal.SetViewValue(this, byteOffset, true, "Uint8", value);
}

function setUint16(byteOffset, value, littleEndian=false){
  return Internal.SetViewValue(this, byteOffset, Internal.ToBoolean(littleEndian), "Uint16", value);
}

function setUint32(byteOffset, value, littleEndian=false){
  return Internal.SetViewValue(this, byteOffset, Internal.ToBoolean(littleEndian), "Uint32", value);
}

Internal.CreateMethodProperty(DataView.prototype, "getFloat32", getFloat32);
Internal.CreateMethodProperty(DataView.prototype, "getFloat64", getFloat64);
Internal.CreateMethodProperty(DataView.prototype, "getInt8", getInt8);
Internal.CreateMethodProperty(DataView.prototype, "getInt16", getInt16);
Internal.CreateMethodProperty(DataView.prototype, "getInt32", getInt32);
Internal.CreateMethodProperty(DataView.prototype, "getUint8", getUint8);
Internal.CreateMethodProperty(DataView.prototype, "getUint16", getUint16);
Internal.CreateMethodProperty(DataView.prototype, "getUint32", getUint32);
Internal.CreateMethodProperty(DataView.prototype, "setFloat32", setFloat32);
Internal.CreateMethodProperty(DataView.prototype, "setFloat64", setFloat64);
Internal.CreateMethodProperty(DataView.prototype, "setInt8", setInt8);
Internal.CreateMethodProperty(DataView.prototype, "setInt16", setInt16);
Internal.CreateMethodProperty(DataView.prototype, "setInt32", setInt32);
Internal.CreateMethodProperty(DataView.prototype, "setUint8", setUint8);
Internal.CreateMethodProperty(DataView.prototype, "setUint16", setUint16);
Internal.CreateMethodProperty(DataView.prototype, "setUint32", setUint32);

Internal.CreateMethodProperty(Internal.TypedArray.prototype, "toString", Array.prototype.toString);

if (typeof Symbol !== 'undefined') {
  Internal.ObjectDefineProperty(DataView.prototype, Symbol.toStringTag, {value: "DataView", writable: false, enumerable: false, configurable: true});
}

})();
