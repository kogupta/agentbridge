-- tracker/migrations/001_init.sql — initial migration, mirrors schema.sql at ledger creation.
-- Later schema changes are new numbered migrations; never edit this file.
-- tracker/schema.sql — dev-only semantic-reads ledger (plan shiny-questing-crane, Phase 3).
-- Not product state. See tracker/README.md. Every connection must set PRAGMA foreign_keys=ON.
PRAGMA foreign_keys = ON;

-- ---------------------------------------------------------------- actors / events

CREATE TABLE IF NOT EXISTS actor (
    id          TEXT PRIMARY KEY,
    kind        TEXT NOT NULL CHECK (kind IN ('human', 'agent', 'reviewer', 'tool')),
    model       TEXT,
    session_ref TEXT
);

CREATE TABLE IF NOT EXISTS event (
    id           INTEGER PRIMARY KEY,
    at           TEXT NOT NULL,
    actor        TEXT NOT NULL REFERENCES actor(id),
    command      TEXT NOT NULL,
    subject      TEXT NOT NULL,
    basis_digest TEXT NOT NULL
);
CREATE TRIGGER IF NOT EXISTS event_no_update BEFORE UPDATE ON event
BEGIN SELECT RAISE(ABORT, 'event is append-only'); END;
CREATE TRIGGER IF NOT EXISTS event_no_delete BEFORE DELETE ON event
BEGIN SELECT RAISE(ABORT, 'event is append-only'); END;

-- ---------------------------------------------------------------- source observations

CREATE TABLE IF NOT EXISTS source_field (
    file            TEXT NOT NULL,
    pointer         TEXT NOT NULL,
    source_event_id INTEGER NOT NULL REFERENCES event(id),
    start_offset    INTEGER NOT NULL,
    end_offset      INTEGER NOT NULL,
    source_sha256   TEXT NOT NULL,
    PRIMARY KEY (file, pointer, source_event_id)
);
CREATE TRIGGER IF NOT EXISTS source_field_no_update BEFORE UPDATE ON source_field
BEGIN SELECT RAISE(ABORT, 'source_field is append-only'); END;
CREATE TRIGGER IF NOT EXISTS source_field_no_delete BEFORE DELETE ON source_field
BEGIN SELECT RAISE(ABORT, 'source_field is append-only'); END;

CREATE TABLE IF NOT EXISTS span_exclusion (
    file            TEXT NOT NULL,
    pointer         TEXT NOT NULL,
    start_offset    INTEGER NOT NULL,
    source_event_id INTEGER NOT NULL,
    end_offset      INTEGER NOT NULL,
    reason          TEXT NOT NULL,
    source_sha256   TEXT NOT NULL,
    event_id        INTEGER NOT NULL REFERENCES event(id),
    PRIMARY KEY (file, pointer, start_offset, source_event_id),
    FOREIGN KEY (file, pointer, source_event_id)
        REFERENCES source_field(file, pointer, source_event_id)
);
CREATE TRIGGER IF NOT EXISTS span_exclusion_no_update BEFORE UPDATE ON span_exclusion
BEGIN SELECT RAISE(ABORT, 'span_exclusion is append-only'); END;
CREATE TRIGGER IF NOT EXISTS span_exclusion_no_delete BEFORE DELETE ON span_exclusion
BEGIN SELECT RAISE(ABORT, 'span_exclusion is append-only'); END;

-- ---------------------------------------------------------------- clauses

