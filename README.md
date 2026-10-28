# JobJam

Track the jobs you're applying for (or already applied to).

- `backend/`: Spring Boot 4 (Java 17), JPA, Flyway, PostgreSQL. Runs on port **8090**.
- `frontend/`: React 19 + TypeScript + Vite. Runs on port **5173** and proxies `/api` to the backend.
- `docker-compose.yml`: PostgreSQL 16 on port **5440** (db/user/password: `jobjam`).

## Run it

```bash
# 1. Database
docker compose up -d

# 2. Backend (from backend/). The API key enables "Extract with AI":
#    cp .env.example .env and put your key in .env (git-ignored),
#    or export ANTHROPIC_API_KEY=sk-ant-... in your shell.
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
| POST   | `/api/jobs/extract` | Extract `{title, details}` from `{url}` or pasted `{postingText}` using Claude |
| POST   | `/api/jobs/extract/screenshots` | Same, from multipart `files` (1-8 PNG/JPEG/WebP/GIF, 5 MB each) and optional `url` |

## AI extraction

`POST /api/jobs/extract` asks Claude (`claude-opus-5-5`, override with `ANTHROPIC_MODEL`) to pull
everything about the job into Markdown `details`. With a `url`, Claude fetches the page through its
server-side web fetch tool; some job sites refuse automated access, so the form also lets you paste
the posting text or upload screenshots of it, which are sent instead. Without `ANTHROPIC_API_KEY` the rest of the app still
works and the endpoint answers 503.

Schema changes go in `backend/src/main/resources/db/migration` as new Flyway migrations (`V2__...sql`).
