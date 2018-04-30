/*
 * Copyright (c) 2018, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
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
