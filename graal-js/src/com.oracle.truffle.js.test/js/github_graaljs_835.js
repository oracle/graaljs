/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option nashorn-compat=true
 */

load('assert.js');

assertSameContent(["", "", ""], "10".match(/a*/g));
assertSameContent(["", ""], "10".match(/$/g));
assertSameContent(["0", ""], "10".match(/0*$/g));
