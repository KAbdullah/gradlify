# 🧪 Gradify - Student Submission & Auto-Grading System

Gradify is a Spring Boot + React application for managing programming assignments and auto-grading Java submissions.

---

## 🚀 Features

- Upload `.java` student files and automatically compile/test them
- Select course, assignment, and test case dynamically
- Score calculated based on test cases passed
- Instant grading feedback (grade, pass/fail count, and total)

---

## 🛠️ Technologies

- Java 17
- Maven 3.9+
- Node.js 18+ and npm
- PostgreSQL 14+ (pgAdmin 4)

---


### 1. Configure PostgreSQL

Run pgAdmin 4 where you host the gradify database with the below mentioned credential.

Create a local database/user (example values):

```bash
createdb gradify
createuser gradify
psql -c "ALTER USER gradify WITH PASSWORD 'gradify';"
psql -c "GRANT ALL PRIVILEGES ON DATABASE gradify TO gradify;"
```

Set backend environment variables:

```bash
spring.datasource.url=jdbc:postgresql://localhost:5433/gradify
spring.datasource.username=gradify
spring.datasource.password=gradify

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

logging.level.org.hibernate=DEBUG
logging.level.org.springframework.orm.jpa=DEBUG
```

## 2. Run the backend

From the repository root:

```bash
mvn spring-boot:run
```

Backend runs on `http://localhost:8082`.

## 3. Run the frontend

In a new terminal:

```bash
cd frontend
npm install
npm start
```

Frontend runs on `http://localhost:3000` and proxies API requests to `http://localhost:8082`.

## 4) Verify locally

- Open `http://localhost:3000`
- Login with one of the seeded users
- Confirm pages load and API actions succeed
