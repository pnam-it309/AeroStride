/**
 * Feedback Context - Custom Toast & Confirm Dialog
 * Project-styled replacement for the default system Alert,
 * following the AeroStride design system (Brand / Spacing / BorderRadius / Fonts)
 */

import React, {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react';
import { Animated, Easing, Modal, Pressable, StyleSheet, Text, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Ionicons from '@expo/vector-icons/Ionicons';
import { Brand, BorderRadius, FontSizes, FontWeights, Spacing } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

export type FeedbackType = 'success' | 'error' | 'warning' | 'info';

export interface ToastOptions {
  type?: FeedbackType;
  title?: string;
  message: string;
  /** Auto-dismiss duration in ms (default 2600) */
  duration?: number;
}

export interface ConfirmOptions {
  title: string;
  message?: string;
  confirmText?: string;
  cancelText?: string;
  /** Style the confirm action as destructive (red) */
  destructive?: boolean;
}

interface FeedbackContextValue {
  showToast: (options: ToastOptions) => void;
  /** Resolves true when confirmed, false when cancelled/dismissed */
  confirm: (options: ConfirmOptions) => Promise<boolean>;
}

const FeedbackContext = createContext<FeedbackContextValue | undefined>(undefined);

const TOAST_ICONS: Record<FeedbackType, keyof typeof Ionicons.glyphMap> = {
  success: 'checkmark-circle',
  error: 'close-circle',
  warning: 'warning',
  info: 'information-circle',
};

const TOAST_COLORS: Record<FeedbackType, string> = {
  success: Brand.success,
  error: Brand.error,
  warning: Brand.warning,
  info: Brand.info,
};

export function FeedbackProvider({ children }: { children: React.ReactNode }) {
  const theme = useTheme();
  const insets = useSafeAreaInsets();

  // ---------- Toast ----------
  interface ToastState {
    id: number;
    type: FeedbackType;
    title?: string;
    message: string;
  }
  const [toastState, setToastState] = useState<ToastState | null>(null);
  const toastAnim = useMemo(() => new Animated.Value(0), []);
  const toastTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(
    () => () => {
      if (toastTimer.current) clearTimeout(toastTimer.current);
    },
    []
  );

  const showToast = useCallback(
    ({ type = 'info', title, message, duration = 2600 }: ToastOptions) => {
      if (toastTimer.current) clearTimeout(toastTimer.current);
      setToastState({ id: Date.now(), type, title, message });
      toastAnim.setValue(0);
      Animated.timing(toastAnim, {
        toValue: 1,
        duration: 220,
        easing: Easing.out(Easing.cubic),
        useNativeDriver: true,
      }).start();
      toastTimer.current = setTimeout(() => {
        Animated.timing(toastAnim, {
          toValue: 0,
          duration: 200,
          easing: Easing.in(Easing.cubic),
          useNativeDriver: true,
        }).start(({ finished }) => {
          if (finished) setToastState(null);
        });
      }, duration);
    },
    [toastAnim]
  );

  // ---------- Confirm dialog ----------
  interface DialogState extends ConfirmOptions {
    id: number;
  }
  const [dialog, setDialog] = useState<DialogState | null>(null);
  const resolverRef = useRef<((value: boolean) => void) | null>(null);
  const dialogAnim = useMemo(() => new Animated.Value(0), []);

  const confirm = useCallback(
    ({ title, message, confirmText, cancelText, destructive = false }: ConfirmOptions) =>
      new Promise<boolean>((resolve) => {
        // Resolve any pending dialog as dismissed before showing a new one
        resolverRef.current?.(false);
        resolverRef.current = resolve;
        setDialog({ id: Date.now(), title, message, confirmText, cancelText, destructive });
        dialogAnim.setValue(0);
        Animated.timing(dialogAnim, {
          toValue: 1,
          duration: 200,
          easing: Easing.out(Easing.cubic),
          useNativeDriver: true,
        }).start();
      }),
    [dialogAnim]
  );

  const settle = useCallback(
    (result: boolean) => {
      resolverRef.current?.(result);
      resolverRef.current = null;
      Animated.timing(dialogAnim, {
        toValue: 0,
        duration: 150,
        easing: Easing.in(Easing.cubic),
        useNativeDriver: true,
      }).start(({ finished }) => {
        if (finished) setDialog(null);
      });
    },
    [dialogAnim]
  );

  return (
    <FeedbackContext.Provider value={{ showToast, confirm }}>
      {children}

      {/* Toast */}
      {toastState && (
        <Animated.View
          pointerEvents="none"
          style={[
            styles.toast,
            {
              top: insets.top + Spacing.two,
              opacity: toastAnim,
              transform: [
                {
                  translateY: toastAnim.interpolate({
                    inputRange: [0, 1],
                    outputRange: [-16, 0],
                  }),
                },
              ],
              backgroundColor: theme.surfaceElevated,
              borderColor: theme.borderLight,
            },
          ]}
        >
          <View
            style={[styles.toastIconWrap, { backgroundColor: TOAST_COLORS[toastState.type] + '1A' }]}
          >
            <Ionicons
              name={TOAST_ICONS[toastState.type]}
              size={18}
              color={TOAST_COLORS[toastState.type]}
            />
          </View>
          <View style={styles.toastContent}>
            {toastState.title ? (
              <Text style={[styles.toastTitle, { color: theme.text }]} numberOfLines={1}>
                {toastState.title}
              </Text>
            ) : null}
            <Text style={[styles.toastMessage, { color: theme.textSecondary }]} numberOfLines={3}>
              {toastState.message}
            </Text>
          </View>
        </Animated.View>
      )}

      {/* Confirm dialog */}
      <Modal
        transparent
        visible={dialog !== null}
        animationType="none"
        statusBarTranslucent
        onRequestClose={() => settle(false)}
      >
        <Animated.View style={[styles.overlay, { opacity: dialogAnim }]}>
          <Pressable style={StyleSheet.absoluteFill} onPress={() => settle(false)} />
          <Animated.View
            style={[
              styles.dialogCard,
              {
                opacity: dialogAnim,
                transform: [
                  {
                    scale: dialogAnim.interpolate({
                      inputRange: [0, 1],
                      outputRange: [0.92, 1],
                    }),
                  },
                ],
                backgroundColor: theme.surfaceElevated,
              },
            ]}
          >
            <View
              style={[
                styles.dialogIconWrap,
                {
                  backgroundColor: dialog?.destructive
                    ? Brand.error + '1A'
                    : Brand.primary + '1A',
                },
              ]}
            >
              <Ionicons
                name={dialog?.destructive ? 'alert-circle' : 'help-circle'}
                size={26}
                color={dialog?.destructive ? Brand.error : Brand.primary}
              />
            </View>

            <Text style={[styles.dialogTitle, { color: theme.text }]}>{dialog?.title}</Text>
            {dialog?.message ? (
              <Text style={[styles.dialogMessage, { color: theme.textSecondary }]}>
                {dialog.message}
              </Text>
            ) : null}

            <View style={styles.dialogActions}>
              <Pressable
                style={({ pressed }) => [
                  styles.dialogBtn,
                  styles.dialogBtnCancel,
                  { borderColor: theme.borderLight, opacity: pressed ? 0.7 : 1 },
                ]}
                onPress={() => settle(false)}
              >
                <Text style={[styles.dialogBtnCancelText, { color: theme.textSecondary }]}>
                  {dialog?.cancelText || 'Hủy'}
                </Text>
              </Pressable>
              <Pressable
                style={({ pressed }) => [
                  styles.dialogBtn,
                  {
                    backgroundColor: dialog?.destructive ? Brand.error : Brand.primary,
                    opacity: pressed ? 0.85 : 1,
                  },
                ]}
                onPress={() => settle(true)}
              >
                <Text style={styles.dialogBtnConfirmText}>{dialog?.confirmText || 'Xác nhận'}</Text>
              </Pressable>
            </View>
          </Animated.View>
        </Animated.View>
      </Modal>
    </FeedbackContext.Provider>
  );
}

export function useFeedback(): FeedbackContextValue {
  const context = useContext(FeedbackContext);
  if (!context) {
    throw new Error('useFeedback must be used within a FeedbackProvider');
  }
  return context;
}

const styles = StyleSheet.create({
  // Toast
  toast: {
    position: 'absolute',
    left: Spacing.three,
    right: Spacing.three,
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two + 2,
    padding: Spacing.two + 4,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
    shadowColor: '#0F172A',
    shadowOpacity: 0.12,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 6 },
    elevation: 6,
    zIndex: 999,
  },
  toastIconWrap: {
    width: 32,
    height: 32,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  toastContent: {
    flex: 1,
    gap: 1,
  },
  toastTitle: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
  },
  toastMessage: {
    fontSize: FontSizes.sm,
    lineHeight: 18,
  },

  // Dialog
  overlay: {
    flex: 1,
    backgroundColor: 'rgba(15, 23, 42, 0.55)',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: Spacing.four,
  },
  dialogCard: {
    width: '100%',
    maxWidth: 360,
    alignItems: 'center',
    paddingTop: Spacing.four,
    paddingHorizontal: Spacing.four,
    paddingBottom: Spacing.three,
    borderRadius: BorderRadius['2xl'],
    borderWidth: 1,
    borderColor: 'rgba(255,255,255,0.8)',
    shadowColor: '#0F172A',
    shadowOpacity: 0.25,
    shadowRadius: 24,
    shadowOffset: { width: 0, height: 12 },
    elevation: 12,
  },
  dialogIconWrap: {
    width: 52,
    height: 52,
    borderRadius: 26,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: Spacing.two + 2,
  },
  dialogTitle: {
    fontSize: FontSizes.md,
    fontWeight: FontWeights.bold,
    textAlign: 'center',
  },
  dialogMessage: {
    fontSize: FontSizes.sm,
    lineHeight: 20,
    textAlign: 'center',
    marginTop: Spacing.one + 2,
  },
  dialogActions: {
    flexDirection: 'row',
    alignSelf: 'stretch',
    gap: Spacing.two,
    marginTop: Spacing.four,
  },
  dialogBtn: {
    flex: 1,
    paddingVertical: Spacing.two + 4,
    borderRadius: BorderRadius.lg,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dialogBtnCancel: {
    borderWidth: 1,
  },
  dialogBtnCancelText: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.semibold,
  },
  dialogBtnConfirmText: {
    color: '#FFFFFF',
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
  },
});
