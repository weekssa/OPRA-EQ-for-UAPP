#!/usr/bin/env python3
"""Build a fail-closed temporary profile from the frozen candidate's snapshot log event."""

from decimal import Decimal, InvalidOperation
from pathlib import Path
import re
import sys

SOURCE_SHA = "92c11fb0e41ae11b118b2e7bb105234d6606dbdb"
EVENT = "event=SNAPSHOT_READ_COMPLETE"
TYPE_MAP = {"peak_dip": "PK", "low_shelf": "LS", "high_shelf": "HS"}
FIELD_RE = re.compile(r"([A-Za-z][A-Za-z0-9]*)=([^\s]+)")


def dec(value: str, label: str) -> Decimal:
    try:
        result = Decimal(value)
    except InvalidOperation as exc:
        raise ValueError(f"{label} is not a number") from exc
    if not result.is_finite():
        raise ValueError(f"{label} must be finite")
    return result


def exact_step(value: Decimal, step: Decimal, label: str) -> None:
    if value % step != 0:
        raise ValueError(f"{label} is not representable at the JA11 native step")


def main() -> int:
    if len(sys.argv) != 3:
        print(f"Usage: {Path(sys.argv[0]).name} <private-logcat.txt> <output-prefix>", file=sys.stderr)
        return 2
    log_path = Path(sys.argv[1])
    prefix = Path(sys.argv[2])
    if not log_path.is_file():
        print("Logcat file not found; no profile generated.", file=sys.stderr)
        return 2

    candidates = []
    for line in log_path.read_text(errors="replace").splitlines():
        if "JA11_DIAG" not in line or EVENT not in line:
            continue
        fields = dict(FIELD_RE.findall(line))
        if fields.get("sourceSha") == SOURCE_SHA:
            candidates.append(fields)
    if not candidates:
        print("No complete snapshot event for the frozen candidate SHA; do not Flash.", file=sys.stderr)
        return 3

    fields = candidates[-1]
    required = {"sessionGeneration", "activeProgram", "globalEqGainDb"} | {f"band{i}" for i in range(5)}
    missing = sorted(required - fields.keys())
    if missing:
        print(f"Snapshot event is incomplete ({', '.join(missing)}); do not Flash.", file=sys.stderr)
        return 4
    if not fields["sessionGeneration"].isdigit():
        print("Invalid session generation; do not Flash.", file=sys.stderr)
        return 4
    if fields["activeProgram"] not in {"OFF", "USER_1"}:
        print("Snapshot program is not a supported JA11 state; do not Flash.", file=sys.stderr)
        return 4

    preamp = dec(fields["globalEqGainDb"], "global EQ gain")
    exact_step(preamp, Decimal("0.1"), "global EQ gain")
    if not Decimal("-12") <= preamp <= Decimal("12"):
        print("Global EQ gain is outside supported JA11 range; do not Flash.", file=sys.stderr)
        return 4

    bands = []
    for index in range(5):
        parts = fields[f"band{index}"].split(",")
        if len(parts) != 4:
            print(f"Band {index + 1} is malformed; do not Flash.", file=sys.stderr)
            return 4
        kind, frequency_raw, gain_raw, q_raw = parts
        if kind not in TYPE_MAP:
            print(f"Band {index + 1} filter type is unsupported; do not Flash.", file=sys.stderr)
            return 4
        frequency = dec(frequency_raw, f"band {index + 1} frequency")
        gain = dec(gain_raw, f"band {index + 1} gain")
        q = dec(q_raw, f"band {index + 1} Q")
        exact_step(frequency, Decimal("1"), f"band {index + 1} frequency")
        exact_step(gain, Decimal("0.1"), f"band {index + 1} gain")
        exact_step(q, Decimal("0.01"), f"band {index + 1} Q")
        if not Decimal("20") <= frequency <= Decimal("20000"):
            print(f"Band {index + 1} frequency is out of range; do not Flash.", file=sys.stderr)
            return 4
        if not Decimal("-24") <= gain <= Decimal("12"):
            print(f"Band {index + 1} gain is out of range; do not Flash.", file=sys.stderr)
            return 4
        if not Decimal("0.1") <= q <= Decimal("10"):
            print(f"Band {index + 1} Q is out of range; do not Flash.", file=sys.stderr)
            return 4
        bands.append((TYPE_MAP[kind], frequency, gain, q))

    profile_path = prefix.with_suffix(".txt")
    summary_path = prefix.with_suffix(".summary.txt")
    if profile_path.exists() or summary_path.exists():
        print("Output already exists; choose a new prefix so prior evidence is preserved.", file=sys.stderr)
        return 5
    profile_lines = [
        f"# JA11 temporary baseline test profile; source SHA {SOURCE_SHA}",
        f"# Captured from session generation {fields['sessionGeneration']} while program was {fields['activeProgram']}",
        f"Preamp: {preamp:.1f}dB",
    ]
    summary_lines = [
        f"sourceSha={SOURCE_SHA}",
        f"sessionGeneration={fields['sessionGeneration']}",
        f"activeProgramAtSnapshot={fields['activeProgram']}",
        f"globalEqGainDb={preamp:.1f}",
    ]
    for index, (kind, frequency, gain, q) in enumerate(bands, start=1):
        profile_lines.append(
            f"Filter {index}: ON {kind} Fc {frequency:.0f}Hz Gain {gain:.1f}dB Q {q:.2f}"
        )
        summary_lines.append(
            f"band{index}={kind},{frequency:.0f},{gain:.1f},{q:.2f}"
        )

    profile_path.parent.mkdir(parents=True, exist_ok=True, mode=0o700)
    profile_path.parent.chmod(0o700)
    profile_path.write_text("\n".join(profile_lines) + "\n")
    summary_path.write_text("\n".join(summary_lines) + "\n")
    profile_path.chmod(0o600)
    summary_path.chmod(0o600)
    print(f"profile={profile_path}")
    print(f"summary={summary_path}")
    print("A complete snapshot for the frozen source was converted. Confirm all six fields in Import Review and require JA11 result Exact before Flash.")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except ValueError as exc:
        print(f"{exc}; do not Flash.", file=sys.stderr)
        raise SystemExit(4)
