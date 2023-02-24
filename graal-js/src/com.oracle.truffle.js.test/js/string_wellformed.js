/*
 * Copyright (c) 2023, 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */
/**
 * Test for String.prototype.isWellFormed() and String.prototype.toWellFormed().
 *
 * @option ecmascript-version=staging
 */
load("assert.js");

assertTrue("\u0000".isWellFormed());
assertTrue("\ud000".isWellFormed());
assertTrue("\ue000".isWellFormed());
assertTrue("\uffff".isWellFormed());

assertFalse("\ud800".isWellFormed());
assertFalse("\udbff".isWellFormed());
assertFalse("\udc00".isWellFormed());
assertFalse("\udfff".isWellFormed());

assertSame("\ufffd", "\ud800".toWellFormed());
assertSame("\ufffd", "\udbff".toWellFormed());
assertSame("\ufffd", "\udc00".toWellFormed());
assertSame("\ufffd", "\udfff".toWellFormed());

assertTrue("\ud83d\ude00".isWellFormed());
assertSame("\ud83d\ude00", "\ud83d\ude00".toWellFormed());
