/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test regexp-legacy-features behavior.
 */

load("assert.js");

class MyRegExp extends RegExp {}

// TypeError: RegExp.prototype.compile cannot be used on subclasses of RegExp.
assertThrows(() => new MyRegExp(/foo/).compile('pattern', ''), TypeError);
assertThrows(() => new MyRegExp('foo').compile('pattern', ''), TypeError);
