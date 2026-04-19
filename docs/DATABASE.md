# Database Schema Documentation

## Overview

The Bookshelf database uses PostgreSQL with the following schema. All tables use UUID primary keys for scalability and security.

## Tables

### Users

Stores user account information and authentication data.

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    avatar_url VARCHAR(255),
    bio TEXT,
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);
```

**Columns:**
- `id` - Unique identifier (UUID)
- `username` - Unique username for login
- `email` - Unique email address
- `password_hash` - Bcrypt hashed password
- `first_name` - User's first name
- `last_name` - User's last name
- `avatar_url` - URL to user's avatar image
- `bio` - User biography
- `role` - User role: USER, MODERATOR, ADMIN
- `is_active` - Account active status
- `created_at` - Account creation timestamp
- `updated_at` - Last update timestamp

**Indexes:**
- `idx_users_username` - For fast username lookups
- `idx_users_email` - For fast email lookups

**Relationships:**
- Has many: Reviews, Collections, Orders

---

### Books

Stores book information and metadata.

```sql
CREATE TABLE books (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    isbn VARCHAR(20) UNIQUE,
    publication_year INTEGER,
    publisher VARCHAR(255),
    cover_url VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);
```

**Columns:**
- `id` - Unique identifier (UUID)
- `title` - Book title
- `description` - Book description
- `isbn` - International Standard Book Number (unique)
- `publication_year` - Year of publication
- `publisher` - Publisher name
- `cover_url` - URL to book cover image
- `created_at` - Record creation timestamp
- `updated_at` - Last update timestamp

**Indexes:**
- `idx_books_title` - For full-text search on titles

**Relationships:**
- Many-to-Many: Authors (through book_authors)
- Many-to-Many: Categories (through book_categories)
- Has many: Reviews, Order Items, Collection Books

---

### Authors

Stores author information.

```sql
CREATE TABLE authors (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    biography TEXT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);
```

**Columns:**
- `id` - Unique identifier (UUID)
- `first_name` - Author's first name
- `last_name` - Author's last name
- `biography` - Author biography
- `created_at` - Record creation timestamp
- `updated_at` - Last update timestamp

**Relationships:**
- Many-to-Many: Books (through book_authors)

---

### Book Authors (Junction Table)

Maps books to their authors (many-to-many relationship).

```sql
CREATE TABLE book_authors (
    book_id UUID NOT NULL,
    author_id UUID NOT NULL,
    PRIMARY KEY (book_id, author_id)
);
```

**Columns:**
- `book_id` - Foreign key to books table
- `author_id` - Foreign key to authors table

---

### Categories

Stores book categories or genres.

```sql
CREATE TABLE categories (
    id UUID PRIMARY KEY,
    name VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);
