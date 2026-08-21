/**
 * Update Service - checks the version manifest published by CI
 * (mobile/app-version.json on the main branch) against the installed build.
 */

import * as Application from 'expo-application';

const MANIFEST_URL =
  'https://raw.githubusercontent.com/pnam-it309/AeroStride/main/mobile/app-version.json';

export interface UpdateManifest {
  version: string;
  buildNumber: number;
  apkUrl: string;
  buildDetailsUrl?: string;
  releasedAt?: string;
}

export interface UpdateCheckResult {
  updateAvailable: boolean;
  installedVersion: string;
  installedBuildNumber: number;
  manifest: UpdateManifest;
}

function compareSemver(a: string, b: string): number {
  const pa = a.split('.').map((n) => parseInt(n, 10) || 0);
  const pb = b.split('.').map((n) => parseInt(n, 10) || 0);
  for (let i = 0; i < Math.max(pa.length, pb.length); i++) {
    const diff = (pa[i] ?? 0) - (pb[i] ?? 0);
    if (diff !== 0) return diff;
  }
  return 0;
}

export async function checkForUpdate(): Promise<UpdateCheckResult | null> {
  const response = await fetch(`${MANIFEST_URL}?t=${Date.now()}`, {
    headers: { 'Cache-Control': 'no-cache' },
  });
  if (!response.ok) return null;

  const manifest = (await response.json()) as UpdateManifest;
  if (!manifest?.version || !manifest?.apkUrl) return null;

  const installedVersion = Application.nativeApplicationVersion ?? '0.0.0';
  const installedBuildNumber = parseInt(Application.nativeBuildVersion ?? '0', 10) || 0;

  // Same-version JS changes are delivered silently via EAS Update (OTA),
  // so only prompt for a new APK when the app version itself increases.
  const updateAvailable = compareSemver(manifest.version, installedVersion) > 0;

  return { updateAvailable, installedVersion, installedBuildNumber, manifest };
}
