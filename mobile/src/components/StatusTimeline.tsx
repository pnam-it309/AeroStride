/**
 * StatusTimeline - order status history timeline
 */

import React from 'react';
import { StyleSheet, View, Text } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing } from '@/constants/theme';
import { formatDateTime, getOrderStatusColor } from '@/utils/format';
import { useTheme } from '@/hooks/use-theme';
import type { OrderStatusHistory } from '@/services/orderService';

interface StatusTimelineProps {
  history: OrderStatusHistory[];
}

export function StatusTimeline({ history }: StatusTimelineProps) {
  const theme = useTheme();

  if (!history || history.length === 0) return null;

  // Sort by time descending (latest first)
  const sortedHistory = [...history].sort((a, b) => (b.thoiGian ?? 0) - (a.thoiGian ?? 0));

  return (
    <View style={styles.container}>
      {sortedHistory.map((item, index) => {
        const isFirst = index === 0;
        const isLast = index === sortedHistory.length - 1;
        const color = isFirst ? getOrderStatusColor(item.trangThai) : theme.textTertiary;

        return (
          <View key={index} style={styles.item}>
            {/* Timeline line */}
            <View style={styles.lineContainer}>
              {!isFirst && (
                <View style={[styles.lineTop, { backgroundColor: theme.border }]} />
              )}
              <View
                style={[
                  styles.dot,
                  {
                    backgroundColor: isFirst ? color : 'transparent',
                    borderColor: color,
                    borderWidth: 2,
                    width: isFirst ? 14 : 10,
                    height: isFirst ? 14 : 10,
                  },
                ]}
              />
              {!isLast && (
                <View style={[styles.lineBottom, { backgroundColor: theme.border }]} />
              )}
            </View>

            {/* Content */}
            <View style={styles.content}>
              <Text
                style={[
                  styles.statusLabel,
                  {
                    color: isFirst ? color : theme.textSecondary,
                    fontWeight: isFirst ? FontWeights.bold : FontWeights.medium,
                  },
                ]}
              >
                {item.trangThaiDisplay}
              </Text>
              <Text style={[styles.time, { color: theme.textTertiary }]}>
                {formatDateTime(item.thoiGian)}
              </Text>
              {item.ghiChu ? (
                <Text style={[styles.note, { color: theme.textSecondary }]}>
                  {item.ghiChu}
                </Text>
              ) : null}
            </View>
          </View>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    paddingLeft: Spacing.one,
  },
  item: {
    flexDirection: 'row',
    minHeight: 52,
  },
  lineContainer: {
    width: 20,
    alignItems: 'center',
  },
  lineTop: {
    width: 2,
    flex: 1,
  },
  lineBottom: {
    width: 2,
    flex: 1,
  },
  dot: {
    borderRadius: 7,
  },
  content: {
    flex: 1,
    paddingLeft: Spacing.two + 2,
    paddingBottom: Spacing.three,
    gap: 2,
  },
  statusLabel: {
    fontSize: FontSizes.sm,
  },
  time: {
    fontSize: FontSizes.xs,
  },
  note: {
    fontSize: FontSizes.xs,
    marginTop: 2,
    fontStyle: 'italic',
  },
});
