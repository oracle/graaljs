/**
 * @option js.extractors
 */

load('../../assert.js');

// Add customMatcher to RegExp.prototype
// This is potentially a built-in feature as part of Pattern Matching
RegExp.prototype[Symbol.customMatcher] = function (value) {
    const match = this.exec(value);
    return !!match && [match];
};

const input = '2025-01-02T12:34:56Z';

const IsoDate = /^(?<year>\d{4})-(?<month>\d{2})-(?<day>\d{2})$/;
const IsoTime = /^(?<hours>\d{2}):(?<minutes>\d{2}):(?<seconds>\d{2})$/;
const IsoDateTime = /^(?<date>[^TZ]+)T(?<time>[^Z]+)Z/;

// Test 1: Basic RegExp extraction with named groups
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

// Test 2: RegExp extraction with indexed groups
{
    const IsoDateTime([, date, time]) = input;
    assertSame(date, '2025-01-02');
    assertSame(time, '12:34:56');
}

// Test 3: Nested RegExp extraction
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

// Test 4: Simple pattern matching
{
    const EmailPattern = /^([^@]+)@([^@]+)$/;
    const email = 'user@example.com';

    const EmailPattern([, username, domain]) = email;

    assertSame('user', username);
    assertSame('example.com', domain);
}

// Test 5: URL parsing with RegExp
{
    const UrlPattern = /^(?<protocol>\w+):\/\/(?<host>[^\/]+)(?<path>\/.*)?$/;
    const url = 'https://example.com/path/to/resource';

    const UrlPattern({ groups: { protocol, host, path } }) = url;

    assertSame('https', protocol);
    assertSame('example.com', host);
    assertSame('/path/to/resource', path);
}

// Test 6: Phone number parsing
{
    const PhonePattern = /^\(?(\d{3})\)?[-.\s]?(\d{3})[-.\s]?(\d{4})$/;
    const phone = '(555) 123-4567';

    const PhonePattern([, areaCode, exchange, subscriber]) = phone;

    assertSame('555', areaCode);
    assertSame('123', exchange);
    assertSame('4567', subscriber);
}

// Test 7: IPv4 address parsing
{
    const IPv4Pattern = /^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})$/;
    const ip = '192.168.1.1';

    const IPv4Pattern([, octet1, octet2, octet3, octet4]) = ip;

    assertSame('192', octet1);
    assertSame('168', octet2);
    assertSame('1', octet3);
    assertSame('1', octet4);
}

// Test 8: Function parameter with RegExp extractor
{
    const DatePattern = /^(\d{4})-(\d{2})-(\d{2})$/;

    function parseDate(DatePattern([, year, month, day])) {
        return {
            year: parseInt(year),
            month: parseInt(month),
            day: parseInt(day)
        };
    }

    const result = parseDate('2025-12-25');

    assertSame(2025, result.year);
    assertSame(12, result.month);
    assertSame(25, result.day);
}

// Test 9: RegExp with optional groups
{
    const OptionalPattern = /^(\w+)(?:\s+(\w+))?$/;

    const OptionalPattern([, first, second]) = 'hello world';
    assertSame('hello', first);
    assertSame('world', second);

    const OptionalPattern([, first2, second2]) = 'single';
    assertSame('single', first2);
    assertSame(undefined, second2);
}

// Test 10: Case-insensitive matching
{
    const CaseInsensitivePattern = /^(yes|no)$/i;

    const CaseInsensitivePattern([match, value]) = 'YES';
    assertSame('YES', match);
    assertSame('YES', value);
}

// Test 11: Multiple RegExp extractions in object
{
    const data = {
        date: '2025-06-15',
        time: '09:30:45'
    };

    const {
        date: IsoDate({ groups: { year, month, day } }),
        time: IsoTime({ groups: { hours, minutes, seconds } })
    } = data;

    assertSame('2025', year);
    assertSame('06', month);
    assertSame('15', day);
    assertSame('09', hours);
    assertSame('30', minutes);
    assertSame('45', seconds);
}

// Test 12: RegExp in array pattern
{
    const Pattern = /^(\d+):(\d+)$/;
    const coordinates = ['10:20', '30:40', '50:60'];

    const [
        Pattern([, x1, y1]),
        Pattern([, x2, y2]),
        Pattern([, x3, y3])
    ] = coordinates;

    assertSame('10', x1);
    assertSame('20', y1);
    assertSame('30', x2);
    assertSame('40', y2);
    assertSame('50', x3);
    assertSame('60', y3);
}