CREATE TABLE IF NOT EXISTS clause (
    id          TEXT NOT NULL,
    rev         INTEGER NOT NULL,
    kind        TEXT NOT NULL CHECK (kind IN ('pre', 'post', 'invariant', 'outcome', 'decision')),
    scope       TEXT NOT NULL CHECK (scope IN ('local', 'interaction')),
    "trigger"   TEXT,
    outcome     TEXT,
    text        TEXT NOT NULL,
    text_sha256 TEXT NOT NULL,
    status      TEXT NOT NULL CHECK (status IN ('active', 'retired')),
    reason      TEXT,
    event_id    INTEGER NOT NULL REFERENCES event(id),
    CHECK (status <> 'retired' OR (reason IS NOT NULL AND length(reason) > 0)),
    PRIMARY KEY (id, rev)
);
CREATE TRIGGER IF NOT EXISTS clause_no_update BEFORE UPDATE ON clause
BEGIN SELECT RAISE(ABORT, 'clause revisions are append-only'); END;
CREATE TRIGGER IF NOT EXISTS clause_no_delete BEFORE DELETE ON clause
BEGIN SELECT RAISE(ABORT, 'clause revisions are append-only'); END;
CREATE TRIGGER IF NOT EXISTS clause_rev_gap BEFORE INSERT ON clause
WHEN NEW.rev <> 1 + COALESCE((SELECT MAX(rev) FROM clause WHERE id = NEW.id), 0)
BEGIN SELECT RAISE(ABORT, 'clause revision must be exactly one past the current maximum'); END;
CREATE TRIGGER IF NOT EXISTS clause_unique_current_text BEFORE INSERT ON clause
WHEN EXISTS (SELECT 1 FROM current_clause c
             WHERE c.text_sha256 = NEW.text_sha256 AND c.id <> NEW.id)
BEGIN SELECT RAISE(ABORT, 'a current clause with this text already exists'); END;

CREATE TABLE IF NOT EXISTS clause_source (
    clause_id       TEXT NOT NULL,
    clause_rev      INTEGER NOT NULL,
    file            TEXT NOT NULL,
    pointer         TEXT NOT NULL,
    start_offset    INTEGER NOT NULL,
    end_offset      INTEGER NOT NULL,
    source_event_id INTEGER NOT NULL,
    source_sha256   TEXT NOT NULL,
    PRIMARY KEY (clause_id, clause_rev, file, pointer, start_offset, source_event_id),
    FOREIGN KEY (clause_id, clause_rev) REFERENCES clause(id, rev),
    FOREIGN KEY (file, pointer, source_event_id)
        REFERENCES source_field(file, pointer, source_event_id)
);
CREATE TRIGGER IF NOT EXISTS clause_source_no_update BEFORE UPDATE ON clause_source
BEGIN SELECT RAISE(ABORT, 'clause_source is append-only'); END;
CREATE TRIGGER IF NOT EXISTS clause_source_no_delete BEFORE DELETE ON clause_source
BEGIN SELECT RAISE(ABORT, 'clause_source is append-only'); END;

-- ---------------------------------------------------------------- model observations

CREATE TABLE IF NOT EXISTS model_action (
    module        TEXT NOT NULL,
    name          TEXT NOT NULL,
    event_id      INTEGER NOT NULL REFERENCES event(id),
    kind          TEXT NOT NULL CHECK (kind IN ('action', 'var', 'val')),
    parameters    TEXT NOT NULL,
    owner         TEXT NOT NULL,
    source_sha256 TEXT NOT NULL,
    PRIMARY KEY (module, name, event_id)
);
CREATE TRIGGER IF NOT EXISTS model_action_no_update BEFORE UPDATE ON model_action
BEGIN SELECT RAISE(ABORT, 'model_action is append-only'); END;
CREATE TRIGGER IF NOT EXISTS model_action_no_delete BEFORE DELETE ON model_action
BEGIN SELECT RAISE(ABORT, 'model_action is append-only'); END;

