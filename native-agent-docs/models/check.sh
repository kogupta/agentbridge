#!/usr/bin/env bash
# Checks for the Quint models in this directory. See README.md.
# Usage: native-agent-docs/models/check.sh [fast|verify|all]   (default: all)
#   fast   = typecheck, simulator runs, ITF replay corpus
#   verify = TLC exhaustive checks: invariants, lemmas, refinement, witnesses, mutants
#
# Check targets come from the model files, so a new or renamed declaration needs no
# edit here:
#   val INV_* / LEM_* / REF_*   must hold             (pass)
#   val W_*                     must be reachable      (not(W) must be violated)
#   action step_mut_<X>         must violate val <X>   (mutant)
#   action stepCorpus           builds the ITF replay corpus
#
# Raw output: .agent-work/evidence/phase2/<name>.txt. One row per check in summary.tsv:
# name, expectation, exit code, verdict, distinct states. The exit code is 1 when any
# check is BAD.
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"

MODE=${1:-all}
SEED=${SEED:-20260915}
M=native-agent-docs/models
E=.agent-work/evidence/phase2
ITF=.agent-work/itf
mkdir -p "$E" "$ITF"
SUMMARY=$E/summary.tsv
[[ -f $SUMMARY ]] || printf 'name\texpect\texit\tverdict\tstates\n' >"$SUMMARY"
BAD=0

# check NAME EXPECT(pass|fail) CMD...
# `pass` needs exit 0 and an [ok] line. `fail` needs violation text: a crash, a parse
# error or a Quint flattening error also exits non-zero and is not a counterexample.
check() {
  local name=$1 expect=$2; shift 2
  echo ">> $name"
  "$@" >"$E/$name.txt" 2>&1
  local rc=$?
  local out="$E/$name.txt"
  local verdict=OK
  if [[ $expect == pass ]]; then
    { [[ $rc -eq 0 ]] && grep -q -E '^\[ok\]' "$out"; } || verdict=BAD
  else
    { [[ $rc -ne 0 ]] && grep -q -E 'is violated|\[violation\]|found a counterexample' "$out"; } || verdict=BAD
  fi
  [[ $verdict == BAD ]] && BAD=1
  local states
  states=$(grep -o -E '[0-9]+ distinct states found' "$out" | head -1 | cut -d' ' -f1)
  printf '%s\t%s\t%s\t%s\t%s\n' "$name" "$expect" "$rc" "$verdict" "${states:--}" >>"$SUMMARY"
  echo "   exit=$rc expect=$expect states=${states:--} -> $verdict"
}

RUN=(--max-samples=10000 --max-steps=30 --seed="$SEED" --backend=rust)
TLC=(--backend=tlc)

# names FILE REGEX: declaration names whose declaration line matches REGEX.
names() { grep -o -E "$2" "$1" | sed -E 's/^\s*(pure\s+)?(val|action)\s+//'; }
holds()     { names "$1" '^\s*val (INV|LEM|REF)_\w+'; }
witnesses() { names "$1" '^\s*val W_\w+'; }
mutants()   { names "$1" '^\s*action step_mut_\w+' | sed 's/^step_mut_//'; }

fast() {
  for f in "$M"/*.qnt; do
    local mod
    mod=$(basename "$f" .qnt)
    check "typecheck_$mod" pass sh -c 'quint typecheck "$1" && echo "[ok]"' _ "$f"
    for i in $(holds "$f"); do
      check "run_${mod}_$i" pass quint run "$f" --main="$mod" --invariant="$i" "${RUN[@]}"
    done
    if grep -q -E '^\s*action stepCorpus\b' "$f"; then
      check "corpus_$mod" pass quint run "$f" --main="$mod" --step=stepCorpus "${RUN[@]}" \
        --mbt --out-itf="$ITF/${mod}_{seq}.itf.json" --n-traces=50
    fi
  done
}

verify() {
  for f in "$M"/*.qnt; do
    local mod
    mod=$(basename "$f" .qnt)
    for i in $(holds "$f"); do
      check "verify_${mod}_$i" pass quint verify "$f" --main="$mod" --invariant="$i" "${TLC[@]}"
    done
    for w in $(witnesses "$f"); do
      check "witness_${mod}_$w" fail quint verify "$f" --main="$mod" --invariant="not($w)" "${TLC[@]}"
    done
    for x in $(mutants "$f"); do
      check "mutant_${mod}_$x" fail quint verify "$f" --main="$mod" --step="step_mut_$x" --invariant="$x" "${TLC[@]}"
    done
  done
}

case $MODE in
  fast) fast ;;
  verify) verify ;;
  all) fast; verify ;;
  *) echo "usage: $0 [fast|verify|all]" >&2; exit 2 ;;
esac

echo; echo "BAD rows:"; awk -F'\t' '$4=="BAD"' "$SUMMARY"
exit $BAD
