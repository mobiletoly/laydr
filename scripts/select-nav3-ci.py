#!/usr/bin/env python3
"""Select Nav3 CI gates without weakening toolchain or release qualification."""

import json
import os
from pathlib import Path
import re
import subprocess
import tomllib


def is_toolchain_path(path: str) -> bool:
    return (
        path in ("gradle.properties", "gradlew", "gradlew.bat")
        or path.endswith(".gradle.kts")
        or path.startswith(("gradle/", "build-logic/", ".github/workflows/", "scripts/"))
    )


def select_gates(paths: list[str] | None, versions: tuple[str, str]) -> dict[str, str]:
    full = paths is None or any(
        is_toolchain_path(path)
        or path.startswith(("laydr-codegen/", "laydr-gradle-plugin/", "verification/nav3-consumer/"))
        for path in paths
    )
    android = full or any(
        path.startswith(("laydr-nav3-androidx/", "laydr-nav-runtime/", "laydr-core/", "laydr-compose/"))
        for path in paths or []
    )
    plugin = full
    publishing = paths is None or any(is_toolchain_path(path) for path in paths)
    selected_versions = list(dict.fromkeys(versions if full else versions[:1]))
    return {
        "consumer_versions": " ".join(selected_versions),
        "android_ui": str(android).lower(),
        "plugin_checks": str(plugin).lower(),
        "publishing_checks": str(publishing).lower(),
    }


def changed_paths(event_name: str, event: dict, repository: Path) -> list[str] | None:
    if event_name == "pull_request":
        pull_request = event.get("pull_request", {})
        before = pull_request.get("base", {}).get("sha")
        after = pull_request.get("head", {}).get("sha")
    elif event_name == "push":
        before, after = event.get("before"), event.get("after")
    else:
        return None
    if not all(
        isinstance(revision, str)
        and re.fullmatch(r"[0-9a-fA-F]{40,64}", revision)
        and set(revision) != {"0"}
        for revision in (before, after)
    ):
        return None
    try:
        result = subprocess.run(
            ["git", "diff", "--name-only", "--no-renames", "-z", before, after, "--"],
            cwd=repository, check=True, capture_output=True,
        )
    except (OSError, subprocess.CalledProcessError):
        return None
    return [path.decode("utf-8", errors="surrogateescape") for path in result.stdout.split(b"\0") if path]


def compiler_versions(repository: Path) -> tuple[str, str]:
    with (repository / "gradle/libs.versions.toml").open("rb") as catalog_file:
        catalog = tomllib.load(catalog_file)["versions"]
    versions = (catalog["compatibilityKotlin"], catalog["kotlin"])
    if not all(isinstance(version, str) and re.fullmatch(r"\d+\.\d+\.\d+", version) for version in versions):
        raise ValueError("CI compiler versions must be explicit catalog release versions")
    return versions


def main() -> None:
    repository = Path(__file__).resolve().parent.parent
    try:
        with Path(os.environ["GITHUB_EVENT_PATH"]).open() as event_file:
            event = json.load(event_file)
        paths = changed_paths(os.environ.get("GITHUB_EVENT_NAME", ""), event, repository)
    except (KeyError, OSError, ValueError, AttributeError, TypeError):
        paths = None
    plan = select_gates(paths, compiler_versions(repository))
    output = "".join(f"{key}={value}\n" for key, value in plan.items())
    print(output, end="")
    if paths is None:
        print("Full qualification selected: manual event or unavailable change history.")
    if output_path := os.environ.get("GITHUB_OUTPUT"):
        with Path(output_path).open("a") as output_file:
            output_file.write(output)


if __name__ == "__main__":
    main()
