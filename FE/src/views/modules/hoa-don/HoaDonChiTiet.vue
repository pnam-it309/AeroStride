<script setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';
import { useNotifications } from '@/services/notificationService';
import {
  ChevronLeftIcon, ReceiptIcon, UserIcon, MapPinIcon,
  CreditCardIcon, TruckIcon, CalendarIcon, PackageIcon,
  CheckIcon, XIcon, PrinterIcon
} from 'vue-tabler-icons';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const order = ref(null);
const updatingStatus = ref(false);

const getStatusInfo = (status) => {
  switch (status) {
    case 1: return { text: 'Chờ xác nhận', color: 'warning', icon: 'mdi-clock-outline' };
    case 2: return { text: 'Đã xác nhận', color: 'info', icon: 'mdi-check-circle-outline' };
    case 3: return { text: 'Đang giao', color: 'primary', icon: 'mdi-truck-delivery-outline' };
    case 4: return { text: 'Hoàn thành', color: 'success', icon: 'mdi-checkbox-marked-circle-outline' };
    case 5: return { text: 'Đã hủy', color: 'error', icon: 'mdi-cancel' };
    default: return { text: 'Không xác định', color: 'grey', icon: 'mdi-help-circle-outline' };
  }
};

const loadOrderDetail = async () => {
  loading.value = true;
  try {
    const data = await dichVuHoaDon.layChiTietHoaDon(route.params.id);
    order.value = data;
  } catch (error) {
    addNotification({ title: 'Lỗi', subtitle: 'Không thể tải thông tin hóa đơn', color: 'error' });
  } finally {
    loading.value = false;
  }
};

const updateStatus = async (newStatus) => {
  updatingStatus.value = true;
  try {
    await dichVuHoaDon.capNhatTrangThaiHoaDon(order.value.id, newStatus);
    addNotification({ title: 'Thành công', subtitle: 'Đã cập nhật trạng thái đơn hàng', color: 'success' });
    await loadOrderDetail();
  } catch (error) {
    addNotification({ title: 'Lỗi', subtitle: 'Cập nhật thất bại', color: 'error' });
  } finally {
    updatingStatus.value = false;
  }
};

const formatCurrency = (val) => new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val || 0);
const formatDate = (date) => date ? new Date(date).toLocaleString('vi-VN') : 'N/A';

onMounted(loadOrderDetail);
</script>

