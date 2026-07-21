# Expo (React Native) dev server. Development target only - shipping a mobile
# app means `eas build`, not a container image, so there is no production stage
# here the way FE.Dockerfile has one.
#
# KNOWN LIMITATION - no hot reload when the repo lives on a Windows drive.
# Metro's file watcher (metro-file-map) has only inotify/FSEvents/Watchman
# backends and NO polling option, unlike chokidar which is why the Vite frontend
# works via CHOKIDAR_USEPOLLING. inotify events do not cross a Docker bind mount
# from an NTFS host, so Metro never learns a file changed. Verified: an edit is
# visible inside the container immediately (`docker exec ... grep`), and appears
# in the bundle after `docker compose restart mobile`, but never while running.
# Fix by putting the repo on the WSL2 filesystem, or run this dev server on the
# host instead. The container is still correct once the repo moves.
FROM node:22-alpine AS build
WORKDIR /app

# Metro shells out to git for its file map in some setups; alpine has neither
# git nor the tooling Expo expects for the interactive CLI.
RUN apk add --no-cache git

# Copy manifests first to leverage Docker layer caching
COPY mobile/package*.json ./

RUN --mount=type=cache,target=/root/.npm \
    npm ci

# Copy source code
COPY mobile/ .

FROM build AS development

ARG MOBILE_PORT
EXPOSE ${MOBILE_PORT}

# Deliberately NOT setting CI=1. It looks like the right way to keep the CLI
# non-interactive, but Metro reads it too and disables watch mode entirely
# ("Metro is running in CI mode, reloads are disabled") - which is the whole
# point of running this in dev. The container has no TTY, so the CLI already
# skips its interactive terminal UI and never tries to open a browser.
ENV EXPO_NO_TELEMETRY=1

# No --web flag: since SDK 50 the platform-specific start commands were folded
# into `expo start`, which lazily serves whichever platform connects. Open
# http://localhost:8081 on the host and Metro serves the web bundle.
# Also no --localhost: that binds 127.0.0.1 inside the container, which the
# published port cannot reach. The default LAN host binds 0.0.0.0.
CMD ["sh", "-c", "npx expo start --port ${MOBILE_PORT}"]
