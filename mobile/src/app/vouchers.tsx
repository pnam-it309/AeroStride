/**
 * Vouchers Screen - public voucher listing
 */

import React, { useEffect, useState, useCallback } from 'react';
import { StyleSheet, View, Text, Pressable, FlatList, RefreshControl } from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { voucherService } from '@/services/voucherService';
import { type Voucher } from '@/services/orderService';
import { formatCurrency, formatDate, formatVoucherValue } from '@/utils/format';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

export default function VouchersScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();

  const [vouchers, setVouchers] = useState<Voucher[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    try {
      const data = await voucherService.getPublicVouchers();
      setVouchers(data);
    } catch (err) {
      console.warn('Failed to load vouchers:', err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const onRefresh = () => {
    setRefreshing(true);
    load();
  };

  const renderVoucher = ({ item }: { item: Voucher }) => {
    const isPercent = item.loaiPhieu === 'PHAN_TRAM';
    return (
      <View style={[styles.card, { backgroundColor: theme.surface, borderColor: theme.border }]}>
        <View style={[styles.cardLeft, { backgroundColor: Brand.primary + '12' }]}>
          <Ionicons name="pricetag" size={24} color={Brand.primary} />
          <Text style={[styles.cardLeftLabel, { color: Brand.primary }]}>
            {isPercent ? 'GIẢM %' : 'GIẢM ₫'}
          </Text>
        </View>
        <View style={styles.cardBody}>
          <Text style={[styles.voucherValue, { color: theme.text }]} numberOfLines={1}>
            {formatVoucherValue(item)}
          </Text>
          {item.ten ? (
            <Text style={[styles.voucherName, { color: theme.textSecondary }]} numberOfLines={1}>
              {item.ten}
            </Text>
          ) : null}
          {item.donHangToiThieu ? (
            <Text style={[styles.voucherCond, { color: theme.textTertiary }]}>
              Đơn tối thiểu {formatCurrency(item.donHangToiThieu)}
            </Text>
          ) : null}
          <View style={styles.voucherFooter}>
            <View style={[styles.codeChip, { borderColor: Brand.primary }]}>
              <Text style={[styles.codeText, { color: Brand.primary }]}>{item.ma}</Text>
            </View>
            {item.ngayKetThuc ? (
              <Text style={[styles.expiry, { color: theme.textTertiary }]}>
                HSD {formatDate(item.ngayKetThuc)}
              </Text>
            ) : null}
          </View>
        </View>
      </View>
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={[styles.header, { paddingTop: insets.top + Spacing.two }]}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Ionicons name="arrow-back" size={24} color={theme.text} />
        </Pressable>
        <Text style={[styles.title, { color: theme.text }]}>Voucher của tôi</Text>
        <View style={{ width: 24 }} />
      </View>

      {loading ? (
        <LoadingSpinner fullScreen />
      ) : (
        <FlatList
          data={vouchers}
          keyExtractor={(item) => item.id}
          renderItem={renderVoucher}
          contentContainerStyle={{ padding: Spacing.three, paddingBottom: insets.bottom + 40 }}
          ItemSeparatorComponent={() => <View style={{ height: Spacing.two + 2 }} />}
          refreshControl={
            <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={Brand.primary} />
          }
          ListEmptyComponent={
            <View style={styles.empty}>
              <Ionicons name="pricetags-outline" size={48} color={theme.textTertiary} />
              <Text style={[styles.emptyText, { color: theme.textSecondary }]}>
                Chưa có voucher khả dụng
              </Text>
            </View>
          }
        />
      )}
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
  card: {
    flexDirection: 'row',
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    overflow: 'hidden',
  },
  cardLeft: {
    width: 84,
    justifyContent: 'center',
    alignItems: 'center',
    gap: 4,
  },
  cardLeftLabel: { fontSize: FontSizes.xs, fontWeight: FontWeights.bold },
  cardBody: { flex: 1, padding: Spacing.three, gap: 3 },
  voucherValue: { fontSize: FontSizes.base, fontWeight: FontWeights.bold },
  voucherName: { fontSize: FontSizes.sm },
  voucherCond: { fontSize: FontSizes.xs },
  voucherFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: Spacing.one + 2,
  },
  codeChip: {
    borderWidth: 1,
    borderStyle: 'dashed',
    borderRadius: BorderRadius.sm,
    paddingHorizontal: Spacing.two,
    paddingVertical: 2,
  },
  codeText: { fontSize: FontSizes.sm, fontWeight: FontWeights.bold, letterSpacing: 1 },
  expiry: { fontSize: FontSizes.xs },
  empty: { alignItems: 'center', gap: Spacing.two, paddingTop: Spacing.seven },
  emptyText: { fontSize: FontSizes.base },
});
