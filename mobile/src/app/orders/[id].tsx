/**
 * Order Detail Screen
 */

import React, { useEffect, useState, useCallback } from 'react';
import { StyleSheet, View, Text, ScrollView, Pressable, Modal, TextInput, ActivityIndicator } from 'react-native';
import { Image } from 'expo-image';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import Ionicons from '@expo/vector-icons/Ionicons';
import { openBrowserAsync } from 'expo-web-browser';
import { Brand, FontSizes, FontWeights, Spacing, BorderRadius } from '@/constants/theme';
import { useTheme } from '@/hooks/use-theme';
import { useFeedback } from '@/context/FeedbackContext';
import { orderService, type Order, type OrderItem } from '@/services/orderService';
import { reviewService } from '@/services/reviewService';
import { profileService } from '@/services/profileService';
import { fileService } from '@/services/fileService';
import { getApiErrorMessage } from '@/services/apiClient';
import { formatCurrency, formatDate, getOrderStatusColor } from '@/utils/format';
import { StatusBadge } from '@/components/ui/Badge';
import { StatusTimeline } from '@/components/StatusTimeline';
import { LoadingSpinner } from '@/components/ui/LoadingSpinner';
import { AddressInputGroup, type AddressData } from '@/components/ui/AddressInputGroup';

