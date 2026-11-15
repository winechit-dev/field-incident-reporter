const fs = require('node:fs');
const path = require('node:path');
const express = require('express');
const jsonServer = require('json-server');
const multer = require('multer');

const CATEGORIES = ['EQUIPMENT', 'ROAD_HAZARD', 'SAFETY', 'OTHER'];
const SEVERITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const STATUSES = ['OPEN', 'IN_PROGRESS', 'RESOLVED'];
const MAX_PHOTOS = 5;
const MAX_PHOTO_BYTES = 10 * 1024 * 1024;
const ID_PATTERN = /^[A-Za-z0-9-]{8,64}$/;

const TECHNICIAN_FIELDS = ['id', 'title', 'description', 'category', 'severity', 'location'];
const OPERATOR_FIELDS = ['status', 'assignee', 'resolutionNote'];

class ApiError extends Error {
  constructor(status, code, message) {
    super(message);
    this.status = status;
    this.code = code;
  }
}

const isString = (v) => typeof v === 'string';

function validateLocation(loc) {
  if (loc === undefined || loc === null) return { latitude: null, longitude: null, address: null };
  if (typeof loc !== 'object' || Array.isArray(loc)) {
    throw new ApiError(422, 'VALIDATION_ERROR', 'location must be an object');
  }
  const { latitude = null, longitude = null, address = null } = loc;
  const num = (v, min, max, name) => {
    if (v === null) return null;
    if (typeof v !== 'number' || Number.isNaN(v) || v < min || v > max) {
      throw new ApiError(422, 'VALIDATION_ERROR', `${name} must be a number between ${min} and ${max}`);
    }
    return v;
  };
  if ((latitude === null) !== (longitude === null)) {
    throw new ApiError(422, 'VALIDATION_ERROR', 'latitude and longitude must be provided together');
  }
  if (address !== null && (!isString(address) || address.length > 300)) {
    throw new ApiError(422, 'VALIDATION_ERROR', 'address must be a string of at most 300 characters');
  }
  return {
    latitude: num(latitude, -90, 90, 'latitude'),
    longitude: num(longitude, -180, 180, 'longitude'),
    address,
  };
}

function validateField(name, value) {
  switch (name) {
    case 'title':
      if (!isString(value) || !value.trim() || value.length > 120) {
        throw new ApiError(422, 'VALIDATION_ERROR', 'title must be 1-120 characters');
      }
      return value.trim();
    case 'description':
      if (!isString(value) || value.length > 2000) {
        throw new ApiError(422, 'VALIDATION_ERROR', 'description must be at most 2000 characters');
      }
      return value;
    case 'category':
      if (!CATEGORIES.includes(value)) {
        throw new ApiError(422, 'VALIDATION_ERROR', `category must be one of ${CATEGORIES.join(', ')}`);
      }
      return value;
    case 'severity':
      if (!SEVERITIES.includes(value)) {
        throw new ApiError(422, 'VALIDATION_ERROR', `severity must be one of ${SEVERITIES.join(', ')}`);
      }
      return value;
    case 'location':
      return validateLocation(value);
    case 'status':
      if (!STATUSES.includes(value)) {
        throw new ApiError(422, 'VALIDATION_ERROR', `status must be one of ${STATUSES.join(', ')}`);
      }
      return value;
    case 'resolutionNote':
      if (value !== null && (!isString(value) || value.length > 2000)) {
        throw new ApiError(422, 'VALIDATION_ERROR', 'resolutionNote must be null or at most 2000 characters');
      }
      return value;
    default:
      return value;
  }
}

const wrap = (fn) => (req, res, next) => {
  try {
    fn(req, res, next);
  } catch (e) {
    next(e);
  }
};

