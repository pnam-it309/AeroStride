/**
 * Help / Support Center Screen
 */

import React, { useState } from 'react';
import { StyleSheet, View, Text, ScrollView, Pressable, Linking } from 'react-native';
import { useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';

const FAQS = [
  {
    q: 'Làm sao để theo dõi đơn hàng?',
    a: 'Vào Tài khoản → Đơn hàng của tôi để xem trạng thái và lịch sử của tất cả đơn hàng.',
  },
  {
    q: 'Tôi có thể hủy đơn hàng không?',
    a: 'Bạn có thể hủy đơn khi đơn đang ở trạng thái "Chờ xác nhận". Mở chi tiết đơn và chọn "Hủy đơn hàng".',
  },
  {
    q: 'Phí vận chuyển được tính thế nào?',
    a: 'Phí vận chuyển là 30.000₫. Đơn hàng từ 5.000.000₫ trở lên được miễn phí vận chuyển.',
  },
  {
    q: 'Có những phương thức thanh toán nào?',
    a: 'Hiện hỗ trợ thanh toán khi nhận hàng (COD) và thanh toán online qua VNPay.',
  },
];

export default function HelpScreen() {
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const [openIndex, setOpenIndex] = useState<number | null>(0);

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <View style={[styles.header, { paddingTop: insets.top + Spacing.two }]}>
        <Pressable onPress={() => router.back()} hitSlop={12}>
          <Ionicons name="arrow-back" size={24} color={theme.text} />
        </Pressable>
        <Text style={[styles.title, { color: theme.text }]}>Trợ giúp</Text>
        <View style={{ width: 24 }} />
      </View>

      <ScrollView
        showsVerticalScrollIndicator={false}
        contentContainerStyle={{ padding: Spacing.three, paddingBottom: insets.bottom + 40 }}
      >
        <Text style={[styles.sectionLabel, { color: theme.textSecondary }]}>CÂU HỎI THƯỜNG GẶP</Text>
        <View style={[styles.card, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          {FAQS.map((faq, i) => {
            const open = openIndex === i;
            return (
              <Pressable
                key={faq.q}
                style={[styles.faqItem, i > 0 && { borderTopColor: theme.borderLight, borderTopWidth: 1 }]}
                onPress={() => setOpenIndex(open ? null : i)}
              >
                <View style={styles.faqQuestion}>
                  <Text style={[styles.faqQ, { color: theme.text }]}>{faq.q}</Text>
                  <Ionicons
                    name={open ? 'chevron-up' : 'chevron-down'}
                    size={18}
                    color={theme.textTertiary}
                  />
                </View>
                {open && <Text style={[styles.faqA, { color: theme.textSecondary }]}>{faq.a}</Text>}
              </Pressable>
            );
          })}
        </View>

        <Text style={[styles.sectionLabel, { color: theme.textSecondary, marginTop: Spacing.four }]}>
          LIÊN HỆ HỖ TRỢ
        </Text>
        <View style={[styles.card, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <Pressable
            style={styles.contactRow}
            onPress={() => Linking.openURL('tel:1900000000')}
          >
            <View style={[styles.contactIcon, { backgroundColor: Brand.success + '15' }]}>
              <Ionicons name="call-outline" size={18} color={Brand.success} />
            </View>
            <View style={{ flex: 1 }}>
              <Text style={[styles.contactLabel, { color: theme.text }]}>Hotline</Text>
              <Text style={[styles.contactValue, { color: theme.textSecondary }]}>1900 0000</Text>
            </View>
            <Ionicons name="chevron-forward" size={18} color={theme.textTertiary} />
          </Pressable>
          <Pressable
            style={[styles.contactRow, { borderTopColor: theme.borderLight, borderTopWidth: 1 }]}
            onPress={() => Linking.openURL('mailto:support@aerostride.com')}
          >
            <View style={[styles.contactIcon, { backgroundColor: Brand.info + '15' }]}>
              <Ionicons name="mail-outline" size={18} color={Brand.info} />
            </View>
            <View style={{ flex: 1 }}>
              <Text style={[styles.contactLabel, { color: theme.text }]}>Email</Text>
              <Text style={[styles.contactValue, { color: theme.textSecondary }]}>
                support@aerostride.com
              </Text>
            </View>
            <Ionicons name="chevron-forward" size={18} color={theme.textTertiary} />
          </Pressable>
        </View>
      </ScrollView>
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
    paddingBottom: Spacing.three,
  },
  title: { fontSize: FontSizes.lg, fontWeight: FontWeights.bold },
  sectionLabel: {
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.semibold,
    letterSpacing: 1,
    marginBottom: Spacing.two,
    paddingLeft: Spacing.one,
  },
  card: { borderRadius: BorderRadius.lg, borderWidth: 1, overflow: 'hidden' },
  faqItem: { padding: Spacing.three },
  faqQuestion: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: Spacing.two },
  faqQ: { flex: 1, fontSize: FontSizes.sm, fontWeight: FontWeights.semibold },
  faqA: { fontSize: FontSizes.sm, lineHeight: 20, marginTop: Spacing.two },
  contactRow: { flexDirection: 'row', alignItems: 'center', gap: Spacing.three, padding: Spacing.three },
  contactIcon: {
    width: 36,
    height: 36,
    borderRadius: BorderRadius.md,
    justifyContent: 'center',
    alignItems: 'center',
  },
  contactLabel: { fontSize: FontSizes.base, fontWeight: FontWeights.medium },
  contactValue: { fontSize: FontSizes.sm, marginTop: 2 },
});
