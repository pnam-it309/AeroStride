/**
 * Account Screen - Profile, Orders, Settings
 */

import React from 'react';
import { StyleSheet, View, Text, Pressable, ScrollView, Alert } from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { useAuth } from '@/context/AuthContext';

interface MenuItemProps {
  icon: keyof typeof Ionicons.glyphMap;
  label: string;
  subtitle?: string;
  onPress: () => void;
  color?: string;
  showBadge?: boolean;
}

function MenuItem({ icon, label, subtitle, onPress, color, showBadge }: MenuItemProps) {
  const theme = useTheme();
  return (
    <Pressable
      style={({ pressed }) => [
        styles.menuItem,
        { backgroundColor: theme.surface, borderColor: theme.border, opacity: pressed ? 0.9 : 1 },
      ]}
      onPress={onPress}
    >
      <View style={[styles.menuIcon, { backgroundColor: (color || Brand.primary) + '15' }]}>
        <Ionicons name={icon} size={20} color={color || Brand.primary} />
      </View>
      <View style={styles.menuContent}>
        <Text style={[styles.menuLabel, { color: theme.text }]}>{label}</Text>
        {subtitle && (
          <Text style={[styles.menuSubtitle, { color: theme.textSecondary }]}>{subtitle}</Text>
        )}
      </View>
      <Ionicons name="chevron-forward" size={18} color={theme.textTertiary} />
    </Pressable>
  );
}