CREATE TABLE IF NOT EXISTS model_obligation (
    id             TEXT NOT NULL,
    rev            INTEGER NOT NULL,
    clause_id      TEXT NOT NULL,
    clause_rev     INTEGER NOT NULL,
    module         TEXT NOT NULL,
    name           TEXT NOT NULL,
    model_event_id INTEGER NOT NULL,
    kind           TEXT NOT NULL CHECK (kind IN ('invariant', 'witness', 'mutant', 'refinement')),
    status         TEXT NOT NULL CHECK (status IN ('active', 'retired')),
    reason         TEXT,
    event_id       INTEGER NOT NULL REFERENCES event(id),
    PRIMARY KEY (id, rev),
    CHECK (status <> 'retired' OR (reason IS NOT NULL AND length(reason) > 0)),
    FOREIGN KEY (clause_id, clause_rev) REFERENCES clause(id, rev),
    FOREIGN KEY (module, name, model_event_id) REFERENCES model_action(module, name, event_id)
);
CREATE TRIGGER IF NOT EXISTS model_obligation_no_update BEFORE UPDATE ON model_obligation
BEGIN SELECT RAISE(ABORT, 'model_obligation revisions are append-only'); END;
CREATE TRIGGER IF NOT EXISTS model_obligation_no_delete BEFORE DELETE ON model_obligation
BEGIN SELECT RAISE(ABORT, 'model_obligation revisions are append-only'); END;
CREATE TRIGGER IF NOT EXISTS model_obligation_rev_gap BEFORE INSERT ON model_obligation
WHEN NEW.rev <> 1 + COALESCE((SELECT MAX(rev) FROM model_obligation WHERE id = NEW.id), 0)
BEGIN SELECT RAISE(ABORT, 'model_obligation revision must be exactly one past the current maximum'); END;

CREATE TABLE IF NOT EXISTS binding (
    id              TEXT NOT NULL,
    rev             INTEGER NOT NULL,
    module          TEXT NOT NULL,
    model_action    TEXT NOT NULL,
    model_event_id  INTEGER NOT NULL,
    java_symbol     TEXT,
    pi_symbol       TEXT,
    verdict         TEXT NOT NULL CHECK (verdict IN ('KEEP', 'ADAPT', 'PORT', 'REPLACE', 'DELETE', 'NEW')),
    note            TEXT NOT NULL CHECK (length(note) <= 280),
    status          TEXT NOT NULL CHECK (status IN ('active', 'retired')),
    reason          TEXT,
    event_id        INTEGER NOT NULL REFERENCES event(id),
    PRIMARY KEY (id, rev),
    CHECK (status <> 'retired' OR (reason IS NOT NULL AND length(reason) > 0)),
    FOREIGN KEY (module, model_action, model_event_id) REFERENCES model_action(module, name, event_id)
);
CREATE TRIGGER IF NOT EXISTS binding_no_update BEFORE UPDATE ON binding
BEGIN SELECT RAISE(ABORT, 'binding revisions are append-only'); END;
CREATE TRIGGER IF NOT EXISTS binding_no_delete BEFORE DELETE ON binding
BEGIN SELECT RAISE(ABORT, 'binding revisions are append-only'); END;
CREATE TRIGGER IF NOT EXISTS binding_rev_gap BEFORE INSERT ON binding
WHEN NEW.rev <> 1 + COALESCE((SELECT MAX(rev) FROM binding WHERE id = NEW.id), 0)
BEGIN SELECT RAISE(ABORT, 'binding revision must be exactly one past the current maximum'); END;
-- A binding may target only the current (latest) observation of its (module, name).
CREATE TRIGGER IF NOT EXISTS binding_current_observation BEFORE INSERT ON binding
WHEN NEW.status = 'active' AND NOT EXISTS (
    SELECT 1 FROM current_model_action c
    WHERE c.module = NEW.module AND c.name = NEW.model_action AND c.event_id = NEW.model_event_id
)
BEGIN SELECT RAISE(ABORT, 'binding targets a non-current model observation'); END;

-- ---------------------------------------------------------------- evidence

