-- expect-abort: revision
INSERT INTO actor(id, kind) VALUES ('tester', 'tool');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (1, '2026-09-15T00:00:00+00:00', 'tester', 'seed', 'selftest', 'deadbeef');
INSERT INTO clause(id, rev, kind, scope, text, text_sha256, status, event_id)
VALUES ('C1', 2, 'pre', 'local', 'gap revision', 'aa11', 'active', 1);
