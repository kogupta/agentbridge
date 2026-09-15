-- expect-abort: non-current
-- A to B to A rescan: three distinct events, current view selects final A,
-- and a binding targeting the stale B observation is rejected.
INSERT INTO actor(id, kind) VALUES ('tester', 'tool');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (1, '2026-09-15T00:00:00+00:00', 'tester', 'scan-models', 'models', 'deadbeef');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (2, '2026-09-15T00:00:01+00:00', 'tester', 'scan-models', 'models', 'deadbeef');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (3, '2026-09-15T00:00:02+00:00', 'tester', 'scan-models', 'models', 'deadbeef');
INSERT INTO model_action(module, name, event_id, kind, parameters, owner, source_sha256)
VALUES ('agent_coarse', 'send', 1, 'action', '()', 'User', 'digestA');
INSERT INTO model_action(module, name, event_id, kind, parameters, owner, source_sha256)
VALUES ('agent_coarse', 'send', 2, 'action', '()', 'User', 'digestB');
INSERT INTO model_action(module, name, event_id, kind, parameters, owner, source_sha256)
VALUES ('agent_coarse', 'send', 3, 'action', '()', 'User', 'digestA');
INSERT INTO binding(id, rev, module, model_action, model_event_id, java_symbol, pi_symbol, verdict, note, status, event_id)
VALUES ('B1', 1, 'agent_coarse', 'send', 2, NULL, NULL, 'NEW', 'targets stale observation B', 'active', 3);
