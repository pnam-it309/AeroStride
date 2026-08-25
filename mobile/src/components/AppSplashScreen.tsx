/**
 * AppSplashScreen - High-end modern startup / loading screen
 * Inspired by the AeroStride web preloader with dark aesthetics,
 * pulsing brand logo, percentage counter and smooth fade-out transition.
 */

import React, { useEffect, useRef, useState } from 'react';
import {
  Animated,
  Dimensions,
  Easing,
  Image,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import * as SplashScreen from 'expo-splash-screen';
import { LinearGradient } from 'expo-linear-gradient';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';

const { width: SCREEN_WIDTH, height: SCREEN_HEIGHT } = Dimensions.get('window');

// Keep native splash visible while custom animated screen prepares
SplashScreen.preventAutoHideAsync().catch(() => {});

export function AppSplashScreen({ onFinish }: { onFinish?: () => void }) {
  const [percent, setPercent] = useState(0);
  const [isReadyToDismiss, setIsReadyToDismiss] = useState(false);

  const containerOpacity = useRef(new Animated.Value(1)).current;
  const logoScale = useRef(new Animated.Value(0.85)).current;
  const logoOpacity = useRef(new Animated.Value(0)).current;
  const glowOpacity = useRef(new Animated.Value(0.3)).current;
  const progressAnim = useRef(new Animated.Value(0)).current;

  useEffect(() => {
    // Hide native splash immediately since custom screen is rendering
    SplashScreen.hideAsync().catch(() => {});

    // 1. Entrance animation for Logo & Glow
    Animated.parallel([
      Animated.timing(logoOpacity, {
        toValue: 1,
        duration: 500,
        useNativeDriver: true,
      }),
      Animated.spring(logoScale, {
        toValue: 1,
        friction: 6,
        tension: 40,
        useNativeDriver: true,
      }),
    ]).start();

    // 2. Continuous pulse animation for ambient glow
    Animated.loop(
      Animated.sequence([
        Animated.timing(glowOpacity, {
          toValue: 0.8,
          duration: 900,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
        Animated.timing(glowOpacity, {
          toValue: 0.3,
          duration: 900,
          easing: Easing.inOut(Easing.ease),
          useNativeDriver: true,
        }),
      ])
    ).start();

    // 3. Progress simulation (0% -> 100%)
    let current = 0;
    const interval = setInterval(() => {
      current += Math.floor(Math.random() * 18) + 8;
      if (current >= 100) {
        current = 100;
        setPercent(100);
        clearInterval(interval);

        Animated.timing(progressAnim, {
          toValue: 1,
          duration: 250,
          useNativeDriver: false,
        }).start(() => {
          setTimeout(() => {
            setIsReadyToDismiss(true);
            // 4. Smooth Exit Animation
            Animated.timing(containerOpacity, {
              toValue: 0,
              duration: 450,
              easing: Easing.out(Easing.ease),
              useNativeDriver: true,
            }).start(() => {
              if (onFinish) onFinish();
            });
          }, 300);
        });
      } else {
        setPercent(current);
        Animated.timing(progressAnim, {
          toValue: current / 100,
          duration: 60,
          useNativeDriver: false,
        }).start();
      }
    }, 60);

    return () => {
      clearInterval(interval);
    };
  }, []);

  const progressWidth = progressAnim.interpolate({
    inputRange: [0, 1],
    outputRange: ['0%', '100%'],
  });

  return (
    <Animated.View
      style={[
        styles.container,
        {
          opacity: containerOpacity,
        },
      ]}
      pointerEvents="none"
    >
      <LinearGradient
        colors={['#070B14', '#0F172A', '#070B14']}
        start={{ x: 0, y: 0 }}
        end={{ x: 1, y: 1 }}
        style={StyleSheet.absoluteFill}
      />

      {/* Background Ambient Glow */}
      <Animated.View
        style={[
          styles.ambientGlow,
          {
            opacity: glowOpacity,
          },
        ]}
      >
        <LinearGradient
          colors={['rgba(32, 138, 239, 0.25)', 'transparent']}
          style={styles.glowCircle}
        />
      </Animated.View>

      {/* Center Content */}
      <View style={styles.centerContent}>
        {/* Animated Brand Logo Container */}
        <Animated.View
          style={[
            styles.logoWrapper,
            {
              opacity: logoOpacity,
              transform: [{ scale: logoScale }],
            },
          ]}
        >
          <View style={styles.logoCard}>
            <Image
              source={require('../../assets/images/logoclient.jpg')}
              style={styles.logoImage}
              resizeMode="contain"
            />
          </View>
        </Animated.View>

        {/* Brand Name Typography */}
        <Animated.View style={[styles.textBlock, { opacity: logoOpacity }]}>
          <Text style={styles.brandTitle}>AEROSTRIDE</Text>
          <View style={styles.taglineBadge}>
            <Text style={styles.brandTagline}>SPEED • INNOVATION • PERFORMANCE</Text>
          </View>
        </Animated.View>
      </View>

      {/* Bottom Loading Progress Bar */}
      <View style={styles.bottomSection}>
        <View style={styles.progressRow}>
          <View style={styles.statusRow}>
            <View
              style={[
                styles.pulseDot,
                { backgroundColor: percent >= 100 ? '#10B981' : Brand.primary },
              ]}
            />
            <Text style={styles.statusText}>
              {percent >= 100 ? 'HỆ THỐNG SẴN SÀNG' : 'ĐANG TẢI DỮ LIỆU...'}
            </Text>
          </View>
          <Text style={styles.percentText}>{percent}%</Text>
        </View>

        <View style={styles.progressBarTrack}>
          <Animated.View style={[styles.progressBarFill, { width: progressWidth }]}>
            <LinearGradient
              colors={[Brand.primary, '#38BDF8']}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 0 }}
              style={StyleSheet.absoluteFill}
            />
          </Animated.View>
        </View>

        <Text style={styles.footerVersion}>AEROSTRIDE MOBILE v1.0.0</Text>
      </View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  container: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    zIndex: 99999,
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: Spacing.seven,
    backgroundColor: '#070B14',
  },
  ambientGlow: {
    position: 'absolute',
    top: SCREEN_HEIGHT * 0.25,
    width: SCREEN_WIDTH * 1.2,
    height: SCREEN_WIDTH * 1.2,
    alignItems: 'center',
    justifyContent: 'center',
  },
  glowCircle: {
    width: '100%',
    height: '100%',
    borderRadius: SCREEN_WIDTH * 0.6,
  },
  centerContent: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    width: '100%',
  },
  logoWrapper: {
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: Spacing.four,
  },
  logoCard: {
    width: 140,
    height: 140,
    borderRadius: 28,
    backgroundColor: '#FFFFFF',
    padding: 12,
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: Brand.primary,
    shadowOffset: { width: 0, height: 12 },
    shadowOpacity: 0.4,
    shadowRadius: 24,
    elevation: 12,
  },
  logoImage: {
    width: '100%',
    height: '100%',
  },
  textBlock: {
    alignItems: 'center',
    marginTop: Spacing.two,
  },
  brandTitle: {
    color: '#FFFFFF',
    fontSize: FontSizes['2xl'] + 2,
    fontWeight: FontWeights.extrabold,
    letterSpacing: 4,
    marginBottom: Spacing.two,
  },
  taglineBadge: {
    backgroundColor: 'rgba(32, 138, 239, 0.12)',
    borderColor: 'rgba(56, 189, 248, 0.3)',
    borderWidth: 1,
    paddingHorizontal: Spacing.three,
    paddingVertical: 4,
    borderRadius: BorderRadius.full,
  },
  brandTagline: {
    color: '#38BDF8',
    fontSize: 10,
    fontWeight: FontWeights.bold,
    letterSpacing: 1.5,
  },
  bottomSection: {
    width: '100%',
    paddingHorizontal: Spacing.five,
    alignItems: 'center',
    gap: Spacing.two,
  },
  progressRow: {
    width: '100%',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.one + 2,
  },
  pulseDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  statusText: {
    color: '#94A3B8',
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.semibold,
    letterSpacing: 0.5,
  },
  percentText: {
    color: '#38BDF8',
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
    fontVariant: ['tabular-nums'],
  },
  progressBarTrack: {
    width: '100%',
    height: 4,
    borderRadius: 2,
    backgroundColor: 'rgba(255, 255, 255, 0.1)',
    overflow: 'hidden',
  },
  progressBarFill: {
    height: '100%',
    borderRadius: 2,
  },
  footerVersion: {
    color: '#475569',
    fontSize: 10,
    letterSpacing: 1,
    marginTop: Spacing.two,
  },
});
