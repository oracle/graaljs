/*
 * Copyright (c) 2024, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * HTMLCloseComment is allowed at the beginning of the script in Annex B only.
 * 
 * @option annex-b=false
 */

load("assert.js");

assertThrows(() => eval('-->'), SyntaxError);
