/**
 * Register Screen - new customer sign up
 */

import React, { useState } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  Pressable,
  KeyboardAvoidingView,
  Platform,
  Alert,
  ScrollView,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import Animated, { FadeInDown } from 'react-native-reanimated';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useAuth } from '@/context/AuthContext';

interface FieldConfig {
  key: 'ten' | 'tenTaiKhoan' | 'email' | 'sdt' | 'matKhau' | 'xacNhan';
  label: string;
  placeholder: string;
  icon: keyof typeof Ionicons.glyphMap;
  keyboardType?: 'default' | 'email-address' | 'phone-pad';
  secure?: boolean;
}

const FIELDS: FieldConfig[] = [
  { key: 'ten', label: 'Họ và tên', placeholder: 'Nguyễn Văn A', icon: 'person-outline' },
  { key: 'tenTaiKhoan', label: 'Tên tài khoản', placeholder: 'Tối thiểu 4 ký tự', icon: 'at-outline' },
  { key: 'email', label: 'Email', placeholder: 'email@example.com', icon: 'mail-outline', keyboardType: 'email-address' },
  { key: 'sdt', label: 'Số điện thoại', placeholder: '0901234567', icon: 'call-outline', keyboardType: 'phone-pad' },
  { key: 'matKhau', label: 'Mật khẩu', placeholder: 'Tối thiểu 6 ký tự', icon: 'lock-closed-outline', secure: true },
  { key: 'xacNhan', label: 'Xác nhận mật khẩu', placeholder: 'Nhập lại mật khẩu', icon: 'lock-closed-outline', secure: true },
];

