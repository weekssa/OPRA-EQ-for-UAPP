#!/usr/bin/env python3
"""Compare EW300 stock-register frequency scales with same-IEM acoustic measurements.

This research tool has no Android/runtime dependency. It consumes REW text exports for the same
EW300 DSP earpieces measured through the stock USB-C cable and a passive 3.5 mm cable, removes the
measurement-level offset, and compares their delta with the captured stock five-peak response.
"""

import argparse
import json
import math
from pathlib import Path
from typing import Dict, List, Sequence, Tuple


STOCK_RAW_FREQUENCIES = (100.0, 200.0, 300.0, 8000.0, 7000.0)
STOCK_GAINS_DB = (-1.1, -0.9, -0.4, -4.8, 0.5)
STOCK_Q = (0.8, 0.8, 1.0, 1.5, 0.5)
SCALE_CANDIDATES = (0.5, 1.0, 2.0, 4.0)


def read_rew(path: Path) -> List[Tuple[float, float]]:
    rows = []
    for line in path.read_text(encoding="utf-8").splitlines():
        if not line or line.startswith("*"):
            continue
        fields = line.split()
        if len(fields) >= 2:
            rows.append((float(fields[0]), float(fields[1])))
    if not rows:
        raise ValueError("No REW frequency-response rows found in %s" % path)
    return rows


def peak_magnitude_db(frequency: float, center: float, gain_db: float, q: float) -> float:
    """RBJ peaking-EQ magnitude at 48 kHz, matching the stock five-band hypothesis."""
    sample_rate = 48000.0
    amplitude = 10.0 ** (gain_db / 40.0)
    omega_0 = 2.0 * math.pi * center / sample_rate
    alpha = math.sin(omega_0) / (2.0 * q)
    b0 = 1.0 + alpha * amplitude
    b1 = -2.0 * math.cos(omega_0)
    b2 = 1.0 - alpha * amplitude
    a0 = 1.0 + alpha / amplitude
    a1 = -2.0 * math.cos(omega_0)
    a2 = 1.0 - alpha / amplitude
    omega = 2.0 * math.pi * frequency / sample_rate
    z1 = complex(math.cos(-omega), math.sin(-omega))
    z2 = z1 * z1
    numerator = b0 + b1 * z1 + b2 * z2
    denominator = a0 + a1 * z1 + a2 * z2
    return 20.0 * math.log10(abs(numerator / denominator))


def median(values: Sequence[float]) -> float:
    ordered = sorted(values)
    middle = len(ordered) // 2
    if len(ordered) % 2:
        return ordered[middle]
    return (ordered[middle - 1] + ordered[middle]) / 2.0


def analyze(paths: Sequence[Path]) -> Dict[str, object]:
    curves = [read_rew(path) for path in paths]
    row_count = len(curves[0])
    if any(len(curve) != row_count for curve in curves):
        raise ValueError("All four REW exports must contain the same frequency grid.")
    frequencies = [row[0] for row in curves[0]]
    if any(any(abs(curve[i][0] - frequencies[i]) > 0.001 for i in range(row_count)) for curve in curves[1:]):
        raise ValueError("All four REW exports must use the same frequency grid.")

    dsp = [(curves[0][i][1] + curves[1][i][1]) / 2.0 for i in range(row_count)]
    analog = [(curves[2][i][1] + curves[3][i][1]) / 2.0 for i in range(row_count)]
    observed = [dsp[i] - analog[i] for i in range(row_count)]
    results = []
    for scale in SCALE_CANDIDATES:
        predicted = []
        selected_observed = []
        for frequency, measured_delta in zip(frequencies, observed):
            if 30.0 <= frequency <= 10000.0:
                predicted.append(sum(
                    peak_magnitude_db(frequency, raw * scale, gain, q)
                    for raw, gain, q in zip(STOCK_RAW_FREQUENCIES, STOCK_GAINS_DB, STOCK_Q)
                ))
                selected_observed.append(measured_delta)
        offset = median([value - estimate for value, estimate in zip(selected_observed, predicted)])
        residuals = [value - (estimate + offset) for value, estimate in zip(selected_observed, predicted)]
        rmse = math.sqrt(sum(value * value for value in residuals) / len(residuals))
        observed_mean = sum(selected_observed) / len(selected_observed)
        predicted_mean = sum(predicted) / len(predicted)
        numerator = sum(
            (value - observed_mean) * (estimate - predicted_mean)
            for value, estimate in zip(selected_observed, predicted)
        )
        denominator = math.sqrt(
            sum((value - observed_mean) ** 2 for value in selected_observed)
            * sum((estimate - predicted_mean) ** 2 for estimate in predicted)
        )
        results.append({
            "scale": scale,
            "level_offset_db": round(offset, 6),
            "rmse_db": round(rmse, 6),
            "correlation": round(numerator / denominator, 6),
        })
    best = min(results, key=lambda item: item["rmse_db"])
    return {
        "method": "same-earpiece DSP-minus-analog delta vs captured stock five-peak RBJ response",
        "fit_range_hz": [30, 10000],
        "results": results,
        "best_scale": best["scale"],
        "best_rmse_db": best["rmse_db"],
        "best_correlation": best["correlation"],
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("dsp_left", type=Path)
    parser.add_argument("dsp_right", type=Path)
    parser.add_argument("analog_left", type=Path)
    parser.add_argument("analog_right", type=Path)
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()
    result = analyze((args.dsp_left, args.dsp_right, args.analog_left, args.analog_right))
    if args.json:
        print(json.dumps(result, indent=2, sort_keys=True))
        return
    for row in result["results"]:
        print(
            "scale={scale:g} offset={level_offset_db:+.3f} dB rmse={rmse_db:.3f} dB correlation={correlation:.3f}".format(
                **row
            )
        )
    print("best_scale={best_scale:g}".format(**result))


if __name__ == "__main__":
    main()
