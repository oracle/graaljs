/*
 * Copyright (c) 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

load('./assert.js');

assertSame('te-alema-lema4e', new Intl.Locale('te-lema4e-alema').toString());
assertSame('lnu-rnun1d', new Intl.Locale('lnu-rnun1d').toString());

assertSame('cs-u-yes', new Intl.Locale('cs-u-yes').toString());
assertSame('de-u-true', new Intl.Locale('de-u-true').toString());

var displayNames = new Intl.DisplayNames('en', {type: 'language'});
assertSame('Telugu (ALEMA_LEMA4E)', displayNames.of('te-lema4e-alema'));
