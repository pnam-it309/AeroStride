/**
 * Linear Progress Bar Component for Mobile
 * Smooth horizontal loading bar using react-native-reanimated
 */

import React, { useEffect } from 'react';
import { StyleSheet, View } from 'react-native';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withTiming,
  Easing,
} from 'react-native-reanimated';
import { Brand } from '@/constants/theme';

interface LinearProgressBarProps {
  loading?: boolean;
  color?: string;
  height?: number;
  backgroundColor?: string;
}

export function LinearProgressBar({
  loading = true,
  color = Brand.primary,
  height = 3,
  backgroundColor = 'transparent',
}: LinearProgressBarProps) {
  const translateX = useSharedValue(-100);

  useEffect(() => {
    if (loading) {
      translateX.value = -100;
      translateX.value = withRepeat(
        withTiming(100, {
          duration: 900,
          easing: Easing.bezier(0.4, 0.0, 0.2, 1),
        }),
        -1,
        false
      );
    }
  }, [loading, translateX]);

  const animatedStyle = useAnimatedStyle(() => ({
    transform: [{ translateX: `${translateX.value}%` as any }],
  }));

  if (!loading) {
    return <View style={{ height }} />;
  }

  return (
    <View style={[styles.container, { height, backgroundColor }]}>
      <Animated.View
        style={[
          styles.bar,
          {
            height,
            backgroundColor: color,
          },
          animatedStyle,
        ]}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    width: '100%',
    overflow: 'hidden',
    position: 'relative',
  },
  bar: {
    width: '50%',
    borderRadius: 2,
    position: 'absolute',
    left: '25%',
  },
});