```

**Columns:**
- `id` - Unique identifier (UUID)
- `name` - Category name (e.g., "Fiction", "Science Fiction")
- `description` - Category description
- `created_at` - Record creation timestamp
- `updated_at` - Last update timestamp

**Relationships:**
- Many-to-Many: Books (through book_categories)

---

### Book Categories (Junction Table)

Maps books to their categories (many-to-many relationship).

```sql
CREATE TABLE book_categories (
    book_id UUID NOT NULL,
    category_id UUID NOT NULL,
    PRIMARY KEY (book_id, category_id)
);
```

**Columns:**
- `book_id` - Foreign key to books table
- `category_id` - Foreign key to categories table

---

### Reviews

Stores user reviews of books.

```sql
CREATE TABLE reviews (
    id UUID PRIMARY KEY,
    book_id UUID NOT NULL,
    user_id UUID NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title VARCHAR(255),
    content TEXT,
    helpful_count INTEGER DEFAULT 0,
    unhelpful_count INTEGER DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (book_id, user_id)
);
```

**Columns:**
- `id` - Unique identifier (UUID)
- `book_id` - Foreign key to books table
- `user_id` - Foreign key to users table
- `rating` - Rating value 1-5
- `title` - Review title
- `content` - Review content
- `helpful_count` - Number of helpful votes
- `unhelpful_count` - Number of unhelpful votes
- `status` - Review status: PENDING, APPROVED, REJECTED
- `created_at` - Review creation timestamp
- `updated_at` - Last update timestamp

**Constraints:**
- Each user can only have one review per book (UNIQUE constraint)
- Rating must be between 1 and 5

**Indexes:**
- `idx_reviews_book_id` - For fast book review lookups
- `idx_reviews_user_id` - For fast user review lookups

**Relationships:**
- Belongs to: User, Book

---

### Collections

Stores user collections (reading lists).

```sql
CREATE TABLE collections (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_public BOOLEAN DEFAULT false,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);
```

**Columns:**
- `id` - Unique identifier (UUID)
- `user_id` - Foreign key to users table
- `name` - Collection name
- `description` - Collection description
- `is_public` - Whether collection is publicly visible
- `status` - Collection status: ACTIVE, ARCHIVED, DELETED
- `created_at` - Creation timestamp
- `updated_at` - Last update timestamp

**Indexes:**
- `idx_collections_user_id` - For fast user collection lookups

**Relationships:**
- Belongs to: User
- Many-to-Many: Books (through collection_books)

---

### Collection Books (Junction Table)

Maps books to collections (many-to-many relationship).

```sql
CREATE TABLE collection_books (
    collection_id UUID NOT NULL,
    book_id UUID NOT NULL,
    added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (collection_id, book_id)
);
```

**Columns:**
- `collection_id` - Foreign key to collections table
- `book_id` - Foreign key to books table
- `added_at` - When book was added to collection

---

### Orders

Stores customer orders.

```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);
```

**Columns:**
- `id` - Unique identifier (UUID)
- `user_id` - Foreign key to users table
- `status` - Order status: PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
- `total_price` - Total order price
- `created_at` - Order creation timestamp
- `updated_at` - Last update timestamp

**Indexes:**
- `idx_orders_user_id` - For fast user order lookups

**Relationships:**
- Belongs to: User
- Has many: Order Items

---

### Order Items

Stores individual items in an order.

```sql
CREATE TABLE order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    book_id UUID NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE
);
```

**Columns:**
- `id` - Unique identifier (UUID)
- `order_id` - Foreign key to orders table
- `book_id` - Foreign key to books table
- `quantity` - Number of copies ordered
- `price` - Price per unit at time of order
- `created_at` - Item creation timestamp

**Indexes:**
- `idx_order_items_order_id` - For fast order item lookups

**Relationships:**
- Belongs to: Order, Book

---

## Entity Relationship Diagram

```
┌──────────────────┐
│      Users       │
├──────────────────┤
│ id (PK)          │
│ username         │
│ email            │
│ password_hash    │
│ ...              │
└──────────────────┘
    │        │
    │        ├─────────────────┐
    │        │                 │
    ▼        ▼                 ▼
