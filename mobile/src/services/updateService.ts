/**
 * Update Service - handles both EAS / OTA bundle updates (expo-updates)
 * and native APK version releases.
 */

import * as Application from 'expo-application';
import * as Updates from 'expo-updates';

const MANIFEST_URL =
  'https://raw.githubusercontent.com/pnam-it309/AeroStride/main/mobile/app-version.json';

export interface UpdateManifest {
  version: string;
  buildNumber: number;
  apkUrl: string;
  buildDetailsUrl?: string;
  releasedAt?: string;
  changelog?: string[];
}

export type UpdateType = 'ota' | 'apk';

export interface UpdateCheckResult {
  updateAvailable: boolean;
  type: UpdateType;
  installedVersion: string;
  installedBuildNumber: number;
  manifest?: UpdateManifest;
  otaManifest?: any;
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

export async function checkForAppUpdate(): Promise<UpdateCheckResult | null> {
  const installedVersion = Application.nativeApplicationVersion ?? '1.0.0';
  const installedBuildNumber = parseInt(Application.nativeBuildVersion ?? '1', 10) || 1;

  // 1. Check for OTA (Over The Air) JS bundle update via expo-updates
  if (!__DEV__ && Updates.isEnabled) {
    try {
      const otaCheck = await Updates.checkForUpdateAsync();
      if (otaCheck.isAvailable) {
        // Pre-download the update bundle
        const fetchResult = await Updates.fetchUpdateAsync();
        if (fetchResult.isNew) {
          return {
            updateAvailable: true,
            type: 'ota',
            installedVersion,
            installedBuildNumber,
            otaManifest: otaCheck.manifest,
          };
        }
      }
    } catch (e) {
      console.warn('OTA check error:', e);
    }
  }

  // 2. Check for native APK release via GitHub manifest
  try {
    const response = await fetch(`${MANIFEST_URL}?t=${Date.now()}`, {
      headers: { 'Cache-Control': 'no-cache' },
    });
    if (response.ok) {
      const manifest = (await response.json()) as UpdateManifest;
      if (manifest?.version && manifest?.apkUrl) {
        const hasNewerVersion = compareSemver(manifest.version, installedVersion) > 0;
        const hasNewerBuild = manifest.buildNumber > installedBuildNumber;

        if (hasNewerVersion || hasNewerBuild) {
          return {
            updateAvailable: true,
            type: 'apk',
            installedVersion,
            installedBuildNumber,
            manifest,
          };
        }
      }
    }
  } catch (e) {
    console.warn('APK check error:', e);
  }

  return {
    updateAvailable: false,
    type: 'ota',
    installedVersion,
    installedBuildNumber,
  };
}

export async function applyOtaUpdate(): Promise<void> {
  try {
    await Updates.reloadAsync();
  } catch (e) {
    console.warn('Failed to reload for OTA update:', e);
  }
}
