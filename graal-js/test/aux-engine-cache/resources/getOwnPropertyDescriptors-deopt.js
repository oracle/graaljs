/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

function kall(input) {
	Object.getOwnPropertyDescriptors(input)	
}

(function main() {

	var o = {a:32, b:44};

	for (var i = 0; i < 15000000; i++) {
		kall(o);
	}

	var f = function() {}

	for (var i = 0; i < 15000000; i++) {
		kall(f);
	}

})();
