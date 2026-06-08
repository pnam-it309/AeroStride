/**
 * My Orders Screen - order list with status tabs
 */

import React, { useEffect, useState, useCallback } from 'react';
import {
  StyleSheet,
  View,
  Text,
  FlatList,
  Pressable,
  RefreshControl,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import Animated, { FadeInDown } from 'react-native-reanimated';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { orderService, type Order } from '@/services/orderService';
import { OrderCard } from '@/components/OrderCard';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

const STATUS_TABS = [
  { key: '', label: 'Tất cả' },
  { key: 'CHO_XAC_NHAN', label: 'Chờ xác nhận' },
  { key: 'DANG_GIAO', label: 'Đang giao' },
  { key: 'HOAN_THANH', label: 'Hoàn thành' },
  { key: 'DA_HUY', label: 'Đã hủy' },
];

export default function OrdersScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();

  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [activeTab, setActiveTab] = useState('');

  const loadOrders = useCallback(
    async (status?: string) => {
      try {
        const data = await orderService.getMyOrders(status || undefined);
        setOrders(data || []);
      } catch (error) {
        console.warn('Failed to load orders:', error);
        setOrders([]);
      } finally {
        setLoading(false);
        setRefreshing(false);
      }
    },
    []
  );

  useEffect(() => {
    setLoading(true);
    loadOrders(activeTab);
  }, [activeTab, loadOrders]);

  const onRefresh = useCallback(() => {
    setRefreshing(true);
    loadOrders(activeTab);
  }, [activeTab, loadOrders]);

  return (
    <View style={[styles.container, { backgroundColor: theme.background, paddingTop: insets.top }]}>
      {/* Header */}
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Ionicons name="arrow-back" size={24} color={theme.text} />
        </Pressable>
        <Text style={[styles.title, { color: theme.text }]}>Đơn hàng của tôi</Text>
        <View style={{ width: 24 }} />
      </View>

      {/* Status Tabs */}
      <FlatList
        data={STATUS_TABS}
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.tabsContainer}
        keyExtractor={(item) => item.key}
        renderItem={({ item }) => {
          const isActive = activeTab === item.key;
          return (
            <Pressable
              style={[
                styles.tab,
                {
                  backgroundColor: isActive ? Brand.primary : theme.surfaceElevated,
                  borderColor: isActive ? Brand.primary : theme.border,
                },
              ]}
              onPress={() => setActiveTab(item.key)}
            >
              <Text
                style={[
                  styles.tabText,
                  { color: isActive ? '#FFFFFF' : theme.textSecondary },
                ]}
              >
                {item.label}
              </Text>
            </Pressable>
          );
        }}
      />

      {/* Orders List */}
      {loading ? (
        <LoadingSpinner fullScreen />
      ) : (
        <FlatList
          data={orders}
          keyExtractor={(item) => item.id}
          renderItem={({ item, index }) => (
            <Animated.View entering={FadeInDown.delay(index * 60).duration(400)}>
              <OrderCard order={item} />
            </Animated.View>
          )}
          contentContainerStyle={styles.listContent}
          showsVerticalScrollIndicator={false}
          refreshControl={
            <RefreshControl
              refreshing={refreshing}
              onRefresh={onRefresh}
              tintColor={Brand.primary}
            />
          }
          ListEmptyComponent={
            <View style={styles.emptyContainer}>
              <Ionicons name="cube-outline" size={48} color={theme.textTertiary} />
              <Text style={[styles.emptyText, { color: theme.textSecondary }]}>
                Chưa có đơn hàng nào
              </Text>
              <Pressable
                style={styles.shopBtn}
                onPress={() => router.push('/shop' as any)}
              >
                <Text style={styles.shopBtnText}>Mua sắm ngay</Text>
              </Pressable>
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
    paddingVertical: Spacing.two + 4,
  },
  title: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.bold,
  },
  tabsContainer: {
    paddingHorizontal: Spacing.three,
    gap: Spacing.two,
    marginBottom: Spacing.three,
  },
  tab: {
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.one + 4,
    borderRadius: BorderRadius.full,
    borderWidth: 1,
  },
  tabText: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
  },
  listContent: {
    paddingHorizontal: Spacing.three,
    paddingBottom: 100,
  },
  emptyContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: Spacing.seven,
    gap: Spacing.three,
  },
  emptyText: {
    fontSize: FontSizes.base,
  },
  shopBtn: {
    backgroundColor: Brand.primary,
    paddingHorizontal: Spacing.four,
    paddingVertical: Spacing.two + 4,
    borderRadius: BorderRadius.full,
  },
  shopBtnText: {
    color: '#FFFFFF',
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
  },
});