CREATE TABLE IF NOT EXISTS evidence (
    id              INTEGER PRIMARY KEY,
    event_id        INTEGER NOT NULL REFERENCES event(id),
    subject         TEXT NOT NULL,
    kind            TEXT NOT NULL CHECK (kind IN ('quint_typecheck', 'quint_run', 'quint_verify',
                                                  'quint_witness', 'quint_mutant', 'quint_refinement',
                                                  'junit', 'jqwik', 'replay', 'pit', 'query', 'precondition')),
    tool_version    TEXT NOT NULL,
    params          TEXT,
    result          TEXT NOT NULL CHECK (result IN ('pass', 'fail', 'counterexample', 'violation', 'timeout')),
    artifact_sha256 TEXT,
    basis_digest    TEXT NOT NULL
);
CREATE TRIGGER IF NOT EXISTS evidence_no_update BEFORE UPDATE ON evidence
BEGIN SELECT RAISE(ABORT, 'evidence is append-only'); END;
CREATE TRIGGER IF NOT EXISTS evidence_no_delete BEFORE DELETE ON evidence
BEGIN SELECT RAISE(ABORT, 'evidence is append-only'); END;

-- ---------------------------------------------------------------- reviews

CREATE TABLE IF NOT EXISTS review_round (
    id            INTEGER PRIMARY KEY,
    subject       TEXT NOT NULL,
    basis_digest  TEXT NOT NULL,
    reviewer      TEXT NOT NULL REFERENCES actor(id),
    event_id      INTEGER NOT NULL REFERENCES event(id)
);
CREATE TRIGGER IF NOT EXISTS review_round_no_update BEFORE UPDATE ON review_round
BEGIN SELECT RAISE(ABORT, 'review_round is append-only'); END;
CREATE TRIGGER IF NOT EXISTS review_round_no_delete BEFORE DELETE ON review_round
BEGIN SELECT RAISE(ABORT, 'review_round is append-only'); END;
CREATE TRIGGER IF NOT EXISTS review_round_one_open BEFORE INSERT ON review_round
WHEN EXISTS (
    SELECT 1 FROM review_round r
    WHERE r.subject = NEW.subject
      AND NOT EXISTS (SELECT 1 FROM review_round_close c WHERE c.round_id = r.id)
)
BEGIN SELECT RAISE(ABORT, 'subject already has an open review round'); END;

CREATE TABLE IF NOT EXISTS review_round_close (
    round_id  INTEGER PRIMARY KEY REFERENCES review_round(id),
    event_id  INTEGER NOT NULL REFERENCES event(id)
);
CREATE TRIGGER IF NOT EXISTS review_round_close_no_update BEFORE UPDATE ON review_round_close
BEGIN SELECT RAISE(ABORT, 'review_round_close is append-only'); END;
CREATE TRIGGER IF NOT EXISTS review_round_close_no_delete BEFORE DELETE ON review_round_close
BEGIN SELECT RAISE(ABORT, 'review_round_close is append-only'); END;

CREATE TABLE IF NOT EXISTS finding (
    id           TEXT NOT NULL,
    rev          INTEGER NOT NULL,
    round_id     INTEGER NOT NULL REFERENCES review_round(id),
    severity     TEXT NOT NULL CHECK (severity IN ('Blocker', 'Major', 'Minor', 'Nit')),
    category     TEXT NOT NULL,
    target_kind  TEXT NOT NULL CHECK (target_kind IN ('clause', 'obligation', 'binding')),
    target_id    TEXT NOT NULL,
    target_rev   INTEGER NOT NULL,
    verifies_rev INTEGER,
    status       TEXT NOT NULL CHECK (status IN ('open', 'addressed', 'rejected', 'deferred', 'verified', 'reopened')),
    reason       TEXT,
    actor        TEXT NOT NULL REFERENCES actor(id),
    event_id     INTEGER NOT NULL REFERENCES event(id),
    CHECK (status NOT IN ('addressed', 'rejected', 'deferred') OR (reason IS NOT NULL AND length(reason) > 0)),
    PRIMARY KEY (id, rev)
);
CREATE TRIGGER IF NOT EXISTS finding_no_update BEFORE UPDATE ON finding
BEGIN SELECT RAISE(ABORT, 'finding revisions are append-only'); END;
CREATE TRIGGER IF NOT EXISTS finding_no_delete BEFORE DELETE ON finding
BEGIN SELECT RAISE(ABORT, 'finding revisions are append-only'); END;
CREATE TRIGGER IF NOT EXISTS finding_rev_gap BEFORE INSERT ON finding
WHEN NEW.rev <> 1 + COALESCE((SELECT MAX(rev) FROM finding WHERE id = NEW.id), 0)
BEGIN SELECT RAISE(ABORT, 'finding revision must be exactly one past the current maximum'); END;
-- Verification: the verified revision must be the immediately preceding revision of the
-- same finding, in a dispositioned state, and created by a different actor.
CREATE TRIGGER IF NOT EXISTS finding_verify_rules BEFORE INSERT ON finding
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

