-- expect-abort: verification
INSERT INTO actor(id, kind) VALUES ('disposer', 'agent');
INSERT INTO actor(id, kind) VALUES ('verifier', 'reviewer');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (1, '2026-09-15T00:00:00+00:00', 'disposer', 'review open', 'schema', 'deadbeef');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (2, '2026-09-15T00:00:01+00:00', 'disposer', 'finding address', 'schema', 'deadbeef');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (3, '2026-09-15T00:00:02+00:00', 'disposer', 'finding verify', 'schema', 'deadbeef');
INSERT INTO review_round(id, subject, basis_digest, reviewer, event_id)
VALUES (1, 'schema', 'deadbeef', 'verifier', 1);
INSERT INTO finding(id, rev, round_id, severity, category, target_kind, target_id, target_rev, status, reason, actor, event_id)
VALUES ('F1', 1, 1, 'Major', 'schema', 'clause', 'C1', 1, 'addressed', 'revised the clause', 'disposer', 2);
INSERT INTO finding(id, rev, round_id, severity, category, target_kind, target_id, target_rev, verifies_rev, status, actor, event_id)
VALUES ('F1', 2, 1, 'Major', 'schema', 'clause', 'C1', 1, 1, 'verified', 'disposer', 3);
