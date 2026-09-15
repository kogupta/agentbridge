-- unbound_model_actions (structural part): a current non-mutant model action (not
-- mut_* or step_mut_*) with no current binding. The verdict-evidence rule (NEW/DELETE absence evidence,
-- KEEP/ADAPT/PORT/REPLACE symbol evidence) is enforced by the binding command
-- layer before insertion.
SELECT m.module, m.name
FROM current_model_action m
WHERE m.kind = 'action'
  AND m.name NOT LIKE 'mut\_%' ESCAPE '\'
  AND m.name NOT LIKE 'step\_mut\_%' ESCAPE '\'
  AND NOT EXISTS (
      SELECT 1 FROM current_binding b
      WHERE b.module = m.module AND b.model_action = m.name);
