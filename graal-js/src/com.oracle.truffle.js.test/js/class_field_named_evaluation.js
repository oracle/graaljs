/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests NamedEvaluation of anonymous function definitions in class field initializers.
 *
 * @option ecmascript-version=2021
 */

load('assert.js');

let C;
C = class { x = class { static y = this.name; } };            assertSame("x", new C().x.y);
C = class { ['x'] = class { static y = this.name; } };        assertSame("x", new C().x.y);
C = class { static x = class { static y = this.name; } };     assertSame("x", C.x.y);
C = class { static ['x'] = class { static y = this.name; } }; assertSame("x", C.x.y);
