/**
 * PrimaryButton – shared gradient CTA button.
 * Replaces the duplicated LinearGradient / Pressable / loading-text pattern across
 * login, register, checkout, change-password, and profile screens.
 */

import React from 'react';
import { StyleSheet, Text, Pressable, ActivityIndicator, type StyleProp, type ViewStyle } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import Ionicons from '@expo/vector-icons/Ionicons';
import { Brand, FontSizes, FontWeights, BorderRadius, Spacing } from '@/constants/theme';

interface PrimaryButtonProps {
  label: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  /** Icon shown to the right of the label when not loading */
  trailingIcon?: React.ComponentProps<typeof Ionicons>['name'];
  /** Override gradient colours (default: Brand.primary → Brand.primaryDark) */
  colors?: [string, string];
  style?: StyleProp<ViewStyle>;
}

export function PrimaryButton({
  label,
  onPress,
  loading = false,
  disabled = false,
  trailingIcon,
  colors = [Brand.primary, Brand.primaryDark],
  style,
}: PrimaryButtonProps) {
  return (
    <Pressable
      onPress={onPress}
      disabled={loading || disabled}
      style={({ pressed }) => [styles.wrapper, { opacity: loading || pressed ? 0.75 : 1 }, style]}
    >
      <LinearGradient
        colors={colors}
        style={styles.gradient}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 0 }}
      >
        {loading ? (
          <ActivityIndicator color="#FFFFFF" size="small" />
        ) : (
          <>
            <Text style={styles.label}>{label}</Text>
            {trailingIcon && (
              <Ionicons name={trailingIcon} size={18} color="#FFFFFF" style={styles.icon} />
            )}
          </>
        )}
      </LinearGradient>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    borderRadius: BorderRadius.lg,
    overflow: 'hidden',
  },
  gradient: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: Spacing.three,
    paddingHorizontal: Spacing.six,
    minHeight: 52,
    gap: Spacing.two,
  },
  label: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
    color: '#FFFFFF',
  },
  icon: {
    marginLeft: 2,
  },
});
