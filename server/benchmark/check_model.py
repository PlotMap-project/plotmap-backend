import os
import json
import time
import requests
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent

YANDEX_API_KEY = os.environ.get("YANDEX_API_KEY", "")
YANDEX_FOLDER_ID = os.environ.get("YANDEX_FOLDER_ID", "")

MODELS = [
    {"name": "YandexGPT Lite", "uri": f"gpt://{YANDEX_FOLDER_ID}/yandexgpt-lite/latest"},
    {"name": "YandexGPT Pro",  "uri": f"gpt://{YANDEX_FOLDER_ID}/yandexgpt/latest"},
]

VALID_EVENT_ROLES = {
    "INCITING_INCIDENT", "RISING_ACTION", "CLIMAX",
    "FALLING_ACTION", "RESOLUTION", "PLOT_TWIST", "REGULAR"
}

VALID_CONNECTION_TYPES = {
    "CAUSAL", "TEMPORAL", "PARALLEL", "CONTRADICTION"
}

SYSTEM_PROMPT = """
Ты — литературный аналитик. Проанализируй текст и построй сюжетный граф.

Верни ТОЛЬКО валидный JSON без markdown, без пояснений, без комментариев.

Формат ответа:
{
  "events": [
    {
      "id": "event_1",
      "title": "string",
      "description": "string",
      "suggestedSystemRole": "INCITING_INCIDENT|RISING_ACTION|CLIMAX|FALLING_ACTION|RESOLUTION|PLOT_TWIST|REGULAR",
      "impactLevel": 1,
      "userNotes": "",
      "color": ""
    }
  ],
  "edges": [
    {
      "sourceEventId": "event_1",
      "targetEventId": "event_2",
      "type": "CAUSAL|TEMPORAL|PARALLEL|CONTRADICTION",
      "strength": 5
    }
  ],
  "characters": [
    {
      "id": "char_1",
      "name": "string",
      "description": "string"
    }
  ],
  "storyArcs": [
    {
      "id": "arc_1",
      "name": "string",
      "description": "string"
    }
  ]
}

Правила:
- suggestedSystemRole строго одно из: INCITING_INCIDENT, RISING_ACTION, CLIMAX, FALLING_ACTION, RESOLUTION, PLOT_TWIST, REGULAR
- type ребра строго одно из: CAUSAL, TEMPORAL, PARALLEL, CONTRADICTION
- impactLevel целое число от 1 до 10
- strength целое число от 1 до 10
- color пустая строка или hex вида #RRGGBB
- id событий: event_1, event_2, ...
- id персонажей: char_1, char_2, ...
- id арок: arc_1, arc_2, ...
- создавай от 5 до 12 событий
- каждое событие должно быть соединено хотя бы одним ребром
- sourceEventId и targetEventId должны совпадать с реальными id из events
""".strip()


def check_credentials():
    if not YANDEX_API_KEY:
        print("ОШИБКА: переменная YANDEX_API_KEY не задана")
        return False
    if not YANDEX_FOLDER_ID:
        print("ОШИБКА: переменная YANDEX_FOLDER_ID не задана")
        return False
    return True


def call_model(model_uri: str, text: str) -> tuple[dict | None, float, str]:
    headers = {
        "Authorization": f"Api-Key {YANDEX_API_KEY}",
        "x-folder-id": YANDEX_FOLDER_ID,
        "Content-Type": "application/json"
    }
    body = {
        "modelUri": model_uri,
        "completionOptions": {
            "stream": False,
            "temperature": 0.3,
            "maxTokens": "8000"
        },
        "messages": [
            {"role": "system", "text": SYSTEM_PROMPT},
            {"role": "user", "text": f"Проанализируй текст:\n\n{text}"}
        ]
    }

    start = time.time()
    try:
        resp = requests.post(
            "https://llm.api.cloud.yandex.net/foundationModels/v1/completion",
            headers=headers,
            json=body,
            timeout=120
        )
        elapsed = time.time() - start

        if resp.status_code != 200:
            return None, elapsed, f"HTTP {resp.status_code}: {resp.text[:300]}"

        clean = resp.json()["result"]["alternatives"][0]["message"]["text"].strip()
        for prefix in ["```json", "```"]:
            if clean.startswith(prefix):
                clean = clean[len(prefix):]
        if clean.endswith("```"):
            clean = clean[:-3]
        clean = clean.strip()

        return json.loads(clean), elapsed, ""

    except json.JSONDecodeError as e:
        return None, time.time() - start, f"JSON parse error: {e}"
    except requests.exceptions.Timeout:
        return None, time.time() - start, "Timeout (120s)"
    except Exception as e:
        return None, time.time() - start, f"Exception: {e}"


