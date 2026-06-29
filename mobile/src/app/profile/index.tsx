/**
 * Profile Screen - view & edit customer profile
 */

import React, { useEffect, useState } from 'react';
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
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { useAuth } from '@/context/AuthContext';
import { profileService, type CustomerProfile } from '@/services/profileService';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';

export default function ProfileScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { isAuthenticated } = useAuth();

  const [profile, setProfile] = useState<CustomerProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);

  const [ten, setTen] = useState('');
  const [sdt, setSdt] = useState('');

  useEffect(() => {
    if (!isAuthenticated) {
      router.replace('/login');
      return;
    }
    profileService
      .getMyProfile()
      .then((data) => {
        setProfile(data);
        setTen(data.ten || '');
        setSdt(data.sdt || '');
      })
      .catch((err) => {
        console.warn('Failed to load profile:', err);
        Alert.alert('Lỗi', err?.response?.data?.message || 'Không thể tải hồ sơ');
      })
      .finally(() => setLoading(false));
  }, [isAuthenticated, router]);

  const handleSave = async () => {
    if (!ten.trim()) {
      Alert.alert('Lỗi', 'Vui lòng nhập họ và tên');
      return;
    }
    if (!sdt.trim()) {
      Alert.alert('Lỗi', 'Vui lòng nhập số điện thoại');
      return;
    }
    setSaving(true);
    try {
      await profileService.updateProfile({ ten: ten.trim(), sdt: sdt.trim() });
      setProfile((prev) => (prev ? { ...prev, ten: ten.trim(), sdt: sdt.trim() } : prev));
      setEditing(false);
      Alert.alert('Thành công', 'Đã cập nhật hồ sơ');
    } catch (err: any) {
      Alert.alert('Lỗi', err?.response?.data?.message || 'Không thể cập nhật hồ sơ');
    } finally {
      setSaving(false);
    }
  };

  const handleCancelEdit = () => {
    setTen(profile?.ten || '');
    setSdt(profile?.sdt || '');
    setEditing(false);
  };

  if (loading) {
    return (
      <View style={[styles.container, { backgroundColor: theme.background }]}>
        <LoadingSpinner fullScreen />
      </View>
    );
  }

  if (!profile) return null;

  const fullAddress = [
    profile.diaChiChiTiet,
    profile.phuongXa,
    profile.quanHuyen,
    profile.tinhThanh,
  ]
    .filter(Boolean)
    .join(', ');

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={{ flex: 1 }}
      >
        <ScrollView
          showsVerticalScrollIndicator={false}
          contentContainerStyle={{ paddingBottom: insets.bottom + 40 }}
        >
          {/* Header */}
          <LinearGradient
            colors={['#0B1120', '#152238', '#1E3A5F']}
            style={[styles.header, { paddingTop: insets.top + Spacing.two }]}
          >
            <View style={styles.headerBar}>
              <Pressable onPress={() => router.back()} hitSlop={12}>
                <Ionicons name="arrow-back" size={24} color="#FFFFFF" />
              </Pressable>
              <Text style={styles.headerTitle}>Hồ sơ của tôi</Text>
              <View style={{ width: 24 }} />
            </View>

            <View style={styles.avatarWrap}>
              <LinearGradient colors={[Brand.primary, Brand.primaryDark]} style={styles.avatar}>
                <Text style={styles.avatarText}>
                  {(profile.ten || profile.tenTaiKhoan || '?').charAt(0).toUpperCase()}
                </Text>
              </LinearGradient>
            </View>
            <Text style={styles.name}>{profile.ten || profile.tenTaiKhoan}</Text>
            <Text style={styles.account}>@{profile.tenTaiKhoan}</Text>
          </LinearGradient>

          {/* Personal info */}
          <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
            <View style={styles.sectionHeader}>
              <Text style={[styles.sectionTitle, { color: theme.text }]}>Thông tin cá nhân</Text>
              {!editing && (
                <Pressable onPress={() => setEditing(true)} hitSlop={8} style={styles.editLink}>
                  <Ionicons name="create-outline" size={16} color={Brand.primary} />
                  <Text style={[styles.editLinkText, { color: Brand.primary }]}>Chỉnh sửa</Text>
                </Pressable>
              )}
            </View>

            {editing ? (
              <>
                <View style={styles.inputGroup}>
                  <Text style={[styles.label, { color: theme.textSecondary }]}>Họ và tên *</Text>
                  <TextInput
                    style={[styles.input, { color: theme.text, backgroundColor: theme.backgroundElement, borderColor: theme.border }]}
                    value={ten}
                    onChangeText={setTen}
                    placeholder="Nguyễn Văn A"
                    placeholderTextColor={theme.textTertiary}
                  />
                </View>
                <View style={styles.inputGroup}>
                  <Text style={[styles.label, { color: theme.textSecondary }]}>Số điện thoại *</Text>
                  <TextInput
                    style={[styles.input, { color: theme.text, backgroundColor: theme.backgroundElement, borderColor: theme.border }]}
                    value={sdt}
                    onChangeText={setSdt}
                    keyboardType="phone-pad"
                    placeholder="0901234567"
                    placeholderTextColor={theme.textTertiary}
                  />
                </View>
                <View style={styles.editActions}>
                  <Pressable
                    style={({ pressed }) => [styles.btnSecondary, { borderColor: theme.border, opacity: pressed ? 0.7 : 1 }]}
                    onPress={handleCancelEdit}
                    disabled={saving}
                  >
                    <Text style={[styles.btnSecondaryText, { color: theme.textSecondary }]}>Hủy</Text>
                  </Pressable>
                  <Pressable
                    style={({ pressed }) => [styles.btnPrimary, { opacity: saving || pressed ? 0.7 : 1 }]}
                    onPress={handleSave}
                    disabled={saving}
                  >
                    <Text style={styles.btnPrimaryText}>{saving ? 'Đang lưu...' : 'Lưu'}</Text>
                  </Pressable>
                </View>
              </>
            ) : (
              <>
                <InfoRow icon="person-outline" label="Họ và tên" value={profile.ten} theme={theme} />
                <InfoRow icon="at-outline" label="Email" value={profile.email} theme={theme} />
                <InfoRow icon="call-outline" label="Số điện thoại" value={profile.sdt} theme={theme} />
                <InfoRow
                  icon="location-outline"
                  label="Địa chỉ mặc định"
                  value={fullAddress || 'Chưa cập nhật'}
                  theme={theme}
                  last
                />
              </>
            )}
          </View>

          {/* Security */}
          <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
            <Text style={[styles.sectionTitle, { color: theme.text }]}>Bảo mật</Text>
            <Pressable
              style={({ pressed }) => [styles.menuRow, { opacity: pressed ? 0.7 : 1 }]}
              onPress={() => router.push('/profile/change-password' as any)}
            >
              <View style={[styles.menuIcon, { backgroundColor: Brand.info + '15' }]}>
                <Ionicons name="lock-closed-outline" size={18} color={Brand.info} />
              </View>
              <Text style={[styles.menuLabel, { color: theme.text }]}>Đổi mật khẩu</Text>
              <Ionicons name="chevron-forward" size={18} color={theme.textTertiary} />
            </Pressable>
          </View>
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

