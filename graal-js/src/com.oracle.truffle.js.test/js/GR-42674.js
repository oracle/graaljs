/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Make sure we do not throw an internal error for invalid paths.
 */

load("assert.js");

// Nul character not allowed
assertThrows(() => load('\0'), Error);

// Malformed input or input contains unmappable characters
assertThrows(() => load('\uDAFF'), Error);
