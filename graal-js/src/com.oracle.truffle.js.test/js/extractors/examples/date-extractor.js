/**
 * @option js.extractors
 */

load('../../assert.js');

const DateExtractor = {
    [Symbol.customMatcher](value) {
        if (value instanceof Date) {
            return [value];
        } else if (typeof value === "number") {
            return [new Date(value)];
        } else if (typeof value === "string") {
            return [new Date(Date.parse(value))];
        }
    }
};

class Book {
    constructor({
        isbn,
        title,
        // Extract `createdAt` as a Date
        createdAt: DateExtractor(createdAt) = new Date(),
        modifiedAt: DateExtractor(modifiedAt) = createdAt
    }) {
        this.isbn = isbn;
        this.title = title;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }
}

{
    const date = new Date("1970-01-01T00:00:00Z");
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
    assertSame(Date.parse(createdAt), book.createdAt.valueOf());
}

{
    const createdAt = new Date("2025-01-01");
    const book = new Book({ isbn: "123", title: "Test Book", createdAt });
    assertSame(book.createdAt.valueOf(), book.modifiedAt.valueOf());
}

{
    const beforeCreation = Date.now();
    const book = new Book({ isbn: "456", title: "Another Book" });
    const afterCreation = Date.now();

    assertTrue(book.createdAt.valueOf() >= beforeCreation);
    assertTrue(book.createdAt.valueOf() <= afterCreation);
}

