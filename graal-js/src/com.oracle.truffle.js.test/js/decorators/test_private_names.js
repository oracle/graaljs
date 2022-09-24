/*
 * Copyright (c) 2022, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/**
 * @option ecmascript-version=staging
 */

load('../assert.js')

ticks = 37;

function assertXSharp(value, context) { assertEqual('#x', context.name); ticks++; }

let C1 = class { @assertXSharp ['#x']; }
let C2 = class { @assertXSharp accessor ['#x']; }
let C3 = class { @assertXSharp ['#x']() {}; }
let C4 = class { @assertXSharp get ['#x']() {}; }
let C5 = class { @assertXSharp set ['#x'](v) {}; }

assertEqual(42, ticks);