function createApp({ dbFile, uploadsDir, now = () => new Date().toISOString() }) {
  fs.mkdirSync(uploadsDir, { recursive: true });
  const app = express();
  const router = jsonServer.router(dbFile);
  const db = router.db;
  const incidents = () => db.get('incidents');
  const users = () => db.get('users');

  // CORS so the Next.js dashboard (another origin) can call the API.
  app.use((req, res, next) => {
    res.set({
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Headers': 'Content-Type, X-User-Id',
      'Access-Control-Allow-Methods': 'GET, POST, PATCH, DELETE, OPTIONS',
    });
    if (req.method === 'OPTIONS') return res.status(204).end();
    next();
  });
  app.use(express.json({ limit: '1mb' }));
  app.use('/uploads', express.static(uploadsDir));

  // Resolve the Current User from the X-User-Id header.
  app.use((req, _res, next) => {
    // Photos and the user picker are public: the picker runs before anyone is chosen.
    if (req.path.startsWith('/uploads') || (req.method === 'GET' && req.path === '/users')) return next();
    const id = req.get('X-User-Id');
    if (!id) return next(new ApiError(401, 'UNAUTHENTICATED', 'X-User-Id header is required'));
    const user = users().find({ id }).value();
    if (!user) return next(new ApiError(401, 'UNKNOWN_USER', `Unknown user ${id}`));
    req.user = user;
    next();
  });

  const toIncident = (req, row) => ({
    ...row,
    photos: row.photos.map((p) => ({ ...p, url: `${req.protocol}://${req.get('host')}/uploads/${p.file}` })),
  });
  const find = (id) => {
    const row = incidents().find({ id }).value();
    if (!row) throw new ApiError(404, 'NOT_FOUND', `Incident ${id} not found`);
    return row;
  };
  const save = (id, patch) => incidents().find({ id }).assign(patch).write();
  const requireTechnicianOwner = (req, row) => {
    if (req.user.role !== 'TECHNICIAN') {
      throw new ApiError(403, 'FORBIDDEN', 'Only a Technician can do this');
    }
    if (row.reportedBy !== req.user.id) {
      throw new ApiError(403, 'FORBIDDEN', 'Only the reporting Technician can do this');
    }
  };
  const requireOpen = (row) => {
    if (row.status !== 'OPEN') {
      throw new ApiError(409, 'NOT_OPEN', 'Incident is already being handled');
    }
  };

  app.get('/users', (req, res) => res.json(users().value()));

  // The list is served by json-server (filtering, sorting, paging, full-text q).
  // Hide the stored photo file names by rewriting the response.
  const renderIncidents = (req, res) => {
    const data = res.locals.data;
    const map = (row) => (row && Array.isArray(row.photos) ? toIncident(req, row) : row);
    res.jsonp(Array.isArray(data) ? data.map(map) : map(data));
  };
  router.render = renderIncidents;

  // `assigned` cannot go to json-server: it cannot match null, and it would drop an unknown
  // key without a trace. Filter here, before json-server counts and pages, by handing the
  // matching rows to a request-scoped json-server router.
  const isAssigned = (row) => row.assignee !== null && row.assignee !== undefined;
  app.get('/incidents', wrap((req, res, next) => {
    const { assigned } = req.query;
    if (assigned === undefined) return next();
    if (assigned !== 'true' && assigned !== 'false') {
      throw new ApiError(422, 'VALIDATION_ERROR', 'assigned must be exactly true or false, given once');
    }
    delete req.query.assigned;
    const rows = incidents().value().filter((row) => isAssigned(row) === (assigned === 'true'));
    const scoped = jsonServer.router({ incidents: rows });
    scoped.render = renderIncidents;
    scoped(req, res, next);
  }));

  app.post('/incidents', wrap((req, res) => {
    if (req.user.role !== 'TECHNICIAN') {
      throw new ApiError(403, 'FORBIDDEN', 'Only a Technician can report an Incident');
    }
    const body = req.body || {};
    const unknown = Object.keys(body).filter((k) => !TECHNICIAN_FIELDS.includes(k));
    if (unknown.length) throw new ApiError(422, 'VALIDATION_ERROR', `Unknown fields: ${unknown.join(', ')}`);
    if (!isString(body.id) || !ID_PATTERN.test(body.id)) {
      throw new ApiError(422, 'VALIDATION_ERROR', 'id must be a client-generated UUID');
    }
    const existing = incidents().find({ id: body.id }).value();
    if (existing) {
      if (existing.reportedBy !== req.user.id) {
        throw new ApiError(409, 'ID_TAKEN', 'This id belongs to another Technician');
      }
      return res.status(200).json(toIncident(req, existing));
    }
    const ts = now();
    const row = {
      id: body.id,
      title: validateField('title', body.title),
      description: validateField('description', body.description ?? ''),
      category: validateField('category', body.category),
      severity: validateField('severity', body.severity),
      status: 'OPEN',
      location: validateField('location', body.location),
      photos: [],
      reportedBy: req.user.id,
      assignee: null,
      resolutionNote: null,
      resolvedAt: null,
      activityLog: [],
      createdAt: ts,
      updatedAt: ts,
    };
    incidents().push(row).write();
    res.status(201).json(toIncident(req, row));
  }));

  app.patch('/incidents/:id', wrap((req, res) => {
    const row = find(req.params.id);
    const body = req.body || {};
    const keys = Object.keys(body);
    if (!keys.length) throw new ApiError(422, 'VALIDATION_ERROR', 'No fields to update');
    const allowed = req.user.role === 'TECHNICIAN' ? TECHNICIAN_FIELDS.filter((f) => f !== 'id') : OPERATOR_FIELDS;
    const forbidden = keys.filter((k) => !allowed.includes(k));
    if (forbidden.length) {
      const known = [...TECHNICIAN_FIELDS, ...OPERATOR_FIELDS];
      const unknown = forbidden.filter((k) => !known.includes(k));
      if (unknown.length) throw new ApiError(422, 'VALIDATION_ERROR', `Unknown fields: ${unknown.join(', ')}`);
      throw new ApiError(403, 'FORBIDDEN', `${req.user.role} cannot change: ${forbidden.join(', ')}`);
    }

    const patch = {};
    if (req.user.role === 'TECHNICIAN') {
      requireTechnicianOwner(req, row);
      requireOpen(row);
      for (const k of keys) patch[k] = validateField(k, body[k]);
    } else {
      for (const k of keys) {
        if (k === 'assignee') {
          if (body.assignee !== null && users().find({ id: body.assignee, role: 'OPERATOR' }).value() === undefined) {
            throw new ApiError(422, 'VALIDATION_ERROR', 'assignee must be null or an Operator id');
          }
          patch.assignee = body.assignee;
        } else {
          patch[k] = validateField(k, body[k]);
        }
      }
      const ts = now();
      if (patch.status && patch.status !== row.status) {
        patch.activityLog = [...row.activityLog, { at: ts, actorId: req.user.id, status: patch.status }];
        patch.resolvedAt = patch.status === 'RESOLVED' ? ts : null;
      }
    }
    patch.updatedAt = now();
    save(row.id, patch);
    res.json(toIncident(req, find(row.id)));
  }));

  app.delete('/incidents/:id', wrap((req, res) => {
    const row = incidents().find({ id: req.params.id }).value();
    if (!row) return res.status(204).end(); // idempotent delete, safe for sync retries
    requireTechnicianOwner(req, row);
    requireOpen(row);
    for (const p of row.photos) fs.rmSync(path.join(uploadsDir, p.file), { force: true });
    incidents().remove({ id: row.id }).write();
    res.status(204).end();
  }));

  const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: MAX_PHOTO_BYTES, files: 1 } });
  const EXT = { 'image/jpeg': 'jpg', 'image/png': 'png' };

  app.post('/incidents/:id/photos', upload.single('file'), wrap((req, res) => {
    const row = find(req.params.id);
    requireTechnicianOwner(req, row);
    const photoId = req.body && req.body.id;
    if (!isString(photoId) || !ID_PATTERN.test(photoId)) {
      throw new ApiError(422, 'VALIDATION_ERROR', 'id must be a client-generated UUID');
    }
    const prior = row.photos.find((p) => p.id === photoId);
    if (prior) return res.status(200).json({ ...prior, url: `${req.protocol}://${req.get('host')}/uploads/${prior.file}` });
    requireOpen(row);
    if (!req.file) throw new ApiError(422, 'VALIDATION_ERROR', 'file is required');
    const ext = EXT[req.file.mimetype];
    if (!ext) throw new ApiError(422, 'VALIDATION_ERROR', 'file must be image/jpeg or image/png');
    if (row.photos.length >= MAX_PHOTOS) {
      throw new ApiError(422, 'PHOTO_LIMIT', `An Incident can have at most ${MAX_PHOTOS} photos`);
    }
    const file = `${photoId}.${ext}`;
    fs.writeFileSync(path.join(uploadsDir, file), req.file.buffer);
    const photo = { id: photoId, file, createdAt: now() };
    save(row.id, { photos: [...row.photos, photo], updatedAt: now() });
    res.status(201).json({ ...photo, url: `${req.protocol}://${req.get('host')}/uploads/${file}` });
  }));

  app.delete('/incidents/:id/photos/:photoId', wrap((req, res) => {
    const row = find(req.params.id);
    requireTechnicianOwner(req, row);
    const photo = row.photos.find((p) => p.id === req.params.photoId);
    if (!photo) return res.status(204).end();
    requireOpen(row);
    fs.rmSync(path.join(uploadsDir, photo.file), { force: true });
    save(row.id, { photos: row.photos.filter((p) => p.id !== photo.id), updatedAt: now() });
    res.status(204).end();
  }));

  // Everything else on the read side goes to json-server; block unhandled writes.
  app.use((req, res, next) => {
    if (req.method !== 'GET' || !/^\/(incidents)(\/|$)/.test(req.path)) {
      return next(new ApiError(405, 'METHOD_NOT_ALLOWED', `${req.method} ${req.path} is not supported`));
    }
    next();
  });
  app.use(router);

  // eslint-disable-next-line no-unused-vars
  app.use((err, _req, res, _next) => {
    if (err instanceof multer.MulterError) {
      const status = err.code === 'LIMIT_FILE_SIZE' ? 413 : 422;
      return res.status(status).json({ error: { code: err.code, message: err.message } });
    }
    if (err instanceof SyntaxError && 'body' in err) {
      return res.status(400).json({ error: { code: 'BAD_JSON', message: 'Malformed JSON body' } });
    }
    if (err instanceof ApiError) {
      return res.status(err.status).json({ error: { code: err.code, message: err.message } });
    }
    console.error(err);
    res.status(500).json({ error: { code: 'INTERNAL', message: 'Unexpected error' } });
  });

  return app;
}

module.exports = { createApp };
