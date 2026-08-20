/**
 * ScreenHeader – shared top bar used by all screens.
 * Handles safe-area padding, back/close button, title, and optional right element.
 */

import React from 'react';
import { StyleSheet, View, Text, Pressable } from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Ionicons from '@expo/vector-icons/Ionicons';
import { FontSizes, FontWeights, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface ScreenHeaderProps {
  title: string;
  /** 'arrow-back' (default) or 'close' */
  backIcon?: 'arrow-back' | 'close';
  /** Extra top padding beyond safe-area (default: Spacing.two) */
  extraTopPadding?: number;
  /** Replaces the spacer on the right side */
  rightElement?: React.ReactNode;
}

export function ScreenHeader({
  title,
  backIcon = 'arrow-back',
  extraTopPadding = Spacing.two,
  rightElement,
}: ScreenHeaderProps) {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();

  return (
    <View
      style={[
        styles.header,
        { paddingTop: insets.top + extraTopPadding, borderBottomColor: theme.borderLight },
      ]}
    >
      <Pressable onPress={() => router.back()} hitSlop={12} style={styles.backBtn}>
        <Ionicons name={backIcon} size={24} color={theme.text} />
      </Pressable>

      <Text style={[styles.title, { color: theme.text }]} numberOfLines={1}>
        {title}
      </Text>

      {/* Right side: custom element or equal-width spacer to keep title centred */}
      <View style={styles.right}>{rightElement ?? <View style={{ width: 24 }} />}</View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: Spacing.four,
    paddingBottom: Spacing.three,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  backBtn: {
    width: 32,
    alignItems: 'flex-start',
  },
  title: {
    flex: 1,
    textAlign: 'center',
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.semibold,
  },
  right: {
    width: 32,
    alignItems: 'flex-end',
  },
});
