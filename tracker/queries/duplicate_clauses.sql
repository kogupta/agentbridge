-- duplicate_clauses: two current clauses with identical text (defense in depth;
-- the BEFORE INSERT trigger blocks the write; this query reports anything that
-- still slipped through).
SELECT a.id AS id_a, b.id AS id_b, a.text_sha256
FROM current_clause a
JOIN current_clause b ON a.id < b.id AND a.text_sha256 = b.text_sha256;
