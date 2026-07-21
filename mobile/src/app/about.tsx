/**
 * About AeroStride Screen
 */

import React from 'react';
import { StyleSheet, View, Text, ScrollView, Pressable } from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

const FEATURES = [
  { icon: 'cube-outline' as const, text: 'Bộ sưu tập giày chính hãng đa dạng' },
  { icon: 'flash-outline' as const, text: 'Đặt hàng nhanh, theo dõi đơn realtime' },
  { icon: 'shield-checkmark-outline' as const, text: 'Thanh toán an toàn COD & VNPay' },
  { icon: 'pricetags-outline' as const, text: 'Ưu đãi, voucher hấp dẫn mỗi ngày' },
];

export default function AboutScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ paddingBottom: insets.bottom + 40 }}
      >
        <LinearGradient
          colors={['#0B1120', '#152238', '#1E3A5F']}
          style={[styles.header, { paddingTop: insets.top + Spacing.two }]}
        >
          <View style={styles.headerBar}>
            <Pressable onPress={() => router.back()} hitSlop={12}>
              <Ionicons name="arrow-back" size={24} color="#FFFFFF" />
            </Pressable>
            <Text style={styles.headerTitle}>Về AeroStride</Text>
            <View style={{ width: 24 }} />
          </View>
          <Text style={styles.logo}>👟</Text>
          <Text style={styles.brand}>AeroStride</Text>
          <Text style={styles.version}>Phiên bản 1.0.0</Text>
        </LinearGradient>

        <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <Text style={[styles.desc, { color: theme.textSecondary }]}>
            AeroStride là cửa hàng giày trực tuyến mang đến trải nghiệm mua sắm hiện đại, nhanh
            chóng và an toàn. Khám phá những đôi giày phù hợp với phong cách của bạn.
          </Text>
        </View>

        <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <Text style={[styles.sectionTitle, { color: theme.text }]}>Tính năng nổi bật</Text>
          {FEATURES.map((f) => (
            <View key={f.text} style={styles.featureRow}>
              <View style={[styles.featureIcon, { backgroundColor: Brand.primary + '15' }]}>
                <Ionicons name={f.icon} size={18} color={Brand.primary} />
              </View>
              <Text style={[styles.featureText, { color: theme.text }]}>{f.text}</Text>
            </View>
          ))}
        </View>

        <Text style={[styles.copyright, { color: theme.textTertiary }]}>
          © 2026 AeroStride. All rights reserved.
        </Text>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { alignItems: 'center', paddingBottom: Spacing.four, gap: Spacing.half },
  headerBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    width: '100%',
    paddingHorizontal: Spacing.three,
    marginBottom: Spacing.three,
  },
  headerTitle: { color: '#FFFFFF', fontSize: FontSizes.lg, fontWeight: FontWeights.bold },
  logo: { fontSize: 48 },
  brand: { color: '#FFFFFF', fontSize: FontSizes.xl, fontWeight: FontWeights.extrabold },
  version: { color: '#94A3B8', fontSize: FontSizes.sm },
  section: {
    marginHorizontal: Spacing.three,
    marginTop: Spacing.three,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    padding: Spacing.three,
  },
  sectionTitle: { fontSize: FontSizes.base, fontWeight: FontWeights.bold, marginBottom: Spacing.three },
  desc: { fontSize: FontSizes.sm, lineHeight: 22 },
  featureRow: { flexDirection: 'row', alignItems: 'center', gap: Spacing.three, marginBottom: Spacing.three },
  featureIcon: {
    width: 36,
    height: 36,
    borderRadius: BorderRadius.md,
    justifyContent: 'center',
    alignItems: 'center',
  },
  featureText: { flex: 1, fontSize: FontSizes.sm },
  copyright: { textAlign: 'center', fontSize: FontSizes.xs, marginTop: Spacing.four },
});
