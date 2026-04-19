# Deployment Guide

This guide covers deployment of the Bookshelf application using Docker and Docker Compose.

## Prerequisites

- Docker 20.10+
- Docker Compose 2.0+
- Git
- At least 2GB free disk space

## Quick Start with Docker Compose

### 1. Clone the Repository

```bash
git clone <repository-url>
cd bookshelf
```

### 2. Create Environment File

Copy the example environment file and update with your settings:

```bash
cp .env.example .env
```

Edit `.env` with your desired configuration:

```env
DB_NAME=bookshelf
DB_USER=bookshelf_user
DB_PASSWORD=your-secure-password
DB_PORT=5432
BACKEND_PORT=8080
FRONTEND_PORT=3000
```

### 3. Build and Start Services

```bash
docker-compose build
docker-compose up -d
```

This will start:
- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080/api
- **PostgreSQL**: localhost:5432

### 4. Verify Services

Check if all services are running:

```bash
docker-compose ps
```

Expected output:
```
NAME                    STATUS
bookshelf-db            Up (healthy)
bookshelf-backend       Up (healthy)
bookshelf-frontend      Up (healthy)
```

## Docker Compose Commands

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f db
```

### Stop Services

```bash
docker-compose down
```

### Remove All Data

```bash
docker-compose down -v
```

## Service Configuration

### Frontend Service

- **Port**: 3000 (configurable via FRONTEND_PORT)
- **Server**: Nginx
- **Build**: Multi-stage Node.js + Nginx
- **Health Check**: Enabled

The frontend is served on port 3000 and proxies API calls to the backend at `/api/`.

### Backend Service

- **Port**: 8080 (configurable via BACKEND_PORT)
- **Framework**: Spring Boot 3.2.0
- **Build**: Maven multi-stage build
- **Database**: PostgreSQL
- **Health Check**: Enabled

The backend API is accessible at `http://localhost:8080/api`.

### Database Service

- **Type**: PostgreSQL 15
- **Port**: 5432 (configurable via DB_PORT)
- **Data Volume**: postgres_data (persistent)
- **Initialization Script**: docs/init-db.sql
- **Health Check**: Enabled

## Database Migrations

Flyway automatically runs migrations on backend startup. Migrations are located in:

```
backend/src/main/resources/db/migration/
```

### Creating New Migrations

1. Create a new SQL file in `backend/src/main/resources/db/migration/`:
   ```
   V2__Add_new_table.sql
   ```

2. Write your migration SQL
3. Rebuild and restart the backend service

The version number must be incremented sequentially.

## Environment Variables

### Database

- `DB_NAME` - Database name (default: bookshelf)
- `DB_USER` - Database username (default: bookshelf_user)
- `DB_PASSWORD` - Database password (required for production)
- `DB_PORT` - Database port (default: 5432)

### Backend

- `BACKEND_PORT` - Backend service port (default: 8080)
- `SPRING_PROFILE` - Spring profile: dev, test, or prod (default: prod)
- `JWT_SECRET` - Secret key for JWT tokens (required)
- `JWT_EXPIRATION` - JWT token expiration in milliseconds

### Frontend

- `FRONTEND_PORT` - Frontend service port (default: 3000)
- `CORS_ALLOWED_ORIGINS` - Comma-separated list of allowed origins

## SSL/TLS Configuration

For production, add an Nginx reverse proxy with SSL:

```yaml
# docker-compose.override.yml
services:
  reverse-proxy:
    image: nginx:alpine
    ports:
      - "443:443"
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ./ssl:/etc/nginx/ssl
    depends_on:
      - frontend
      - backend
```

## Scaling

To scale backend services:

```bash
docker-compose up -d --scale backend=3
```

Note: Database and frontend should run as single instances.

## Monitoring and Logs

### Check Service Health

```bash
docker-compose ps
```

### View Specific Service Logs

```bash
# Frontend errors
docker-compose logs frontend

# Backend errors
docker-compose logs backend

# Database errors
docker-compose logs db
```

### Database Backups

Backup the database:

```bash
docker-compose exec db pg_dump -U bookshelf_user bookshelf > backup.sql
```

Restore from backup:

```bash
docker-compose exec -T db psql -U bookshelf_user bookshelf < backup.sql
```

## Troubleshooting

### Services won't start

1. Check ports are not in use:
   ```bash
   netstat -an | grep 3000
   netstat -an | grep 8080
   netstat -an | grep 5432
   ```

2. Check Docker daemon is running:
   ```bash
   docker ps
   ```

### Database connection errors

1. Verify PostgreSQL is healthy:
   ```bash
   docker-compose exec db pg_isready
   ```

2. Check environment variables match docker-compose.yml

### Frontend can't reach backend

1. Verify backend is running:
   ```bash
   docker-compose logs backend
   ```

2. Check nginx.conf proxy settings

### Out of disk space

```bash
docker system prune -a
docker volume prune
```

## Production Deployment

For production deployment:

1. Use environment-specific `.env` files
2. Set strong passwords in DB_PASSWORD and JWT_SECRET
3. Configure SSL/TLS certificates
4. Set SPRING_PROFILE=prod
5. Use proper reverse proxy (Nginx, HAProxy)
6. Enable database backups and monitoring
7. Use Docker volumes or external storage for data
8. Set resource limits in docker-compose.yml

## Additional Resources

- [Docker Documentation](https://docs.docker.com/)
- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
