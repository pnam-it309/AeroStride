/**
 * UpdateModal – shows a dialog when a newer APK build is available,
 * with a button that opens the latest APK download link.
 */

import React, { useCallback, useEffect, useState } from 'react';
import { Linking, Modal, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import Ionicons from '@expo/vector-icons/Ionicons';
import { PrimaryButton } from '@/components/ui/PrimaryButton';
import { Brand, BorderRadius, FontSizes, FontWeights, Spacing } from '@/constants/theme';
import { checkForUpdate, type UpdateCheckResult } from '@/services/updateService';

const DISMISSED_KEY = 'aerostride_update_dismissed';

export function UpdateModal() {
  const [result, setResult] = useState<UpdateCheckResult | null>(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    if (__DEV__ || Platform.OS !== 'android') return;

    let cancelled = false;
    (async () => {
      try {
        const check = await checkForUpdate();
        if (cancelled || !check?.updateAvailable) return;

        const dismissed = await AsyncStorage.getItem(DISMISSED_KEY);
        const updateKey = `${check.manifest.version}-${check.manifest.buildNumber}`;
        if (dismissed === updateKey) return;

        setResult(check);
        setVisible(true);
      } catch {
        // Network errors are non-fatal – skip the update check silently.
      }
    })();

    return () => {
      cancelled = true;
    };
  }, []);

  const handleUpdate = useCallback(() => {
    if (result) Linking.openURL(result.manifest.apkUrl);
  }, [result]);

  const handleDismiss = useCallback(() => {
    setVisible(false);
    if (result) {
      const updateKey = `${result.manifest.version}-${result.manifest.buildNumber}`;
      AsyncStorage.setItem(DISMISSED_KEY, updateKey).catch(() => {});
    }
  }, [result]);

  if (!result) return null;

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={handleDismiss}>
      <View style={styles.backdrop}>
        <View style={styles.card}>
          <View style={styles.iconWrapper}>
            <Ionicons name="cloud-download-outline" size={32} color={Brand.primary} />
          </View>
          <Text style={styles.title}>Đã có phiên bản mới!</Text>
          <Text style={styles.message}>
            Phiên bản {result.manifest.version} (build {result.manifest.buildNumber}) đã sẵn sàng.
            Bạn đang dùng phiên bản {result.installedVersion} (build {result.installedBuildNumber}).
            Cập nhật ngay để trải nghiệm các tính năng mới nhất.
          </Text>
          <PrimaryButton
            label="Tải bản cập nhật"
            trailingIcon="download-outline"
            onPress={handleUpdate}
            style={styles.updateButton}
          />
          <Pressable onPress={handleDismiss} style={styles.laterButton}>
            <Text style={styles.laterText}>Để sau</Text>
          </Pressable>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.55)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: Spacing.four,
  },
  card: {
    width: '100%',
    maxWidth: 360,
    backgroundColor: '#FFFFFF',
    borderRadius: BorderRadius['2xl'],
    padding: Spacing.four,
    alignItems: 'center',
  },
  iconWrapper: {
    width: 64,
    height: 64,
    borderRadius: BorderRadius.full,
    backgroundColor: 'rgba(32, 138, 239, 0.1)',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.three,
  },
  title: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.bold,
    color: '#0F172A',
    marginBottom: Spacing.two,
  },
  message: {
    fontSize: FontSizes.sm,
    color: '#475569',
    textAlign: 'center',
    lineHeight: 20,
    marginBottom: Spacing.four,
  },
  updateButton: {
    alignSelf: 'stretch',
  },
  laterButton: {
    marginTop: Spacing.three,
    paddingVertical: Spacing.one,
    paddingHorizontal: Spacing.three,
  },
  laterText: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
    color: '#94A3B8',
  },
});