def check_structure(result: dict) -> dict[str, bool]:
    events = result.get("events", [])
    edges = result.get("edges", [])
    event_ids = {e.get("id") for e in events}

    return {
        "has_events": len(events) > 0,
        "has_edges": len(edges) > 0,
        "has_characters": len(result.get("characters", [])) > 0,
        "has_story_arcs": len(result.get("storyArcs", [])) > 0,
        "events_have_id": all("id" in e for e in events),
        "events_have_title": all("title" in e for e in events),
        "events_have_role": all("suggestedSystemRole" in e for e in events),
        "events_have_impact": all("impactLevel" in e for e in events),
        "edges_have_source": all("sourceEventId" in e for e in edges),
        "edges_have_target": all("targetEventId" in e for e in edges),
        "edges_have_type": all("type" in e for e in edges),
        "edges_have_strength": all("strength" in e for e in edges),
        "valid_roles": all(
            e.get("suggestedSystemRole") in VALID_EVENT_ROLES for e in events
        ),
        "valid_edge_types": all(
            e.get("type") in VALID_CONNECTION_TYPES for e in edges
        ),
        "valid_impact_range": all(
            isinstance(e.get("impactLevel"), int) and 1 <= e.get("impactLevel", 0) <= 10
            for e in events
        ),
        "valid_strength_range": all(
            isinstance(e.get("strength"), int) and 1 <= e.get("strength", 0) <= 10
            for e in edges
        ),
        "edge_refs_valid": all(
            e.get("sourceEventId") in event_ids and e.get("targetEventId") in event_ids
            for e in edges
        ),
        "no_self_loops": all(
            e.get("sourceEventId") != e.get("targetEventId") for e in edges
        ),
    }


def fuzzy_match(gt_list: list[str], result_list: list[str]) -> int:
    matched = 0
    for gt in gt_list:
        gt_words = set(gt.lower().split())
        for res in result_list:
            res_words = set(res.lower().split())
            overlap = len(gt_words & res_words) / max(len(gt_words), 1)
            if overlap >= 0.5:
                matched += 1
                break
    return matched


def check_completeness(
        result: dict,
        ground_truth: dict,
        mandatory_events: list[str]
) -> dict:
    gt_events = [e["title"] for e in ground_truth.get("events", [])]
    result_events = [e.get("title", "") for e in result.get("events", [])]
    gt_chars = [c["name"] for c in ground_truth.get("characters", [])]
    result_chars = [c.get("name", "") for c in result.get("characters", [])]

    ev_matched = fuzzy_match(gt_events, result_events)
    ch_matched = fuzzy_match(gt_chars, result_chars)
    man_found = fuzzy_match(mandatory_events, result_events) if mandatory_events else None

    return {
        "gt_events_count": len(gt_events),
        "result_events_count": len(result_events),
        "events_recall": ev_matched / max(len(gt_events), 1),
        "gt_chars_count": len(gt_chars),
        "result_chars_count": len(result_chars),
        "chars_recall": ch_matched / max(len(gt_chars), 1),
        "gt_edges_count": len(ground_truth.get("edges", [])),
        "result_edges_count": len(result.get("edges", [])),
        "mandatory_events_found": man_found,
        "mandatory_events_total": len(mandatory_events) if mandatory_events else None,
    }


def check_graph_quality(result: dict) -> dict:
    events = result.get("events", [])
    edges = result.get("edges", [])
    event_ids = {e.get("id") for e in events}

    connected = set()
    for e in edges:
        connected.add(e.get("sourceEventId"))
        connected.add(e.get("targetEventId"))
    orphans = event_ids - connected
    role_counts = {}
    for e in events:
        role = e.get("suggestedSystemRole", "UNKNOWN")
        role_counts[role] = role_counts.get(role, 0) + 1

    type_counts = {}
    for e in edges:
        t = e.get("type", "UNKNOWN")
        type_counts[t] = type_counts.get(t, 0) + 1

    return {
        "total_events": len(events),
        "total_edges": len(edges),
        "orphan_count": len(orphans),
        "orphan_ids": sorted(orphans),
        "self_loops": sum(
            1 for e in edges
            if e.get("sourceEventId") == e.get("targetEventId")
        ),
        "role_counts": role_counts,
        "type_counts": type_counts,
    }


def compute_score(struct: dict, comp: dict, quality: dict) -> float:
    s = 0.0
    struct_score = sum(1 for v in struct.values() if v) / len(struct) * 40
    s += struct_score
    s += comp["events_recall"] * 25
    s += comp["chars_recall"]  * 15
    if comp["mandatory_events_total"]:
        s += (comp["mandatory_events_found"] / comp["mandatory_events_total"]) * 15
    else:
        s += 15
    if quality["orphan_count"] == 0:
        s += 3
    if quality["self_loops"] == 0:
        s += 2
    return round(s, 1)


