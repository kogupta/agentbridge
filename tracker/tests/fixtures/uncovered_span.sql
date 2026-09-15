-- expect-query: uncovered_source_spans 1 row
INSERT INTO actor(id, kind) VALUES ('tester', 'tool');
INSERT INTO event(id, at, actor, command, subject, basis_digest)
VALUES (1, '2026-09-15T00:00:00+00:00', 'tester', 'scan-sources', 'sources', 'deadbeef');
INSERT INTO source_field(file, pointer, source_event_id, start_offset, end_offset, source_sha256)
VALUES ('native-agent-docs/product.md', 'I4', 1, 0, 100, 'ff00');
INSERT INTO clause(id, rev, kind, scope, text, text_sha256, status, event_id)
VALUES ('C1', 1, 'post', 'local', 'each accepted tool call gets exactly one result', 'ee55', 'active', 1);
INSERT INTO clause_source(clause_id, clause_rev, file, pointer, start_offset, end_offset, source_event_id, source_sha256)
VALUES ('C1', 1, 'native-agent-docs/product.md', 'I4', 0, 40, 1, 'ff00');
