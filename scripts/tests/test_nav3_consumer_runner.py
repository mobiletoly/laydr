"""Check runner task selection; real Gradle runs provide compatibility proof."""

import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


REPOSITORY = Path(__file__).resolve().parents[2]


class Nav3ConsumerRunnerTest(unittest.TestCase):
    def invoke_runner(self, arguments, empty_publications=False):
        with tempfile.TemporaryDirectory(prefix="laydr-runner-test-") as directory:
            root = Path(directory)
            (root / "scripts").mkdir()
            shutil.copy2(REPOSITORY / "scripts/check-nav3-consumers.sh", root / "scripts/check-nav3-consumers.sh")
            fixture = root / "verification/nav3-consumer"
            fixture.mkdir(parents=True)
            (fixture / "android-only.gradle.kts").write_text("// Test orchestration fixture\n")
            gradlew = root / "gradlew"
            gradlew.write_text("""#!/usr/bin/env python3
import json
from pathlib import Path
import sys
root = Path(__file__).resolve().parent
output = root / 'build/nav3-validation'
output.mkdir(parents=True, exist_ok=True)
with (root / 'build/calls.jsonl').open('a') as log:
    log.write(json.dumps(sys.argv[1:]) + '\\n')
if 'writeNav3ConsumerVersions' in sys.argv:
    (output / 'consumer-versions.tsv').write_text('2.4.10\\t1.12.1\\t1.1.2\\t9.3.1\\t1.13.0\\n2.4.21\\t1.12.1\\t1.1.2\\t9.4.1\\t1.13.0\\n')
    (output / 'jvm-android-publication-tasks.txt').write_text(PUBLICATIONS)
""".replace("PUBLICATIONS", repr("" if empty_publications else ":laydr-core:publishJvmPublicationToNav3VerificationRepository\n")))
            gradlew.chmod(0o755)
            tools = root / "test-tools"
            tools.mkdir()
            uname = tools / "uname"
            uname.write_text("#!/bin/sh\nprintf '%s\\n' Darwin\n")
            uname.chmod(0o755)
            env = {**os.environ, "PATH": str(tools) + os.pathsep + os.environ["PATH"]}
            result = subprocess.run(["bash", str(root / "scripts/check-nav3-consumers.sh"), *arguments], env=env, capture_output=True, text=True)
            calls_file = root / "build/calls.jsonl"
            calls = [json.loads(line) for line in calls_file.read_text().splitlines()] if calls_file.exists() else []
            scopes = [path.read_text().strip() for path in root.glob("build/nav3-consumers.*/scope.txt")]
            return result, calls, scopes

    def test_automatic_scope_uses_selected_publications_and_omits_native_and_wasm(self):
        result, calls, scopes = self.invoke_runner(["--scope=jvm-android", "2.4.10"])
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(scopes, ["jvm-android"])
        self.assertEqual(len(calls), 4)
        self.assertIn(":laydr-core:publishJvmPublicationToNav3VerificationRepository", calls[1])
        self.assertNotIn("publishAllPublicationsToNav3VerificationRepository", calls[1])
        self.assertIn(":kmp:jvmTest", calls[2])
        self.assertIn(":androidApp:assembleDebug", calls[2])
        self.assertIn("-PconsumerQualificationScope=jvm-android", calls[2])
        self.assertIn("-PconsumerAndroidOnly=true", calls[3])
        self.assertFalse(any("linkDebugFramework" in argument or "compileDevelopmentExecutable" in argument for call in calls for argument in call))

    def test_default_scope_keeps_both_compilers_and_full_target_coverage(self):
        result, calls, scopes = self.invoke_runner([])
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual(scopes, ["full"])
        self.assertEqual(len(calls), 6)
        self.assertIn("publishAllPublicationsToNav3VerificationRepository", calls[1])
        for call, version in [(calls[2], "2.4.10"), (calls[4], "2.4.21")]:
            self.assertIn("-PconsumerKotlinVersion=" + version, call)
            for target in [":kmp:linkDebugFrameworkIosArm64", ":kmp:linkDebugFrameworkIosSimulatorArm64", ":kmp:compileDevelopmentExecutableKotlinWasmJs"]:
                self.assertIn(target, call)

    def test_invalid_scope_and_compiler_fail(self):
        for arguments, expected in [(["--scope=unknown"], "Unsupported consumer qualification scope"), (["--scope=jvm-android", "9.9.9"], "Unsupported qualification compiler")]:
            result, _, _ = self.invoke_runner(arguments)
            self.assertNotEqual(result.returncode, 0)
            self.assertIn(expected, result.stderr)

    def test_empty_limited_publication_selection_fails(self):
        result, _, _ = self.invoke_runner(["--scope=jvm-android"], empty_publications=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("No JVM/Android verification publication tasks", result.stderr)


if __name__ == "__main__":
    unittest.main()
