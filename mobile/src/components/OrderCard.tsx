/**
 * OrderCard - order item in list
 */

import React from 'react';
import { StyleSheet, View, Text, Pressable } from 'react-native';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { StatusBadge } from '@/components/ui/Badge';
import { formatCurrency, formatDate, getOrderStatusColor } from '@/utils/format';
import { useTheme } from '@/hooks/use-theme';
import type { Order } from '@/services/orderService';

interface OrderCardProps {
  order: Order;
}

export function OrderCard({ order }: OrderCardProps) {
  const router = useRouter();
  const theme = useTheme();
  const statusColor = getOrderStatusColor(order.trangThai);
  const itemCount = order.items?.length ?? 0;

  return (
    <Pressable
      style={({ pressed }) => [
        styles.card,
        {
          backgroundColor: theme.surface,
          borderColor: theme.border,
          opacity: pressed ? 0.92 : 1,
        },
      ]}
      onPress={() => router.push(`/orders/${order.id}`)}
    >
      <View style={styles.header}>
        <View style={styles.orderInfo}>
          <Text style={[styles.orderCode, { color: theme.text }]}>{order.maHoaDon}</Text>
          <Text style={[styles.date, { color: theme.textTertiary }]}>
            {formatDate(order.ngayTao)}
          </Text>
        </View>
        <StatusBadge
          status={order.trangThai}
          label={order.trangThaiDisplay}
          color={statusColor}
        />
      </View>

      <View style={[styles.divider, { backgroundColor: theme.borderLight }]} />

      <View style={styles.footer}>
        <View style={styles.footerLeft}>
          <Ionicons name="cube-outline" size={16} color={theme.textSecondary} />
          <Text style={[styles.itemCount, { color: theme.textSecondary }]}>
            {itemCount} sản phẩm
          </Text>
        </View>
        <View style={styles.footerRight}>
          <Text style={[styles.totalLabel, { color: theme.textSecondary }]}>Tổng:</Text>
          <Text style={[styles.totalPrice, { color: theme.text }]}>
            {formatCurrency(order.tongTienSauGiam ?? order.tongTien)}
          </Text>
        </View>
      </View>

      <View style={styles.chevron}>
        <Ionicons name="chevron-forward" size={18} color={theme.textTertiary} />
      </View>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    padding: Spacing.three,
    marginBottom: Spacing.two,
    position: 'relative',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    gap: 12,
  },
  orderInfo: {
    flex: 1,
    gap: 2,
  },
  orderCode: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
  },
  date: {
    fontSize: FontSizes.xs,
  },
  divider: {
    height: 1,
    marginVertical: Spacing.two + 2,
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingRight: 20,
  },
  footerLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  itemCount: {
    fontSize: FontSizes.sm,
  },
  footerRight: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  totalLabel: {
    fontSize: FontSizes.sm,
  },
  totalPrice: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
  },
  chevron: {
    position: 'absolute',
    right: Spacing.three,
    top: '50%',
    marginTop: -9,
  },
});
