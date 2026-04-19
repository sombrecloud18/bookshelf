# API Documentation

Base URL: `http://localhost:8080/api`

## Authentication

The API uses JWT (JSON Web Token) authentication. Include the token in the Authorization header:

```
Authorization: Bearer {token}
```

### Auth Endpoints

#### Login
```
POST /auth/login
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password"
}

Response: 200 OK
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400,
  "user": {
    "id": "uuid",
    "username": "user@example.com",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "USER"
  }
}
```

#### Register
```
POST /auth/register
Content-Type: application/json

{
  "username": "newuser",
  "email": "user@example.com",
  "password": "password",
  "firstName": "John",
  "lastName": "Doe"
}

Response: 201 Created
{
  "id": "uuid",
  "username": "newuser",
  "email": "user@example.com",
  "token": "eyJhbGciOiJIUzI1NiIs..."
}
```

#### Refresh Token
```
POST /auth/refresh
Authorization: Bearer {token}

Response: 200 OK
{
  "token": "new-token",
  "expiresIn": 86400
}
```

## Books

### Get All Books
```
GET /books
Query Parameters:
  - page: int (default: 0)
  - size: int (default: 20)
  - sort: string (default: "title,asc")
  - category: string (optional)

Response: 200 OK
{
  "content": [
    {
      "id": "uuid",
      "title": "Book Title",
      "description": "Book description",
      "isbn": "978-0-123456-78-9",
      "publicationYear": 2023,
      "publisher": "Publisher Name",
      "coverUrl": "https://...",
      "authors": [
        {
          "id": "uuid",
          "firstName": "John",
          "lastName": "Doe"
        }
      ],
      "categories": ["Fiction", "Drama"]
    }
  ],
  "totalPages": 5,
  "totalElements": 100,
  "currentPage": 0
}
```

### Get Book by ID
```
GET /books/{id}

Response: 200 OK
{
  "id": "uuid",
  "title": "Book Title",
  "description": "Book description",
  ... (full book object)
}
```

### Create Book (Admin only)
```
POST /books
Authorization: Bearer {admin-token}
Content-Type: application/json

{
  "title": "New Book",
  "description": "Description",
  "isbn": "978-0-123456-78-9",
  "publicationYear": 2024,
  "publisher": "Publisher",
  "authorIds": ["uuid1", "uuid2"],
  "categoryIds": ["uuid3", "uuid4"]
}

Response: 201 Created
{
  "id": "uuid",
  ... (created book object)
}
```

### Update Book (Admin only)
```
PUT /books/{id}
Authorization: Bearer {admin-token}
Content-Type: application/json

{
  "title": "Updated Title",
  "description": "Updated description",
  ... (fields to update)
}

Response: 200 OK
{
  "id": "uuid",
  ... (updated book object)
}
```

### Delete Book (Admin only)
```
DELETE /books/{id}
Authorization: Bearer {admin-token}

Response: 204 No Content
```

## Reviews

### Get Book Reviews
```
GET /books/{bookId}/reviews
Query Parameters:
  - page: int (default: 0)
  - size: int (default: 10)
  - sort: string (default: "createdAt,desc")

Response: 200 OK
{
  "content": [
    {
      "id": "uuid",
      "bookId": "uuid",
      "userId": "uuid",
      "userName": "user@example.com",
      "rating": 5,
      "title": "Great book!",
      "content": "This book is amazing...",
      "helpfulCount": 10,
      "unhelpfulCount": 1,
      "status": "APPROVED",
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ],
  "totalPages": 3,
  "totalElements": 25,
  "currentPage": 0
}
```

### Create Review
```
POST /books/{bookId}/reviews
Authorization: Bearer {token}
Content-Type: application/json

{
  "rating": 5,
  "title": "Great book!",
  "content": "This book exceeded my expectations..."
}

Response: 201 Created
{
  "id": "uuid",
  "bookId": "bookId",
  "rating": 5,
  "title": "Great book!",
  "content": "This book exceeded my expectations...",
  "status": "PENDING",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

### Update Review
```
PUT /reviews/{id}
Authorization: Bearer {token}
Content-Type: application/json

{
  "rating": 4,
  "title": "Good book",
  "content": "Updated review..."
}

Response: 200 OK
{
  "id": "uuid",
  ... (updated review)
}
```

### Delete Review
```
DELETE /reviews/{id}
Authorization: Bearer {token}

Response: 204 No Content
```

### Mark Review as Helpful
```
POST /reviews/{id}/helpful
Authorization: Bearer {token}

Response: 200 OK
{
  "id": "uuid",
  "helpfulCount": 11
}
```

## Users

### Get User Profile
```
GET /users/profile
Authorization: Bearer {token}

Response: 200 OK
{
  "id": "uuid",
  "username": "username",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "avatarUrl": "https://...",
  "bio": "User bio",
  "createdAt": "2023-01-01T00:00:00Z"
}
```

### Get User by ID
```
GET /users/{id}

