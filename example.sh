#!/usr/bin/env bash
# Run a krossterm example directly with java (no Gradle launcher overhead).
# Usage: ./example.sh <ExampleName>
#
# Requires the project to have been compiled first:
#   ./gradlew compileExamplesKotlin

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
BUILD="$DIR/build"
CP_CLASSES=(
    "$BUILD/classes/kotlin/examples"
    "$BUILD/classes/java/examples"
    "$BUILD/resources/examples"
    "$BUILD/classes/kotlin/main"
    "$BUILD/classes/java/main"
    "$BUILD/resources/main"
)

# ---- dependency jars ----
KOTLIN_VER="2.3.21"
COROUTINES_VER="1.10.2"
IO_VER="0.6.0"
JLINE_VER="3.27.1"

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
if [[ ! -d "$BUILD/classes/kotlin/examples" ]]; then
    echo "error: compiled examples not found under $BUILD" >&2
    echo "       run: ./gradlew compileExamplesKotlin" >&2
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
