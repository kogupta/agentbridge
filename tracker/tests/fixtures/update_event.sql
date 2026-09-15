-- expect-abort: append-only
INSERT INTO actor(id, kind) VALUES ('tester', 'tool');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (1, '2026-09-15T00:00:00+00:00', 'tester', 'seed', 'selftest', 'deadbeef');
UPDATE event SET subject = 'mutated' WHERE id = 1;
