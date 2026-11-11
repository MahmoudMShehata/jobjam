# JobJam

Track the jobs you're applying for (or already applied to).

- `backend/`: Spring Boot 4 (Java 17), JPA, Flyway, PostgreSQL. Runs on port **8090**.
- `frontend/`: React 19 + TypeScript + Vite. Runs on port **5173** and proxies `/api` to the backend.
- `docker-compose.yml`: PostgreSQL 16 on port **5440** (db/user/password: `jobjam`).

## Run it

```bash
# 1. Database
docker compose up -d

# 2. Local AI model for "Extract with AI" (free; see "AI extraction" below)
brew install ollama && brew services start ollama
ollama pull qwen3-vl:8b-instruct   # first time only, ~6 GB

# 3. Backend (from backend/). Optional settings go in .env (see .env.example).
./mvnw spring-boot:run

# 4. Frontend (from frontend/)
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
| POST   | `/api/jobs/extract` | Extract `{title, description, requirements, benefits, details}` from `{url}` or pasted `{postingText}` using AI |
| POST   | `/api/jobs/extract/screenshots` | Same, from multipart `files` (1-8 PNG/JPEG/WebP/GIF, 5 MB each) and optional `url` |

## AI extraction

`POST /api/jobs/extract` asks an AI model to pull everything about the job into Markdown `details`.
`AI_PROVIDER` picks the model; the rest of the app works without either, and the endpoint answers 503.

- **`ollama`** (default, free): a vision model running locally in [Ollama](https://ollama.com),
  `qwen3-vl:8b-instruct` unless `OLLAMA_MODEL` says otherwise. The backend downloads posting links itself
  (`PageFetcher`), so pages that only render with JavaScript or block bots cannot be read. It is
  slower and less precise than Claude, and refuses links to local or private network addresses.
- **`anthropic`**: Claude (`claude-opus-5-5`, override with `ANTHROPIC_MODEL`; `claude-haiku-5-5` is
  much cheaper), with `ANTHROPIC_API_KEY`. Claude fetches links through its server-side web fetch tool.

Some job sites refuse automated access either way, so the form also lets you paste the posting text
or upload screenshots of it, which are sent instead.

Schema changes go in `backend/src/main/resources/db/migration` as new Flyway migrations (`V2__...sql`).
