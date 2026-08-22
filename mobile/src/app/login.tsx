/**
 * Login Screen - Premium dark UI
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
  ScrollView,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import Animated, { FadeInDown } from 'react-native-reanimated';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useAuth } from '@/context/AuthContext';
import { useFeedback } from '@/context/FeedbackContext';
import { getApiErrorMessage } from '@/services/apiClient';

export default function LoginScreen() {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { login, socialLogin } = useAuth();
  const { showToast } = useFeedback();

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [socialLoading, setSocialLoading] = useState<'GOOGLE' | 'FACEBOOK' | null>(null);

  const handleLogin = async () => {
    if (!username.trim() || !password.trim()) {
      showToast({ type: 'error', title: 'Lỗi', message: 'Vui lòng nhập tài khoản và mật khẩu' });
      return;
    }

    setLoading(true);
    try {
      await login({
        username: username.trim(),
        password: password.trim(),
        loginType: 'CLIENT',
      });
      router.back();
    } catch (error: any) {
      const message = getApiErrorMessage(error, 'Tài khoản hoặc mật khẩu không đúng');
      showToast({ type: 'error', title: 'Đăng nhập thất bại', message });
    } finally {
      setLoading(false);
    }
  };

  const handleSocialLogin = async (provider: 'GOOGLE' | 'FACEBOOK') => {
    setSocialLoading(provider);
    try {
      await socialLogin({
        provider,
        email: provider === 'GOOGLE' ? 'mobile.google.user@aerostride.vn' : 'mobile.fb.user@aerostride.vn',
        name: provider === 'GOOGLE' ? 'Khách Hàng Google' : 'Khách Hàng Facebook',
        avatarUrl: provider === 'GOOGLE' 
          ? 'https://lh3.googleusercontent.com/a/default-user=s96-c'
          : 'https://upload.wikimedia.org/wikipedia/commons/b/b8/2021_Facebook_icon.svg',
        providerId: (provider === 'GOOGLE' ? 'm_gg_' : 'm_fb_') + Date.now(),
      });
      router.back();
    } catch (error: any) {
      const message =
        error?.response?.data?.message || `Đăng nhập qua ${provider} thất bại`;
      showToast({ type: 'error', title: 'Đăng nhập thất bại', message });
    } finally {
      setSocialLoading(null);
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
          {/* Close button */}
          <Pressable
            style={styles.closeButton}
            onPress={() => router.back()}
            hitSlop={16}
          >
            <Ionicons name="close" size={24} color="#94A3B8" />
          </Pressable>

          {/* Brand */}
          <Animated.View entering={FadeInDown.duration(500)} style={styles.brandSection}>
            <Text style={styles.logo}>👟</Text>
            <Text style={styles.brandName}>AeroStride</Text>
            <Text style={styles.brandTagline}>Đăng nhập để tiếp tục mua sắm</Text>
          </Animated.View>

          {/* Form */}
          <Animated.View entering={FadeInDown.delay(200).duration(500)} style={styles.form}>
            <View style={styles.inputGroup}>
              <Text style={styles.label}>Tài khoản</Text>
              <View style={styles.inputContainer}>
                <Ionicons name="person-outline" size={18} color="#64748B" />
                <TextInput
                  style={styles.input}
                  placeholder="Email hoặc số điện thoại"
                  placeholderTextColor="#475569"
                  value={username}
                  onChangeText={setUsername}
                  autoCapitalize="none"
                  autoCorrect={false}
                />
              </View>
            </View>

            <View style={styles.inputGroup}>
              <Text style={styles.label}>Mật khẩu</Text>
              <View style={styles.inputContainer}>
                <Ionicons name="lock-closed-outline" size={18} color="#64748B" />
                <TextInput
                  style={styles.input}
                  placeholder="Nhập mật khẩu"
                  placeholderTextColor="#475569"
                  value={password}
                  onChangeText={setPassword}
                  secureTextEntry={!showPassword}
                />
                <Pressable onPress={() => setShowPassword(!showPassword)} hitSlop={8}>
                  <Ionicons
                    name={showPassword ? 'eye-off-outline' : 'eye-outline'}
                    size={18}
                    color="#64748B"
                  />
                </Pressable>
              </View>
            </View>

            <Pressable
              style={({ pressed }) => [
                styles.loginBtn,
                { opacity: loading || pressed ? 0.8 : 1 },
              ]}
              onPress={handleLogin}
              disabled={loading || !!socialLoading}
            >
              <LinearGradient
                colors={[Brand.primary, Brand.primaryDark]}
                style={styles.loginGradient}
                start={{ x: 0, y: 0 }}
                end={{ x: 1, y: 0 }}
              >
                {loading ? (
                  <Text style={styles.loginText}>Đang xử lý...</Text>
                ) : (
                  <>
                    <Text style={styles.loginText}>Đăng nhập</Text>
                    <Ionicons name="arrow-forward" size={18} color="#FFFFFF" />
                  </>
                )}
              </LinearGradient>
            </Pressable>

            {/* Social Divider */}
            <View style={styles.socialDivider}>
              <View style={styles.dividerLine} />
              <Text style={styles.dividerText}>Hoặc đăng nhập với</Text>
              <View style={styles.dividerLine} />
            </View>

            {/* Social Buttons with Official Colors */}
            <View style={styles.socialRow}>
              <Pressable
                style={({ pressed }) => [
                  styles.socialBtn,
                  styles.googleBtn,
                  { opacity: pressed || socialLoading === 'GOOGLE' ? 0.75 : 1 },
                ]}
                onPress={() => handleSocialLogin('GOOGLE')}
                disabled={loading || !!socialLoading}
              >
                <Ionicons name="logo-google" size={20} color="#EA4335" />
                <Text style={styles.socialBtnText}>Google</Text>
              </Pressable>

              <Pressable
                style={({ pressed }) => [
                  styles.socialBtn,
                  styles.facebookBtn,
                  { opacity: pressed || socialLoading === 'FACEBOOK' ? 0.75 : 1 },
                ]}
                onPress={() => handleSocialLogin('FACEBOOK')}
                disabled={loading || !!socialLoading}
              >
                <Ionicons name="logo-facebook" size={20} color="#1877F2" />
                <Text style={styles.socialBtnText}>Facebook</Text>
              </Pressable>
            </View>
          </Animated.View>

          {/* Register link */}
          <Animated.View entering={FadeInDown.delay(300).duration(500)} style={styles.registerRow}>
            <Text style={styles.registerHint}>Chưa có tài khoản?</Text>
            <Pressable onPress={() => router.push('/register' as any)} hitSlop={8}>
              <Text style={styles.registerLink}>Đăng ký ngay</Text>
            </Pressable>
          </Animated.View>

          {/* Skip */}
          <Animated.View entering={FadeInDown.delay(400).duration(500)}>
            <Pressable
              style={({ pressed }) => [styles.skipButton, { opacity: pressed ? 0.6 : 1 }]}
              onPress={() => router.back()}
            >
              <Text style={styles.skipText}>Bỏ qua, tiếp tục mua sắm</Text>
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
    position: 'absolute',
    top: 0,
    right: 0,
    width: 40,
    height: 40,
    borderRadius: 20,
    backgroundColor: 'rgba(255,255,255,0.06)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  brandSection: {
    alignItems: 'center',
    marginBottom: Spacing.six,
    gap: Spacing.two,
  },
  logo: {
    fontSize: 56,
  },
  brandName: {
    color: '#FFFFFF',
    fontSize: FontSizes['3xl'],
    fontWeight: FontWeights.extrabold,
    letterSpacing: -1,
  },
  brandTagline: {
    color: '#64748B',
    fontSize: FontSizes.base,
  },
  form: {
    gap: Spacing.three,
  },
  inputGroup: {
    gap: Spacing.one + 2,
  },
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
  loginBtn: {
    borderRadius: BorderRadius.lg,
    overflow: 'hidden',
    marginTop: Spacing.two,
  },
  loginGradient: {
    flexDirection: 'row',
    paddingVertical: Spacing.three,
    justifyContent: 'center',
    alignItems: 'center',
    gap: Spacing.two,
  },
  loginText: {
    color: '#FFFFFF',
    fontSize: FontSizes.md,
    fontWeight: FontWeights.bold,
  },
  socialDivider: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: Spacing.two,
    gap: Spacing.three,
  },
  dividerLine: {
    flex: 1,
    height: 1,
    backgroundColor: 'rgba(255,255,255,0.1)',
  },
  dividerText: {
    color: '#94A3B8',
    fontSize: FontSizes.xs,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
  },
  socialRow: {
    flexDirection: 'row',
    gap: Spacing.three,
  },
  socialBtn: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    gap: Spacing.two,
    paddingVertical: Spacing.three,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    backgroundColor: 'rgba(255,255,255,0.05)',
  },
  googleBtn: {
    borderColor: 'rgba(234, 67, 53, 0.3)',
    backgroundColor: 'rgba(234, 67, 53, 0.06)',
  },
  facebookBtn: {
    borderColor: 'rgba(24, 119, 242, 0.3)',
    backgroundColor: 'rgba(24, 119, 242, 0.06)',
  },
  socialBtnText: {
    color: '#FFFFFF',
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.semibold,
  },
  registerRow: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: Spacing.one + 2,
    marginTop: Spacing.four,
  },
  registerHint: {
    color: '#64748B',
    fontSize: FontSizes.sm,
  },
  registerLink: {
    color: Brand.primaryLight,
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
  },
  skipButton: {
    alignItems: 'center',
    paddingVertical: Spacing.three,
  },
  skipText: {
    color: '#64748B',
    fontSize: FontSizes.sm,
  },
});
