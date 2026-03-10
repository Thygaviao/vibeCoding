# Ms Ly English - Homework Submission MVP

## Tech stack
- Frontend: React + TypeScript + Vite + react-i18next
- Backend: Kotlin + Ktor
- Database: PostgreSQL
- Storage: local disk (`backend/uploads`)
- Auth: JWT admin login

## Project structure

```
.
├── backend
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── .env.example
│   └── src/main
│       ├── kotlin/com/mslyenglish
│       │   ├── Application.kt
│       │   ├── config/
│       │   ├── db/
│       │   ├── models/
│       │   ├── routes/
│       │   └── services/
│       └── resources
│           ├── application.yaml
│           └── db/migration/V1__init.sql
└── frontend
    ├── package.json
    ├── .env.example
    └── src
        ├── api/
        ├── components/
        ├── i18n/
        ├── layouts/
        ├── pages/admin/
        ├── pages/public/
        └── types/
```

## Local setup

### 1) PostgreSQL
Create database:
```sql
CREATE DATABASE msly_english;
```

### 2) Backend
```bash
cd backend
cp .env.example .env
# adjust src/main/resources/application.yaml if needed
gradle run
```

Default admin:
- username: `admin`
- password: `Admin123!`

### 3) Frontend
```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Frontend URL: http://localhost:5173
Backend URL: http://localhost:8080

## API summary
- `POST /api/submissions`
- `POST /api/admin/auth/login`
- `POST /api/admin/auth/logout`
- `GET /api/admin/auth/me`
- `GET /api/admin/submissions`
- `GET /api/admin/submissions/:id`
- `PATCH /api/admin/submissions/:id/status`
- `GET /api/admin/files/:id/download`
