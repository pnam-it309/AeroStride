/**
 * UpdateModal – shows a modal dialog when a new code update (OTA)
 * or a new APK build is available.
 */

import React, { useCallback, useEffect, useState } from 'react';
import {
  ActivityIndicator,
  Linking,
  Modal,
  Platform,
  Pressable,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { LinearGradient } from 'expo-linear-gradient';
import Ionicons from '@expo/vector-icons/Ionicons';
import { Brand, BorderRadius, FontSizes, FontWeights, Spacing } from '@/constants/theme';
import {
  checkForAppUpdate,
  applyOtaUpdate,
  type UpdateCheckResult,
} from '@/services/updateService';
import { useFeedback } from '@/context/FeedbackContext';

const DISMISSED_KEY = 'aerostride_update_dismissed';

let globalCheckForUpdate: ((manual?: boolean) => Promise<void>) | null = null;

export function triggerAppUpdateCheck(manual = true) {
  if (globalCheckForUpdate) {
    globalCheckForUpdate(manual);
  }
}

export function UpdateModal() {
  const [result, setResult] = useState<UpdateCheckResult | null>(null);
  const [visible, setVisible] = useState(false);
  const [applying, setApplying] = useState(false);
  const { showToast } = useFeedback();

  const performCheck = useCallback(async (manual = false) => {
    try {
      const check = await checkForAppUpdate();
      if (!check || !check.updateAvailable) {
        if (manual) {
          showToast({
            type: 'success',
            title: 'Phiên bản mới nhất',
            message: `Bạn đang sử dụng phiên bản tối ưu nhất (${check?.installedVersion || '1.0.0'}).`,
          });
        }
        return;
      }

      if (!manual) {
        const dismissed = await AsyncStorage.getItem(DISMISSED_KEY);
        const updateKey =
          check.type === 'apk' && check.manifest
            ? `apk-${check.manifest.version}-${check.manifest.buildNumber}`
            : `ota-${check.installedVersion}`;
        if (dismissed === updateKey) return;
      }

      setResult(check);
      setVisible(true);
    } catch {
      if (manual) {
        showToast({
          type: 'error',
          title: 'Kiểm tra thất bại',
          message: 'Không thể kết nối đến máy chủ cập nhật. Vui lòng thử lại sau.',
        });
      }
    }
  }, [showToast]);

  useEffect(() => {
    globalCheckForUpdate = performCheck;

    // Automatic check on app launch
    performCheck(false);

    return () => {
      globalCheckForUpdate = null;
    };
  }, [performCheck]);

  const handleUpdate = useCallback(async () => {
    if (!result) return;

    if (result.type === 'ota') {
      setApplying(true);
      await applyOtaUpdate();
      setApplying(false);
      setVisible(false);
    } else if (result.type === 'apk' && result.manifest?.apkUrl) {
      Linking.openURL(result.manifest.apkUrl);
      setVisible(false);
    }
  }, [result]);

  const handleDismiss = useCallback(() => {
    setVisible(false);
    if (result) {
      const updateKey =
        result.type === 'apk' && result.manifest
          ? `apk-${result.manifest.version}-${result.manifest.buildNumber}`
          : `ota-${result.installedVersion}`;
      AsyncStorage.setItem(DISMISSED_KEY, updateKey).catch(() => {});
    }
  }, [result]);

  if (!result || !visible) return null;

  const isOta = result.type === 'ota';
  const newVer = result.manifest?.version || result.installedVersion;

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={handleDismiss}>
      <View style={styles.backdrop}>
        <View style={styles.card}>
          {/* Header Gradient Icon */}
          <LinearGradient
            colors={[Brand.primaryLight, Brand.primaryDark]}
            start={{ x: 0, y: 0 }}
            end={{ x: 1, y: 1 }}
            style={styles.iconWrapper}
          >
            <Ionicons
              name={isOta ? 'sparkles' : 'cloud-download'}
              size={32}
              color="#FFFFFF"
            />
          </LinearGradient>

          {/* Badge */}
          <View style={styles.badgeContainer}>
            <Text style={styles.badgeText}>
              {isOta ? 'BẢN CẬP NHẬT MỚI' : `PHIÊN BẢN MỚI v${newVer}`}
            </Text>
          </View>

          {/* Title */}
          <Text style={styles.title}>
            {isOta ? 'Đã có bản cập nhật mới!' : 'Đã có bản nâng cấp ứng dụng!'}
          </Text>

          {/* Message */}
          <Text style={styles.message}>
            {isOta
              ? 'Hệ thống đã tải xong bản cập nhật tính năng và sửa lỗi mới nhất. Khởi động lại ứng dụng ngay để áp dụng.'
              : `Phiên bản ${newVer} đã sẵn sàng với nhiều cải tiến hiệu năng và tính năng mới. Vui lòng tải bản cập nhật để có trải nghiệm tốt nhất.`}
          </Text>

          {/* Primary Action Button */}
          <Pressable
            style={({ pressed }) => [styles.updateButton, { opacity: pressed || applying ? 0.85 : 1 }]}
            onPress={handleUpdate}
            disabled={applying}
          >
            <LinearGradient
              colors={[Brand.primary, Brand.primaryDark]}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={styles.gradientBtn}
            >
              {applying ? (
                <ActivityIndicator size="small" color="#FFFFFF" />
              ) : (
                <>
                  <Ionicons
                    name={isOta ? 'refresh-circle-outline' : 'download-outline'}
                    size={20}
                    color="#FFFFFF"
                  />
                  <Text style={styles.updateButtonText}>
                    {isOta ? 'Khởi động lại ngay' : 'Tải bản cập nhật'}
                  </Text>
                </>
              )}
            </LinearGradient>
          </Pressable>

          {/* Secondary Dismiss Button */}
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
    backgroundColor: 'rgba(11, 17, 32, 0.7)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: Spacing.four,
  },
  card: {
    width: '100%',
    maxWidth: 360,
    backgroundColor: '#FFFFFF',
    borderRadius: BorderRadius['2xl'],
    padding: Spacing.five,
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 10 },
    shadowOpacity: 0.25,
    shadowRadius: 20,
    elevation: 10,
  },
  iconWrapper: {
    width: 64,
    height: 64,
    borderRadius: 32,
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.three,
    shadowColor: Brand.primary,
    shadowOffset: { width: 0, height: 6 },
    shadowOpacity: 0.35,
    shadowRadius: 10,
    elevation: 6,
  },
  badgeContainer: {
    backgroundColor: '#EFF6FF',
    borderColor: Brand.primary + '30',
    borderWidth: 1,
    paddingHorizontal: Spacing.two + 2,
    paddingVertical: 3,
    borderRadius: BorderRadius.full,
    marginBottom: Spacing.two,
  },
  badgeText: {
    color: Brand.primary,
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.bold,
    letterSpacing: 0.5,
  },
  title: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.bold,
    color: '#0F172A',
    textAlign: 'center',
    marginBottom: Spacing.two,
  },
  message: {
    fontSize: FontSizes.sm,
    color: '#64748B',
    textAlign: 'center',
    lineHeight: 20,
    marginBottom: Spacing.four,
  },
  updateButton: {
    width: '100%',
    borderRadius: BorderRadius.lg,
    overflow: 'hidden',
    marginBottom: Spacing.two,
  },
  gradientBtn: {
    paddingVertical: Spacing.three,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.two,
  },
  updateButtonText: {
    color: '#FFFFFF',
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
  },
  laterButton: {
    paddingVertical: Spacing.two,
    paddingHorizontal: Spacing.four,
  },
  laterText: {
    color: '#94A3B8',
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
  },
});
