/**
 * @option js.extractors
 */

load('../assert.js');

const DateExtractor = {
    [Symbol.customMatcher](value) {
        if (value instanceof Date) {
            return [value];
        } else if (typeof value === "number") {
            return [new Date(value)];
        } else if (typeof value === "string") {
            return [Date.parse(value)];
        }
    }
};

class Book {
    constructor({
        isbn,
        title,
        // Extract `createdAt` as an Instant
        createdAt: DateExtractor(createdAt) = Date.now(),
        modifiedAt: DateExtractor(modifiedAt) = createdAt
    }) {
        this.isbn = isbn;
        this.title = title;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }
}

{
    const date = Date.parse("1970-01-01T00:00:00Z")
    const book = new Book({ isbn: "...", title: "...", createdAt: date });
    assertSame(date.valueOf(), book.createdAt.valueOf());
}

{
    const msSinceEpoch = 1000;
    const book = new Book({ isbn: "...", title: "...", createdAt: msSinceEpoch });
    assertSame(msSinceEpoch, book.createdAt.valueOf());
}

{
    const createdAt = "1970-01-01T00Z";
    const book = new Book({ isbn: "...", title: "...", createdAt });
    assertSame(Date.parse(createdAt).valueOf(), book.createdAt.valueOf());
}