┌──────────────────┐  ┌──────────────────┐  ┌──────────────────┐
│    Reviews       │  │  Collections     │  │     Orders       │
├──────────────────┤  ├──────────────────┤  ├──────────────────┤
│ id (PK)          │  │ id (PK)          │  │ id (PK)          │
│ book_id (FK)     │  │ user_id (FK)     │  │ user_id (FK)     │
│ user_id (FK)     │  │ name             │  │ status           │
│ rating           │  │ is_public        │  │ total_price      │
│ ...              │  │ ...              │  │ ...              │
└──────────────────┘  └──────────────────┘  └──────────────────┘
    │                      │                      │
    └──────┬───────────────┴──────────────────────┤
           │                                      │
    ┌──────▼────────────┐                  ┌─────▼───────────┐
    │      Books       │                  │  OrderItems    │
    ├──────────────────┤                  ├────────────────┤
    │ id (PK)          │                  │ id (PK)        │
    │ title            │                  │ order_id (FK)  │
    │ isbn             │                  │ book_id (FK)   │
    │ ...              │                  │ quantity       │
    └──────────────────┘                  │ price          │
         │      │                         └────────────────┘
         │      │
    ┌────▼──┴───────┐         ┌────────────────────┐
    │ BookAuthors   │         │  BookCategories    │
    ├──────────────┤         ├────────────────────┤
    │ book_id (FK) │         │ book_id (FK)       │
    │ author_id(FK)│         │ category_id (FK)   │
    └────┬──▲──────┘         └────┬───▲───────────┘
         │  │                     │   │
    ┌────▼──┴──────┐         ┌────▼───┴──────────┐
    │    Authors   │         │   Categories     │
    ├──────────────┤         ├──────────────────┤
    │ id (PK)      │         │ id (PK)          │
    │ first_name   │         │ name             │
    │ last_name    │         │ description      │
    │ biography    │         │ ...              │
    └──────────────┘         └──────────────────┘
```

## Common Queries

### Get Book with Authors and Categories
```sql
SELECT b.*,
       json_agg(DISTINCT a.*) as authors,
       json_agg(DISTINCT c.*) as categories
FROM books b
LEFT JOIN book_authors ba ON b.id = ba.book_id
LEFT JOIN authors a ON ba.author_id = a.id
LEFT JOIN book_categories bc ON b.id = bc.book_id
LEFT JOIN categories c ON bc.category_id = c.id
WHERE b.id = $1
GROUP BY b.id;
```

### Get Book Reviews with User Info
```sql
SELECT r.*, u.username, u.avatar_url
FROM reviews r
JOIN users u ON r.user_id = u.id
WHERE r.book_id = $1 AND r.status = 'APPROVED'
ORDER BY r.created_at DESC;
```

### Get User Collections with Book Count
```sql
SELECT c.*, COUNT(cb.book_id) as book_count
FROM collections c
LEFT JOIN collection_books cb ON c.id = cb.collection_id
WHERE c.user_id = $1
GROUP BY c.id;
```

### Get User Orders with Items
```sql
SELECT o.*,
       json_agg(json_build_object(
           'id', oi.id,
           'book_id', oi.book_id,
           'quantity', oi.quantity,
           'price', oi.price
       )) as items
FROM orders o
LEFT JOIN order_items oi ON o.id = oi.order_id
WHERE o.user_id = $1
GROUP BY o.id
ORDER BY o.created_at DESC;
```

## Performance Considerations

1. **Indexing**: All foreign keys and frequently queried fields are indexed
2. **Pagination**: Use LIMIT and OFFSET for large result sets
3. **Materialized Views**: Consider for frequently aggregated data
4. **Partitioning**: Consider partitioning reviews and orders by date for very large datasets
5. **Connection Pooling**: Use connection pool (HikariCP in Spring Boot) to manage database connections

## Migration Strategy

New migrations should:
1. Be added to `backend/src/main/resources/db/migration/`
2. Follow naming convention: `V{version}__Description.sql`
3. Be idempotent where possible
4. Include rollback information in comments

Example:
```sql
-- V2__Add_user_roles.sql
-- Adds role-based access control to users table

ALTER TABLE users ADD COLUMN role VARCHAR(50) DEFAULT 'USER' NOT NULL;
CREATE INDEX idx_users_role ON users(role);

-- Rollback:
-- DROP INDEX IF EXISTS idx_users_role;
-- ALTER TABLE users DROP COLUMN role;
```

## Backups

Regular backups recommended:

```bash
# Full backup
pg_dump -U bookshelf_user -h localhost bookshelf > backup_$(date +%Y%m%d_%H%M%S).sql

# Restore from backup
psql -U bookshelf_user -h localhost bookshelf < backup.sql
```
