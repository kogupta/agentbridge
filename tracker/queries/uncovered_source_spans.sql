-- uncovered_source_spans: character ranges of current source fields covered by
-- neither clause_source nor span_exclusion. Boundary-anchored segment merge:
-- every uncovered segment starts at a span or field boundary with no cover.
WITH spans AS (
    SELECT file, pointer, start_offset, end_offset FROM clause_source
    UNION ALL
    SELECT file, pointer, start_offset, end_offset FROM span_exclusion
),
bounds AS (
    SELECT c.file, c.pointer, c.start_offset AS b FROM current_source_field c
    UNION SELECT c.file, c.pointer, c.end_offset FROM current_source_field c
    UNION SELECT s.file, s.pointer, s.start_offset FROM spans s
    UNION SELECT s.file, s.pointer, s.end_offset FROM spans s
),
segments AS (
    SELECT bo.file, bo.pointer, bo.b AS start_offset,
           (SELECT MIN(b2.b) FROM bounds b2
            WHERE b2.file = bo.file AND b2.pointer = bo.pointer AND b2.b > bo.b) AS end_offset
    FROM bounds bo
    JOIN current_source_field c ON c.file = bo.file AND c.pointer = bo.pointer
    WHERE bo.b >= c.start_offset AND bo.b < c.end_offset
)
SELECT s.file, s.pointer, s.start_offset, s.end_offset
FROM segments s
WHERE s.end_offset IS NOT NULL
  AND s.start_offset < s.end_offset
  AND NOT EXISTS (
      SELECT 1 FROM spans sp
      WHERE sp.file = s.file AND sp.pointer = s.pointer
        AND sp.start_offset <= s.start_offset AND sp.end_offset >= s.end_offset)
ORDER BY s.file, s.pointer, s.start_offset;
