/**
 * Support Chat Screen - AeroStride AI & Staff Chat
 * Designed with custom AeroStride Glassmorphism & Brand aesthetics
 */

import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  StyleSheet,
  View,
  Text,
  TextInput,
  Pressable,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  ActivityIndicator,
  Keyboard,
} from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { LinearGradient } from 'expo-linear-gradient';
import { Ionicons, MaterialCommunityIcons, Feather } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { chatService, type ChatMessage, SENDER_CUSTOMER } from '@/services/chatService';
import { ChatSocket } from '@/services/chatSocket';

const SENDER_BOT = 'bot';
const SENDER_STAFF = 'staff';
const POLL_DELAYS = [1200, 2600, 4200, 6000];

export default function ChatScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const listRef = useRef<FlatList<ChatMessage>>(null);
  const pollTimers = useRef<ReturnType<typeof setTimeout>[]>([]);
  const socketRef = useRef<ChatSocket | null>(null);
  const sessionIdRef = useRef<string | null>(null);

  const [sessionId, setSessionId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [suggestions, setSuggestions] = useState<string[]>([]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [connected, setConnected] = useState(false);
  const [isKeyboardVisible, setIsKeyboardVisible] = useState(false);

  useEffect(() => {
    const showSub = Keyboard.addListener(
      Platform.OS === 'ios' ? 'keyboardWillShow' : 'keyboardDidShow',
      () => {
        setIsKeyboardVisible(true);
        setTimeout(() => {
          listRef.current?.scrollToEnd({ animated: true });
        }, 100);
      }
    );
    const hideSub = Keyboard.addListener(
      Platform.OS === 'ios' ? 'keyboardWillHide' : 'keyboardDidHide',
      () => {
        setIsKeyboardVisible(false);
      }
    );
    return () => {
      showSub.remove();
      hideSub.remove();
    };
  }, []);

  const mergeMessage = useCallback((incoming: ChatMessage) => {
    setMessages((prev) => {
      if (prev.some((m) => m.id === incoming.id)) return prev;
      if (incoming.sender === SENDER_CUSTOMER) {
        const idx = prev.findIndex(
          (m) => m.id.startsWith('local-') && m.text === incoming.text
        );
        if (idx !== -1) {
          const next = [...prev];
          next[idx] = incoming;
          return next;
        }
      }
      return [...prev, incoming];
    });
  }, []);

  const reloadHistory = useCallback(async (sid: string) => {
    try {
      const history = await chatService.getHistory(sid);
      setMessages((prev) => {
        const pending = prev.filter(
          (m) => m.id.startsWith('local-') && !history.some((h) => h.text === m.text)
        );
        return [...history, ...pending];
      });
    } catch {
      // keep current messages on transient errors
    }
  }, []);

  const schedulePolls = useCallback(
    (sid: string) => {
      pollTimers.current.forEach(clearTimeout);
      pollTimers.current = POLL_DELAYS.map((delay) =>
        setTimeout(() => reloadHistory(sid), delay)
      );
    },
    [reloadHistory]
  );

  useEffect(() => {
    let active = true;
    const socket = new ChatSocket();
    socketRef.current = socket;

    (async () => {
      const sid = await chatService.getSessionId();
      if (!active) return;
      setSessionId(sid);
      sessionIdRef.current = sid;

      const [history, sugg] = await Promise.all([
        chatService.getHistory(sid).catch(() => [] as ChatMessage[]),
        chatService.getWelcomeSuggestions(sid),
      ]);
      if (!active) return;
      setMessages(history);
      setSuggestions(sugg);

      socket.connect({
        onStatusChange: (isUp) => active && setConnected(isUp),
        onMessage: (msg) => {
          if (!active) return;
          if (msg.sessionId && msg.sessionId !== sessionIdRef.current) return;
          mergeMessage(msg);
        },
      });
    })();

    return () => {
      active = false;
      pollTimers.current.forEach(clearTimeout);
      socket.disconnect();
    };
  }, [mergeMessage]);

  const send = async (textToSend?: string) => {
    const content = (textToSend ?? input).trim();
    if (!content || !sessionId || sending) return;

    if (!textToSend) setInput('');
    setSending(true);

    const optimistic: ChatMessage = {
      id: `local-${Date.now()}`,
      sender: SENDER_CUSTOMER,
      text: content,
      time: new Date().toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' }),
    };
    setMessages((prev) => [...prev, optimistic]);

    try {
      await chatService.sendMessage(sessionId, content);
      schedulePolls(sessionId);
    } catch (err) {
      console.warn('Failed to send chat message:', err);
    } finally {
      setSending(false);
    }
  };

  const renderItem = ({ item }: { item: ChatMessage }) => {
    const isMine = item.sender === SENDER_CUSTOMER;
    const isBot = item.sender === SENDER_BOT || item.sender === 'bot';
    const isStaff = item.sender === SENDER_STAFF || item.sender === 'staff';

    if (item.sender === 'system') {
      return (
        <View style={styles.systemWrap}>
          <Text style={[styles.systemText, { color: theme.textTertiary }]}>{item.text}</Text>
        </View>
      );
    }

    return (
      <View style={[styles.bubbleRow, isMine ? styles.rowMine : styles.rowOther]}>
        {!isMine && (
          <View style={[styles.avatarWrap, isBot ? styles.botAvatarWrap : styles.staffAvatarWrap]}>
            {isBot ? (
              <LinearGradient
                colors={[Brand.primaryLight, Brand.primaryDark]}
                style={styles.avatarGradient}
              >
                <MaterialCommunityIcons name="robot-outline" size={16} color="#FFFFFF" />
              </LinearGradient>
            ) : (
              <LinearGradient
                colors={[Brand.accent, Brand.accentLight]}
                style={styles.avatarGradient}
              >
                <Ionicons name="headset" size={15} color="#FFFFFF" />
              </LinearGradient>
            )}
          </View>
        )}

        <View style={styles.bubbleContainer}>
          {!isMine && (
            <View style={styles.senderHeader}>
              <Text style={[styles.senderName, { color: isBot ? Brand.primary : Brand.accent }]}>
                {isBot ? 'AeroStride AI' : 'Chăm sóc khách hàng'}
              </Text>
              {isBot && (
                <View style={styles.aiTag}>
                  <Ionicons name="sparkles" size={10} color="#FFD700" />
                  <Text style={styles.aiTagText}>AI</Text>
                </View>
              )}
            </View>
          )}

          {isMine ? (
            <LinearGradient
              colors={[Brand.primary, Brand.primaryDark]}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={[styles.bubble, styles.bubbleMine]}
            >
              <Text style={styles.bubbleTextMine}>{item.text}</Text>
              {item.time ? (
                <Text style={styles.bubbleTimeMine}>{item.time}</Text>
              ) : null}
            </LinearGradient>
          ) : (
            <View style={[styles.bubble, styles.bubbleOther, { backgroundColor: theme.surfaceElevated, borderColor: theme.borderLight }]}>
              <Text style={[styles.bubbleTextOther, { color: theme.text }]}>{item.text}</Text>
              {item.time ? (
                <Text style={[styles.bubbleTimeOther, { color: theme.textTertiary }]}>{item.time}</Text>
              ) : null}
            </View>
          )}
        </View>
      </View>
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      {/* Signature AeroStride Header */}
      <View style={[styles.header, { paddingTop: insets.top + Spacing.two, borderBottomColor: theme.borderLight, backgroundColor: theme.surface }]}>
        <Pressable
          onPress={() => router.back()}
          hitSlop={12}
          style={({ pressed }) => [styles.backBtn, { opacity: pressed ? 0.7 : 1, backgroundColor: theme.backgroundElement }]}
        >
          <Ionicons name="chevron-back" size={22} color={theme.text} />
        </Pressable>

        <View style={styles.headerCenter}>
          <View style={styles.brandTitleRow}>
            <Text style={[styles.title, { color: theme.text }]}>Trợ lý AeroStride</Text>
            <Ionicons name="sparkles" size={14} color="#FFB800" />
          </View>
          <View style={styles.statusRow}>
            <View style={[styles.statusDot, { backgroundColor: connected ? Brand.success : Brand.warning }]} />
            <Text style={[styles.subtitle, { color: connected ? Brand.success : theme.textTertiary }]}>
              {connected ? 'Trực tuyến 24/7' : 'Đang kết nối...'}
            </Text>
          </View>
        </View>

        <View style={styles.headerRightAction}>
          <Pressable
            onPress={() => setMessages([])}
            hitSlop={10}
            style={({ pressed }) => [styles.clearBtn, { opacity: pressed ? 0.7 : 1, backgroundColor: theme.backgroundElement }]}
          >
            <Ionicons name="refresh-outline" size={18} color={theme.textSecondary} />
          </Pressable>
        </View>
      </View>

      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        keyboardVerticalOffset={Platform.OS === 'ios' ? insets.top : 0}
        style={{ flex: 1 }}
      >
        <FlatList
          ref={listRef}
          data={messages}
          keyExtractor={(item) => item.id}
          renderItem={renderItem}
          contentContainerStyle={styles.listContent}
          onContentSizeChange={() => listRef.current?.scrollToEnd({ animated: true })}
          ListEmptyComponent={
            <View style={styles.emptyWrap}>
              <LinearGradient
                colors={[Brand.primaryLight + '20', Brand.primary + '10']}
                style={styles.emptyIconContainer}
              >
                <MaterialCommunityIcons name="robot-happy-outline" size={44} color={Brand.primary} />
                <View style={styles.emptySparkle}>
                  <Ionicons name="sparkles" size={14} color="#FFD700" />
                </View>
              </LinearGradient>

              <Text style={[styles.emptyTitle, { color: theme.text }]}>
                Xin chào! Tôi có thể giúp gì cho bạn?
              </Text>
              <Text style={[styles.emptyText, { color: theme.textSecondary }]}>
                Hỏi về tư vấn chọn size giày, chính sách đổi trả, ưu đãi hoặc tra cứu đơn hàng...
              </Text>
            </View>
          }
        />

        {/* Suggestion Chips */}
        {messages.length === 0 && suggestions.length > 0 && (
          <View style={styles.suggestionsContainer}>
            <Text style={[styles.suggestionsHeader, { color: theme.textTertiary }]}>Gợi ý câu hỏi:</Text>
            <View style={styles.suggestionsList}>
              {suggestions.slice(0, 4).map((s) => (
                <Pressable
                  key={s}
                  style={({ pressed }) => [
                    styles.chip,
                    {
                      borderColor: Brand.primary + '40',
                      backgroundColor: theme.surfaceElevated,
                      opacity: pressed ? 0.75 : 1,
                    },
                  ]}
                  onPress={() => send(s)}
                >
                  <Ionicons name="chatbubble-outline" size={13} color={Brand.primary} style={{ marginRight: 6 }} />
                  <Text style={[styles.chipText, { color: theme.text }]} numberOfLines={1}>
                    {s}
                  </Text>
                </Pressable>
              ))}
            </View>
          </View>
        )}

        {/* Input Bar */}
        <View
          style={[
            styles.inputBar,
            {
              backgroundColor: theme.surface,
              borderTopColor: theme.borderLight,
              paddingBottom: isKeyboardVisible
                ? Spacing.two
                : Math.max(insets.bottom, Platform.OS === 'android' ? 14 : 8) + Spacing.two,
            },
          ]}
        >
          <View style={[styles.inputContainer, { backgroundColor: theme.backgroundElement, borderColor: theme.borderLight }]}>
            <TextInput
              style={[styles.textInput, { color: theme.text }]}
              placeholder="Nhập câu hỏi cho trợ lý AI..."
              placeholderTextColor={theme.textTertiary}
              value={input}
              onChangeText={setInput}
              multiline
              maxLength={500}
              onSubmitEditing={() => send(input)}
            />
          </View>

          <Pressable
            style={({ pressed }) => [
              styles.sendBtn,
              {
                opacity: pressed ? 0.85 : 1,
                transform: [{ scale: pressed ? 0.95 : 1 }],
              },
            ]}
            onPress={() => send(input)}
            disabled={!input.trim() || sending}
          >
            <LinearGradient
              colors={input.trim() ? [Brand.primaryLight, Brand.primaryDark] : ['#94A3B8', '#64748B']}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 1 }}
              style={styles.sendGradient}
            >
              {sending ? (
                <ActivityIndicator size="small" color="#FFFFFF" />
              ) : (
                <Ionicons name="send" size={16} color="#FFFFFF" style={{ marginLeft: 2 }} />
              )}
            </LinearGradient>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.two + 4,
    borderBottomWidth: 1,
  },
  backBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerCenter: {
    alignItems: 'center',
  },
  brandTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  title: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
    letterSpacing: -0.2,
  },
  statusRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    marginTop: 2,
  },
  statusDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  subtitle: {
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.medium,
  },
  clearBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  headerRightAction: {
    width: 36,
  },
  listContent: {
    padding: Spacing.three,
    gap: Spacing.three,
    flexGrow: 1,
  },
  bubbleRow: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: Spacing.two,
    maxWidth: '85%',
  },
  rowMine: {
    alignSelf: 'flex-end',
  },
  rowOther: {
    alignSelf: 'flex-start',
  },
  avatarWrap: {
    width: 32,
    height: 32,
    borderRadius: 16,
    overflow: 'hidden',
    marginBottom: 4,
  },
  avatarGradient: {
    width: '100%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
  },
  botAvatarWrap: {
    borderWidth: 1.5,
    borderColor: Brand.primaryLight,
  },
  staffAvatarWrap: {
    borderWidth: 1.5,
    borderColor: Brand.accentLight,
  },
  bubbleContainer: {
    flexShrink: 1,
  },
  senderHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
    marginBottom: 4,
    marginLeft: 2,
  },
  senderName: {
    fontSize: 11,
    fontWeight: FontWeights.bold,
  },
  aiTag: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 2,
    backgroundColor: '#0F172A',
    paddingHorizontal: 5,
    paddingVertical: 1,
    borderRadius: 6,
  },
  aiTagText: {
    color: '#FFD700',
    fontSize: 9,
    fontWeight: FontWeights.bold,
  },
  bubble: {
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two + 2,
    borderRadius: BorderRadius.xl,
  },
  bubbleMine: {
    borderBottomRightRadius: 4,
  },
  bubbleOther: {
    borderBottomLeftRadius: 4,
    borderWidth: 1,
  },
  bubbleTextMine: {
    color: '#FFFFFF',
    fontSize: FontSizes.sm + 1,
    lineHeight: 20,
    fontWeight: FontWeights.regular,
  },
  bubbleTextOther: {
    fontSize: FontSizes.sm + 1,
    lineHeight: 20,
  },
  bubbleTimeMine: {
    fontSize: 10,
    color: 'rgba(255, 255, 255, 0.7)',
    marginTop: 4,
    alignSelf: 'flex-end',
  },
  bubbleTimeOther: {
    fontSize: 10,
    marginTop: 4,
    alignSelf: 'flex-end',
  },
  systemWrap: {
    alignItems: 'center',
    paddingVertical: Spacing.one,
  },
  systemText: {
    fontSize: FontSizes.xs,
    fontStyle: 'italic',
  },
  emptyWrap: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: Spacing.five,
    paddingTop: Spacing.six,
  },
  emptyIconContainer: {
    width: 80,
    height: 80,
    borderRadius: 40,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: Spacing.three,
    borderWidth: 1.5,
    borderColor: Brand.primary + '30',
    position: 'relative',
  },
  emptySparkle: {
    position: 'absolute',
    top: 4,
    right: 4,
    backgroundColor: '#0F172A',
    borderRadius: 10,
    padding: 3,
    borderWidth: 1,
    borderColor: '#FFD700',
  },
  emptyTitle: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
    textAlign: 'center',
    marginBottom: Spacing.one,
  },
  emptyText: {
    fontSize: FontSizes.sm,
    textAlign: 'center',
    lineHeight: 19,
  },
  suggestionsContainer: {
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.two,
  },
  suggestionsHeader: {
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.semibold,
    marginBottom: Spacing.one,
    textTransform: 'uppercase',
    letterSpacing: 0.5,
  },
  suggestionsList: {
    gap: Spacing.one + 2,
  },
  chip: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two,
  },
  chipText: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
    flexShrink: 1,
  },
  inputBar: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: Spacing.two,
    paddingHorizontal: Spacing.three,
    paddingTop: Spacing.two,
    borderTopWidth: 1,
  },
  inputContainer: {
    flex: 1,
    borderWidth: 1,
    borderRadius: BorderRadius.xl,
    paddingHorizontal: Spacing.three,
    paddingVertical: Platform.OS === 'ios' ? Spacing.two : Spacing.one,
    minHeight: 44,
    maxHeight: 120,
    justifyContent: 'center',
  },
  textInput: {
    fontSize: FontSizes.base,
    paddingTop: 0,
    paddingBottom: 0,
  },
  sendBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    overflow: 'hidden',
  },
  sendGradient: {
    width: '100%',
    height: '100%',
    justifyContent: 'center',
    alignItems: 'center',
  },
});
