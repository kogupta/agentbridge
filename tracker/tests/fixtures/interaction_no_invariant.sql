-- expect-query: interaction_without_obligation 1 row
INSERT INTO actor(id, kind) VALUES ('tester', 'tool');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (1, '2026-09-15T00:00:00+00:00', 'tester', 'clause add', 'clauses', 'deadbeef');
INSERT INTO clause(id, rev, kind, scope, text, text_sha256, status, event_id)
VALUES ('C1', 1, 'post', 'interaction', 'delivery is disabled after close', 'dd44', 'active', 1);
