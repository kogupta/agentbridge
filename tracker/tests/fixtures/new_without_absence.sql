-- expect-query: unbound_model_actions 1 row
INSERT INTO actor(id, kind) VALUES ('tester', 'tool');
INSERT INTO event(id, at, actor, command, subject, basis_digest) VALUES (1, 't', 'tester', 'scan-models', 'agent_coarse', 'digest');
INSERT INTO model_action(module, name, event_id, kind, parameters, owner, source_sha256)
VALUES ('agent_coarse', 'send', 1, 'action', '[]', 'agent_coarse', 'digest');