<template>
  <v-container fluid class="pa-6 bg-grey-lighten-4 min-h-screen">
    <!-- Header -->
    <div class="d-flex align-center justify-space-between mb-6">
      <div class="d-flex align-center">
        <v-btn icon variant="tonal" color="primary" class="mr-4 rounded-lg" @click="router.back()">
          <ChevronLeftIcon size="24" />
        </v-btn>
        <div v-if="order">
          <div class="d-flex align-center">
            <h2 class="text-h4 font-weight-medium text-slate-900 mr-4">Hóa đơn #{{ order.maHoaDon }}</h2>
            <v-chip :color="getStatusInfo(order.trangThai).color" variant="flat" class="font-weight-medium px-4">
              {{ getStatusInfo(order.trangThai).text }}
            </v-chip>
          </div>
          <div class="text-subtitle-1 text-slate-500">Ngày tạo: {{ formatDate(order.ngayTao) }}</div>
        </div>
      </div>
      <div class="d-flex gap-3">
        <v-btn variant="outlined" color="primary" class="text-none font-weight-bold px-6 rounded-lg" prepend-icon="mdi-printer">
          In hóa đơn
        </v-btn>
        <v-menu v-if="order && order.trangThai < 4">
          <template v-slot:activator="{ props }">
            <v-btn color="primary" class="text-none font-weight-bold px-8 rounded-lg" v-bind="props" :loading="updatingStatus">
              Cập nhật trạng thái
            </v-btn>
          </template>
          <v-list class="rounded-lg shadow-xl">
            <v-list-item v-if="order.trangThai === 1" @click="updateStatus(2)" prepend-icon="mdi-check">
              <v-list-item-title>Xác nhận đơn hàng</v-list-item-title>
            </v-list-item>
            <v-list-item v-if="order.trangThai === 2" @click="updateStatus(3)" prepend-icon="mdi-truck">
              <v-list-item-title>Giao hàng</v-list-item-title>
            </v-list-item>
            <v-list-item v-if="order.trangThai === 3" @click="updateStatus(4)" prepend-icon="mdi-check-all">
              <v-list-item-title>Đã giao & Hoàn thành</v-list-item-title>
            </v-list-item>
            <v-divider></v-divider>
            <v-list-item @click="updateStatus(5)" color="error" prepend-icon="mdi-close">
              <v-list-item-title class="text-error">Hủy đơn hàng</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu>
      </div>
    </div>

    <v-row v-if="order">
      <!-- Left Column: Items & Timeline -->
      <v-col cols="12" lg="8">
        <!-- Products -->
        <v-card class="rounded-xl border shadow-none mb-6 overflow-hidden">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <PackageIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-medium">Sản phẩm đã đặt</span>
          </v-card-title>
          <v-table class="order-table">
            <thead class="bg-grey-lighten-5">
              <tr>
                <th class="py-4">Sản phẩm</th>
                <th class="text-center py-4">Số lượng</th>
                <th class="text-right py-4">Đơn giá</th>
                <th class="text-right py-4">Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in order.listsHoaDonChiTiet" :key="item.id">
                <td class="py-4">
                  <div class="d-flex align-center">
                    <div class="rounded-lg border bg-grey-lighten-4 mr-4 d-flex align-center justify-center p-2" style="width: 48px; height: 48px">
                      <v-icon>mdi-shoe-sneaker</v-icon>
                    </div>
                    <div>
                      <div class="font-weight-bold">{{ item.tenSanPham }}</div>
                      <div class="text-caption text-grey">Màu: {{ item.tenMauSac }} | Size: {{ item.tenKichThuoc }}</div>
                    </div>
                  </div>
                </td>
                <td class="text-center">{{ item.soLuong }}</td>
                <td class="text-right">{{ formatCurrency(item.donGia) }}</td>
                <td class="text-right font-weight-medium">{{ formatCurrency(item.thanhTien) }}</td>
              </tr>
            </tbody>
          </v-table>
          
          <v-divider></v-divider>
          
          <div class="pa-6 d-flex flex-column align-end bg-grey-lighten-5">
            <div class="d-flex justify-space-between mb-2" style="width: 250px">
              <span class="text-slate-500">Tạm tính:</span>
              <span class="font-weight-bold">{{ formatCurrency(order.tongTien) }}</span>
            </div>
            <div class="d-flex justify-space-between mb-2 text-error" style="width: 250px">
              <span>Giảm giá:</span>
              <span class="font-weight-bold">- {{ formatCurrency(order.tongTien - (order.tongTienSauGiam || order.tongTien)) }}</span>
            </div>
            <div class="d-flex justify-space-between mb-2" style="width: 250px">
              <span class="text-slate-500">Phí vận chuyển:</span>
              <span class="font-weight-bold">{{ formatCurrency(order.phiVanChuyen || 0) }}</span>
            </div>
            <v-divider class="my-4" style="width: 250px"></v-divider>
            <div class="d-flex justify-space-between" style="width: 250px">
              <span class="text-h6 font-weight-medium">Tổng cộng:</span>
              <span class="text-h5 font-weight-medium text-primary">{{ formatCurrency(order.tongTienSauGiam || order.tongTien) }}</span>
            </div>
          </div>
        </v-card>

        <!-- Status Timeline (Mockup for UI) -->
        <v-card class="rounded-xl border shadow-none">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <CalendarIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-medium">Lịch sử đơn hàng</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-timeline side="end" align="start">
              <v-timeline-item v-for="(log, idx) in order.listsLichSuHoaDon" :key="idx" :dot-color="getStatusInfo(log.trangThaiMoi).color" >
                <div class="d-flex justify-space-between">
                  <div>
                    <div class="font-weight-bold">{{ getStatusInfo(log.trangThaiMoi).text }}</div>
                    <div class="text-body-2 text-slate-600">{{ log.ghiChu || 'Cập nhật trạng thái hệ thống' }}</div>
                    <div class="text-caption text-primary mt-1">Người thực hiện: {{ log.nguoiThucHien || 'Hệ thống' }}</div>
                  </div>
                  <div class="text-caption text-grey">{{ formatDate(log.ngayTao) }}</div>
                </div>
              </v-timeline-item>
              
              <!-- Fallback if no history loaded -->
              <v-timeline-item v-if="!order.listsLichSuHoaDon || order.listsLichSuHoaDon.length === 0" dot-color="success" >
                <div class="d-flex justify-space-between">
                  <div>
                    <div class="font-weight-bold">Khởi tạo đơn hàng</div>
                    <div class="text-caption text-grey">Đơn hàng được ghi nhận vào hệ thống</div>
                  </div>
                  <div class="text-caption text-grey">{{ formatDate(order.ngayTao) }}</div>
                </div>
              </v-timeline-item>
            </v-timeline>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Right Column: Customer & Info -->
      <v-col cols="12" lg="4">
        <!-- Customer Info -->
        <v-card class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <UserIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-medium">Khách hàng</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <div class="d-flex align-center mb-4">
              <v-avatar color="primary-lighten-4" size="48" class="mr-4 rounded-lg">
                <span class="font-weight-medium text-primary">{{ (order.tenKhachHang || 'K').charAt(0) }}</span>
              </v-avatar>
              <div>
                <div class="font-weight-medium text-h6">{{ order.tenKhachHang || 'Khách lẻ' }}</div>
                <div class="text-subtitle-2 text-slate-500">{{ order.soDienThoai }}</div>
              </div>
            </div>
            <v-divider class="mb-4"></v-divider>
            <div class="d-flex align-center mb-2">
              <v-icon start  class="mr-2 text-grey">mdi-email-outline</v-icon>
              <span class="text-subtitle-2">{{ order.email || 'Chưa cung cấp email' }}</span>
            </div>
          </v-card-text>
        </v-card>

        <!-- Shipping Info -->
        <v-card class="rounded-xl border shadow-none mb-6">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <MapPinIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-medium">Giao hàng</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <div class="text-subtitle-2 font-weight-bold mb-1">Địa chỉ nhận hàng:</div>
            <div class="text-body-2 text-slate-600 mb-4">{{ order.diaChi || 'Nhận tại quầy' }}</div>
            
            <div class="text-subtitle-2 font-weight-bold mb-1">Phương thức vận chuyển:</div>
            <div class="d-flex align-center mb-4">
              <v-chip variant="tonal" color="primary" class="font-weight-bold">
                {{ order.loaiDon === 'TAI_QUAY' ? 'Nhận tại cửa hàng' : 'Giao hàng tận nơi' }}
              </v-chip>
            </div>

            <v-divider class="mb-4"></v-divider>
            
            <div class="text-subtitle-2 font-weight-bold mb-1">Ngày dự kiến nhận:</div>
            <div class="text-body-2 text-slate-600 mb-4">{{ order.ngayDuKienNhan ? formatDate(order.ngayDuKienNhan) : 'Chưa cập nhật' }}</div>

            <div class="text-subtitle-2 font-weight-bold mb-1">Ghi chú vận chuyển:</div>
            <div class="text-body-2 text-slate-600 italic">{{ order.ghiChu || 'Không có ghi chú' }}</div>
          </v-card-text>
        </v-card>

        <!-- Payment Info -->
        <v-card class="rounded-xl border shadow-none">
          <v-card-title class="pa-5 border-b d-flex align-center">
            <CreditCardIcon size="20" class="mr-2 text-primary" />
            <span class="font-weight-medium">Thanh toán</span>
          </v-card-title>
          <v-card-text class="pa-6">
            <v-divider class="mb-4"></v-divider>
            
            <div class="text-subtitle-2 font-weight-bold mb-3">Lịch sử giao dịch:</div>
            <v-list density="compact" class="pa-0">
              <v-list-item v-for="pay in order.listsGiaoDichThanhToan" :key="pay.id" class="px-0 mb-2">
                <template v-slot:prepend>
                  <v-icon :color="pay.loaiGiaoDich === 'TIEN_MAT' ? 'success' : 'primary'" size="20">
                    {{ pay.loaiGiaoDich === 'TIEN_MAT' ? 'mdi-cash' : 'mdi-bank' }}
                  </v-icon>
                </template>
                <div class="ml-2">
                  <div class="d-flex justify-space-between align-center">
                    <span class="font-weight-bold text-caption">{{ pay.tenPhuongThuc }}</span>
                    <span class="font-weight-bold text-primary">{{ formatCurrency(pay.soTien) }}</span>
                  </div>
                  <div class="text-caption text-grey text-truncate" style="max-width: 200px">
                    {{ pay.maGiaoDichNgoai || 'Giao dịch nội bộ' }}
                  </div>
                </div>
              </v-list-item>
            </v-list>
            
            <v-divider class="mb-4" v-if="order.listsGiaoDichThanhToan?.length > 0"></v-divider>
            <div class="d-flex align-center gap-2">
              <v-icon color="success">mdi-shield-check</v-icon>
              <span class="font-weight-bold text-success">Đã xác nhận thanh toán</span>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
    
    <div v-else-if="loading" class="d-flex flex-column align-center justify-center py-16">
      <v-progress-circular indeterminate color="primary" size="64"></v-progress-circular>
      <div class="mt-4 text-slate-500">Đang tải thông tin hóa đơn...</div>
    </div>
  </v-container>
</template>

<style scoped>
.order-table :deep(th) {
  background: transparent !important;
  font-weight: 700 !important;
  text-transform: uppercase;
  font-size: 0.75rem;
  letter-spacing: 0.05em;
  color: #64748b;
}
.gap-3 { gap: 12px; }
</style>
