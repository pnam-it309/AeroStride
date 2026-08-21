#!/usr/bin/env node
/**
 * Updates mobile/app-version.json and the root README.md with the latest
 * EAS build artifact. Run by .github/workflows/mobile-cd.yml after each build.
 *
 * Usage: node scripts/update-release-info.js <path-to-eas-build-json>
 * (the JSON file is the output of `eas build ... --json`)
 */

const fs = require('fs');
const path = require('path');

const buildJsonPath = process.argv[2];
if (!buildJsonPath) {
  console.error('Usage: node scripts/update-release-info.js <path-to-eas-build-json>');
  process.exit(1);
}

const raw = JSON.parse(fs.readFileSync(buildJsonPath, 'utf8'));
const build = Array.isArray(raw) ? raw[0] : raw;

if (!build || !build.artifacts || !build.artifacts.buildUrl) {
  console.error('No build artifact URL found in EAS build output.');
  process.exit(1);
}

const apkUrl = build.artifacts.buildUrl;
const buildId = build.id;
const version = build.appVersion || '1.0.0';
const buildNumber = parseInt(build.appBuildVersion, 10) || 0;
const owner = (build.project && build.project.ownerAccount && build.project.ownerAccount.name) || 'pnamits-team';
const slug = (build.project && build.project.slug) || 'aerostride';
const buildDetailsUrl = `https://expo.dev/accounts/${owner}/projects/${slug}/builds/${buildId}`;

const mobileDir = path.resolve(__dirname, '..');
const repoRoot = path.resolve(mobileDir, '..');

// 1. Update mobile/app-version.json (consumed by the in-app update modal)
const manifestPath = path.join(mobileDir, 'app-version.json');
const manifest = {
  version,
  buildNumber,
  apkUrl,
  buildDetailsUrl,
  releasedAt: new Date().toISOString(),
};
fs.writeFileSync(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);
console.log(`Updated ${manifestPath}`);

// 2. Update README.md download links
const readmePath = path.join(repoRoot, 'README.md');
let readme = fs.readFileSync(readmePath, 'utf8');

readme = readme
  // Direct APK artifact links
  .replace(/https:\/\/expo\.dev\/artifacts\/eas\/[A-Za-z0-9_-]+\.apk/g, apkUrl)
  // Build details page links
  .replace(/https:\/\/expo\.dev\/accounts\/[^/\s]+\/projects\/[^/\s]+\/builds\/[a-f0-9-]+/g, buildDetailsUrl)
  // Version in the shields.io badge label
  .replace(/APK%20v[0-9.]+/g, `APK%20v${version}`)
  // Version in the download link text, e.g. "AeroStride-v1.0.0.apk (101 MB)"
  .replace(/AeroStride-v[0-9.]+\.apk( \([^)]*\))?/g, `AeroStride-v${version}.apk`)
  // Short build id in link text, e.g. "Expo Build #7fdd67df"
  .replace(/Expo Build #[a-f0-9]+/g, `Expo Build #${buildId.split('-')[0]}`);

fs.writeFileSync(readmePath, readme);
console.log(`Updated ${readmePath}`);
console.log(`APK: ${apkUrl}`);
console.log(`Version: ${version} (build ${buildNumber})`);
