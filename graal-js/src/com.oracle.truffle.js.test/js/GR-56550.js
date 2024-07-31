/*
 * Copyright (c) 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * Test sorting int and double arrays with hole elements at the end.
 */

const v1 = [981517771,-1337791284,7,4096,8,64,10];
const v2 = [1000000.0,-435.92083294324425,-4.0,-72924.4167712992,-166.99785787478345,0.7783900556580967,998508.9254321649,223.77879728853213];

v1.sort().shift();
v1.length ^= 512;
v1.sort().shift();

v2.sort().shift();
v2.length ^= 512;
v2.sort().shift();
