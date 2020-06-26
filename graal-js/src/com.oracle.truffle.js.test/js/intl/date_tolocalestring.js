/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * Licensed under the Universal Permissive License v 1.0 as shown at http://oss.oracle.com/licenses/upl.
 */

/*
 * @option intl-402
 * @option timezone=Asia/Tokyo
 * @option locale=en-US
 */

load('../assert.js');

let d = new Date("2020-06-26 13:37 UTC");
assertSame("6/26/2020, 10:37:00 PM", d.toLocaleString());
assertSame("26/06/2020, 22:37:00", d.toLocaleString("en-GB"));
assertSame("2020/6/26 22:37:00", d.toLocaleString("ja-JP"));
assertSame("26/06/2020, 15:37:00", d.toLocaleString("en-GB", {timeZone: "Europe/Vienna"}));
assertSame("6/26/2020, 6:37:00 AM", d.toLocaleString(undefined, {timeZone: "America/Los_Angeles"}));
assertSame("2020-06-26, 6:37:00 a.m.", d.toLocaleString("en-CA", {timeZone: "America/Vancouver"}));

true;
