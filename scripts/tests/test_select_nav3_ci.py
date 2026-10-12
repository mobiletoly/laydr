"""Protect compiler selection and conservative CI path/event handling."""

import importlib.util
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest


REPOSITORY = Path(__file__).resolve().parents[2]
SCRIPT = REPOSITORY / "scripts/select-nav3-ci.py"
MODULE_SPEC = importlib.util.spec_from_file_location("select_nav3_ci", SCRIPT)
selector = importlib.util.module_from_spec(MODULE_SPEC)
MODULE_SPEC.loader.exec_module(selector)


class SelectNav3CiTest(unittest.TestCase):
    def test_runtime_paths_use_older_compiler_and_preserve_android_when_needed(self):
        versions = selector.compiler_versions(REPOSITORY)
        for path, android in [
            ("laydr-nav3-kmp/src/commonMain/Stack.kt", "false"),
            ("laydr-nav3-kmp-adaptive/src/commonMain/Scenes.kt", "false"),
            ("laydr-nav3-androidx/src/main/Stack.kt", "true"),
            ("laydr-nav-runtime/src/commonMain/Owner.kt", "true"),
            ("laydr-compose/src/commonMain/Host.kt", "true"),
            ("laydr-core/src/commonMain/Route.kt", "true"),
        ]:
            with self.subTest(path=path):
                self.assertEqual(selector.select_gates([path], versions, False), {
                    "consumer_matrix": json.dumps([{"kotlin": versions[0], "scope": "jvm-android"}]), "android_ui": android,
                    "plugin_checks": "false", "publishing_checks": "false",
                })

    def test_sensitive_push_paths_keep_automatic_consumer_scope(self):
        versions = selector.compiler_versions(REPOSITORY)
        for path, publishing in [
            ("gradle/libs.versions.toml", "true"),
            ("gradle/wrapper/gradle-wrapper.properties", "true"),
            ("build.gradle.kts", "true"), ("settings.gradle.kts", "true"),
            ("laydr-nav3-kmp/build.gradle.kts", "true"),
            ("gradle.properties", "true"), ("gradlew", "true"), ("gradlew.bat", "true"),
            ("build-logic/conventions/src/main/kotlin/Publishing.kt", "true"),
            ("laydr-codegen/src/main/kotlin/Generator.kt", "false"),
            ("laydr-gradle-plugin/src/main/kotlin/Plugin.kt", "false"),
            ("verification/nav3-consumer/kmp/build.gradle.kts", "true"),
            ("scripts/select-nav3-ci.py", "true"), (".github/workflows/publish.yml", "true"),
        ]:
            with self.subTest(path=path):
                plan = selector.select_gates([path], versions, False)
                self.assertEqual(json.loads(plan["consumer_matrix"]), [{"kotlin": versions[0], "scope": "jvm-android"}])
                self.assertEqual(plan["android_ui"], "true")
                self.assertEqual(plan["plugin_checks"], "true")
                self.assertEqual(plan["publishing_checks"], publishing)

    def test_prefix_boundaries_do_not_enable_unrelated_gates(self):
        plan = selector.select_gates(["docs/gradle/example.gradle.kts.md", "laydr-core-tools/readme.md"], ("2.4.10", "2.4.21"), False)
        self.assertEqual(plan["android_ui"], "false")
        self.assertEqual(plan["publishing_checks"], "false")
        self.assertEqual(json.loads(plan["consumer_matrix"]), [{"kotlin": "2.4.10", "scope": "jvm-android"}])

    def test_unknown_history_selects_all_and_compilers_follow_catalog_inputs(self):
        self.assertEqual(selector.select_gates(None, ("3.0.1", "3.0.2"), True), {
            "consumer_matrix": json.dumps([{"kotlin": "3.0.1", "scope": "full"}, {"kotlin": "3.0.2", "scope": "full"}]), "android_ui": "true",
            "plugin_checks": "true", "publishing_checks": "true",
        })

    def test_missing_push_history_enables_owner_checks_without_native_linking(self):
        plan = selector.select_gates(None, ("2.4.10", "2.4.21"), False)
        self.assertEqual(json.loads(plan["consumer_matrix"]), [{"kotlin": "2.4.10", "scope": "jvm-android"}])
        for key in ["android_ui", "plugin_checks", "publishing_checks"]:
            self.assertEqual(plan[key], "true")

    def test_event_diffs_include_deleted_and_renamed_sensitive_paths(self):
        with tempfile.TemporaryDirectory(prefix="laydr-ci-selection-") as directory:
            repository = Path(directory)

            def git(*arguments):
                return subprocess.check_output(["git", *arguments], cwd=repository, text=True).strip()

            git("init", "-q", "-b", "main")
            (repository / "gradle.properties").write_text("laydr.version=0.3.1\n")
            git("add", ".")
            git("-c", "user.name=CI Test", "-c", "user.email=ci@example.invalid", "commit", "-qm", "base")
            before = git("rev-parse", "HEAD")
            (repository / "docs").mkdir()
            git("mv", "gradle.properties", "docs/old-properties.txt")
            git("-c", "user.name=CI Test", "-c", "user.email=ci@example.invalid", "commit", "-qam", "move")
            after = git("rev-parse", "HEAD")
            for event_name, event in [
                ("push", {"before": before, "after": after}),
                ("pull_request", {"pull_request": {"base": {"sha": before}, "head": {"sha": after}}}),
            ]:
                with self.subTest(event=event_name):
                    paths = selector.changed_paths(event_name, event, repository)
                    self.assertIn("gradle.properties", paths)
                    self.assertIn("docs/old-properties.txt", paths)
                    self.assertEqual(selector.select_gates(paths, ("2.4.10", "2.4.21"), False)["publishing_checks"], "true")
            for event_name, event in [
                ("workflow_dispatch", {}), ("unknown", {}), ("push", {}),
                ("push", {"before": "0" * 40, "after": after}),
                ("push", {"before": "f" * 40, "after": after}),
            ]:
                with self.subTest(fallback=event):
                    self.assertIsNone(selector.changed_paths(event_name, event, repository))

    def test_cli_writes_github_outputs_and_handles_malformed_event(self):
        versions = selector.compiler_versions(REPOSITORY)
        with tempfile.TemporaryDirectory(prefix="laydr-ci-cli-") as directory:
            event_file = Path(directory) / "event.json"
            output_file = Path(directory) / "output"
            event_file.write_text("{")
            env = {**os.environ, "GITHUB_EVENT_NAME": "push", "GITHUB_EVENT_PATH": str(event_file), "GITHUB_OUTPUT": str(output_file)}
            result = subprocess.run(["python3", str(SCRIPT)], env=env, check=True, capture_output=True, text=True)
            self.assertIn("All owner checks selected", result.stdout)
            expected_matrix = json.dumps([{"kotlin": versions[0], "scope": "jvm-android"}])
            self.assertIn(f"consumer_matrix={expected_matrix}\n", output_file.read_text())
            self.assertIn("android_ui=true\n", output_file.read_text())
            for event_name in ["workflow_dispatch", "unknown"]:
                output_file.unlink()
                env["GITHUB_EVENT_NAME"] = event_name
                subprocess.run(["python3", str(SCRIPT)], env=env, check=True, capture_output=True, text=True)
                expected_matrix = json.dumps([{"kotlin": version, "scope": "full"} for version in versions])
                self.assertIn(f"consumer_matrix={expected_matrix}\n", output_file.read_text())


if __name__ == "__main__":
    unittest.main()
