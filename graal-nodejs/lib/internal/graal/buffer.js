/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

// When NIO buffers are enabled, GraalJSAccess ensures that this module is loaded with the builtins constructor as extra argument.
const NIOBufferPrototypeAllocator = arguments.length === 5 ? arguments[4] : undefined;

function patchBufferPrototype(proto) {
	if (NIOBufferPrototypeAllocator) {
		const bufferBuiltin = NIOBufferPrototypeAllocator(proto.utf8Write, proto.utf8Slice);
		proto.utf8Write = bufferBuiltin.utf8Write;
		proto.utf8Slice = bufferBuiltin.utf8Slice;
	}
}

module.exports = {
	install: patchBufferPrototype
}