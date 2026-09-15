-- expect-abort: CHECK
INSERT INTO actor(id, kind) VALUES ('tester', 'tool');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (1, '2026-09-15T00:00:00+00:00', 'tester', 'seed', 'selftest', 'deadbeef');
INSERT INTO clause(id, rev, kind, scope, text, text_sha256, status, reason, event_id)
VALUES ('C1', 1, 'pre', 'local', 'retired text', 'bb22', 'retired', NULL, 1);
