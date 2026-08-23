import React from 'react';
import { StyleSheet, Pressable, View, Text, Platform } from 'react-native';
import { useRouter, usePathname } from 'expo-router';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, BorderRadius, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

interface FloatingChatButtonProps {
  bottomOffset?: number;
}

export function FloatingChatButton({ bottomOffset = 80 }: FloatingChatButtonProps) {
  const router = useRouter();
  const pathname = usePathname();
  const theme = useTheme();

  // Ẩn nút nếu đang ở chính trang chat
  if (pathname === '/chat') {
    return null;
  }

  return (
    <View style={[styles.wrapper, { bottom: bottomOffset }]} pointerEvents="box-none">
      <Pressable
        style={({ pressed }) => [
          styles.container,
          {
            transform: [{ scale: pressed ? 0.92 : 1 }],
          },
        ]}
        onPress={() => router.push('/chat' as any)}
        accessibilityLabel="Chat với Trợ lý AI AeroStride"
        accessibilityRole="button"
      >
        {/* Glowing aura effect */}
        <View style={styles.glowAura} />

        <LinearGradient
          colors={[Brand.primaryLight, Brand.primary, Brand.primaryDark]}
          start={{ x: 0, y: 0 }}
          end={{ x: 1, y: 1 }}
          style={styles.gradient}
        >
          <View style={styles.iconContainer}>
            <Ionicons name="chatbubbles" size={24} color="#FFFFFF" />
            <View style={styles.sparkleBadge}>
              <Ionicons name="sparkles" size={10} color="#FFD700" />
            </View>
          </View>
          
          <View style={styles.onlineDot} />
        </LinearGradient>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    position: 'absolute',
    right: Spacing.four,
    zIndex: 9999,
  },
  container: {
    width: 56,
    height: 56,
    borderRadius: BorderRadius.full,
    justifyContent: 'center',
    alignItems: 'center',
    ...Platform.select({
      ios: {
        shadowColor: Brand.primary,
        shadowOffset: { width: 0, height: 6 },
        shadowOpacity: 0.45,
        shadowRadius: 10,
      },
      android: {
        elevation: 8,
      },
      default: {
        filter: 'drop-shadow(0px 6px 12px rgba(32, 138, 239, 0.4))',
      },
    }),
  },
  glowAura: {
    position: 'absolute',
    width: 64,
    height: 64,
    borderRadius: BorderRadius.full,
    backgroundColor: Brand.primary,
    opacity: 0.2,
  },
  gradient: {
    width: 54,
    height: 54,
    borderRadius: 27,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1.5,
    borderColor: 'rgba(255, 255, 255, 0.6)',
  },
  iconContainer: {
    position: 'relative',
    justifyContent: 'center',
    alignItems: 'center',
  },
  sparkleBadge: {
    position: 'absolute',
    top: -6,
    right: -6,
    backgroundColor: '#0F172A',
    borderRadius: 8,
    padding: 2,
    borderWidth: 1,
    borderColor: '#FFD700',
  },
  onlineDot: {
    position: 'absolute',
    bottom: 4,
    right: 4,
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: '#10B981',
    borderWidth: 2,
    borderColor: '#FFFFFF',
  },
});
