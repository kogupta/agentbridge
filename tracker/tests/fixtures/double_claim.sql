-- expect-abort: active claim
INSERT INTO actor(id, kind) VALUES ('tester', 'tool');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (1, '2026-09-15T00:00:00+00:00', 'tester', 'claim acquire', 'phase-3', 'deadbeef');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (2, '2026-09-15T00:00:01+00:00', 'tester', 'claim acquire', 'phase-3', 'deadbeef');
INSERT INTO claim_event(id, subject, actor, kind, lease_token, lease_until, event_id)
VALUES (1, 'phase-3', 'tester', 'acquire', 'tok1', '2999-01-01T00:00:00+00:00', 1);
INSERT INTO claim_event(id, subject, actor, kind, lease_token, lease_until, event_id)
VALUES (2, 'phase-3', 'tester', 'acquire', 'tok2', '2999-01-01T00:00:00+00:00', 2);
