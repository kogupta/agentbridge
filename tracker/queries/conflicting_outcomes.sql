-- conflicting_outcomes: two current outcome clauses with the same trigger and
-- different outcomes.
SELECT a.id AS id_a, b.id AS id_b, a."trigger" AS trigger, a.outcome AS outcome_a, b.outcome AS outcome_b
FROM current_clause a
JOIN current_clause b ON a.id < b.id
WHERE a.kind = 'outcome' AND b.kind = 'outcome'
  AND a."trigger" IS NOT NULL AND a."trigger" = b."trigger"
  AND a.outcome IS NOT NULL AND b.outcome IS NOT NULL
  AND a.outcome <> b.outcome;
