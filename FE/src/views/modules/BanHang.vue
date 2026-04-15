<script setup>
import { ref, onMounted } from 'vue';
import { dichVuDonHang } from '@/services/sales/dichVuDonHang';

const loading = ref(false);
const orders = ref([]);
const selectedOrder = ref(null);
const searchQuery = ref('');
const showCreateOrderDialog = ref(false);
const showAddProductDialog = ref(false);

const loadOrders = async () => {
  loading.value = true;
  try {
    orders.value = await dichVuDonHang.layDonHangCho();
  } catch (error) {
    console.error('Error loading orders:', error);
  } finally {
    loading.value = false;
  }
};

const createNewOrder = async () => {
  try {
    const newOrder = await dichVuDonHang.taoDonHang();
    orders.value.unshift(newOrder);
    selectedOrder.value = newOrder;
    showCreateOrderDialog.value = false;
  } catch (error) {
    console.error('Error creating order:', error);
  }
};

const deleteOrder = async (orderId) => {
  try {
    await dichVuDonHang.xoaDonHang(orderId);
    orders.value = orders.value.filter(order => order.id !== orderId);
    if (selectedOrder.value?.id === orderId) {
      selectedOrder.value = null;
    }
  } catch (error) {
    console.error('Error deleting order:', error);
  }
};

const selectOrder = (order) => {
  selectedOrder.value = order;
};

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount);
};

onMounted(() => {
  loadOrders();
});
</script>

<template>
  <div>
    <!-- Header -->
    <div class="d-flex justify-space-between align-center mb-6">
      <div>
        <h1 class="text-h4 font-weight-bold">Bán hàng</h1>
        <p class="text-subtitle-1 text-medium-emphasis">Quản lý đơn hàng tại quầy</p>
      </div>
      <v-btn 
        color="primary" 
        size="large"
        @click="showCreateOrderDialog = true"
      >
        <v-icon start>mdi-plus</v-icon>
        Tạo đơn hàng mới
      </v-btn>
    </div>

    <v-row>
      <!-- Orders List -->
      <v-col cols="12" md="4">
        <v-card elevation="2">
          <v-card-title class="d-flex justify-space-between align-center">
            <span>Đơn hàng chờ</span>
            <v-chip color="primary" variant="tonal">{{ orders.length }}</v-chip>
          </v-card-title>
          
          <v-card-text class="pa-0">
            <v-text-field
              v-model="searchQuery"
              placeholder="Tìm kiếm đơn hàng..."
              prepend-inner-icon="mdi-magnify"
              variant="outlined"
              density="compact"
              class="ma-3"
            ></v-text-field>
            
            <v-list v-if="orders.length > 0">
              <v-list-item
                v-for="order in orders"
                :key="order.id"
                @click="selectOrder(order)"
                :active="selectedOrder?.id === order.id"
                class="cursor-pointer"
              >
                <template v-slot:prepend>
                  <v-icon color="primary">mdi-receipt</v-icon>
                </template>
                <v-list-item-title class="font-weight-medium">
                  {{ order.id || 'Đơn hàng mới' }}
                </v-list-item-title>
                <v-list-item-subtitle>
                  {{ formatCurrency(order.totalAmount || 0) }}
                </v-list-item-subtitle>
                <template v-slot:append>
                  <v-btn
                    icon="mdi-delete"
                    variant="text"
                    color="error"
                    size="small"
                    @click.stop="deleteOrder(order.id)"
                  ></v-btn>
                </template>
              </v-list-item>
            </v-list>
            
            <v-card-text v-else class="text-center py-8">
              <v-icon size="48" color="grey-lighten-1" class="mb-2">mdi-receipt-outline</v-icon>
              <p class="text-medium-emphasis">Chưa có đơn hàng nào</p>
            </v-card-text>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Order Details -->
      <v-col cols="12" md="8">
        <v-card elevation="2" v-if="selectedOrder">
          <v-card-title>
            <span>Chi tiết đơn hàng: {{ selectedOrder.id }}</span>
          </v-card-title>
          
          <v-card-text>
            <v-row>
              <v-col cols="12" md="6">
                <v-text-field
                  label="Khách hàng"
                  placeholder="Nhập tên khách hàng"
                  variant="outlined"
                  prepend-inner-icon="mdi-account"
                ></v-text-field>
              </v-col>
              <v-col cols="12" md="6">
                <v-text-field
                  label="Số điện thoại"
                  placeholder="Nhập số điện thoại"
                  variant="outlined"
                  prepend-inner-icon="mdi-phone"
                ></v-text-field>
              </v-col>
            </v-row>

            <v-divider class="my-4"></v-divider>

            <div class="d-flex justify-space-between align-center mb-3">
              <h3 class="text-h6">Sản phẩm</h3>
              <v-btn
                color="primary"
                variant="tonal"
                @click="showAddProductDialog = true"
              >
                <v-icon start>mdi-plus</v-icon>
                Thêm sản phẩm
              </v-btn>
            </div>

            <v-table>
              <thead>
                <tr>
                  <th>Sản phẩm</th>
                  <th>Size</th>
                  <th>Đơn giá</th>
                  <th>Số lượng</th>
                  <th>Thành tiền</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="!selectedOrder.items || selectedOrder.items.length === 0">
                  <td colspan="6" class="text-center py-4">
                    <p class="text-medium-emphasis">Chưa có sản phẩm nào</p>
                  </td>
                </tr>
                <tr v-else>
                  <!-- Order items will be displayed here -->
                </tr>
              </tbody>
            </v-table>

            <v-divider class="my-4"></v-divider>

            <div class="d-flex justify-space-between align-center">
              <div>
                <v-text-field
                  label="Ghi chú"
                  placeholder="Nhập ghi chú cho đơn hàng"
                  variant="outlined"
                  rows="2"
                ></v-text-field>
              </div>
              <div class="text-right">
                <p class="text-h5 font-weight-bold">
                  Tổng: {{ formatCurrency(selectedOrder.totalAmount || 0) }}
                </p>
                <v-btn
                  color="success"
                  size="large"
                  class="mt-2"
                  :disabled="!selectedOrder.items || selectedOrder.items.length === 0"
                >
                  <v-icon start>mdi-cash-register</v-icon>
                  Thanh toán
                </v-btn>
              </div>
            </div>
          </v-card-text>
        </v-card>

        <v-card elevation="2" v-else class="text-center py-12">
          <v-icon size="64" color="grey-lighten-1" class="mb-4">mdi-receipt-outline</v-icon>
          <h3 class="text-h5 font-weight-medium mb-2">Chọn đơn hàng</h3>
          <p class="text-medium-emphasis">Vui lòng chọn một đơn hàng để xem chi tiết</p>
        </v-card>
      </v-col>
    </v-row>

    <!-- Create Order Dialog -->
    <v-dialog v-model="showCreateOrderDialog" max-width="400">
      <v-card>
        <v-card-title>Tạo đơn hàng mới</v-card-title>
        <v-card-text>
          <p>Bạn có chắc muốn tạo một đơn hàng mới không?</p>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn @click="showCreateOrderDialog = false">Hủy</v-btn>
          <v-btn color="primary" @click="createNewOrder">Tạo mới</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Add Product Dialog -->
    <v-dialog v-model="showAddProductDialog" max-width="800">
      <v-card>
        <v-card-title>Thêm sản phẩm</v-card-title>
        <v-card-text>
          <p>Tính năng thêm sản phẩm sẽ được triển khai sau</p>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn @click="showAddProductDialog = false">Đóng</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>
