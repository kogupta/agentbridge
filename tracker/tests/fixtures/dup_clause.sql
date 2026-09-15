-- expect-abort: already exists
INSERT INTO actor(id, kind) VALUES ('tester', 'tool');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (1, '2026-09-15T00:00:00+00:00', 'tester', 'seed', 'selftest', 'deadbeef');
INSERT INTO clause(id, rev, kind, scope, text, text_sha256, status, event_id)
VALUES ('C1', 1, 'pre', 'local', 'one result per call', 'cc33', 'active', 1);
INSERT INTO clause(id, rev, kind, scope, text, text_sha256, status, event_id)
VALUES ('C2', 1, 'pre', 'local', 'exactly one result per call', 'cc33', 'active', 1);
