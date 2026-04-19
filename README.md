# Bookshelf - Online Bookstore Platform

A comprehensive, modern full-stack web application for an online bookstore with user authentication, book catalog management, reviews, collections, and order processing.

## 🎯 Features

- **User Authentication**: Secure login/registration with JWT tokens
- **Book Catalog**: Browse and search thousands of books
- **User Reviews**: Rate and review books with moderation system
- **Collections**: Create and manage personal book collections
- **Orders**: Place and track book orders
- **Admin Panel**: Manage books, reviews, and users
- **Responsive Design**: Works seamlessly on desktop and mobile devices

## 🏗️ Project Structure

```
bookshelf/
├── frontend/                    # Vue.js 3 SPA with Vite
├── backend/                     # Spring Boot REST API with Maven
├── docs/                        # Documentation
├── docker-compose.yml          # Docker Compose configuration
├── Dockerfile.frontend         # Frontend Docker image
├── Dockerfile.backend          # Backend Docker image
├── Dockerfile.db               # Database Docker image
├── .dockerignore                # Docker ignore rules
├── .env.example                # Example environment variables
└── README.md                   # This file
```

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose (recommended)
- Node.js 18+ (for frontend development)
- Java 17+ & Maven 3.8+ (for backend development)
- PostgreSQL 14+ (for local development)

### Using Docker (Recommended)

```bash
# Clone repository
git clone <repository-url>
cd bookshelf

# Copy environment template
cp .env.example .env

# Build and start all services
docker-compose up -d

# View logs
docker-compose logs -f
```

Services will be available at:
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **Database**: localhost:5432

See [DEPLOYMENT.md](./docs/DEPLOYMENT.md) for detailed Docker instructions.

### Local Development

For local development setup, see [DEVELOPMENT.md](./docs/DEVELOPMENT.md).

**Frontend:**
```bash
cd frontend
npm install
npm run dev
```

**Backend:**
```bash
cd backend
mvn spring-boot:run
```

## 📚 Documentation

- **[DEVELOPMENT.md](./docs/DEVELOPMENT.md)** - Local development setup and workflow
- **[DEPLOYMENT.md](./docs/DEPLOYMENT.md)** - Docker and production deployment
- **[API.md](./docs/API.md)** - REST API documentation with examples
- **[DATABASE.md](./docs/DATABASE.md)** - Database schema and relationships

## 🛠️ Technology Stack

### Frontend
- Vue.js 3 with Vite
- TypeScript
- Pinia (State Management)
- Vue Router

### Backend
- Spring Boot 3.2.0
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway (Database Migrations)
- JWT Authentication

### DevOps
- Docker & Docker Compose
- Nginx (Web Server)

## 🚀 Quick Commands

```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f

# Backend development
cd backend && mvn spring-boot:run

# Frontend development
cd frontend && npm run dev
```

## 📝 License

All rights reserved. Proprietary software.
