from __future__ import annotations

import statistics
from typing import Any, Dict, Iterable, List, Optional

from .utils import parse_float


def numeric_values(rows: Iterable[Dict[str, Any]], key: str) -> List[float]:
    values: List[float] = []
    for row in rows:
        value = parse_float(row.get(key))
        if value is not None:
            values.append(value)
    return values


def numeric_summary(values: Iterable[float]) -> Dict[str, Optional[float]]:
    vals = sorted(float(v) for v in values)
    if not vals:
        return {"count": 0, "min": None, "median": None, "avg": None, "p90": None, "max": None, "stddev": None}
    count = len(vals)
    p90_index = min(count - 1, int(round((count - 1) * 0.90)))
    return {
        "count": count,
        "min": round(vals[0], 4),
        "median": round(statistics.median(vals), 4),
        "avg": round(statistics.mean(vals), 4),
        "p90": round(vals[p90_index], 4),
        "max": round(vals[-1], 4),
        "stddev": round(statistics.stdev(vals), 4) if count >= 2 else 0.0,
    }


def median_metric(rows: Iterable[Dict[str, Any]], key: str) -> Optional[float]:
    summary = numeric_summary(numeric_values(rows, key))
    return summary["median"]
