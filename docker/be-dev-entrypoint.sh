#!/bin/sh
# Dev entrypoint for the backend container: bootRun + a source watcher.
#
# bootRun on its own compiles once and never looks at src/ again, so editing a
# .java file changed nothing. Here bootRun runs with spring-boot-devtools on the
# classpath (see BE/build.gradle) and this loop recompiles into /app/build
# whenever src/ changes; devtools notices the new classes and restarts the
# application context in a few seconds instead of the container restarting.
#
# The watcher polls mtimes rather than using `gradle --continuous`, because
# continuous build depends on inotify events, and those do not cross a Docker
# bind mount from a Windows host. This is the same reason the frontend sets
# CHOKIDAR_USEPOLLING.
set -e

cd /app

STAMP=/tmp/.aerostride-build-stamp
INTERVAL="${BE_WATCH_INTERVAL:-2}"

touch "$STAMP"

./gradlew bootRun &
APP_PID=$!

# Stop the watcher (and the whole container) when the app exits or on SIGTERM.
trap 'kill "$APP_PID" 2>/dev/null; exit 0' TERM INT

while kill -0 "$APP_PID" 2>/dev/null; do
  if [ -n "$(find src -newer "$STAMP" -type f 2>/dev/null | head -n 1)" ]; then
    # Stamp before compiling so edits made mid-build are picked up next pass.
    touch "$STAMP"
    echo "[dev-watch] change in src/ -> ./gradlew classes"
    if ./gradlew classes --quiet; then
      echo "[dev-watch] compiled; devtools will reload"
    else
      echo "[dev-watch] compile failed - previous classes kept, app still running"
    fi
  fi
  sleep "$INTERVAL"
done

wait "$APP_PID"
