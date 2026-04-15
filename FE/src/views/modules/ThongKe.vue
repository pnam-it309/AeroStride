<script setup>
import { ref, onMounted } from 'vue';

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
  <div>
    <!-- Header -->
    <div class="d-flex justify-space-between align-center mb-6">
      <div>
        <h1 class="text-h4 font-weight-bold">Thống kê & Báo cáo</h1>
        <p class="text-subtitle-1 text-medium-emphasis">Phân tích dữ liệu kinh doanh</p>
      </div>
    </div>

    <!-- Period Selector -->
    <v-card elevation="2" class="mb-4">
      <v-card-text>
        <v-row align="center">
          <v-col cols="12" md="3">
            <v-select
              v-model="selectedPeriod"
              :items="periodOptions"
              label="Kỳ thống kê"
              variant="outlined"
              @update:model-value="loadStatistics"
            ></v-select>
          </v-col>
          <v-col cols="12" md="2">
            <v-select
              v-model="selectedYear"
              :items="Array.from({length: 5}, (_, i) => new Date().getFullYear() - i)"
              label="Năm"
              variant="outlined"
              @update:model-value="loadStatistics"
            ></v-select>
          </v-col>
          <v-col cols="12" md="2" v-if="selectedPeriod === 'month'">
            <v-select
              v-model="selectedMonth"
              :items="Array.from({length: 12}, (_, i) => ({ title: `Tháng ${i + 1}`, value: i + 1 }))"
              label="Tháng"
              variant="outlined"
              @update:model-value="loadStatistics"
            ></v-select>
          </v-col>
          <v-col cols="12" md="2">
            <v-btn color="primary" variant="tonal" @click="loadStatistics" class="mt-1">
              <v-icon start>mdi-refresh</v-icon>
              Làm mới
            </v-btn>
          </v-col>
        </v-row>
      </v-card-text>
    </v-card>

    <!-- Revenue Statistics -->
    <v-row class="mb-4">
      <v-col cols="12" sm="6" md="3">
        <v-card elevation="2" class="pa-4">
          <div class="d-flex align-center justify-space-between">
            <div>
              <p class="text-subtitle-2 text-medium-emphasis mb-1">Tổng doanh thu</p>
              <p class="text-h4 font-weight-bold text-primary">{{ formatCurrency(revenueStats.totalRevenue) }}</p>
              <div class="d-flex align-center mt-2">
                <v-icon 
                  :color="getGrowthColor(revenueStats.growthRate)" 
                  size="small"
                  :icon="getGrowthIcon(revenueStats.growthRate)"
                ></v-icon>
                <span 
                  :class="'text-' + getGrowthColor(revenueStats.growthRate)"
                  class="text-caption ml-1"
                >
                  {{ Math.abs(revenueStats.growthRate) }}%
                </span>
              </div>
            </div>
            <v-icon size="40" color="primary">mdi-currency-usd</v-icon>
          </div>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
        <v-card elevation="2" class="pa-4">
          <div class="d-flex align-center justify-space-between">
            <div>
              <p class="text-subtitle-2 text-medium-emphasis mb-1">Tổng đơn hàng</p>
              <p class="text-h4 font-weight-bold text-success">{{ formatNumber(revenueStats.totalOrders) }}</p>
              <p class="text-caption text-medium-emphasis mt-1">Trong kỳ</p>
            </div>
            <v-icon size="40" color="success">mdi-shopping</v-icon>
          </div>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
        <v-card elevation="2" class="pa-4">
          <div class="d-flex align-center justify-space-between">
            <div>
              <p class="text-subtitle-2 text-medium-emphasis mb-1">Giá trị trung bình</p>
              <p class="text-h4 font-weight-bold text-info">{{ formatCurrency(revenueStats.averageOrderValue) }}</p>
              <p class="text-caption text-medium-emphasis mt-1">Mỗi đơn hàng</p>
            </div>
            <v-icon size="40" color="info">mdi-chart-line</v-icon>
          </div>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="3">
        <v-card elevation="2" class="pa-4">
          <div class="d-flex align-center justify-space-between">
            <div>
              <p class="text-subtitle-2 text-medium-emphasis mb-1">Tỷ lệ tăng trưởng</p>
              <p class="text-h4 font-weight-bold" :class="'text-' + getGrowthColor(revenueStats.growthRate)">
                {{ revenueStats.growthRate }}%
              </p>
              <p class="text-caption text-medium-emphasis mt-1">So với kỳ trước</p>
            </div>
            <v-icon 
              size="40" 
              :color="getGrowthColor(revenueStats.growthRate)"
              :icon="getGrowthIcon(revenueStats.growthRate)"
            ></v-icon>
          </div>
        </v-card>
      </v-col>
    </v-row>

    <v-row>
      <!-- Top Products -->
      <v-col cols="12" lg="6">
        <v-card elevation="2">
          <v-card-title>
            <span>Sản phẩm bán chạy</span>
          </v-card-title>
          <v-card-text>
            <v-list>
              <v-list-item v-for="(product, index) in topProducts" :key="product.name">
                <template v-slot:prepend>
                  <div class="text-h6 font-weight-bold text-primary mr-3">{{ index + 1 }}</div>
                </template>
                <v-list-item-title class="font-weight-medium">{{ product.name }}</v-list-item-title>
                <v-list-item-subtitle>
                  {{ formatNumber(product.quantity) }} đã bán • {{ formatCurrency(product.revenue) }}
                </v-list-item-subtitle>
                <template v-slot:append>
                  <div class="text-right">
                    <div class="d-flex align-center">
                      <v-icon 
                        :color="getGrowthColor(product.growth)" 
                        size="small"
                        :icon="getGrowthIcon(product.growth)"
                      ></v-icon>
                      <span 
                        :class="'text-' + getGrowthColor(product.growth)"
                        class="text-caption ml-1"
                      >
                        {{ Math.abs(product.growth) }}%
                      </span>
                    </div>
                  </div>
                </template>
              </v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Sales by Category -->
      <v-col cols="12" lg="6">
        <v-card elevation="2">
          <v-card-title>
            <span>Doanh thu theo danh mục</span>
          </v-card-title>
          <v-card-text>
            <div v-for="category in salesByCategory" :key="category.name" class="mb-3">
              <div class="d-flex justify-space-between align-center mb-1">
                <span class="font-weight-medium">{{ category.name }}</span>
                <span class="text-primary font-weight-medium">{{ formatCurrency(category.value) }}</span>
              </div>
              <v-progress-linear
                :model-value="category.percentage"
                color="primary"
                height="8"
                rounded
              ></v-progress-linear>
              <div class="text-right mt-1">
                <span class="text-caption text-medium-emphasis">{{ category.percentage }}%</span>
              </div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Monthly Revenue Chart -->
      <v-col cols="12">
        <v-card elevation="2">
          <v-card-title>
            <span>Doanh thu theo tháng</span>
          </v-card-title>
          <v-card-text>
            <v-row>
              <v-col 
                v-for="month in monthlyRevenue" 
                :key="month.month"
                cols="12" 
                sm="6" 
                md="4" 
                lg="3"
              >
                <v-card variant="tonal" color="primary" class="pa-3 text-center">
                  <div class="text-h6 font-weight-bold text-primary">{{ month.month }}</div>
                  <div class="text-subtitle-1 font-weight-medium">{{ formatCurrency(month.revenue) }}</div>
                </v-card>
              </v-col>
            </v-row>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </div>
</template>
