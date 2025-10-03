load('../js/assert.js');

// potentially built-in as part of Pattern Matching
RegExp.prototype[Symbol.customMatcher] = function (value) {
    const match = this.exec(value);
    return !!match && [match];
};

const input = '2025-01-02T12:34:56Z';

const IsoDate = /^(?<year>\d{4})-(?<month>\d{2})-(?<day>\d{2})$/;
const IsoTime = /^(?<hours>\d{2}):(?<minutes>\d{2}):(?<seconds>\d{2})$/;
const IsoDateTime = /^(?<date>[^TZ]+)T(?<time>[^Z]+)Z/;

{
    const IsoDateTime({ groups: { date, time } }) = input;
    assertSame(date, '2025-01-02');
    assertSame(time, '12:34:56');

    const IsoDate({ groups: { year, month, day } }) = date;
    assertSame(year, '2025');
    assertSame(month, '01');
    assertSame(day, '02');

    const IsoTime({ groups: { hours, minutes, seconds } }) = time;
    assertSame(hours, '12');
    assertSame(minutes, '34');
    assertSame(seconds, '56');
}

{
    const IsoDateTime([, date, time]) = input;
    assertSame(date, '2025-01-02');
    assertSame(time, '12:34:56');
}

{
    const IsoDateTime({
        groups: {
            date: IsoDate({ groups: { year, month, day } }),
            time: IsoTime({ groups: { hours, minutes, seconds } })
        }
    }) = input;
    assertSame(year, '2025');
    assertSame(month, '01');
    assertSame(day, '02');
    assertSame(hours, '12');
    assertSame(minutes, '34');
    assertSame(seconds, '56');
}
