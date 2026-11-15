const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { createApp } = require('../src/app');

let server, base, tmp;
const TECH = 'u-tech-1';
const OTHER_TECH = 'u-tech-2';
const OP = 'u-op-1';
const uuid = () => crypto.randomUUID();

before(async () => {
  tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'fir-'));
  fs.copyFileSync(path.join(__dirname, '..', 'seed.json'), path.join(tmp, 'db.json'));
  const app = createApp({ dbFile: path.join(tmp, 'db.json'), uploadsDir: path.join(tmp, 'uploads') });
  await new Promise((r) => { server = app.listen(0, r); });
  base = `http://127.0.0.1:${server.address().port}`;
});
after(() => { server.close(); fs.rmSync(tmp, { recursive: true, force: true }); });

const call = (method, url, user, body) =>
  fetch(base + url, {
    method,
    headers: { ...(user ? { 'X-User-Id': user } : {}), ...(body ? { 'Content-Type': 'application/json' } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });

const newIncident = (extra = {}) => ({
  id: uuid(), title: 'Broken light', description: 'Flickers', category: 'EQUIPMENT', severity: 'LOW', ...extra,
});

async function create(user = TECH, extra) {
  const body = newIncident(extra);
  const res = await call('POST', '/incidents', user, body);
  assert.equal(res.status, 201);
  return res.json();
}

const photoForm = (id, type = 'image/jpeg') => {
  const f = new FormData();
  f.set('id', id);
  f.set('file', new Blob([Buffer.from([0xff, 0xd8, 0xff, 0xd9])], { type }), 'p.jpg');
  return f;
};
const uploadPhoto = (incidentId, user, id = uuid(), type) =>
  fetch(`${base}/incidents/${incidentId}/photos`, { method: 'POST', headers: { 'X-User-Id': user }, body: photoForm(id, type) });

test('requires a known X-User-Id', async () => {
  assert.equal((await call('GET', '/incidents')).status, 401);
  assert.equal((await call('GET', '/incidents', 'nobody')).status, 401);
  assert.equal((await call('GET', '/incidents', TECH)).status, 200);
});

test('user list is public for the picker', async () => {
  const res = await call('GET', '/users');
  assert.equal(res.status, 200);
  assert.ok((await res.json()).some((u) => u.role === 'OPERATOR'));
});

test('create sets server fields and forces OPEN', async () => {
  const inc = await create(TECH, { status: undefined });
  assert.equal(inc.status, 'OPEN');
  assert.equal(inc.reportedBy, TECH);
  assert.ok(inc.createdAt && inc.updatedAt);
  assert.deepEqual(inc.photos, []);
});

test('create rejects client-set status and invalid values', async () => {
  assert.equal((await call('POST', '/incidents', TECH, newIncident({ status: 'RESOLVED' }))).status, 422);
  assert.equal((await call('POST', '/incidents', TECH, newIncident({ severity: 'EXTREME' }))).status, 422);
  assert.equal((await call('POST', '/incidents', TECH, newIncident({ title: ' ' }))).status, 422);
  assert.equal((await call('POST', '/incidents', TECH, { ...newIncident(), id: 'x' })).status, 422);
  assert.equal((await call('POST', '/incidents', OP, newIncident())).status, 403);
});

test('create is idempotent by client id', async () => {
  const body = newIncident();
  assert.equal((await call('POST', '/incidents', TECH, body)).status, 201);
  const again = await call('POST', '/incidents', TECH, body);
  assert.equal(again.status, 200);
  assert.equal((await again.json()).id, body.id);
  assert.equal((await call('POST', '/incidents', OTHER_TECH, body)).status, 409);
});

test('technician can edit own OPEN incident, others cannot', async () => {
  const inc = await create();
  const ok = await call('PATCH', `/incidents/${inc.id}`, TECH, { title: 'New title' });
  assert.equal(ok.status, 200);
  assert.equal((await ok.json()).title, 'New title');
  assert.equal((await call('PATCH', `/incidents/${inc.id}`, OTHER_TECH, { title: 'x' })).status, 403);
  assert.equal((await call('PATCH', `/incidents/${inc.id}`, TECH, { status: 'RESOLVED' })).status, 403);
  assert.equal((await call('PATCH', `/incidents/${inc.id}`, TECH, { bogus: 1 })).status, 422);
});

test('operator changes status, logs activity and sets resolvedAt', async () => {
  const inc = await create();
  const a = await (await call('PATCH', `/incidents/${inc.id}`, OP, { status: 'IN_PROGRESS', assignee: OP })).json();
  assert.equal(a.status, 'IN_PROGRESS');
  assert.equal(a.assignee, OP);
  assert.equal(a.activityLog.length, 1);
  const b = await (await call('PATCH', `/incidents/${inc.id}`, OP, { status: 'RESOLVED', resolutionNote: 'Fixed' })).json();
  assert.ok(b.resolvedAt);
  assert.equal(b.activityLog.length, 2);
  assert.equal(b.activityLog[1].actorId, OP);
  assert.equal((await call('PATCH', `/incidents/${inc.id}`, OP, { assignee: TECH })).status, 422);
  assert.equal((await call('PATCH', `/incidents/${inc.id}`, OP, { title: 'x' })).status, 403);
});

test('technician edit or delete after status change returns 409 and leaves data intact', async () => {
  const inc = await create();
  await call('PATCH', `/incidents/${inc.id}`, OP, { status: 'IN_PROGRESS' });
  const edit = await call('PATCH', `/incidents/${inc.id}`, TECH, { title: 'late edit' });
  assert.equal(edit.status, 409);
  assert.equal((await edit.json()).error.code, 'NOT_OPEN');
  assert.equal((await call('DELETE', `/incidents/${inc.id}`, TECH)).status, 409);
  const after = await (await call('GET', `/incidents/${inc.id}`, TECH)).json();
  assert.equal(after.title, inc.title);
});

test('delete is allowed while OPEN and idempotent', async () => {
  const inc = await create();
  assert.equal((await call('DELETE', `/incidents/${inc.id}`, OTHER_TECH)).status, 403);
  assert.equal((await call('DELETE', `/incidents/${inc.id}`, TECH)).status, 204);
  assert.equal((await call('DELETE', `/incidents/${inc.id}`, TECH)).status, 204);
  assert.equal((await call('GET', `/incidents/${inc.id}`, TECH)).status, 404);
});

test('photo upload is idempotent, capped at 5 and image-only', async () => {
  const inc = await create();
  const id = uuid();
  const first = await uploadPhoto(inc.id, TECH, id);
  assert.equal(first.status, 201);
  const photo = await first.json();
  assert.equal((await uploadPhoto(inc.id, TECH, id)).status, 200);
  assert.equal((await fetch(photo.url)).status, 200);
  for (let i = 0; i < 4; i++) assert.equal((await uploadPhoto(inc.id, TECH)).status, 201);
  const sixth = await uploadPhoto(inc.id, TECH);
  assert.equal(sixth.status, 422);
  assert.equal((await sixth.json()).error.code, 'PHOTO_LIMIT');
  const other = await create();
  assert.equal((await uploadPhoto(other.id, TECH, uuid(), 'text/plain')).status, 422);
  assert.equal((await uploadPhoto(other.id, OTHER_TECH)).status, 403);
});

test('photos can be removed while OPEN, not after', async () => {
  const inc = await create();
  const id = uuid();
  await uploadPhoto(inc.id, TECH, id);
  assert.equal((await call('DELETE', `/incidents/${inc.id}/photos/${id}`, TECH)).status, 204);
  assert.equal((await (await call('GET', `/incidents/${inc.id}`, TECH)).json()).photos.length, 0);
  const id2 = uuid();
  await uploadPhoto(inc.id, TECH, id2);
  await call('PATCH', `/incidents/${inc.id}`, OP, { status: 'IN_PROGRESS' });
  assert.equal((await call('DELETE', `/incidents/${inc.id}/photos/${id2}`, TECH)).status, 409);
  assert.equal((await uploadPhoto(inc.id, TECH)).status, 409);
});

test('list supports filtering and sorting for the dashboard', async () => {
  const res = await call('GET', '/incidents?status=OPEN&severity=HIGH&_sort=createdAt&_order=desc', OP);
  const rows = await res.json();
  assert.ok(rows.length >= 1);
  assert.ok(rows.every((r) => r.status === 'OPEN' && r.severity === 'HIGH'));
  const mine = await (await call('GET', `/incidents?reportedBy=${OTHER_TECH}`, OTHER_TECH)).json();
  assert.ok(mine.every((r) => r.reportedBy === OTHER_TECH));
});

test('unsupported writes are rejected', async () => {
  assert.equal((await call('POST', '/users', OP, { name: 'x' })).status, 405);
  assert.equal((await call('PUT', '/incidents/abc', OP, {})).status, 405);
});
