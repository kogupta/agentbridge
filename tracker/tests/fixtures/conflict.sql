-- expect-query: conflicting_outcomes 1 row
INSERT INTO actor(id, kind) VALUES ('tester', 'tool');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (1, '2026-09-15T00:00:00+00:00', 'tester', 'clause add', 'clauses', 'deadbeef');
INSERT INTO clause(id, rev, kind, scope, "trigger", outcome, text, text_sha256, status, event_id)
VALUES ('C1', 1, 'outcome', 'interaction', 'context budget exceeded', 'stop with CONTEXT_FULL', 'budget outcome A', '1001', 'active', 1);
INSERT INTO clause(id, rev, kind, scope, "trigger", outcome, text, text_sha256, status, event_id)
VALUES ('C2', 1, 'outcome', 'interaction', 'context budget exceeded', 'compact and continue', 'budget outcome B', '1002', 'active', 1);
