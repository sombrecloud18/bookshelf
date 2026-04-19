# Development Guide

This guide covers local development setup for the Bookshelf application.

## Prerequisites

- Node.js 18+ and npm
- Java 17+ (OpenJDK or Oracle JDK)
- Maven 3.8+
- PostgreSQL 14+ (for local database)
- Git

## Project Structure

```
bookshelf/
├── frontend/                          # Vue.js 3 application
│   ├── src/
│   │   ├── components/               # Vue components
│   │   ├── pages/                    # Page components
│   │   ├── layouts/                  # Layout components
│   │   ├── stores/                   # Pinia stores
│   │   ├── router/                   # Vue Router config
│   │   ├── constants/                # Constants
│   │   ├── assets/                   # Static assets
│   │   ├── style.css                # Global styles
│   │   ├── main.ts                  # Entry point
│   │   └── App.vue                   # Root component
│   ├── package.json
│   ├── vite.config.js
│   ├── eslint.config.js
│   └── index.html
│
├── backend/                           # Spring Boot application
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/bookshelf/
│   │   │   │       ├── BookshelfApplication.java
│   │   │   │       ├── config/       # Spring configurations
│   │   │   │       ├── controller/   # REST controllers
│   │   │   │       ├── service/      # Business logic
│   │   │   │       ├── repository/   # Data access
│   │   │   │       ├── entity/       # JPA entities
│   │   │   │       ├── dto/          # Data Transfer Objects
│   │   │   │       ├── security/     # Security configs
│   │   │   │       ├── exception/    # Custom exceptions
│   │   │   │       └── util/         # Utility classes
│   │   │   └── resources/
│   │   │       ├── application.properties
│   │   │       ├── db/
│   │   │       │   └── migration/    # Flyway migrations
│   │   │       └── templates/
│   │   └── test/
│   │       └── java/                 # Unit tests
│   └── pom.xml
│
└── docs/                              # Documentation
    ├── README.md
    ├── DEVELOPMENT.md
    ├── DEPLOYMENT.md
    ├── API.md
    └── DATABASE.md
```

## Frontend Development

### Setup

```bash
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will be available at http://localhost:5173 (Vite default) or configured port.

### Available Scripts

```bash
# Development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Lint code
npm run lint

# Format code
npm run format
```

### Frontend Stack

- **Vue 3** - Progressive JavaScript framework
- **Vite** - Next generation build tool
- **TypeScript** - Type-safe JavaScript
- **Pinia** - State management
- **Vue Router** - Client-side routing
- **Tailwind CSS** - Utility-first CSS (configured)

### Development Workflow

1. Create components in `src/components/`
2. Create pages in `src/pages/`
3. Use Pinia stores for state in `src/stores/`
4. Define routes in `src/router/index.js`
5. Test locally with `npm run dev`

### Hot Module Replacement

Vite provides HMR out of the box. Changes to `.vue` files are instantly reflected.

## Backend Development

### Setup

```bash
cd backend

# Install dependencies (Maven will download them)
mvn clean install

# Start development server
mvn spring-boot:run
```

The backend API will be available at http://localhost:8080/api

### Development Configuration

Create `src/main/resources/application-dev.properties`:

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.flyway.enabled=false
logging.level.root=DEBUG
```

Run with development profile:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

### Available Maven Commands

```bash
# Build project
mvn clean build

# Run tests
mvn test

# Run with development server
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Package for production
mvn clean package

# View dependencies
mvn dependency:tree
```

### Backend Structure

**Controllers** - Handle HTTP requests
```java
@RestController
@RequestMapping("/api/books")
public class BookController {
    @GetMapping
    public List<BookDTO> getAllBooks() { }
}
```

**Services** - Business logic
```java
@Service
public class BookService {
    public List<Book> getAllBooks() { }
}
```

**Repositories** - Database access
```java
public interface BookRepository extends JpaRepository<Book, UUID> {
}
```

**Entities** - JPA models
```java
@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue
    private UUID id;
}
```

**DTOs** - Data Transfer Objects for API
```java
public record BookDTO(UUID id, String title, String description) {
}
```

## Database Development

### Local PostgreSQL Setup

Install PostgreSQL and create database:

