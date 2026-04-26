<script setup>
import { ref, onMounted } from 'vue';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';

const loading = ref(false);
const selectedPeriod = ref('month');
const selectedYear = ref(new Date().getFullYear());
const selectedMonth = ref(new Date().getMonth() + 1);

const revenueStats = ref({
  totalRevenue: 0,
  totalOrders: 0,
  averageOrderValue: 0,
  growthRate: 0
});

const topProducts = ref([]);
const salesByCategory = ref([]);
const monthlyRevenue = ref([]);

const periodOptions = [
  { title: 'Hôm nay', value: 'today' },
  { title: 'Tuần này', value: 'week' },
  { title: 'Tháng này', value: 'month' },
  { title: 'Quý này', value: 'quarter' },
  { title: 'Năm này', value: 'year' }
];

const loadStatistics = async () => {
  loading.value = true;
  try {
    // Mock data for demonstration
    revenueStats.value = {
      totalRevenue: 2500000000,
      totalOrders: 1250,
      averageOrderValue: 2000000,
      growthRate: 15.5
    };

    topProducts.value = [
      { name: 'Nike Air Max 270', revenue: 450000000, quantity: 180, growth: 12.5 },
      { name: 'Adidas Ultra Boost', revenue: 380000000, quantity: 152, growth: 8.3 },
      { name: 'Converse Chuck 70', revenue: 320000000, quantity: 200, growth: -2.1 },
      { name: 'Vans Old Skool', revenue: 280000000, quantity: 140, growth: 18.7 },
      { name: 'New Balance 574', revenue: 250000000, quantity: 125, growth: 6.2 }
    ];

    salesByCategory.value = [
      { name: 'Giày chạy bộ', value: 850000000, percentage: 34 },
      { name: 'Giày thời trang', value: 650000000, percentage: 26 },
      { name: 'Giày篮球', value: 500000000, percentage: 20 },
      { name: 'Giày casual', value: 350000000, percentage: 14 },
      { name: 'Khác', value: 150000000, percentage: 6 }
    ];

    monthlyRevenue.value = [
      { month: 'T1', revenue: 180000000 },
      { month: 'T2', revenue: 220000000 },
      { month: 'T3', revenue: 195000000 },
      { month: 'T4', revenue: 240000000 },
      { month: 'T5', revenue: 210000000 },
      { month: 'T6', revenue: 280000000 },
      { month: 'T7', revenue: 320000000 },
      { month: 'T8', revenue: 290000000 },
      { month: 'T9', revenue: 260000000 },
      { month: 'T10', revenue: 310000000 },
      { month: 'T11', revenue: 285000000 },
      { month: 'T12', revenue: 330000000 }
    ];
  } catch (error) {
    console.error('Error loading statistics:', error);
  } finally {
    loading.value = false;
  }
};

const formatCurrency = (amount) => {
  return new Intl.NumberFormat('vi-VN', {
    style: 'currency',
    currency: 'VND'
  }).format(amount);
};

const formatNumber = (num) => {
  return new Intl.NumberFormat('vi-VN').format(num);
};

const getGrowthColor = (growth) => {
  return growth >= 0 ? 'success' : 'error';
};

const getGrowthIcon = (growth) => {
  return growth >= 0 ? 'mdi-trending-up' : 'mdi-trending-down';
};

