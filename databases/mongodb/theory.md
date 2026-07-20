# MongoDB: Documents, Embedding vs Referencing

## 1. Definition

MongoDB is a document database storing JSON-like BSON documents in
collections, with no fixed schema enforced by the database itself.

## 2. Why it exists

Some data doesn't fit cleanly into normalized relational tables — deeply
nested, variably-structured, or extremely high-write-volume data can be
simpler and faster to model as self-contained documents.

## 3. How it works — embedding vs referencing

```text
Embedding: store related data inside the same document.
  Good when: data is always read together, rarely updated independently,
             and won't grow unbounded (e.g., an address embedded in a user document).

Referencing: store an ID pointing to a document in another collection.
  Good when: data is large, shared across many parent documents, or updated
             independently (e.g., a product referenced by many order documents).
```

## 4. Code example

```javascript
// Embedded (good: address rarely changes independently, always read with user)
{ _id: 1, name: "Alice", address: { city: "Colombo", zip: "00100" } }

// Referenced (good: product is large, shared, and updated independently)
{ _id: 501, customerId: 1, items: [{ productId: "P123", quantity: 2 }] }
```

## 5. Production use case

Product catalogs (flexible attributes per category), event logging, and
content management systems are common MongoDB use cases where rigid
relational schemas would be awkward.

## 6. Common mistakes

- Embedding unbounded arrays (e.g., all of a user's orders embedded directly
  in the user document) — documents have a size limit and this pattern grows forever.
- Ignoring eventual consistency implications when reading from secondary replicas.
- Treating MongoDB's flexible schema as "no schema design needed" — access
  patterns still need to be thought through up front, just differently than in SQL.

## 7. Trade-offs

| Embedding | Referencing |
|---|---|
| One query, faster reads | Multiple queries or lookups, more flexible |
| Risk of unbounded document growth | Avoids document size limits |

## 8. Interview questions

1. When would you embed data vs reference it in MongoDB?
2. What's a concrete risk of embedding unbounded arrays in a document?
3. How does MongoDB's eventual consistency model affect application design?
4. Redis vs MongoDB vs a relational database — when would you pick each?

## 9. Short interview answer

"I embed when data is always read together and won't grow unbounded, and
reference when data is large, shared, or updated independently — the classic
failure mode is embedding an array that grows forever, like all of a user's
orders inside the user document, until it hits MongoDB's document size limit."

## 10. Related topics

- [SQL fundamentals](sql.md)
