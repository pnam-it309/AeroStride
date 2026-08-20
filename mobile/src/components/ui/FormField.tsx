/**
 * FormField – shared themed label + TextInput wrapper.
 * Replaces the duplicated inputGroup / label / input pattern across
 * login, register, checkout, profile, and change-password screens.
 */

import React from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  type TextInputProps,
} from 'react-native';
import Ionicons from '@expo/vector-icons/Ionicons';
import { FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface FormFieldProps extends TextInputProps {
  label: string;
  /** Leading icon name from Ionicons (optional) */
  icon?: React.ComponentProps<typeof Ionicons>['name'];
  /** Extra styles applied to the outer wrapper */
  containerStyle?: object;
}

export function FormField({ label, icon, containerStyle, style, ...inputProps }: FormFieldProps) {
  const theme = useTheme();

  return (
    <View style={[styles.group, containerStyle]}>
      <Text style={[styles.label, { color: theme.textSecondary }]}>{label}</Text>
      <View
        style={[
          styles.inputRow,
          {
            backgroundColor: theme.backgroundElement,
            borderColor: theme.border,
          },
        ]}
      >
        {icon && (
          <Ionicons name={icon} size={18} color={theme.textTertiary} style={styles.icon} />
        )}
        <TextInput
          style={[styles.input, { color: theme.text }, style]}
          placeholderTextColor={theme.textTertiary}
          {...inputProps}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  group: {
    marginBottom: Spacing.three,
  },
  label: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
    marginBottom: Spacing.one,
  },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.three,
    minHeight: 48,
  },
  icon: {
    marginRight: Spacing.two,
  },
  input: {
    flex: 1,
    fontSize: FontSizes.base,
    paddingVertical: Spacing.two,
  },
});
