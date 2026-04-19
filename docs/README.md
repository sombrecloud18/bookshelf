# Bookshelf Documentation

## Project Overview

Bookshelf is a comprehensive online bookstore platform with user authentication, book catalog management, user reviews, collections, and order management features.

## Architecture

The project is organized as a multi-module Maven structure:

- **Frontend**: Vue.js 3 SPA with Vite bundler
- **Backend**: Spring Boot REST API with Spring Security and PostgreSQL
- **Database**: PostgreSQL with Flyway migrations

## Documentation Contents

- [API Documentation](./API.md) - REST API endpoints and usage
- [Database Schema](./DATABASE.md) - Database tables and relationships
- [Deployment Guide](./DEPLOYMENT.md) - Docker and deployment instructions
- [Development Guide](./DEVELOPMENT.md) - Setup and development workflow

## Technology Stack

### Frontend
- Vue.js 3
- Vite
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

### Infrastructure
- Docker & Docker Compose
- PostgreSQL
- Nginx (optional reverse proxy)

## Quick Start

See [Deployment Guide](./DEPLOYMENT.md) for Docker setup or [Development Guide](./DEVELOPMENT.md) for local development.

## Project Structure

```
bookshelf/
├── frontend/               # Vue.js application
├── backend/                # Spring Boot backend
├── docs/                   # Documentation
├── docker-compose.yml      # Docker Compose configuration
├── Dockerfile.frontend     # Frontend Docker image
├── Dockerfile.backend      # Backend Docker image
├── Dockerfile.db           # Database Docker image (PostgreSQL)
└── .dockerignore          # Docker ignore rules
```

## Getting Started

### Prerequisites
- Docker & Docker Compose (for containerized setup)
- Node.js 18+ and npm (for frontend development)
- Java 17+ and Maven 3.8+ (for backend development)
- PostgreSQL 14+ (for local database)

### Using Docker (Recommended)

```bash
docker-compose up -d
```

This will start:
- Frontend on http://localhost:3000
- Backend on http://localhost:8080
- Database on localhost:5432

### Local Development

See [Development Guide](./DEVELOPMENT.md) for detailed setup instructions.

## API Endpoints

Base URL: `http://localhost:8080/api`

Key endpoints:
- `POST /auth/login` - User login
- `POST /auth/register` - User registration
- `GET /books` - Get all books
- `POST /reviews` - Create review
- `GET /users/{id}` - Get user profile
- `POST /orders` - Create order

See [API Documentation](./API.md) for complete endpoint list.

## Contributing

1. Create a feature branch
2. Make your changes
3. Test locally
4. Submit a pull request

## License

Proprietary - All rights reserved
