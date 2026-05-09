#!/usr/bin/env bash
# Run a krossterm example directly with java (no Gradle launcher overhead).
# Usage: ./example.sh <ExampleName>
#
# Requires the project to have been compiled first:
#   ./gradlew :examples:compileKotlin

set -euo pipefail

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
GRADLE_CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/caches/modules-2/files-2.1"

# ---- available examples ----
declare -A EXAMPLES=(
    [InteractiveDemo]="Alternate screen with mouse capture; shows all incoming events live"
    [Notify]="Send desktop notifications via OSC 9 / OSC 99 / OSC 777"
    [Progress]="Animate OSC 9;4 taskbar/tab progress through all states"
    [KeyDisplay]="Raw mode; prints every key/event as it arrives (Esc to quit)"
    [EventStreamCoroutines]="pollEvent with a 1-second heartbeat tick (Esc to quit)"
    [Stylize]="Showcase of all color/attribute combinations"
    [Link]="Clickable OSC 8 hyperlink (requires a supporting terminal)"
    [IsTty]="Detect whether stdout is a real TTY and print the terminal type"
    [TuiShowcase]="krossterm-tui tour: frames, tables, row/column, progress bars, spinner"
)

if [[ $# -eq 0 ]]; then
    echo "Usage: ./example.sh <ExampleName>"
    echo ""
    echo "Available examples:"
    for name in $(echo "${!EXAMPLES[@]}" | tr ' ' '\n' | sort); do
        printf "  %-26s %s\n" "$name" "${EXAMPLES[$name]}"
    done
    exit 0
fi

EXAMPLE="$1"

if [[ -z "${EXAMPLES[$EXAMPLE]+x}" ]]; then
    echo "error: unknown example '$EXAMPLE'" >&2
    echo "       run ./example.sh with no arguments to see available examples" >&2
    exit 1
fi

# ---- build directories ----
EXAMPLES_BUILD="$DIR/examples/build"
KROSSTERM_BUILD="$DIR/krossterm/build"
KROSSTERM_TUI_BUILD="$DIR/krossterm-tui/build"
CP_CLASSES=(
    "$EXAMPLES_BUILD/classes/kotlin/main"
    "$EXAMPLES_BUILD/classes/java/main"
    "$EXAMPLES_BUILD/resources/main"
    "$KROSSTERM_BUILD/classes/kotlin/main"
    "$KROSSTERM_BUILD/classes/java/main"
    "$KROSSTERM_BUILD/resources/main"
    "$KROSSTERM_TUI_BUILD/classes/kotlin/main"
    "$KROSSTERM_TUI_BUILD/classes/java/main"
    "$KROSSTERM_TUI_BUILD/resources/main"
)

# ---- dependency jars (keep in sync with gradle/libs.versions.toml) ----
KOTLIN_VER="2.3.21"
COROUTINES_VER="1.10.2"
IO_VER="0.9.0"
JLINE_VER="4.1.0"

jar() { find "$GRADLE_CACHE/$1" -name "$2" -print -quit 2>/dev/null; }

CP_JARS=(
    "$(jar "org.jetbrains.kotlin/kotlin-stdlib/$KOTLIN_VER"           "kotlin-stdlib-${KOTLIN_VER}.jar")"
    "$(jar "org.jetbrains.kotlinx/kotlinx-coroutines-core-jvm/$COROUTINES_VER" "kotlinx-coroutines-core-jvm-${COROUTINES_VER}.jar")"
    "$(jar "org.jetbrains.kotlinx/kotlinx-io-core-jvm/$IO_VER"        "kotlinx-io-core-jvm-${IO_VER}.jar")"
    "$(jar "org.jetbrains.kotlinx/kotlinx-io-bytestring-jvm/$IO_VER"  "kotlinx-io-bytestring-jvm-${IO_VER}.jar")"
    "$(jar "org.jline/jline-terminal/$JLINE_VER"                       "jline-terminal-${JLINE_VER}.jar")"
    "$(jar "org.jline/jline-terminal-ffm/$JLINE_VER"                   "jline-terminal-ffm-${JLINE_VER}.jar")"
    "$(jar "org.jline/jline-native/$JLINE_VER"                         "jline-native-${JLINE_VER}.jar")"
    "$(jar "org.jetbrains/annotations/23.0.0"                          "annotations-23.0.0.jar")"
)

# ---- build classpath string ----
IFS=':' eval 'CP="${CP_CLASSES[*]}:${CP_JARS[*]}"'

# ---- sanity checks ----
if [[ ! -d "$EXAMPLES_BUILD/classes/kotlin/main" ]]; then
    echo "error: compiled examples not found under $EXAMPLES_BUILD" >&2
    echo "       run: ./gradlew :examples:compileKotlin" >&2
    exit 1
fi

for j in "${CP_JARS[@]}"; do
    if [[ -z "$j" || ! -f "$j" ]]; then
        echo "error: missing dependency jar: $j" >&2
        echo "       run: ./gradlew dependencies  (to populate the Gradle cache)" >&2
        exit 1
    fi
done

MAIN="io.github.krossterm.examples.${EXAMPLE}Kt"
exec java \
    --enable-native-access=ALL-UNNAMED \
    -Dfile.encoding=UTF-8 \
    -cp "$CP" \
    "$MAIN"
