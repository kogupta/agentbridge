-- interaction_without_obligation: a current interaction clause with no current
-- invariant or refinement obligation.
SELECT c.id
FROM current_clause c
WHERE c.scope = 'interaction'
  AND NOT EXISTS (
      SELECT 1 FROM current_model_obligation o
      WHERE o.clause_id = c.id AND o.kind IN ('invariant', 'refinement'));
