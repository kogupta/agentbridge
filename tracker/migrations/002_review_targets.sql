-- 002: review targets for Phase 2c and model scan semantics.
-- - finding.target_kind adds 'model' (target_id = module, target_rev = scan event id)
--   and 'artifact' (target_id = repository path, target_rev = 0; pinned by the
--   round basis digest). finding gains `summary`.
-- - review_round_close gains `outcome` (passed | failed).
-- - model_action gains `locator` (file:line).
-- - current_model_action selects the latest scan of each module, so a declaration
--   removed from a module is no longer current.
-- SQLite cannot change a CHECK constraint in place, so `finding` is rebuilt.
BEGIN IMMEDIATE;

ALTER TABLE model_action ADD COLUMN locator TEXT;

ALTER TABLE review_round_close ADD COLUMN outcome TEXT NOT NULL DEFAULT 'failed'
    CHECK (outcome IN ('passed', 'failed'));

-- Replace current_model_action before the finding rename: RENAME checks every trigger,
-- and binding_current_observation reads this view.
DROP VIEW IF EXISTS current_model_action;
CREATE VIEW current_model_action AS
SELECT m.* FROM model_action m
WHERE m.event_id = (SELECT MAX(event_id) FROM model_action m2 WHERE m2.module = m.module);

DROP VIEW IF EXISTS current_finding;

CREATE TABLE finding_new (
    id           TEXT NOT NULL,
    rev          INTEGER NOT NULL,
    round_id     INTEGER NOT NULL REFERENCES review_round(id),
    severity     TEXT NOT NULL CHECK (severity IN ('Blocker', 'Major', 'Minor', 'Nit')),
    category     TEXT NOT NULL,
    target_kind  TEXT NOT NULL CHECK (target_kind IN ('clause', 'obligation', 'binding', 'model', 'artifact')),
    target_id    TEXT NOT NULL,
    target_rev   INTEGER NOT NULL,
    verifies_rev INTEGER,
    status       TEXT NOT NULL CHECK (status IN ('open', 'addressed', 'rejected', 'deferred', 'verified', 'reopened')),
    reason       TEXT,
    actor        TEXT NOT NULL REFERENCES actor(id),
    event_id     INTEGER NOT NULL REFERENCES event(id),
    summary      TEXT NOT NULL DEFAULT '',
    CHECK (status NOT IN ('addressed', 'rejected', 'deferred') OR (reason IS NOT NULL AND length(reason) > 0)),
    PRIMARY KEY (id, rev)
);
INSERT INTO finding_new(id, rev, round_id, severity, category, target_kind, target_id, target_rev,
                        verifies_rev, status, reason, actor, event_id)
SELECT id, rev, round_id, severity, category, target_kind, target_id, target_rev,
       verifies_rev, status, reason, actor, event_id
FROM finding;
DROP TABLE finding;
ALTER TABLE finding_new RENAME TO finding;

CREATE TRIGGER finding_no_update BEFORE UPDATE ON finding
BEGIN SELECT RAISE(ABORT, 'finding revisions are append-only'); END;
CREATE TRIGGER finding_no_delete BEFORE DELETE ON finding
BEGIN SELECT RAISE(ABORT, 'finding revisions are append-only'); END;
CREATE TRIGGER finding_rev_gap BEFORE INSERT ON finding
WHEN NEW.rev <> 1 + COALESCE((SELECT MAX(rev) FROM finding WHERE id = NEW.id), 0)
BEGIN SELECT RAISE(ABORT, 'finding revision must be exactly one past the current maximum'); END;
CREATE TRIGGER finding_verify_rules BEFORE INSERT ON finding
WHEN NEW.status = 'verified' AND (
       NEW.verifies_rev IS NULL
       OR NEW.verifies_rev <> NEW.rev - 1
       OR NOT EXISTS (
           SELECT 1 FROM finding prev
           WHERE prev.id = NEW.id
             AND prev.rev = NEW.verifies_rev
             AND prev.status IN ('addressed', 'rejected', 'deferred')
             AND prev.actor <> NEW.actor
       )
    )
BEGIN SELECT RAISE(ABORT, 'verification must target the prior dispositioned revision by a second actor'); END;

CREATE VIEW current_finding AS
SELECT f.* FROM finding f
WHERE f.rev = (SELECT MAX(rev) FROM finding f2 WHERE f2.id = f.id)
  AND f.status <> 'retired';

COMMIT;
