/**
 * Notifications Screen - empty state for now (no BE notification endpoint yet)
 */

import React from 'react';
import { StyleSheet, View, Text, Pressable } from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { FontSizes, FontWeights, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export default function NotificationsScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={[styles.header, { paddingTop: insets.top + Spacing.two }]}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Ionicons name="arrow-back" size={24} color={theme.text} />
        </Pressable>
        <Text style={[styles.title, { color: theme.text }]}>Thông báo</Text>
        <View style={{ width: 24 }} />
      </View>

      <View style={styles.empty}>
        <Ionicons name="notifications-off-outline" size={56} color={theme.textTertiary} />
        <Text style={[styles.emptyTitle, { color: theme.text }]}>Chưa có thông báo</Text>
        <Text style={[styles.emptyText, { color: theme.textSecondary }]}>
          Các cập nhật đơn hàng và ưu đãi sẽ xuất hiện ở đây.
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.three,
  },
  title: { fontSize: FontSizes.lg, fontWeight: FontWeights.bold },
  empty: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.two,
    paddingHorizontal: Spacing.five,
  },
  emptyTitle: { fontSize: FontSizes.md, fontWeight: FontWeights.bold, marginTop: Spacing.two },
  emptyText: { fontSize: FontSizes.sm, textAlign: 'center', lineHeight: 20 },
});
