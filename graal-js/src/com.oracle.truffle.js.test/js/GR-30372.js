/*
 * Copyright (c) 2021, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Tests class imports of JavaImporter
 * 
 * @option nashorn-compat
 */

load('assert.js');

// Class imports are not prioritized, just the order of imports matters.
// The last import is considered first (like Nashorn does).

var result;

var imports = JavaImporter(java.util.List, java.awt.List);
with (imports) result = List;
assertSame(java.awt.List, result);

var imports = JavaImporter(java.awt.List, java.util.List);
with (imports) result = List;
assertSame(java.util.List, result);

var imports = JavaImporter(java.util, java.awt.List);
with (imports) result = List;
assertSame(java.awt.List, result);

var imports = JavaImporter(java.awt.List, java.util);
with (imports) result = List;
assertSame(java.util.List, result);

var imports = JavaImporter(java.util, java.awt);
with (imports) result = List;
assertSame(java.awt.List, result);

var imports = JavaImporter(java.awt, java.util);
with (imports) result = List;
assertSame(java.util.List, result);