function InfoRow({
  icon,
  label,
  value,
  theme,
  last,
}: {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  value?: string;
  theme: ReturnType<typeof useTheme>;
  last?: boolean;
}) {
  return (
    <View style={[styles.infoRow, !last && { borderBottomColor: theme.borderLight, borderBottomWidth: 1 }]}>
      <Ionicons name={icon} size={18} color={theme.textTertiary} />
      <View style={{ flex: 1 }}>
        <Text style={[styles.infoLabel, { color: theme.textTertiary }]}>{label}</Text>
        <Text style={[styles.infoValue, { color: theme.text }]}>{value || '—'}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    alignItems: 'center',
    paddingBottom: Spacing.four,
    gap: Spacing.one,
  },
  headerBar: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    width: '100%',
    paddingHorizontal: Spacing.three,
    marginBottom: Spacing.three,
  },
  headerTitle: {
    color: '#FFFFFF',
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.bold,
  },
  avatarWrap: { marginBottom: Spacing.one },
  avatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarText: {
    color: '#FFFFFF',
    fontSize: FontSizes['2xl'],
    fontWeight: FontWeights.bold,
  },
  name: {
    color: '#FFFFFF',
    fontSize: FontSizes.xl,
    fontWeight: FontWeights.bold,
  },
  account: {
    color: '#94A3B8',
    fontSize: FontSizes.sm,
  },
  section: {
    marginHorizontal: Spacing.three,
    marginTop: Spacing.three,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    padding: Spacing.three,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.two,
  },
  sectionTitle: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
    marginBottom: Spacing.two,
  },
  editLink: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  editLinkText: { fontSize: FontSizes.sm, fontWeight: FontWeights.semibold },
  infoRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    paddingVertical: Spacing.two + 2,
  },
  infoLabel: { fontSize: FontSizes.xs },
  infoValue: { fontSize: FontSizes.base, fontWeight: FontWeights.medium, marginTop: 2 },
  inputGroup: { marginBottom: Spacing.three, gap: Spacing.one + 2 },
  label: { fontSize: FontSizes.sm, fontWeight: FontWeights.medium },
  input: {
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + 2,
    fontSize: FontSizes.base,
  },
  editActions: { flexDirection: 'row', gap: Spacing.two, marginTop: Spacing.one },
  btnSecondary: {
    flex: 1,
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingVertical: Spacing.three,
    alignItems: 'center',
  },
  btnSecondaryText: { fontSize: FontSizes.base, fontWeight: FontWeights.semibold },
  btnPrimary: {
    flex: 1,
    backgroundColor: Brand.primary,
    borderRadius: BorderRadius.md,
    paddingVertical: Spacing.three,
    alignItems: 'center',
  },
  btnPrimaryText: { color: '#FFFFFF', fontSize: FontSizes.base, fontWeight: FontWeights.bold },
  menuRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.three,
    paddingVertical: Spacing.two,
  },
  menuIcon: {
    width: 36,
    height: 36,
    borderRadius: BorderRadius.md,
    justifyContent: 'center',
    alignItems: 'center',
  },
  menuLabel: { flex: 1, fontSize: FontSizes.base, fontWeight: FontWeights.medium },
});