-- ---------------------------------------------------------------- claims

CREATE TABLE IF NOT EXISTS claim_event (
    id          INTEGER PRIMARY KEY,
    subject     TEXT NOT NULL,
    actor       TEXT NOT NULL REFERENCES actor(id),
    kind        TEXT NOT NULL CHECK (kind IN ('acquire', 'release')),
    lease_token TEXT NOT NULL,
    lease_until TEXT NOT NULL,
    event_id    INTEGER NOT NULL REFERENCES event(id)
);
CREATE TRIGGER IF NOT EXISTS claim_event_no_update BEFORE UPDATE ON claim_event
BEGIN SELECT RAISE(ABORT, 'claim_event is append-only'); END;
CREATE TRIGGER IF NOT EXISTS claim_event_no_delete BEFORE DELETE ON claim_event
BEGIN SELECT RAISE(ABORT, 'claim_event is append-only'); END;
CREATE TRIGGER IF NOT EXISTS claim_no_double_acquire BEFORE INSERT ON claim_event
WHEN NEW.kind = 'acquire' AND EXISTS (
    SELECT 1 FROM active_claims a WHERE a.subject = NEW.subject
)
BEGIN SELECT RAISE(ABORT, 'subject already has an active claim'); END;

-- ---------------------------------------------------------------- views

CREATE VIEW IF NOT EXISTS current_clause AS
SELECT c.* FROM clause c
WHERE c.rev = (SELECT MAX(rev) FROM clause c2 WHERE c2.id = c.id)
  AND c.status <> 'retired';

CREATE VIEW IF NOT EXISTS current_model_obligation AS
SELECT o.* FROM model_obligation o
WHERE o.rev = (SELECT MAX(rev) FROM model_obligation o2 WHERE o2.id = o.id)
  AND o.status <> 'retired';

CREATE VIEW IF NOT EXISTS current_binding AS
SELECT b.* FROM binding b
WHERE b.rev = (SELECT MAX(rev) FROM binding b2 WHERE b2.id = b.id)
  AND b.status <> 'retired';

CREATE VIEW IF NOT EXISTS current_finding AS
SELECT f.* FROM finding f
WHERE f.rev = (SELECT MAX(rev) FROM finding f2 WHERE f2.id = f.id)
  AND f.status <> 'retired';

CREATE VIEW IF NOT EXISTS current_model_action AS
SELECT m.* FROM model_action m
WHERE m.event_id = (SELECT MAX(event_id) FROM model_action m2
                    WHERE m2.module = m.module AND m2.name = m.name);

CREATE VIEW IF NOT EXISTS current_source_field AS
SELECT s.* FROM source_field s
WHERE s.source_event_id = (SELECT MAX(source_event_id) FROM source_field s2
                           WHERE s2.file = s.file AND s2.pointer = s.pointer);

CREATE VIEW IF NOT EXISTS active_claims AS
SELECT ce.* FROM claim_event ce
WHERE ce.kind = 'acquire'
  AND ce.id = (SELECT MAX(id) FROM claim_event ce2 WHERE ce2.subject = ce.subject)
  AND ce.lease_until > datetime('now');

CREATE VIEW IF NOT EXISTS open_rounds AS
SELECT r.* FROM review_round r
WHERE NOT EXISTS (SELECT 1 FROM review_round_close c WHERE c.round_id = r.id);
