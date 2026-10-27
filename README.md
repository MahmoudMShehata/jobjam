# JobJam

Track the jobs you're applying for (or already applied to).

- `backend/`: Spring Boot 4 (Java 17), JPA, Flyway, PostgreSQL. Runs on port **8090**.
- `frontend/`: React 19 + TypeScript + Vite. Runs on port **5173** and proxies `/api` to the backend.
- `docker-compose.yml`: PostgreSQL 16 on port **5440** (db/user/password: `jobjam`).

## Run it

```bash
# 1. Database
docker compose up -d

# 2. Backend (from backend/)
./mvnw spring-boot:run

# 3. Frontend (from frontend/)
npm install   # first time only
npm run dev
```

Open http://localhost:5173.

## API

| Method | Path             | Description            |
|--------|------------------|------------------------|
| GET    | `/api/jobs`      | List jobs, newest first |
| GET    | `/api/jobs/{id}` | Get one job            |
| POST   | `/api/jobs`      | Create a job (`title` required) |
| DELETE | `/api/jobs/{id}` | Delete a job           |

Schema changes go in `backend/src/main/resources/db/migration` as new Flyway migrations (`V2__...sql`).
