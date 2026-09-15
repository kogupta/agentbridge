-- evidence_for: all evidence rows recorded for one subject.
-- Parameter: :subject (required).
SELECT e.id, e.kind, e.result, e.tool_version, e.params, e.artifact_sha256, e.basis_digest
FROM evidence e
WHERE e.subject = :subject
ORDER BY e.id;
