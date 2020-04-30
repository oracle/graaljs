/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests of "inferred" names of functions/classes.
 * 
 * @option ecmascript-version=2021
 */

load('assert.js');

assertSame('100', ({ 1e2: function() {} })[100].name);
assertSame('42', ({ 42n: function() {} })[42].name);
assertSame('foo', ({ "foo": function() {} }).foo.name);

assertSame('100', ({ 1e2: class {} })[100].name);
assertSame('42', ({ 42n: class {} })[42].name);
assertSame('foo', ({ "foo": class {} }).foo.name);

assertSame('100', (class { static 1e2 = function() {} })[100].name);
assertSame('42', (class { static 42n = function() {} })[42].name);
assertSame('foo', (class { static "foo" = function() {} }).foo.name);

assertSame('100', (class { static 1e2 = class {} })[100].name);
assertSame('42', (class { static 42n = class {} })[42].name);
assertSame('foo', (class { static "foo" = class {} }).foo.name);

true;
