/**
 * Loading Spinner Component
 * Animated pulsing indicator with brand color
 */

import React, { useEffect } from 'react';
import { StyleSheet, View } from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withTiming,
  withSequence,
  Easing,
} from 'react-native-reanimated';
import { Brand } from '@/constants/theme';

interface LoadingSpinnerProps {
  size?: number;
  color?: string;
  fullScreen?: boolean;
}

export function LoadingSpinner({
  size = 40,
  color = Brand.primary,
  fullScreen = false,
}: LoadingSpinnerProps) {
  const scale = useSharedValue(1);
  const opacity = useSharedValue(0.6);

  useEffect(() => {
    scale.value = withRepeat(
      withSequence(
        withTiming(1.2, { duration: 600, easing: Easing.inOut(Easing.ease) }),
        withTiming(1, { duration: 600, easing: Easing.inOut(Easing.ease) })
      ),
      -1,
      true
    );
    opacity.value = withRepeat(
      withSequence(
        withTiming(1, { duration: 600 }),
        withTiming(0.6, { duration: 600 })
      ),
      -1,
      true
    );
  }, [scale, opacity]);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: scale.value }],
    opacity: opacity.value,
  }));

  const dots = [0, 1, 2].map((index) => {
    const dotSize = size * 0.25;
    return (
      <Animated.View
        key={index}
        style={[
          {
            width: dotSize,
            height: dotSize,
            borderRadius: dotSize / 2,
            backgroundColor: color,
            marginHorizontal: dotSize * 0.4,
          },
          index === 1 ? animatedStyle : undefined,
        ]}
      />
    );
  });

  if (fullScreen) {
    return (
      <View style={styles.fullScreen}>
        <View style={styles.dotsContainer}>{dots}</View>
      </View>
    );
  }

  return <View style={styles.dotsContainer}>{dots}</View>;
}

const styles = StyleSheet.create({
  fullScreen: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  dotsContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 16,
  },
});