export default function OrderDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const theme = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const { showToast, confirm } = useFeedback();

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [cancelling, setCancelling] = useState(false);
  const [payingVnPay, setPayingVnPay] = useState(false);

  // Review states
  const [eligibilityMap, setEligibilityMap] = useState<Record<string, boolean>>({});
  const [reviewModalVisible, setReviewModalVisible] = useState(false);
  const [selectedReviewItem, setSelectedReviewItem] = useState<OrderItem | null>(null);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);
  const [customerId, setCustomerId] = useState<string>('');

  // Edit Shipping Modal states
  const [editShippingModalVisible, setEditShippingModalVisible] = useState(false);
  const [editTenNguoiNhan, setEditTenNguoiNhan] = useState('');
  const [editSoDienThoai, setEditSoDienThoai] = useState('');
  const [editAddressData, setEditAddressData] = useState<AddressData>({
    tinhThanh: '',
    quanHuyen: '',
    phuongXa: '',
    diaChiChiTiet: '',
    fullAddress: '',
  });
  const [editGhiChu, setEditGhiChu] = useState('');
  const [savingShipping, setSavingShipping] = useState(false);

  const loadOrder = useCallback(async () => {
    if (!id) return;
    try {
      const data = await orderService.getOrderDetail(id);
      setOrder(data);

      if (data.trangThai === 'HOAN_THANH' && data.items?.length) {
        try {
          const profile = await profileService.getMyProfile();
          if (profile?.id) {
            setCustomerId(profile.id);
            const map: Record<string, boolean> = {};
            await Promise.all(
              data.items.map(async (item) => {
                if (item.idSanPham) {
                  try {
                    const ok = await reviewService.checkEligibility(data.id, item.idSanPham, profile.id);
                    map[item.idSanPham] = ok;
                  } catch {
                    map[item.idSanPham] = false;
                  }
                }
              })
            );
            setEligibilityMap(map);
          }
        } catch {
          // ignore profile load error for guest/offline
        }
      }
    } catch (err: any) {
      console.warn('Failed to load order:', err);
      showToast({ type: 'error', title: 'Lỗi', message: getApiErrorMessage(err, 'Không thể tải đơn hàng') });
    } finally {
      setLoading(false);
    }
  }, [id, showToast]);

  useEffect(() => {
    loadOrder();
  }, [loadOrder]);

  const handleCancel = async () => {
    if (!order) return;
    const confirmed = await confirm({
      title: 'Hủy đơn hàng',
      message: 'Bạn có chắc muốn hủy đơn hàng này?',
      confirmText: 'Hủy đơn',
      cancelText: 'Không',
      destructive: true,
    });
    if (!confirmed) return;

    setCancelling(true);
    try {
      await orderService.cancelOrder(order.id);
      setOrder((prev) =>
        prev
          ? { ...prev, trangThai: 'DA_HUY', trangThaiDisplay: 'Đã hủy', choPhepHuy: false, choPhepSuaThongTin: false }
          : null
      );
      showToast({ type: 'success', title: 'Thành công', message: 'Đơn hàng đã được hủy' });
    } catch (err: any) {
      showToast({ type: 'error', title: 'Lỗi', message: getApiErrorMessage(err, 'Không thể hủy đơn hàng') });
    } finally {
      setCancelling(false);
    }
  };

  const handleRepayVnPay = async () => {
    if (!order) return;
    setPayingVnPay(true);
    try {
      const returnUrl = 'aerostride://orders/' + order.id;
      const paymentUrl = await orderService.createVnPayUrl(order.id, returnUrl);
      if (paymentUrl) {
        await openBrowserAsync(paymentUrl);
        await loadOrder();
      }
    } catch (err: any) {
      showToast({ type: 'error', title: 'Lỗi thanh toán', message: getApiErrorMessage(err, 'Không thể tạo liên kết thanh toán VNPay') });
    } finally {
      setPayingVnPay(false);
    }
  };

  const handleOpenEditShippingModal = () => {
    if (!order) return;
    setEditTenNguoiNhan(order.tenNguoiNhan || '');
    setEditSoDienThoai(order.soDienThoaiNguoiNhan || '');
    setEditGhiChu(order.ghiChu || '');
    setEditShippingModalVisible(true);
  };

  const handleSaveShipping = async () => {
    if (!order) return;
    if (!editTenNguoiNhan.trim()) {
      showToast({ type: 'error', title: 'Lỗi', message: 'Vui lòng nhập tên người nhận' });
      return;
    }
    if (!editSoDienThoai.trim()) {
      showToast({ type: 'error', title: 'Lỗi', message: 'Vui lòng nhập số điện thoại' });
      return;
    }
    if (!editAddressData.tinhThanh) {
      showToast({ type: 'error', title: 'Lỗi', message: 'Vui lòng chọn Tỉnh / Thành phố' });
      return;
    }
    if (!editAddressData.quanHuyen) {
      showToast({ type: 'error', title: 'Lỗi', message: 'Vui lòng chọn Quận / Huyện' });
      return;
    }
    if (!editAddressData.phuongXa) {
      showToast({ type: 'error', title: 'Lỗi', message: 'Vui lòng chọn Phường / Xã' });
      return;
    }
    if (!editAddressData.diaChiChiTiet.trim()) {
      showToast({ type: 'error', title: 'Lỗi', message: 'Vui lòng nhập địa chỉ chi tiết (số nhà, tên đường...)' });
      return;
    }

    setSavingShipping(true);
    try {
      const updated = await orderService.updateShippingInfo(order.id, {
        tenNguoiNhan: editTenNguoiNhan.trim(),
        soDienThoaiNguoiNhan: editSoDienThoai.trim(),
        diaChiNguoiNhan: editAddressData.fullAddress.trim(),
        ghiChu: editGhiChu.trim(),
      });
      setOrder(updated);
      setEditShippingModalVisible(false);
      showToast({ type: 'success', title: 'Thành công', message: 'Đã cập nhật thông tin giao nhận thành công (1/1 lần)' });
    } catch (err: any) {
      showToast({ type: 'error', title: 'Lỗi', message: getApiErrorMessage(err, 'Không thể cập nhật thông tin giao nhận') });
    } finally {
      setSavingShipping(false);
    }
  };

  const handleOpenReviewModal = (item: OrderItem) => {
    setSelectedReviewItem(item);
    setRating(5);
    setComment('');
    setReviewModalVisible(true);
  };

  const handleSubmitReview = async () => {
    if (!selectedReviewItem || !customerId || !order) return;
    if (!comment.trim()) {
      showToast({ type: 'warning', title: 'Thông báo', message: 'Vui lòng nhập nội dung đánh giá của bạn' });
      return;
    }

    setSubmittingReview(true);
    try {
      await reviewService.submitReview({
        idHoaDon: order.id,
        idSanPham: selectedReviewItem.idSanPham,
        idKhachHang: customerId,
        diemDanhGia: rating,
        noiDung: comment.trim(),
      });
      setReviewModalVisible(false);
      setEligibilityMap((prev) => ({
        ...prev,
        [selectedReviewItem.idSanPham]: false,
      }));
      showToast({ type: 'success', title: 'Cảm ơn bạn', message: 'Đánh giá của bạn đã được gửi thành công!' });
    } catch (err: any) {
      showToast({ type: 'error', title: 'Lỗi', message: getApiErrorMessage(err, 'Không thể gửi đánh giá') });
    } finally {
      setSubmittingReview(false);
    }
  };

  if (loading) {
    return (
      <View style={[styles.loadingContainer, { backgroundColor: theme.background }]}>
        <LoadingSpinner fullScreen />
      </View>
    );
  }

  if (!order) return null;

  const canCancel = !!order.choPhepHuy;
  const statusColor = getOrderStatusColor(order.trangThai);

  return (
    <View style={[styles.container, { backgroundColor: theme.background }]}>
      <ScrollView showsVerticalScrollIndicator={false} contentContainerStyle={{ paddingBottom: 120 }}>
        {/* Header */}
        <View style={[styles.header, { paddingTop: insets.top + Spacing.two }]}>
          <Pressable onPress={() => router.back()} hitSlop={12}>
            <Ionicons name="arrow-back" size={24} color={theme.text} />
          </Pressable>
          <Text style={[styles.title, { color: theme.text }]}>Chi tiết đơn hàng</Text>
          <View style={{ width: 24 }} />
        </View>

        {/* Status Card */}
        <View style={[styles.statusCard, { backgroundColor: theme.surfaceElevated, borderColor: theme.border }]}>
          <View style={styles.statusHeader}>
            <View>
              <Text style={[styles.orderCode, { color: theme.text }]}>{order.maHoaDon}</Text>
              <Text style={[styles.orderDate, { color: theme.textSecondary }]}>
                {formatDate(order.ngayTao)}
              </Text>
            </View>
            <StatusBadge
              label={order.trangThaiDisplay || order.trangThai}
              color={statusColor}
            />
          </View>

          {/* Refund Notice Banner if refund surplus exists */}
          {order.tienHoanTraTruoc != null && order.tienHoanTraTruoc > 0 && (
            <View style={styles.refundBanner}>
              <Ionicons name="cash-outline" size={20} color={Brand.success} />
              <View style={{ flex: 1 }}>
                <Text style={styles.refundBannerTitle}>
                  Khoản tiền hoàn thừa: {formatCurrency(order.tienHoanTraTruoc)}
                </Text>
                <Text style={styles.refundBannerDesc}>
                  Cửa hàng sẽ liên hệ hoàn tiền chênh lệch này cho bạn.
                </Text>
              </View>
            </View>
          )}

          {/* Repay VNPay banner button */}
          {order.choPhepThanhToanLai && (
            <Pressable
              style={[styles.repayButton, { opacity: payingVnPay ? 0.7 : 1 }]}
              onPress={handleRepayVnPay}
              disabled={payingVnPay}
            >
              {payingVnPay ? (
                <ActivityIndicator size="small" color="#FFFFFF" />
              ) : (
                <>
                  <Ionicons name="card-outline" size={18} color="#FFFFFF" style={{ marginRight: 6 }} />
                  <Text style={styles.repayButtonText}>Thanh toán lại qua VNPay</Text>
                </>
              )}
            </Pressable>
          )}
        </View>

        {/* Order Status History Timeline */}
        {order.lichSuTrangThai && order.lichSuTrangThai.length > 0 && (
          <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
            <Text style={[styles.sectionTitle, { color: theme.text }]}>Lịch sử đơn hàng</Text>
            <StatusTimeline history={order.lichSuTrangThai} />
          </View>
        )}

        {/* Shipping Info */}
        <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <View style={styles.sectionHeaderRow}>
            <Text style={[styles.sectionTitle, { color: theme.text, marginBottom: 0 }]}>
              Thông tin nhận hàng
            </Text>
            {order.choPhepSuaThongTin ? (
              <Pressable
                style={[styles.editShippingBtn, { backgroundColor: Brand.primaryLight + '20' }]}
                onPress={handleOpenEditShippingModal}
              >
                <Ionicons name="pencil" size={14} color={Brand.primary} />
                <Text style={[styles.editShippingText, { color: Brand.primary }]}>Sửa</Text>
              </Pressable>
            ) : order.daSuaThongTin ? (
              <View style={[styles.editedChip, { backgroundColor: theme.backgroundElement }]}>
                <Text style={[styles.editedChipText, { color: theme.textTertiary }]}>
                  Đã sửa (1/1 lần)
                </Text>
              </View>
            ) : null}
          </View>

          <View style={styles.shippingRow}>
            <Ionicons name="person-outline" size={18} color={theme.textTertiary} />
            <Text style={[styles.shippingText, { color: theme.text }]}>{order.tenNguoiNhan}</Text>
          </View>
          <View style={styles.shippingRow}>
            <Ionicons name="call-outline" size={18} color={theme.textTertiary} />
            <Text style={[styles.shippingText, { color: theme.text }]}>
              {order.soDienThoaiNguoiNhan}
            </Text>
          </View>
          <View style={styles.shippingRow}>
            <Ionicons name="location-outline" size={18} color={theme.textTertiary} />
            <Text style={[styles.shippingText, { color: theme.text }]} numberOfLines={2}>
              {order.diaChiNguoiNhan}
            </Text>
          </View>
          {order.ghiChu ? (
            <View style={styles.shippingRow}>
              <Ionicons name="chatbubble-ellipses-outline" size={18} color={theme.textTertiary} />
              <Text style={[styles.shippingText, { color: theme.textSecondary }]}>
                {order.ghiChu}
              </Text>
            </View>
          ) : null}
        </View>

        {/* Product Items */}
        <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <Text style={[styles.sectionTitle, { color: theme.text }]}>
            Sản phẩm ({order.items?.length || 0})
          </Text>
          {order.items?.map((item) => (
            <View key={item.id} style={[styles.itemRow, { borderTopColor: theme.borderLight }]}>
              <View style={[styles.itemImage, { backgroundColor: theme.backgroundElement }]}>
                {item.hinhAnh ? (
                  <Image
                    source={fileService.getImageSource(item.hinhAnh)}
                    style={{ width: '100%', height: '100%' }}
                    contentFit="cover"
                    cachePolicy="memory-disk"
                    transition={200}
                  />
                ) : (
                  <Ionicons name="footsteps-outline" size={20} color={theme.textTertiary} />
                )}
              </View>
              <View style={styles.itemInfo}>
                <Text style={[styles.itemName, { color: theme.text }]} numberOfLines={2}>
                  {item.tenSanPham}
                </Text>
                <Text style={[styles.itemVariant, { color: theme.textSecondary }]}>
                  {[item.tenMauSac, item.tenKichThuoc].filter(Boolean).join(' · ')}
                </Text>
                <View style={styles.itemPriceRow}>
                  <Text style={[styles.itemPrice, { color: Brand.primary }]}>
                    {formatCurrency(item.donGia)}
                  </Text>
                  <Text style={[styles.itemQty, { color: theme.textTertiary }]}>
                    x{item.soLuong}
                  </Text>
                </View>
                {order.trangThai === 'HOAN_THANH' && (
                  <View style={{ marginTop: Spacing.two, alignItems: 'flex-start' }}>
                    {eligibilityMap[item.idSanPham] ? (
                      <Pressable
                        style={[styles.reviewBtn, { backgroundColor: Brand.primary }]}
                        onPress={() => handleOpenReviewModal(item)}
                      >
                        <Text style={styles.reviewBtnText}>Viết đánh giá</Text>
                      </Pressable>
                    ) : (
                      eligibilityMap[item.idSanPham] === false && (
                        <Text style={[styles.reviewedText, { color: theme.textTertiary }]}>
                          Đã đánh giá
                        </Text>
                      )
                    )}
                  </View>
                )}
              </View>
            </View>
          ))}
        </View>

        {/* Price Summary */}
        <View style={[styles.section, { backgroundColor: theme.surface, borderColor: theme.border }]}>
          <Text style={[styles.sectionTitle, { color: theme.text }]}>Thanh toán</Text>
          <View style={styles.priceRow}>
            <Text style={[styles.priceLabel, { color: theme.textSecondary }]}>Tạm tính</Text>
            <Text style={[styles.priceValue, { color: theme.text }]}>
              {formatCurrency(order.tongTien)}
            </Text>
          </View>
          {order.phiVanChuyen > 0 && (
            <View style={styles.priceRow}>
              <Text style={[styles.priceLabel, { color: theme.textSecondary }]}>Phí vận chuyển</Text>
              <Text style={[styles.priceValue, { color: theme.text }]}>
                {formatCurrency(order.phiVanChuyen)}
              </Text>
            </View>
          )}
          {order.tienGiam > 0 && (
            <View style={styles.priceRow}>
              <Text style={[styles.priceLabel, { color: theme.textSecondary }]}>Giảm giá</Text>
              <Text style={[styles.priceValue, { color: Brand.success }]}>
                -{formatCurrency(order.tienGiam)}
              </Text>
            </View>
          )}
          {order.maVoucher && (
            <View style={styles.priceRow}>
              <Text style={[styles.priceLabel, { color: theme.textSecondary }]}>Voucher</Text>
              <Text style={[styles.priceValue, { color: Brand.accent }]}>{order.maVoucher}</Text>
            </View>
          )}
          <View style={[styles.totalRow, { borderTopColor: theme.border }]}>
            <Text style={[styles.totalLabel, { color: theme.text }]}>Tổng cộng</Text>
            <Text style={[styles.totalValue, { color: Brand.primary }]}>
              {formatCurrency(order.tongTienSauGiam ?? order.tongTien)}
            </Text>
          </View>
          <View style={styles.priceRow}>
            <Text style={[styles.priceLabel, { color: theme.textSecondary }]}>Phương thức</Text>
            <Text style={[styles.priceValue, { color: theme.text }]}>
              {order.phuongThucThanhToan === 'COD' ? 'Tiền mặt khi nhận' : order.phuongThucThanhToan}
            </Text>
          </View>
        </View>
      </ScrollView>

      {/* Bottom Cancel Button */}
      {canCancel && (
        <View
          style={[
            styles.bottomBar,
            {
              backgroundColor: theme.surface,
              borderTopColor: theme.border,
              paddingBottom: insets.bottom + Spacing.two,
            },
          ]}
        >
          <Pressable
            style={({ pressed }) => [styles.cancelBtn, { opacity: cancelling || pressed ? 0.7 : 1 }]}
            onPress={handleCancel}
            disabled={cancelling}
          >
            <Text style={styles.cancelText}>
              {cancelling ? 'Đang xử lý...' : 'Hủy đơn hàng'}
            </Text>
          </Pressable>
        </View>
      )}

      {/* Review Modal */}
      <Modal
        visible={reviewModalVisible}
        transparent
        animationType="slide"
        onRequestClose={() => setReviewModalVisible(false)}
      >
        <Pressable style={styles.modalOverlay} onPress={() => setReviewModalVisible(false)}>
          <Pressable
            style={[
              styles.modalSheet,
              {
                backgroundColor: theme.surfaceElevated,
                paddingBottom: insets.bottom + Spacing.four,
              },
            ]}
            onPress={(e) => e.stopPropagation()}
          >
            <View style={styles.modalHeader}>
              <Text style={[styles.modalTitle, { color: theme.text }]}>Đánh giá sản phẩm</Text>
              <Pressable onPress={() => setReviewModalVisible(false)} hitSlop={12}>
                <Ionicons name="close" size={22} color={theme.textSecondary} />
              </Pressable>
            </View>

            {selectedReviewItem && (
              <View style={[styles.selectedProductCard, { borderColor: theme.border }]}>
                <Image
                  source={fileService.getImageSource(selectedReviewItem.hinhAnh)}
                  style={styles.selectedProductImage}
                  contentFit="cover"
                />
                <View style={{ flex: 1, gap: 4 }}>
                  <Text style={[styles.selectedProductName, { color: theme.text }]} numberOfLines={2}>
                    {selectedReviewItem.tenSanPham}
                  </Text>
                  <Text style={{ fontSize: FontSizes.xs, color: theme.textSecondary }}>
                    {[selectedReviewItem.tenMauSac, selectedReviewItem.tenKichThuoc]
                      .filter(Boolean)
                      .join(' · ')}
                  </Text>
                </View>
              </View>
            )}

            <View style={styles.ratingSection}>
              <Text style={[styles.ratingLabel, { color: theme.text }]}>Chất lượng sản phẩm</Text>
              <View style={styles.ratingStars}>
                {Array.from({ length: 5 }).map((_, i) => {
                  const starVal = i + 1;
                  const isSelected = starVal <= rating;
                  return (
                    <Pressable key={i} onPress={() => setRating(starVal)}>
                      <Ionicons
                        name={isSelected ? 'star' : 'star-outline'}
                        size={32}
                        color="#FFB300"
                        style={{ marginHorizontal: 4 }}
                      />
                    </Pressable>
                  );
                })}
              </View>
            </View>

            <TextInput
              style={[
                styles.reviewInput,
                {
                  borderColor: theme.border,
                  color: theme.text,
                  backgroundColor: theme.background,
                },
              ]}
              placeholder="Hãy chia sẻ cảm nhận của bạn về sản phẩm này nhé (chất liệu, form dáng, màu sắc...)"
              placeholderTextColor={theme.textTertiary}
              multiline
              numberOfLines={4}
              value={comment}
              onChangeText={setComment}
            />

            <Pressable
              style={({ pressed }) => [
                styles.submitBtn,
                {
                  opacity: submittingReview || pressed ? 0.7 : 1,
                  backgroundColor: Brand.primary,
                },
              ]}
              onPress={handleSubmitReview}
              disabled={submittingReview}
            >
              <Text style={styles.submitText}>
                {submittingReview ? 'Đang gửi...' : 'Gửi đánh giá'}
              </Text>
            </Pressable>
          </Pressable>
        </Pressable>
      </Modal>

      {/* Edit Shipping Address Modal */}
      <Modal
        visible={editShippingModalVisible}
        transparent
        animationType="slide"
        onRequestClose={() => setEditShippingModalVisible(false)}
      >
        <Pressable style={styles.modalOverlay} onPress={() => setEditShippingModalVisible(false)}>
          <Pressable
            style={[
              styles.modalSheet,
              {
                backgroundColor: theme.surfaceElevated,
                paddingBottom: insets.bottom + Spacing.four,
              },
            ]}
            onPress={(e) => e.stopPropagation()}
          >
            <View style={styles.modalHeader}>
              <Text style={[styles.modalTitle, { color: theme.text }]}>Sửa thông tin nhận hàng</Text>
              <Pressable onPress={() => setEditShippingModalVisible(false)} hitSlop={12}>
                <Ionicons name="close" size={22} color={theme.textSecondary} />
              </Pressable>
            </View>

            <Text style={[styles.editWarningText, { color: Brand.warning }]}>
              Lưu ý: Bạn chỉ được phép đổi thông tin nhận hàng tối đa 1 lần duy nhất khi đơn đang chờ xác nhận.
            </Text>

            <View style={styles.inputGroup}>
              <Text style={[styles.inputLabel, { color: theme.textSecondary }]}>Tên người nhận</Text>
              <TextInput
                style={[styles.inputField, { borderColor: theme.border, color: theme.text, backgroundColor: theme.background }]}
                placeholder="Nhập họ và tên"
                placeholderTextColor={theme.textTertiary}
                value={editTenNguoiNhan}
                onChangeText={setEditTenNguoiNhan}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={[styles.inputLabel, { color: theme.textSecondary }]}>Số điện thoại</Text>
              <TextInput
                style={[styles.inputField, { borderColor: theme.border, color: theme.text, backgroundColor: theme.background }]}
                placeholder="Nhập số điện thoại"
                placeholderTextColor={theme.textTertiary}
                keyboardType="phone-pad"
                value={editSoDienThoai}
                onChangeText={setEditSoDienThoai}
              />
            </View>

            <View style={styles.inputGroup}>
              <AddressInputGroup
                initialFullAddress={order.diaChiNguoiNhan}
                onChange={setEditAddressData}
              />
            </View>

            <View style={styles.inputGroup}>
              <Text style={[styles.inputLabel, { color: theme.textSecondary }]}>Ghi chú</Text>
              <TextInput
                style={[styles.inputField, { borderColor: theme.border, color: theme.text, backgroundColor: theme.background }]}
                placeholder="Ghi chú thêm cho người giao hàng"
                placeholderTextColor={theme.textTertiary}
                value={editGhiChu}
                onChangeText={setEditGhiChu}
              />
            </View>

            <Pressable
              style={({ pressed }) => [
                styles.submitBtn,
                {
                  opacity: savingShipping || pressed ? 0.7 : 1,
                  backgroundColor: Brand.primary,
                  marginTop: Spacing.three,
                },
              ]}
              onPress={handleSaveShipping}
              disabled={savingShipping}
            >
              <Text style={styles.submitText}>
                {savingShipping ? 'Đang lưu...' : 'Lưu thông tin'}
              </Text>
            </Pressable>
          </Pressable>
        </Pressable>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  loadingContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: Spacing.four,
    paddingBottom: Spacing.three,
  },
  title: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.bold,
  },
  statusCard: {
    marginHorizontal: Spacing.four,
    marginBottom: Spacing.three,
    padding: Spacing.four,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
  },
  statusHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  orderCode: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
  },
  orderDate: {
    fontSize: FontSizes.xs,
    marginTop: 2,
  },
  refundBanner: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#ECFDF5',
    borderWidth: 1,
    borderColor: '#A7F3D0',
    borderRadius: BorderRadius.md,
    padding: Spacing.three,
    marginTop: Spacing.three,
    gap: Spacing.two,
  },
  refundBannerTitle: {
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.bold,
    color: Brand.success,
  },
  refundBannerDesc: {
    fontSize: FontSizes.xs,
    color: '#065F46',
    marginTop: 2,
  },
  repayButton: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: Brand.primary,
    paddingVertical: Spacing.three,
    borderRadius: BorderRadius.md,
    marginTop: Spacing.three,
  },
  repayButtonText: {
    color: '#FFFFFF',
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
  },
  section: {
    marginHorizontal: Spacing.four,
    marginBottom: Spacing.three,
    padding: Spacing.four,
    borderRadius: BorderRadius.lg,
    borderWidth: 1,
  },
  sectionHeaderRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.three,
  },
  sectionTitle: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
    marginBottom: Spacing.three,
  },
  editShippingBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    paddingHorizontal: Spacing.two,
    paddingVertical: 4,
    borderRadius: BorderRadius.sm,
  },
  editShippingText: {
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.bold,
  },
  editedChip: {
    paddingHorizontal: Spacing.two,
    paddingVertical: 4,
    borderRadius: BorderRadius.sm,
  },
  editedChipText: {
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.medium,
  },
  shippingRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.two,
    marginBottom: Spacing.two,
  },
  shippingText: {
    fontSize: FontSizes.sm,
    flex: 1,
  },
  itemRow: {
    flexDirection: 'row',
    paddingVertical: Spacing.three,
    borderTopWidth: 1,
    gap: Spacing.three,
  },
  itemImage: {
    width: 64,
    height: 64,
    borderRadius: BorderRadius.md,
    overflow: 'hidden',
    justifyContent: 'center',
    alignItems: 'center',
  },
  itemInfo: {
    flex: 1,
    justifyContent: 'center',
  },
  itemName: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
  },
  itemVariant: {
    fontSize: FontSizes.xs,
    marginTop: 2,
  },
  itemPriceRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: Spacing.one,
  },
  itemPrice: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
  },
  itemQty: {
    fontSize: FontSizes.xs,
  },
  reviewBtn: {
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.one,
    borderRadius: BorderRadius.sm,
  },
  reviewBtnText: {
    color: '#FFF',
    fontSize: FontSizes.xs,
    fontWeight: FontWeights.bold,
  },
  reviewedText: {
    fontSize: FontSizes.xs,
    fontStyle: 'italic',
  },
  priceRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: Spacing.two,
  },
  priceLabel: {
    fontSize: FontSizes.sm,
  },
  priceValue: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
  },
  totalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingTop: Spacing.two,
    marginTop: Spacing.one,
    marginBottom: Spacing.two,
    borderTopWidth: 1,
  },
  totalLabel: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
  },
  totalValue: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
  },
  bottomBar: {
    position: 'absolute',
    bottom: 0,
    left: 0,
    right: 0,
    padding: Spacing.four,
    borderTopWidth: 1,
  },
  cancelBtn: {
    backgroundColor: '#FEE2E2',
    paddingVertical: Spacing.three,
    borderRadius: BorderRadius.md,
    alignItems: 'center',
  },
  cancelText: {
    color: Brand.error,
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  modalSheet: {
    borderTopLeftRadius: BorderRadius.xl,
    borderTopRightRadius: BorderRadius.xl,
    padding: Spacing.four,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.three,
  },
  modalTitle: {
    fontSize: FontSizes.base,
    fontWeight: FontWeights.bold,
  },
  selectedProductCard: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: Spacing.two,
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    marginBottom: Spacing.three,
    gap: Spacing.two,
  },
  selectedProductImage: {
    width: 48,
    height: 48,
    borderRadius: BorderRadius.sm,
  },
  selectedProductName: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
  },
  ratingSection: {
    alignItems: 'center',
    marginBottom: Spacing.three,
  },
  ratingLabel: {
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.medium,
    marginBottom: Spacing.two,
  },
  ratingStars: {
    flexDirection: 'row',
    justifyContent: 'center',
  },
  reviewInput: {
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    padding: Spacing.three,
    textAlignVertical: 'top',
    fontSize: FontSizes.sm,
    minHeight: 80,
    marginBottom: Spacing.four,
  },
  submitBtn: {
    paddingVertical: Spacing.three,
    borderRadius: BorderRadius.md,
    alignItems: 'center',
  },
  submitText: {
    color: '#FFF',
    fontSize: FontSizes.sm,
    fontWeight: FontWeights.bold,
  },
  editWarningText: {
    fontSize: FontSizes.xs,
    marginBottom: Spacing.three,
    lineHeight: 16,
  },
  inputGroup: {
    marginBottom: Spacing.three,
  },
  inputLabel: {
    fontSize: FontSizes.xs,
    marginBottom: 4,
    fontWeight: FontWeights.medium,
  },
  inputField: {
    borderWidth: 1,
    borderRadius: BorderRadius.md,
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.two,
    fontSize: FontSizes.sm,
  },
});