def print_test_result(
        test_file_name: str,
        test_name: str,
        result: dict | None,
        elapsed: float,
        err: str,
        struct: dict | None,
        comp: dict | None,
        quality: dict | None,
        s: float
):
    print()
    print(f"[{test_file_name}] {test_name}")
    print(f"{'─' * 30}")

    if result is None:
        print(f"FAIL {err}")
        print(f"Время: {elapsed:.1f}s")
        return

    print(f"Оценка: {s}/100")
    print(f"Время: {elapsed:.1f}s")

    print(f"События: найдено {comp['result_events_count']}, "
          f"эталон {comp['gt_events_count']}, "
          f"recall {comp['events_recall']:.0%}")
    print(f"Персонажи: найдено {comp['result_chars_count']}, "
          f"эталон {comp['gt_chars_count']}, "
          f"recall {comp['chars_recall']:.0%}")
    print(f"Рёбра: найдено {comp['result_edges_count']}, "
          f"эталон {comp['gt_edges_count']}")

    if comp["mandatory_events_total"]:
        print(f"Mandatory: {comp['mandatory_events_found']}"
              f"/{comp['mandatory_events_total']}")

    print(f"Сироты: {quality['orphan_count']}"
          + (f" {quality['orphan_ids']}" if quality["orphan_count"] else ""))
    print(f"Self-loops: {quality['self_loops']}")

    roles_str = "  ".join(f"{k}:{v}" for k, v in sorted(quality["role_counts"].items()))
    types_str = "  ".join(f"{k}:{v}" for k, v in sorted(quality["type_counts"].items()))
    print(f"Роли: {roles_str}")
    print(f"Типы рёбер: {types_str}")

    failed = [k for k, v in struct.items() if not v]
    if failed:
        print(f"Структура FAIL: {failed}")
    else:
        print(f"Структура: OK")


def print_table(all_results: dict):
    print("=" * 60)
    print("ИТОГОВАЯ ТАБЛИЦА")
    print("=" * 60)
    header = f"{'Модель':<22} {'Avg Score':>10} {'Avg Time':>10} {'Tests OK':>10}"
    print(header)
    print(f"  {'─' * 55}")

    for model_name, data in all_results.items():
        ok = sum(1 for t in data["tests"] if t["score"] > 0)
        total = len(data["tests"])
        print(
            f"  {model_name:<22}"
            f"  {data['avg_score']:>7}/100"
            f"  {data['avg_time']:>7.1f}s"
            f"  {ok:>5}/{total}"
        )
    print("=" * 60)


def run():
    if not check_credentials():
        return

    test_dir = BASE_DIR / "test-data"
    if not test_dir.exists():
        print(f"Папка {test_dir} не найдена")
        return

    test_files = sorted(test_dir.glob("test*.json"))
    if not test_files:
        print(f"Нет файлов test*.json в {test_dir}")
        return

    print()
    all_results: dict[str, dict] = {}
    for model in MODELS:
        print(model["name"])
        print("-" * 30)
        model_key = model["name"]
        all_results[model_key] = {
            "tests": [],
            "avg_score": 0.0,
            "avg_time": 0.0
        }

        total_score = 0.0
        total_time = 0.0

        for file in test_files:
            with open(file, encoding="utf-8") as f:
                test = json.load(f)

            test_name = test.get("name", file.name)
            text = test.get("text", "")
            ground_truth = test.get("ground_truth", {})
            mandatory_events = test.get("mandatory_events", [])

            result, elapsed, err = call_model(model["uri"], text)
            total_time += elapsed

            if result is None:
                print_test_result(
                    file.name, test_name,
                    None, elapsed, err,
                    None, None, None, 0.0
                )
                all_results[model_key]["tests"].append({
                    "file": file.name, "score": 0.0,
                    "time": elapsed, "error": err
                })
                time.sleep(1)
                continue

            struct = check_structure(result)
            comp = check_completeness(result, ground_truth, mandatory_events)
            quality = check_graph_quality(result)
            s = compute_score(struct, comp, quality)
            total_score += s

            print_test_result(
                file.name, test_name,
                result, elapsed, "",
                struct, comp, quality, s
            )

            all_results[model_key]["tests"].append({
                "file": file.name,
                "score": s,
                "time": elapsed,
                "events_recall": comp["events_recall"],
                "chars_recall": comp["chars_recall"],
                "struct_fails": [k for k, v in struct.items() if not v],
                "orphans": quality["orphan_count"],
                "self_loops": quality["self_loops"],
            })

            time.sleep(1)

        n = max(len(test_files), 1)
        all_results[model_key]["avg_score"] = round(total_score / n, 1)
        all_results[model_key]["avg_time"]  = round(total_time  / n, 1)

    print()
    print_table(all_results)

    out_path = BASE_DIR / "benchmark_results.json"
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(all_results, f, indent=2, ensure_ascii=False)


if __name__ == "__main__":
    run()
