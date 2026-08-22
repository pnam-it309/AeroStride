/**
 * Change Password Screen
 */

import React, { useState } from 'react';
import { StyleSheet, View, Pressable, Text, ScrollView, KeyboardAvoidingView, Platform } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Ionicons from '@expo/vector-icons/Ionicons';
import { Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { useFeedback } from '@/context/FeedbackContext';
import { profileService } from '@/services/profileService';
import { getApiErrorMessage } from '@/services/apiClient';
import { ScreenHeader } from '@/components/ui/ScreenHeader';
import { FormField } from '@/components/ui/FormField';
import { PrimaryButton } from '@/components/ui/PrimaryButton';

export default function ChangePasswordScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const { showToast } = useFeedback();

  const [matKhauCu, setMatKhauCu] = useState('');
  const [matKhauMoi, setMatKhauMoi] = useState('');
  const [xacNhanMatKhau, setXacNhanMatKhau] = useState('');
  const [show, setShow] = useState(false);
  const [saving, setSaving] = useState(false);

  const handleSubmit = async () => {
    if (!matKhauCu || !matKhauMoi || !xacNhanMatKhau) {
      showToast({ type: 'error', title: 'Lỗi', message: 'Vui lòng nhập đầy đủ thông tin' });
      return;
    }
    if (matKhauMoi.length < 6) {
      showToast({ type: 'error', title: 'Lỗi', message: 'Mật khẩu mới phải có ít nhất 6 ký tự' });
      return;
    }
    if (matKhauMoi !== xacNhanMatKhau) {
      showToast({ type: 'error', title: 'Lỗi', message: 'Mật khẩu xác nhận không khớp' });
      return;
    }
    setSaving(true);
    try {
      await profileService.changePassword({ matKhauCu, matKhauMoi, xacNhanMatKhau });
      showToast({ type: 'success', title: 'Thành công', message: 'Đã đổi mật khẩu' });
    } catch (err: any) {
      showToast({ type: 'error', title: 'Lỗi', message: getApiErrorMessage(err, 'Không thể đổi mật khẩu') });
    } finally {
      setSaving(false);
    }
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <ScreenHeader title="Đổi mật khẩu" />
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={{ flex: 1 }}>
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={[styles.content, { paddingBottom: insets.bottom + 40 }]}
          keyboardShouldPersistTaps="handled"
        >
          <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
            <FormField
              label="Mật khẩu hiện tại"
              icon="lock-closed-outline"
              value={matKhauCu}
              onChangeText={setMatKhauCu}
              placeholder="Nhập mật khẩu hiện tại"
              secureTextEntry={!show}
              autoCapitalize="none"
            />
            <FormField
              label="Mật khẩu mới"
              icon="lock-closed-outline"
              value={matKhauMoi}
              onChangeText={setMatKhauMoi}
              placeholder="Tối thiểu 6 ký tự"
              secureTextEntry={!show}
              autoCapitalize="none"
            />
            <FormField
              label="Xác nhận mật khẩu mới"
              icon="lock-closed-outline"
              value={xacNhanMatKhau}
              onChangeText={setXacNhanMatKhau}
              placeholder="Nhập lại mật khẩu mới"
              secureTextEntry={!show}
              autoCapitalize="none"
            />

            <Pressable style={styles.showToggle} onPress={() => setShow((s) => !s)} hitSlop={8}>
              <Ionicons
                name={show ? 'eye-off-outline' : 'eye-outline'}
                size={16}
                color={theme.textSecondary}
              />
              <Text style={[styles.showToggleText, { color: theme.textSecondary }]}>
                {show ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
              </Text>
            </Pressable>

            <PrimaryButton
              label="Đổi mật khẩu"
              onPress={handleSubmit}
              loading={saving}
            />
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  content: { padding: Spacing.three },
  section: {
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    padding: Spacing.three,
  },
  showToggle: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: Spacing.three,
  },
  showToggleText: { fontSize: 13 },
});