onMounted(() => {
  loadStatistics();
});
</script>
<template>
  <div class="pa-6 font-body">
    <!-- Breadcrumbs -->
    <AdminBreadcrumbs
        :items="[
            { title: 'Quản lý bán hàng', disabled: false, href: '#' },
            { title: 'Thống kê', disabled: true }
        ]"
    />

    <div class="mb-4"></div>

    <!-- Period Selector -->
    <v-card class="premium-card mb-6">
      <v-card-text class="pa-6">
        <v-row align="center">
          <v-col cols="12" md="3">
            <div class="filter-field-label">Kỳ thống kê</div>
            <v-select
              v-model="selectedPeriod"
              :items="periodOptions"
              variant="outlined"
              density="compact"
              hide-details
              @update:model-value="loadStatistics"
            ></v-select>
          </v-col>
          <v-col cols="12" md="2">
            <div class="filter-field-label">Năm</div>
            <v-select
              v-model="selectedYear"
              :items="Array.from({length: 5}, (_, i) => new Date().getFullYear() - i)"
              variant="outlined"
              density="compact"
              hide-details
              @update:model-value="loadStatistics"
            ></v-select>
          </v-col>
          <v-col cols="12" md="2" v-if="selectedPeriod === 'month'">
            <div class="filter-field-label">Tháng</div>
            <v-select
              v-model="selectedMonth"
              :items="Array.from({length: 12}, (_, i) => ({ title: `Tháng ${i + 1}`, value: i + 1 }))"
              variant="outlined"
              density="compact"
              hide-details
              @update:model-value="loadStatistics"
            ></v-select>
          </v-col>
          <v-col cols="12" md="auto" class="ml-auto align-self-end">
            <v-btn color="primary" variant="flat" @click="loadStatistics" class="px-6 font-weight-bold" height="40">
              <v-icon start size="18">mdi-refresh</v-icon>
              Cập nhật dữ liệu
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- Revenue Statistics -->
    <v-row class="mb-2">
      <v-col cols="12" sm="6" md="3">
        <v-card class="premium-card pa-6 h-100">
          <div class="d-flex align-center justify-space-between mb-4">
            <div class="icon-blob bg-primary-light">
              <v-icon color="primary">mdi-currency-usd</v-icon>
            </div>
          </div>
          <div>
            <p class="text-caption text-slate-500 font-weight-medium text-uppercase mb-1 tracking-wider">Tổng doanh thu</p>
            <p class="text-h5 font-weight-bold text-dark">{{ formatCurrency(revenueStats.totalRevenue) }}</p>
            <div class="d-flex align-center mt-2">
              <v-chip size="x-small" :color="getGrowthColor(revenueStats.growthRate)" variant="flat" class="font-weight-medium px-2">
                <v-icon start size="12" :icon="getGrowthIcon(revenueStats.growthRate)"></v-icon>
                {{ Math.abs(revenueStats.growthRate) }}%
              </v-chip>
              <span class="text-caption text-slate-400 ml-2">So với kỳ trước</span>
            </div>
          </div>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
        <v-card class="premium-card pa-6 h-100">
          <div class="d-flex align-center justify-space-between mb-4">
            <div class="icon-blob bg-success-light">
              <v-icon color="success">mdi-shopping</v-icon>
            </div>
          </div>
          <div>
            <p class="text-caption text-slate-500 font-weight-medium text-uppercase mb-1 tracking-wider">Tổng đơn hàng</p>
            <p class="text-h5 font-weight-bold text-dark">{{ formatNumber(revenueStats.totalOrders) }}</p>
            <p class="text-caption text-slate-400 mt-2">Trong thời gian này</p>
          </div>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
        <v-card class="premium-card pa-6 h-100">
          <div class="d-flex align-center justify-space-between mb-4">
            <div class="icon-blob bg-info-light">
              <v-icon color="info">mdi-chart-line</v-icon>
            </div>
          </div>
          <div>
            <p class="text-caption text-slate-500 font-weight-medium text-uppercase mb-1 tracking-wider">Giá trị trung bình</p>
            <p class="text-h5 font-weight-bold text-dark">{{ formatCurrency(revenueStats.averageOrderValue) }}</p>
            <p class="text-caption text-slate-400 mt-2">Mỗi đơn hàng thành công</p>
          </div>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
        <v-card class="premium-card pa-6 h-100">
          <div class="d-flex align-center justify-space-between mb-4">
            <div class="icon-blob bg-warning-light">
              <v-icon color="warning">mdi-trending-up</v-icon>
            </div>
          </div>
          <div>
            <p class="text-caption text-slate-500 font-weight-medium text-uppercase mb-1 tracking-wider">Tỷ lệ tăng trưởng</p>
            <p class="text-h5 font-weight-bold" :class="'text-' + getGrowthColor(revenueStats.growthRate)">
              {{ revenueStats.growthRate }}%
            </p>
            <p class="text-caption text-slate-400 mt-2">Hiệu suất kinh doanh</p>
          </div>
        </v-card>
      </v-col>
    </v-row>

    <div class="mb-4"></div>

    <v-row>
      <!-- Top Products -->
      <v-col cols="12" lg="6">
        <v-card class="premium-card h-100">
          <div class="card-title-bar">
            <span class="font-weight-bold text-dark text-uppercase" style="font-size: 13px; letter-spacing: 0.05em;">Sản phẩm bán chạy</span>
            <v-icon color="slate-400">mdi-crown-outline</v-icon>
          </div>
          <v-list class="pa-4">
            <v-list-item v-for="(product, index) in topProducts" :key="product.name" class="rounded-lg mb-2 bg-slate-50">
              <template v-slot:prepend>
                <div class="text-h6 font-weight-bold text-primary mr-4" style="min-width: 24px;">{{ index + 1 }}</div>
              </template>
              <v-list-item-title class="font-weight-medium text-slate-900">{{ product.name }}</v-list-item-title>
              <v-list-item-subtitle class="font-weight-medium">
                {{ formatNumber(product.quantity) }} sản phẩm <span class="mx-1">•</span> {{ formatCurrency(product.revenue) }}
              </v-list-item-subtitle>
              <template v-slot:append>
                <v-chip size="x-small" :color="getGrowthColor(product.growth)" variant="tonal" class="font-weight-medium px-2">
                  {{ product.growth >= 0 ? '+' : '' }}{{ product.growth }}%
                </v-chip>
              </template>
            </v-list-item>
          </v-list>
        </v-card>
      </v-col>

      <!-- Sales by Category -->
      <v-col cols="12" lg="6">
        <v-card class="premium-card h-100">
          <div class="card-title-bar">
            <span class="font-weight-bold text-dark text-uppercase" style="font-size: 13px; letter-spacing: 0.05em;">Doanh thu theo danh mục</span>
            <v-icon color="slate-400">mdi-chart-pie</v-icon>
          </div>
          <v-card-text class="pa-6">
            <div v-for="category in salesByCategory" :key="category.name" class="mb-6">
              <div class="d-flex justify-space-between align-center mb-2">
                <span class="font-weight-medium text-slate-700">{{ category.name }}</span>
                <span class="text-primary font-weight-bold">{{ formatCurrency(category.value) }}</span>
              </div>
              <v-progress-linear
                :model-value="category.percentage"
                color="primary"
                height="10"
                rounded
              ></v-progress-linear>
               <div class="text-right mt-1">
                <span class="text-caption text-slate-400 font-weight-medium">{{ category.percentage }}%</span>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Monthly Revenue Grid -->
      <v-col cols="12">
        <v-card class="premium-card">
          <div class="card-title-bar">
            <span class="font-weight-bold text-dark text-uppercase" style="font-size: 13px; letter-spacing: 0.05em;">Doanh thu theo tháng (Năm {{ selectedYear }})</span>
            <v-icon color="slate-400">mdi-calendar-month</v-icon>
          </div>
          <v-card-text class="pa-6">
            <v-row>
              <v-col 
                v-for="month in monthlyRevenue" 
                :key="month.month"
                cols="12" sm="6" md="3" lg="2"
              >
                 <div class="pa-4 text-center rounded-lg border bg-slate-50 hover-addr-card h-100">
                  <div class="text-caption text-slate-500 font-weight-medium text-uppercase mb-2 tracking-wider">{{ month.month }}</div>
                  <div class="text-subtitle-1 font-weight-bold text-dark">{{ formatCurrency(month.revenue) }}</div>
                </div>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>

<style scoped>
.bg-info-light { background: #e0f2fe; }
.bg-warning-light { background: #fff7ed; }
.bg-primary-light { background: #eff6ff; }
.bg-success-light { background: #ecfdf5; }
</style>




