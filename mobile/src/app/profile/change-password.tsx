/**
 * Change Password Screen
 */

import React, { useState } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  ScrollView,
  Pressable,
  Alert,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { profileService } from '@/services/profileService';

export default function ChangePasswordScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();

  const [matKhauCu, setMatKhauCu] = useState('');
  const [matKhauMoi, setMatKhauMoi] = useState('');
  const [xacNhanMatKhau, setXacNhanMatKhau] = useState('');
  const [show, setShow] = useState(false);
  const [saving, setSaving] = useState(false);

  const handleSubmit = async () => {
    if (!matKhauCu || !matKhauMoi || !xacNhanMatKhau) {
      Alert.alert('Lỗi', 'Vui lòng nhập đầy đủ thông tin');
      return;
    }
    if (matKhauMoi.length < 6) {
      Alert.alert('Lỗi', 'Mật khẩu mới phải có ít nhất 6 ký tự');
      return;
    }
    if (matKhauMoi !== xacNhanMatKhau) {
      Alert.alert('Lỗi', 'Mật khẩu xác nhận không khớp');
      return;
    }
    setSaving(true);
    try {
      await profileService.changePassword({ matKhauCu, matKhauMoi, xacNhanMatKhau });
      Alert.alert('Thành công', 'Đã đổi mật khẩu', [
        { text: 'OK', onPress: () => router.back() },
      ]);
    } catch (err: any) {
      Alert.alert('Lỗi', err?.response?.data?.message || 'Không thể đổi mật khẩu');
    } finally {
      setSaving(false);
    }
  };

  const renderInput = (
    label: string,
    value: string,
    onChange: (t: string) => void,
    placeholder: string
  ) => (
    <View style={styles.inputGroup}>
      <Text style={[styles.label, { color: theme.textSecondary }]}>{label}</Text>
      <View
        style={[
          styles.inputContainer,
          { backgroundColor: theme.backgroundElement, borderColor: theme.border },
        ]}
      >
        <Ionicons name="lock-closed-outline" size={18} color={theme.textTertiary} />
        <TextInput
          style={[styles.input, { color: theme.text }]}
          value={value}
          onChangeText={onChange}
          placeholder={placeholder}
          placeholderTextColor={theme.textTertiary}
          secureTextEntry={!show}
          autoCapitalize="none"
        />
      </View>
    </View>
  );

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={{ flex: 1 }}
      >
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{ paddingBottom: insets.bottom + 40 }}
          keyboardShouldPersistTaps="handled"
        >
          <View style={[styles.header, { paddingTop: insets.top + Spacing.two }]}>
            <Pressable onPress={() => router.back()} hitSlop={12}>
              <Ionicons name="arrow-back" size={24} color={theme.text} />
            </Pressable>
            <Text style={[styles.title, { color: theme.text }]}>Đổi mật khẩu</Text>
            <View style={{ width: 24 }} />
          </View>

          <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
            {renderInput('Mật khẩu hiện tại', matKhauCu, setMatKhauCu, 'Nhập mật khẩu hiện tại')}
            {renderInput('Mật khẩu mới', matKhauMoi, setMatKhauMoi, 'Tối thiểu 6 ký tự')}
            {renderInput('Xác nhận mật khẩu mới', xacNhanMatKhau, setXacNhanMatKhau, 'Nhập lại mật khẩu mới')}

            <Pressable
              style={styles.showToggle}
              onPress={() => setShow((s) => !s)}
              hitSlop={8}
            >
              <Ionicons
                name={show ? 'eye-off-outline' : 'eye-outline'}
                size={16}
                color={theme.textSecondary}
              />
              <Text style={[styles.showToggleText, { color: theme.textSecondary }]}>
                {show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
              </Text>
            </Pressable>

            <Pressable
              style={({ pressed }) => [styles.submitBtn, { opacity: saving || pressed ? 0.7 : 1 }]}
              onPress={handleSubmit}
              disabled={saving}
            >
              <Text style={styles.submitText}>{saving ? 'Đang xử lý...' : 'Đổi mật khẩu'}</Text>
            </Pressable>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
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
  section: {
    marginHorizontal: Spacing.three,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    padding: Spacing.three,
  },
  inputGroup: { marginBottom: Spacing.three, gap: Spacing.one + 2 },
  label: { fontSize: FontSizes.sm, fontWeight: FontWeights.medium },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + 2,
    gap: Spacing.two,
  },
  input: { flex: 1, fontSize: FontSizes.base },
  showToggle: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: Spacing.three,
  },
  showToggleText: { fontSize: FontSizes.sm },
  submitBtn: {
    backgroundColor: Brand.primary,
    paddingVertical: Spacing.three,
    borderRadius: BorderRadius.lg,
    alignItems: 'center',
  },
  submitText: { color: '#FFFFFF', fontSize: FontSizes.base, fontWeight: FontWeights.bold },
});
