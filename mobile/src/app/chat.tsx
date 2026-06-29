/**
 * Support Chat Screen - talks to BE customer chat (AI + staff)
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
} from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { chatService, type ChatMessage, SENDER_CUSTOMER } from '@/services/chatService';
import { ChatSocket } from '@/services/chatSocket';

const POLL_DELAYS = [1200, 2600, 4200, 6000];

const senderLabel = (sender: string): string => {
  if (sender === 'bot') return 'Trợ lý AI';
  if (sender === 'staff') return 'Nhân viên';
  return 'Hỗ trợ';
};

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

  // Merge a single incoming message: dedupe by id and reconcile the optimistic
  // local bubble (which has a temporary `local-` id) with its server echo.
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
        // Preserve any optimistic local bubbles not yet returned by the server
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

      // Live updates: BE broadcasts every message to /topic/messages; only keep ours.
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
      socketRef.current = null;
    };
  }, [mergeMessage]);

  useEffect(() => {
    if (messages.length > 0) {
      requestAnimationFrame(() => listRef.current?.scrollToEnd({ animated: true }));
    }
  }, [messages]);

  const send = useCallback(
    async (text: string) => {
      const trimmed = text.trim();
      if (!trimmed || !sessionId || sending) return;
      setInput('');
      setSuggestions([]);
      setSending(true);

      // Optimistic customer bubble
      const optimistic: ChatMessage = {
        id: `local-${Date.now()}`,
        sender: SENDER_CUSTOMER,
        text: trimmed,
        time: '',
      };
      setMessages((prev) => [...prev, optimistic]);

      try {
        await chatService.sendMessage(sessionId, trimmed);
        await reloadHistory(sessionId);
        schedulePolls(sessionId);
      } catch {
        // leave the optimistic message; user can retry
      } finally {
        setSending(false);
      }
    },
    [sessionId, sending, reloadHistory, schedulePolls]
  );

  const renderItem = ({ item }: { item: ChatMessage }) => {
    const isMine = item.sender === SENDER_CUSTOMER;
    const isSystem = item.sender === 'system';
    if (isSystem) {
      return (
        <View style={styles.systemWrap}>
          <Text style={[styles.systemText, { color: theme.textTertiary }]}>{item.text}</Text>
        </View>
      );
    }
    return (
      <View style={[styles.bubbleRow, isMine ? styles.rowMine : styles.rowOther]}>
        {!isMine && (
          <View style={[styles.botAvatar, { backgroundColor: Brand.primary + '15' }]}>
            <Ionicons name="headset-outline" size={16} color={Brand.primary} />
          </View>
        )}
        <View
          style={[
            styles.bubble,
            isMine
              ? { backgroundColor: Brand.primary, borderBottomRightRadius: 4 }
              : { backgroundColor: theme.surface, borderColor: theme.border, borderWidth: 1, borderBottomLeftRadius: 4 },
          ]}
        >
          {!isMine && (
            <Text style={[styles.senderName, { color: Brand.primary }]}>
              {senderLabel(item.sender)}
            </Text>
          )}
          <Text style={[styles.bubbleText, { color: isMine ? '#FFFFFF' : theme.text }]}>
            {item.text}
          </Text>
          {item.time ? (
            <Text style={[styles.bubbleTime, { color: isMine ? 'rgba(255,255,255,0.7)' : theme.textTertiary }]}>
              {item.time}
            </Text>
          ) : null}
        </View>
      </View>
    );
  };

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={[styles.header, { paddingTop: insets.top + Spacing.two, borderBottomColor: theme.borderLight }]}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Ionicons name="arrow-back" size={24} color={theme.text} />
        </Pressable>
        <View style={styles.headerCenter}>
          <Text style={[styles.title, { color: theme.text }]}>Hỗ trợ AeroStride</Text>
          <Text style={[styles.subtitle, { color: connected ? Brand.success : theme.textTertiary }]}>
            {connected ? '● Trực tuyến' : '○ Đang kết nối...'}
          </Text>
        </View>
        <View style={{ width: 24 }} />
      </View>

      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        keyboardVerticalOffset={Platform.OS === 'ios' ? 0 : 0}
        style={{ flex: 1 }}
      >
        <FlatList
          ref={listRef}
          data={messages}
          keyExtractor={(item) => item.id}
          renderItem={renderItem}
          contentContainerStyle={styles.listContent}
          ListEmptyComponent={
            <View style={styles.emptyWrap}>
              <View style={[styles.emptyIcon, { backgroundColor: Brand.primary + '12' }]}>
                <Ionicons name="chatbubbles-outline" size={40} color={Brand.primary} />
              </View>
              <Text style={[styles.emptyTitle, { color: theme.text }]}>Chào bạn 👋</Text>
              <Text style={[styles.emptyText, { color: theme.textSecondary }]}>
                Chúng tôi có thể giúp gì cho bạn hôm nay?
              </Text>
            </View>
          }
        />

        {/* Welcome suggestion chips */}
        {messages.length === 0 && suggestions.length > 0 && (
          <View style={styles.suggestions}>
            {suggestions.slice(0, 4).map((s) => (
              <Pressable
                key={s}
                style={[styles.chip, { borderColor: Brand.primary, backgroundColor: Brand.primary + '08' }]}
                onPress={() => send(s)}
              >
                <Text style={[styles.chipText, { color: Brand.primary }]} numberOfLines={1}>
                  {s}
                </Text>
              </Pressable>
            ))}
          </View>
        )}

        {/* Input bar */}
        <View
          style={[
            styles.inputBar,
            { backgroundColor: theme.surface, borderTopColor: theme.border, paddingBottom: insets.bottom + Spacing.two },
          ]}
        >
          <TextInput
            style={[styles.textInput, { color: theme.text, backgroundColor: theme.backgroundElement, borderColor: theme.border }]}
            placeholder="Nhập tin nhắn..."
            placeholderTextColor={theme.textTertiary}
            value={input}
            onChangeText={setInput}
            multiline
            onSubmitEditing={() => send(input)}
          />
          <Pressable
            style={({ pressed }) => [
              styles.sendBtn,
              { backgroundColor: input.trim() ? Brand.primary : theme.textTertiary, opacity: pressed ? 0.8 : 1 },
            ]}
            onPress={() => send(input)}
            disabled={!input.trim() || sending}
          >
            <Ionicons name="send" size={18} color="#FFFFFF" />
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.two + 2,
    borderBottomWidth: 1,
  },
  headerCenter: { alignItems: 'center' },
  title: { fontSize: FontSizes.md, fontWeight: FontWeights.bold },
  subtitle: { fontSize: FontSizes.xs, marginTop: 1 },
  listContent: { padding: Spacing.three, gap: Spacing.two, flexGrow: 1 },
  bubbleRow: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: Spacing.one + 2,
    maxWidth: '88%',
  },
  rowMine: { alignSelf: 'flex-end', justifyContent: 'flex-end' },
  rowOther: { alignSelf: 'flex-start' },
  botAvatar: {
    width: 28,
    height: 28,
    borderRadius: 14,
    justifyContent: 'center',
    alignItems: 'center',
  },
  bubble: {
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two,
    borderRadius: BorderRadius.lg,
    flexShrink: 1,
  },
  senderName: { fontSize: 11, fontWeight: FontWeights.bold, marginBottom: 2 },
  bubbleText: { fontSize: FontSizes.sm, lineHeight: 20 },
  bubbleTime: { fontSize: 10, marginTop: 3, alignSelf: 'flex-end' },
  systemWrap: { alignItems: 'center', paddingVertical: Spacing.one },
  systemText: { fontSize: FontSizes.xs, fontStyle: 'italic' },
  emptyWrap: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: Spacing.two, paddingHorizontal: Spacing.five },
  emptyIcon: {
    width: 80,
    height: 80,
    borderRadius: 40,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: Spacing.two,
  },
  emptyTitle: { fontSize: FontSizes.lg, fontWeight: FontWeights.bold },
  emptyText: { fontSize: FontSizes.sm, textAlign: 'center' },
  suggestions: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: Spacing.two,
    paddingHorizontal: Spacing.three,
    paddingBottom: Spacing.two,
  },
  chip: {
    borderWidth: 1,
    borderRadius: BorderRadius.full,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.one + 2,
    maxWidth: '100%',
  },
  chipText: { fontSize: FontSizes.sm, fontWeight: FontWeights.medium },
  inputBar: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    gap: Spacing.two,
    paddingHorizontal: Spacing.three,
    paddingTop: Spacing.two + 2,
    borderTopWidth: 1,
  },
  textInput: {
    flex: 1,
    borderWidth: 1,
    borderRadius: BorderRadius.lg,
    paddingHorizontal: Spacing.three,
    paddingTop: Spacing.two,
    paddingBottom: Spacing.two,
    fontSize: FontSizes.base,
    maxHeight: 110,
  },
  sendBtn: {
    width: 42,
    height: 42,
    borderRadius: 21,
    justifyContent: 'center',
    alignItems: 'center',
  },
});
