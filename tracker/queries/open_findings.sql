-- open_findings: current findings in status open or reopened, worst severity first.
SELECT f.id, f.severity, f.category, f.target_kind, f.target_id, f.target_rev, f.status, f.summary
FROM current_finding f
WHERE f.status IN ('open', 'reopened')
ORDER BY CASE f.severity WHEN 'Blocker' THEN 1 WHEN 'Major' THEN 2 WHEN 'Minor' THEN 3 ELSE 4 END, f.id;
