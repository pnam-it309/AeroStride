<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { dichVuXacThuc } from '@/services/auth/dichVuXacThuc';

const router = useRouter();
const loading = ref(false);
const stats = ref({
  totalOrders: 0,
  totalRevenue: 0,
  totalCustomers: 0,
  totalProducts: 0,
  pendingOrders: 0,
  todayOrders: 0
});

const recentOrders = ref([]);
const topProducts = ref([]);

const currentUser = dichVuXacThuc.layUserHienTai();

const navigateToModule = (path) => {
  router.push(path);
};

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount);
};

const loadDashboardData = async () => {
  loading.value = true;
  try {
    // Load dashboard statistics
    // This would be implemented with actual API calls
    stats.value = {
      totalOrders: 1250,
      totalRevenue: 2500000000,
      totalCustomers: 850,
      totalProducts: 156,
      pendingOrders: 23,
      todayOrders: 18
    };
    
    recentOrders.value = [
      { id: 'HD001', customer: 'Nguyễn Văn A', amount: 2500000, status: 'completed', date: '2024-01-15' },
      { id: 'HD002', customer: 'Trần Thị B', amount: 1800000, status: 'pending', date: '2024-01-15' },
      { id: 'HD003', customer: 'Lê Văn C', amount: 3200000, status: 'processing', date: '2024-01-14' }
    ];
    
    topProducts.value = [
      { name: 'Nike Air Max 270', sold: 45, revenue: 135000000 },
      { name: 'Adidas Ultra Boost', sold: 38, revenue: 114000000 },
      { name: 'Converse Chuck 70', sold: 32, revenue: 64000000 }
    ];
  } catch (error) {
    console.error('Error loading dashboard data:', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadDashboardData();
});
</script>

<template>
    <v-row>
        <!-- Welcome Section -->
        <v-col cols="12">
            <v-card class="pa-6" elevation="2">
                <div class="d-flex align-center justify-space-between">
                    <div>
                        <h2 class="text-h4 font-weight-bold mb-2">Chào mừng trở lại, {{ currentUser?.username || 'Admin' }}!</h2>
                        <p class="text-subtitle-1 text-medium-emphasis">Quản lý cửa hàng giày AeroStride của bạn</p>
                    </div>
                    <div class="text-right">
                        <p class="text-caption text-medium-emphasis">Vai trò:</p>
                        <p class="text-h6 font-weight-medium text-primary">{{ currentUser?.role || 'ADMIN' }}</p>
                    </div>
                </div>
            </v-card>
        </v-col>

        <!-- Statistics Cards -->
        <v-col cols="12" sm="6" md="3">
            <v-card class="pa-4 text-center" elevation="2" color="primary" variant="tonal">
                <v-icon size="40" color="primary" class="mb-2">mdi-shopping</v-icon>
                <div class="text-h4 font-weight-bold text-primary">{{ stats.totalOrders }}</div>
                <div class="text-subtitle-1 text-medium-emphasis">Tổng đơn hàng</div>
            </v-card>
        </v-col>

        <v-col cols="12" sm="6" md="3">
            <v-card class="pa-4 text-center" elevation="2" color="success" variant="tonal">
                <v-icon size="40" color="success" class="mb-2">mdi-currency-usd</v-icon>
                <div class="text-h4 font-weight-bold text-success">{{ formatCurrency(stats.totalRevenue) }}</div>
                <div class="text-subtitle-1 text-medium-emphasis">Doanh thu</div>
            </v-card>
        </v-col>

        <v-col cols="12" sm="6" md="3">
            <v-card class="pa-4 text-center" elevation="2" color="info" variant="tonal">
                <v-icon size="40" color="info" class="mb-2">mdi-account-group</v-icon>
                <div class="text-h4 font-weight-bold text-info">{{ stats.totalCustomers }}</div>
                <div class="text-subtitle-1 text-medium-emphasis">Khách hàng</div>
            </v-card>
        </v-col>

        <v-col cols="12" sm="6" md="3">
            <v-card class="pa-4 text-center" elevation="2" color="warning" variant="tonal">
                <v-icon size="40" color="warning" class="mb-2">mdi-package-variant</v-icon>
                <div class="text-h4 font-weight-bold text-warning">{{ stats.totalProducts }}</div>
                <div class="text-subtitle-1 text-medium-emphasis">Sản phẩm</div>
            </v-card>
        </v-col>

        <!-- Quick Actions -->
        <v-col cols="12">
            <v-card class="pa-6" elevation="2">
                <h3 class="text-h5 font-weight-bold mb-4">Hành động nhanh</h3>
                <v-row>
                    <v-col cols="12" sm="6" md="3">
                        <v-btn 
                            block 
                            size="large" 
                            color="primary" 
                            variant="tonal"
                            @click="navigateToModule('/main/ban-hang')"
                            class="pa-4"
                        >
                            <v-icon start>mdi-point-of-sale</v-icon>
                            Bán hàng
                        </v-btn>
                    </v-col>
                    <v-col cols="12" sm="6" md="3">
                        <v-btn 
                            block 
                            size="large" 
                            color="success" 
                            variant="tonal"
                            @click="navigateToModule('/main/san-pham')"
                            class="pa-4"
                        >
                            <v-icon start>mdi-package-variant</v-icon>
                            Quản lý sản phẩm
                        </v-btn>
                    </v-col>
                    <v-col cols="12" sm="6" md="3">
                        <v-btn 
                            block 
                            size="large" 
                            color="info" 
                            variant="tonal"
                            @click="navigateToModule('/main/khach-hang')"
                            class="pa-4"
                        >
                            <v-icon start>mdi-account-group</v-icon>
                            Khách hàng
                        </v-btn>
                    </v-col>
                    <v-col cols="12" sm="6" md="3">
                        <v-btn 
                            block 
                            size="large" 
                            color="warning" 
                            variant="tonal"
                            @click="navigateToModule('/main/hoa-don')"
                            class="pa-4"
                        >
                            <v-icon start>mdi-receipt</v-icon>
                            Hóa đơn
                        </v-btn>
                    </v-col>
                </v-row>
            </v-card>
        </v-col>

        <!-- Recent Orders -->
        <v-col cols="12" lg="8">
            <v-card class="pa-6" elevation="2">
                <div class="d-flex align-center justify-space-between mb-4">
                    <h3 class="text-h5 font-weight-bold">Đơn hàng gần đây</h3>
                    <v-btn 
                        variant="tonal" 
                        color="primary"
                        @click="navigateToModule('/main/hoa-don')"
                    >
                        Xem tất cả
                    </v-btn>
                </div>
                <v-table>
                    <thead>
                        <tr>
                            <th>Mã đơn</th>
                            <th>Khách hàng</th>
                            <th>Số tiền</th>
                            <th>Trạng thái</th>
                            <th>Ngày</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="order in recentOrders" :key="order.id">
                            <td class="font-weight-medium">{{ order.id }}</td>
                            <td>{{ order.customer }}</td>
                            <td>{{ formatCurrency(order.amount) }}</td>
                            <td>
                                <v-chip 
                                    :color="order.status === 'completed' ? 'success' : order.status === 'pending' ? 'warning' : 'info'"
                                    size="small"
                                    variant="tonal"
                                >
                                    {{ order.status === 'completed' ? 'Hoàn thành' : order.status === 'pending' ? 'Chờ xử lý' : 'Đang xử lý' }}
                                </v-chip>
                            </td>
                            <td>{{ order.date }}</td>
                        </tr>
                    </tbody>
                </v-table>
            </v-card>
        </v-col>

        <!-- Top Products -->
        <v-col cols="12" lg="4">
            <v-card class="pa-6" elevation="2">
                <h3 class="text-h5 font-weight-bold mb-4">Sản phẩm bán chạy</h3>
                <v-list>
                    <v-list-item v-for="(product, index) in topProducts" :key="product.name">
                        <template v-slot:prepend>
                            <div class="text-h6 font-weight-bold text-primary mr-3">{{ index + 1 }}</div>
                        </template>
                        <v-list-item-title class="font-weight-medium">{{ product.name }}</v-list-item-title>
                        <v-list-item-subtitle>
                            {{ product.sold }} đã bán • {{ formatCurrency(product.revenue) }}
                        </v-list-item-subtitle>
                    </v-list-item>
                </v-list>
            </v-card>
        </v-col>
    </v-row>
</template>
