# Backend (mock API)

json-server 0.17 plus a thin Express layer that enforces the contract rules (see `openapi.yaml`).

```
npm install
npm start        # http://localhost:3000  (Android emulator: http://10.0.2.2:3000)
npm run reset    # restore seed data and clear uploaded photos
npm test
```

- `seed.json` is the starting data. `db.json` is created from it on first start and is git-ignored.
- Uploaded photos live in `uploads/` and are served from `/uploads/<file>`.
- Every request needs an `X-User-Id` header, except `GET /users` and `/uploads/*`: the user picker runs before anyone is chosen. `GET /users` lists the seeded Technicians and Operators.
- Errors are `{ "error": { "code", "message" } }`. Branch on `code`; the codes are listed per operation in `openapi.yaml`.
- Set `PORT` or `DB_FILE` to override the defaults.
