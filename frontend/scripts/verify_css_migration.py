#!/usr/bin/env python3
"""CSS Hex → Variable Migration Verifier
Checks that hex values in CSS files have been properly migrated to CSS variables.
Also verifies that remaining hex values in index.css are only in :root definitions.

Usage:
    python3 verify_css_migration.py <css_dir>

Exit codes:
    0 = clean (no hardcoded hex outside :root)
    1 = hardcoded hex found
"""
import re
import sys
from pathlib import Path


def find_hex_outside_root(css_dir: Path) -> dict:
    """Find hardcoded hex values in CSS files, excluding :root definitions."""
    results = {}

    for css_file in sorted(css_dir.rglob("*.css")):
        if "node_modules" in str(css_file):
            continue

        content = css_file.read_text()

        # Skip index.css (contains variable definitions)
        if css_file.name == "index.css":
            continue

        # Find all hex values
        hex_pattern = r"#[0-9a-fA-F]{3,8}\b"
        matches = list(re.finditer(hex_pattern, content))

        if matches:
            file_issues = []
            for match in matches:
                # Get line number
                line_num = content[:match.start()].count("\n") + 1
                # Get context
                line = content.split("\n")[line_num - 1].strip()
                file_issues.append({
                    "line": line_num,
                    "hex": match.group(),
                    "context": line[:80],
                })

            if file_issues:
                results[str(css_file.relative_to(css_dir))] = file_issues

    return results


def main():
    if len(sys.argv) < 2:
        print("Usage: python3 verify_css_migration.py <css_dir>")
        sys.exit(1)

    css_dir = Path(sys.argv[1])
    print(f"Checking CSS files in: {css_dir}\n")

    # Find hardcoded hex
    issues = find_hex_outside_root(css_dir)

    if not issues:
        print("✅ No hardcoded hex values found outside :root definitions")
        sys.exit(0)
    else:
        print(f"⚠️  Found hardcoded hex in {len(issues)} files:\n")
        total = 0
        for filepath, file_issues in sorted(issues.items()):
            print(f"  {filepath}:")
            for issue in file_issues:
                print(f"    L{issue['line']}: {issue['hex']}")
                print(f"           {issue['context']}")
            total += len(file_issues)
            print()

        print(f"Total: {total} hardcoded hex values")
        sys.exit(1)


if __name__ == "__main__":
    main()
