-- expect-query: stale_findings 1 row
INSERT INTO actor(id, kind) VALUES ('reviewer1', 'reviewer');
INSERT INTO actor(id, kind) VALUES ('fixer', 'agent');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (1, '2026-09-15T00:00:00+00:00', 'reviewer1', 'review open', 'clauses', 'deadbeef');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (2, '2026-09-15T00:00:01+00:00', 'reviewer1', 'finding add', 'clauses', 'deadbeef');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (3, '2026-09-15T00:00:02+00:00', 'fixer', 'clause revise', 'clauses', 'deadbeef');
INSERT INTO review_round(id, subject, basis_digest, reviewer, event_id)
VALUES (1, 'clauses', 'deadbeef', 'reviewer1', 1);
INSERT INTO clause(id, rev, kind, scope, text, text_sha256, status, event_id)
VALUES ('C1', 1, 'pre', 'local', 'original text', '2001', 'active', 2);
INSERT INTO finding(id, rev, round_id, severity, category, target_kind, target_id, target_rev, status, actor, event_id)
VALUES ('F1', 1, 1, 'Major', 'wording', 'clause', 'C1', 1, 'open', 'reviewer1', 2);
INSERT INTO clause(id, rev, kind, scope, text, text_sha256, status, event_id)
VALUES ('C1', 2, 'pre', 'local', 'revised text', '2002', 'active', 3);
