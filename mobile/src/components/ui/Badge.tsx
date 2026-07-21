/**
 * Badge Component - cart count, status badges
 */

import React from 'react';
import { StyleSheet, View, Text, ViewStyle } from 'react-native';
import { Brand, FontSizes, FontWeights, BorderRadius } from '@/constants/theme';

interface BadgeProps {
  count?: number;
  label?: string;
  color?: string;
  textColor?: string;
  size?: 'sm' | 'md';
  style?: ViewStyle;
}

export function Badge({
  count,
  label,
  color = Brand.error,
  textColor = '#FFFFFF',
  size = 'sm',
  style,
}: BadgeProps) {
  const displayText = label ?? (count != null ? (count > 99 ? '99+' : String(count)) : '');

  if (!displayText && count === 0) return null;
  if (count != null && count <= 0) return null;

  const isMd = size === 'md';

  return (
    <View
      style={[
        styles.badge,
        {
          backgroundColor: color,
          minWidth: isMd ? 22 : 18,
          height: isMd ? 22 : 18,
          paddingHorizontal: isMd ? 6 : 4,
        },
        style,
      ]}
    >
      <Text
        style={[
          styles.text,
          {
            color: textColor,
            fontSize: isMd ? FontSizes.xs : 10,
          },
        ]}
      >
        {displayText}
      </Text>
    </View>
  );
}

// Status badge for orders
interface StatusBadgeProps {
  status: string;
  label: string;
  color: string;
}

export function StatusBadge({ label, color }: StatusBadgeProps) {
  return (
    <View style={[styles.statusBadge, { backgroundColor: color + '20' }]}>
      <View style={[styles.statusDot, { backgroundColor: color }]} />
      <Text style={[styles.statusText, { color }]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    borderRadius: BorderRadius.full,
    alignItems: 'center',
    justifyContent: 'center',
  },
  text: {
    fontWeight: FontWeights.bold,
    textAlign: 'center',
  },
  statusBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: BorderRadius.full,
    gap: 6,
  },
  statusDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  statusText: {
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.semibold,
  },
});
