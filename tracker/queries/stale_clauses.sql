-- stale_clauses: current clauses whose recorded source observation is no longer
-- the current observation for that (file, pointer) — changed or missing.
SELECT DISTINCT cs.clause_id, cs.clause_rev, cs.file, cs.pointer
FROM clause_source cs
JOIN current_clause c ON c.id = cs.clause_id AND c.rev = cs.clause_rev
WHERE NOT EXISTS (
    SELECT 1 FROM current_source_field cur
    WHERE cur.file = cs.file AND cur.pointer = cs.pointer
      AND cur.source_event_id = cs.source_event_id
      AND cur.source_sha256 = cs.source_sha256);