```bash
psql -U postgres

CREATE USER bookshelf_user WITH PASSWORD 'bookshelf_password';
CREATE DATABASE bookshelf OWNER bookshelf_user;

GRANT ALL PRIVILEGES ON DATABASE bookshelf TO bookshelf_user;
```

### Running Migrations

Migrations run automatically on application startup. To run manually:

```bash
# Using Flyway Maven plugin (if configured)
mvn flyway:migrate
```

### Creating New Migrations

1. Create file: `backend/src/main/resources/db/migration/V{N}__Description.sql`
2. Example:
   ```sql
   V2__Add_user_roles.sql
   ```
3. Migrations run in version order automatically

### Database Queries

Connect to database:

```bash
psql -U bookshelf_user -d bookshelf -h localhost
```

Useful commands:
```sql
\dt                    -- List all tables
\d table_name         -- Describe table
SELECT * FROM users;  -- Query data
```

## Git Workflow

### Branch Naming

```
feature/feature-name
bugfix/bug-description
docs/documentation-name
```

### Commit Messages

```
feat: add book catalog feature
fix: resolve login validation issue
docs: update deployment guide
refactor: simplify user service
test: add book repository tests
```

### Pull Request Process

1. Create feature branch from `main`
2. Make changes and commit
3. Push to remote
4. Create pull request with description
5. Request review
6. Make requested changes
7. Merge to `main`

## Testing

### Frontend Testing

```bash
cd frontend

# Run tests (if configured)
npm run test

# Run tests with coverage
npm run test:coverage
```

### Backend Testing

```bash
cd backend

# Run all tests
mvn test

# Run specific test
mvn test -Dtest=BookRepositoryTest

# Run with coverage
mvn test jacoco:report
```

### Test Examples

**Frontend Component Test**
```javascript
import { mount } from '@vue/test-utils'
import Card from '@/components/Card.vue'

describe('Card Component', () => {
  it('renders properly', () => {
    const wrapper = mount(Card, {
      props: { title: 'Test' }
    })
    expect(wrapper.text()).toContain('Test')
  })
})
```

**Backend Unit Test**
```java
@SpringBootTest
public class BookServiceTest {
    @Test
    public void testGetAllBooks() {
        List<Book> books = bookService.getAllBooks();
        assertNotNull(books);
    }
}
```

## Debugging

### Frontend Debugging

1. Use Vue DevTools browser extension
2. Open browser DevTools (F12)
3. Set breakpoints in Sources tab
4. Check Network tab for API calls
5. Use console.log() for debugging

### Backend Debugging

In IDE (IntelliJ IDEA):
1. Set breakpoints in code
2. Run `mvn spring-boot:run` or use IDE debug button
3. Code will pause at breakpoints
4. Use debug console to inspect variables

Command line debugging:
```bash
mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"
```

## Performance Tips

### Frontend
- Use lazy loading for routes
- Implement virtual scrolling for large lists
- Minimize bundle size with code splitting
- Use production build for testing: `npm run build && npm run preview`

### Backend
- Add database indexes for frequently queried columns
- Use pagination for large result sets
- Cache frequently accessed data
- Use projections in queries to fetch only needed fields

## Common Issues

### Port Already in Use
```bash
# Find process using port
lsof -i :8080
lsof -i :3000

# Kill process
kill -9 <PID>
```

### Database Connection Error
- Verify PostgreSQL is running
- Check connection string in application.properties
- Verify credentials match

### Module Not Found
```bash
# Frontend
rm -rf node_modules package-lock.json
npm install

# Backend
mvn clean install
```

### Hot Reload Not Working
- For frontend: restart `npm run dev`
- For backend: use Spring DevTools or restart IDE

## IDE Setup

### IntelliJ IDEA

1. Open project: `File > Open > bookshelf`
2. Project Structure: Set Java version to 17
3. Install Vue plugin: `File > Settings > Plugins > Search Vue`
4. Configure Inspections for better code analysis

### VS Code

1. Install extensions:
   - Volar (Vue.js)
   - Extension Pack for Java
   - Spring Boot Extension Pack
   - ESLint
   - Prettier

2. Settings:
   - Format on Save: enabled
   - Default Formatter: Prettier

## Resources

- [Vue.js Documentation](https://vuejs.org/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Maven Documentation](https://maven.apache.org/)
- [Flyway Documentation](https://flywaydb.org/)