export default function RegisterScreen() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { register } = useAuth();

  const [form, setForm] = useState({
    ten: '',
    tenTaiKhoan: '',
    email: '',
    sdt: '',
    matKhau: '',
    xacNhan: '',
  });
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);

  const setField = (key: FieldConfig['key'], value: string) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const handleRegister = async () => {
    if (!form.ten.trim() || !form.tenTaiKhoan.trim() || !form.email.trim() || !form.sdt.trim()) {
      Alert.alert('Lỗi', 'Vui lòng nhập đầy đủ thông tin');
      return;
    }
    if (form.tenTaiKhoan.trim().length < 4) {
      Alert.alert('Lỗi', 'Tên tài khoản phải có ít nhất 4 ký tự');
      return;
    }
    if (form.matKhau.length < 6) {
      Alert.alert('Lỗi', 'Mật khẩu phải có ít nhất 6 ký tự');
      return;
    }
    if (form.matKhau !== form.xacNhan) {
      Alert.alert('Lỗi', 'Mật khẩu xác nhận không khớp');
      return;
    }

    setLoading(true);
    try {
      await register({
        ten: form.ten.trim(),
        tenTaiKhoan: form.tenTaiKhoan.trim(),
        email: form.email.trim(),
        sdt: form.sdt.trim(),
        matKhau: form.matKhau,
      });
      // Auto-logged-in by the BE response; close back to where we came from
      router.back();
    } catch (error: any) {
      const message = error?.response?.data?.message || 'Đăng ký thất bại. Vui lòng thử lại.';
      Alert.alert('Đăng ký thất bại', message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <LinearGradient colors={['#0B1120', '#152238', '#0B1120']} style={styles.gradient}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        style={styles.keyboardView}
      >
        <ScrollView
          contentContainerStyle={[
            styles.scrollContent,
            { paddingTop: insets.top + Spacing.four, paddingBottom: insets.bottom + Spacing.four },
          ]}
          keyboardShouldPersistTaps="handled"
          showsVerticalScrollIndicator={false}
        >
          <Pressable style={styles.closeButton} onPress={() => router.back()} hitSlop={16}>
            <Ionicons name="arrow-back" size={24} color="#94A3B8" />
          </Pressable>

          <Animated.View entering={FadeInDown.duration(500)} style={styles.brandSection}>
            <Text style={styles.logo}>👟</Text>
            <Text style={styles.brandName}>Tạo tài khoản</Text>
            <Text style={styles.brandTagline}>Tham gia AeroStride ngay hôm nay</Text>
          </Animated.View>

          <Animated.View entering={FadeInDown.delay(150).duration(500)} style={styles.form}>
            {FIELDS.map((field) => (
              <View key={field.key} style={styles.inputGroup}>
                <Text style={styles.label}>{field.label}</Text>
                <View style={styles.inputContainer}>
                  <Ionicons name={field.icon} size={18} color="#64748B" />
                  <TextInput
                    style={styles.input}
                    placeholder={field.placeholder}
                    placeholderTextColor="#475569"
                    value={form[field.key]}
                    onChangeText={(t) => setField(field.key, t)}
                    autoCapitalize={field.key === 'ten' ? 'words' : 'none'}
                    autoCorrect={false}
                    keyboardType={field.keyboardType ?? 'default'}
                    secureTextEntry={field.secure && !showPassword}
                  />
                  {field.secure && (
                    <Pressable onPress={() => setShowPassword((s) => !s)} hitSlop={8}>
                      <Ionicons
                        name={showPassword ? 'eye-off-outline' : 'eye-outline'}
                        size={18}
                        color="#64748B"
                      />
                    </Pressable>
                  )}
                </View>
              </View>
            ))}

            <Pressable
              style={({ pressed }) => [styles.registerBtn, { opacity: loading || pressed ? 0.8 : 1 }]}
              onPress={handleRegister}
              disabled={loading}
            >
              <LinearGradient
                colors={[Brand.primary, Brand.primaryDark]}
                style={styles.registerGradient}
                start={{ x: 0, y: 0 }}
                end={{ x: 1, y: 0 }}
              >
                {loading ? (
                  <Text style={styles.registerText}>Đang xử lý...</Text>
                ) : (
                  <>
                    <Text style={styles.registerText}>Đăng ký</Text>
                    <Ionicons name="arrow-forward" size={18} color="#FFFFFF" />
                  </>
                )}
              </LinearGradient>
            </Pressable>
          </Animated.View>

          <Animated.View entering={FadeInDown.delay(300).duration(500)} style={styles.loginRow}>
            <Text style={styles.loginHint}>Đã có tài khoản?</Text>
            <Pressable onPress={() => router.back()} hitSlop={8}>
              <Text style={styles.loginLink}>Đăng nhập</Text>
            </Pressable>
          </Animated.View>
        </ScrollView>
      </KeyboardAvoidingView>
    </LinearGradient>
  );
}

const styles = StyleSheet.create({
  gradient: { flex: 1 },
  keyboardView: { flex: 1 },
  scrollContent: {
    flexGrow: 1,
    paddingHorizontal: Spacing.four,
    justifyContent: 'center',
  },
  closeButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: 'rgba(255,255,255,0.06)',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: Spacing.two,
  },
  brandSection: {
    alignItems: 'center',
    marginBottom: Spacing.four,
    gap: Spacing.one,
  },
  logo: { fontSize: 48 },
  brandName: {
    color: '#FFFFFF',
    fontSize: FontSizes['2xl'],
    fontWeight: FontWeights.extrabold,
    letterSpacing: -0.5,
  },
  brandTagline: {
    color: '#64748B',
    fontSize: FontSizes.base,
  },
  form: { gap: Spacing.three },
  inputGroup: { gap: Spacing.one + 2 },
  label: {
    color: '#94A3B8',
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
    paddingLeft: Spacing.one,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255,255,255,0.06)',
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.08)',
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + 4,
    gap: Spacing.two,
  },
  input: {
    flex: 1,
    color: '#F1F5F9',
    fontSize: FontSizes.base,
  },
  registerBtn: {
    borderRadius: BorderRadius.lg,
    overflow: 'hidden',
    marginTop: Spacing.two,
  },
  registerGradient: {
    flexDirection: 'row',
    paddingVertical: Spacing.three,
    justifyContent: 'center',
    alignItems: 'center',
    gap: Spacing.two,
  },
  registerText: {
    color: '#FFFFFF',
    fontSize: FontSizes.md,
    fontWeight: FontWeights.bold,
  },
  loginRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: Spacing.one + 2,
    marginTop: Spacing.four,
  },
  loginHint: { color: '#64748B', fontSize: FontSizes.sm },
  loginLink: {
    color: Brand.primaryLight,
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
  },
});