export default function AccountScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { user, isAuthenticated, logout } = useAuth();

  const handleLogout = () => {
    Alert.alert('Đăng xuất', 'Bạn có chắc muốn đăng xuất?', [
      { text: 'Hủy', style: 'cancel' },
      {
        text: 'Đăng xuất',
        style: 'destructive',
        onPress: () => logout(),
      },
    ]);
  };

  return (
    <ScrollView
      style={[styles.container, { backgroundColor: theme.background }]}
      contentContainerStyle={{ paddingBottom: 100 }}
      showsVerticalScrollIndicator={false}
    >
      {/* Profile Header */}
      <LinearGradient
        colors={['#0B1120', '#152238', '#1E3A5F']}
        style={[styles.profileHeader, { paddingTop: insets.top + Spacing.three }]}
      >
        {isAuthenticated && user ? (
          <>
            <View style={styles.avatarContainer}>
              <LinearGradient
                colors={[Brand.primary, Brand.primaryDark]}
                style={styles.avatar}
              >
                <Text style={styles.avatarText}>
                  {user.username.charAt(0).toUpperCase()}
                </Text>
              </LinearGradient>
            </View>
            <Text style={styles.username}>{user.username}</Text>
            <Text style={styles.role}>
              {user.role === 'ROLE_KHACH_HANG' ? 'Khách hàng' : user.role}
            </Text>
            <Pressable
              style={({ pressed }) => [styles.editProfileBtn, { opacity: pressed ? 0.8 : 1 }]}
              onPress={() => router.push('/profile' as any)}
            >
              <Ionicons name="create-outline" size={14} color="#FFFFFF" />
              <Text style={styles.editProfileText}>Chỉnh sửa hồ sơ</Text>
            </Pressable>
          </>
        ) : (
          <>
            <View style={styles.avatarContainer}>
              <View style={[styles.avatar, { backgroundColor: 'rgba(255,255,255,0.1)' }]}>
                <Ionicons name="person-outline" size={32} color="#94A3B8" />
              </View>
            </View>
            <Text style={styles.username}>Chào bạn!</Text>
            <Pressable
              style={({ pressed }) => [styles.loginButton, { opacity: pressed ? 0.8 : 1 }]}
              onPress={() => router.push('/login')}
            >
              <Text style={styles.loginButtonText}>Đăng nhập</Text>
            </Pressable>
          </>
        )}
      </LinearGradient>

      {/* Menu Items */}
      <View style={styles.menuSection}>
        <Text style={[styles.sectionTitle, { color: theme.textSecondary }]}>Đơn hàng</Text>
        <MenuItem
          icon="receipt-outline"
          label="Đơn hàng của tôi"
          subtitle="Xem tất cả đơn hàng"
          onPress={() => {
            if (!isAuthenticated) {
              router.push('/login');
              return;
            }
            router.push('/orders');
          }}
        />
      </View>

      <View style={styles.menuSection}>
        <Text style={[styles.sectionTitle, { color: theme.textSecondary }]}>Khuyến mãi</Text>
        <MenuItem
          icon="pricetag-outline"
          label="Voucher"
          subtitle="Xem voucher khả dụng"
          onPress={() => router.push('/vouchers' as any)}
          color={Brand.accent}
        />
      </View>

      <View style={styles.menuSection}>
        <Text style={[styles.sectionTitle, { color: theme.textSecondary }]}>Cài đặt</Text>
        <MenuItem
          icon="chatbubbles-outline"
          label="Chat hỗ trợ"
          subtitle="Trò chuyện với chúng tôi"
          onPress={() => router.push('/chat' as any)}
          color={Brand.primary}
        />
        <MenuItem
          icon="notifications-outline"
          label="Thông báo"
          subtitle="Quản lý thông báo"
          onPress={() => router.push('/notifications' as any)}
          color={Brand.info}
        />
        <MenuItem
          icon="help-circle-outline"
          label="Trợ giúp"
          subtitle="Trung tâm hỗ trợ"
          onPress={() => router.push('/help' as any)}
          color={Brand.success}
        />
        <MenuItem
          icon="information-circle-outline"
          label="Về AeroStride"
          subtitle="Phiên bản 1.0.0"
          onPress={() => router.push('/about' as any)}
          color={Brand.warning}
        />
      </View>

      {isAuthenticated && (
        <View style={styles.menuSection}>
          <Pressable
            style={({ pressed }) => [
              styles.logoutButton,
              { borderColor: Brand.error + '40', opacity: pressed ? 0.8 : 1 },
            ]}
            onPress={handleLogout}
          >
            <Ionicons name="log-out-outline" size={20} color={Brand.error} />
            <Text style={[styles.logoutText, { color: Brand.error }]}>Đăng xuất</Text>
          </Pressable>
        </View>
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  profileHeader: {
    alignItems: 'center',
    paddingBottom: Spacing.five,
    gap: Spacing.two,
  },
  avatarContainer: {
    marginBottom: Spacing.one,
  },
  avatar: {
    width: 72,
    height: 72,
    borderRadius: 36,
    justifyContent: 'center',
    alignItems: 'center',
  },
  avatarText: {
    color: '#FFFFFF',
    fontSize: FontSizes['2xl'],
    fontWeight: FontWeights.bold,
  },
  username: {
    color: '#FFFFFF',
    fontSize: FontSizes.xl,
    fontWeight: FontWeights.bold,
  },
  role: {
    color: '#94A3B8',
    fontSize: FontSizes.sm,
  },
  editProfileBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    backgroundColor: 'rgba(255,255,255,0.12)',
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.one + 2,
    borderRadius: BorderRadius.full,
    marginTop: Spacing.one,
  },
  editProfileText: {
    color: '#FFFFFF',
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.semibold,
  },
  loginButton: {
    backgroundColor: Brand.primary,
    paddingHorizontal: Spacing.four,
    paddingVertical: Spacing.two,
    borderRadius: BorderRadius.full,
    marginTop: Spacing.one,
  },
  loginButtonText: {
    color: '#FFFFFF',
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
  },
  menuSection: {
    paddingHorizontal: Spacing.three,
    marginTop: Spacing.four,
    gap: Spacing.two,
  },
  sectionTitle: {
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.semibold,
    textTransform: 'uppercase',
    letterSpacing: 1,
    paddingLeft: Spacing.one,
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: Spacing.three,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    gap: Spacing.three,
  },
  menuIcon: {
    width: 40,
    height: 40,
    borderRadius: BorderRadius.md,
    justifyContent: 'center',
    alignItems: 'center',
  },
  menuContent: {
    flex: 1,
    gap: 2,
  },
  menuLabel: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
  },
  menuSubtitle: {
    fontSize: FontSizes.xs,
  },
  logoutButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    paddingVertical: Spacing.three,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    gap: Spacing.two,
  },
  logoutText: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
  },
});
