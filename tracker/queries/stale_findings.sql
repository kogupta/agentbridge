-- stale_findings: a finding whose target has a current revision past target_rev.
SELECT f.id, f.target_kind, f.target_id, f.target_rev, f.status
FROM current_finding f
WHERE f.status IN ('open', 'addressed', 'rejected', 'deferred', 'reopened')
  AND (
      (f.target_kind = 'clause' AND EXISTS (
          SELECT 1 FROM current_clause t WHERE t.id = f.target_id AND t.rev > f.target_rev))
   OR (f.target_kind = 'obligation' AND EXISTS (
          SELECT 1 FROM current_model_obligation t WHERE t.id = f.target_id AND t.rev > f.target_rev))
   OR (f.target_kind = 'binding' AND EXISTS (
          SELECT 1 FROM current_binding t WHERE t.id = f.target_id AND t.rev > f.target_rev)));