Response: 200 OK
{
  "id": "uuid",
  "username": "username",
  "firstName": "John",
  "lastName": "Doe",
  "avatarUrl": "https://...",
  "bio": "User bio"
}
```

### Update User Profile
```
PUT /users/profile
Authorization: Bearer {token}
Content-Type: application/json

{
  "firstName": "Jane",
  "lastName": "Smith",
  "bio": "Updated bio",
  "avatarUrl": "https://..."
}

Response: 200 OK
{
  "id": "uuid",
  ... (updated user)
}
```

### Change Password
```
PUT /users/password
Authorization: Bearer {token}
Content-Type: application/json

{
  "currentPassword": "current-password",
  "newPassword": "new-password"
}

Response: 200 OK
{
  "message": "Password changed successfully"
}
```

## Collections

### Get User Collections
```
GET /collections
Authorization: Bearer {token}
Query Parameters:
  - page: int (default: 0)
  - size: int (default: 20)

Response: 200 OK
{
  "content": [
    {
      "id": "uuid",
      "userId": "uuid",
      "name": "My Favorite Books",
      "description": "Books I loved",
      "isPublic": true,
      "bookCount": 15,
      "createdAt": "2023-06-15T10:00:00Z"
    }
  ],
  "totalPages": 2,
  "totalElements": 35
}
```

### Get Collection by ID
```
GET /collections/{id}

Response: 200 OK
{
  "id": "uuid",
  "name": "My Favorite Books",
  "description": "Books I loved",
  "isPublic": true,
  "creator": {
    "id": "uuid",
    "username": "user"
  },
  "books": [
    { ... book objects ... }
  ],
  "createdAt": "2023-06-15T10:00:00Z"
}
```

### Create Collection
```
POST /collections
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "My Favorite Books",
  "description": "Books I loved",
  "isPublic": true
}

Response: 201 Created
{
  "id": "uuid",
  ... (created collection)
}
```

### Add Book to Collection
```
POST /collections/{id}/books/{bookId}
Authorization: Bearer {token}

Response: 200 OK
{
  "id": "uuid",
  "bookCount": 16
}
```

### Remove Book from Collection
```
DELETE /collections/{id}/books/{bookId}
Authorization: Bearer {token}

Response: 204 No Content
```

### Delete Collection
```
DELETE /collections/{id}
Authorization: Bearer {token}

Response: 204 No Content
```

## Orders

### Get User Orders
```
GET /orders
Authorization: Bearer {token}
Query Parameters:
  - page: int (default: 0)
  - size: int (default: 20)
  - status: string (optional) - PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED

Response: 200 OK
{
  "content": [
    {
      "id": "uuid",
      "status": "DELIVERED",
      "totalPrice": 99.99,
      "itemCount": 3,
      "createdAt": "2024-01-10T15:30:00Z",
      "updatedAt": "2024-01-12T10:00:00Z"
    }
  ],
  "totalPages": 5,
  "totalElements": 87
}
```

### Get Order Details
```
GET /orders/{id}
Authorization: Bearer {token}

Response: 200 OK
{
  "id": "uuid",
  "status": "DELIVERED",
  "totalPrice": 99.99,
  "items": [
    {
      "id": "uuid",
      "book": {
        "id": "uuid",
        "title": "Book Title",
        "isbn": "978-0-123456-78-9"
      },
      "quantity": 2,
      "price": 25.99
    }
  ],
  "createdAt": "2024-01-10T15:30:00Z",
  "updatedAt": "2024-01-12T10:00:00Z"
}
```

### Create Order
```
POST /orders
Authorization: Bearer {token}
Content-Type: application/json

{
  "items": [
    {
      "bookId": "uuid",
      "quantity": 2
    }
  ]
}

Response: 201 Created
{
  "id": "uuid",
  "status": "PENDING",
  "totalPrice": 99.99,
  ... (order object)
}
```

### Cancel Order
```
PUT /orders/{id}/cancel
Authorization: Bearer {token}

Response: 200 OK
{
  "id": "uuid",
  "status": "CANCELLED"
}
```

## Error Responses

All error responses follow this format:

```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/books",
  "details": {
    "field": "title",
    "message": "Title is required"
  }
}
```

### Common Status Codes

- `200 OK` - Request succeeded
- `201 Created` - Resource created
- `204 No Content` - Request succeeded, no content
- `400 Bad Request` - Invalid request
- `401 Unauthorized` - Missing or invalid token
- `403 Forbidden` - Access denied
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource already exists
- `500 Internal Server Error` - Server error

## Rate Limiting

Rate limits are applied per user:
- 100 requests per minute for authenticated users
- 20 requests per minute for unauthenticated users

Headers:
```
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1705325400
```

## Pagination

List endpoints support pagination:

Query Parameters:
- `page`: Zero-indexed page number (default: 0)
- `size`: Number of items per page (default: 20, max: 100)
- `sort`: Sort field and direction (e.g., "createdAt,desc")

Response includes:
- `content`: Array of items
- `totalPages`: Total number of pages
- `totalElements`: Total number of items
- `currentPage`: Current page number
- `pageSize`: Items per page
