/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
load("assert.js");

assertSame(this, (() => (() => eval("this"))())());
assertSame(this, (a => (b => ((c, d) => eval("this"))())())());
assertSame(this, (() => ((...rest) => eval("this"))())());
assertSame(this, (() => ((thiz = eval("this")) => thiz)())());

true;
