<script setup>
import { ref, onMounted } from 'vue';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import { dichVuThongKe } from '@/services/admin/dichVuThongKe';
import apexchart from 'vue3-apexcharts';

const loading = ref(false);
const selectedPeriod = ref('month');
const selectedYear = ref(new Date().getFullYear());
const selectedMonth = ref(new Date().getMonth() + 1);

const revenueStats = ref({
  totalRevenue: 0,
  totalOrders: 0,
  averageOrderValue: 0,
  growthRate: 0,
  donHangHoanThanh: 0,
  donHangChoXacNhan: 0,
  donHangDangGiao: 0,
  donHangDaHuy: 0,
  tongKhachHang: 0,
  sanPhamSapHet: 0
});

const topProducts = ref([]);
const salesByCategory = ref([]);
const monthlyRevenue = ref([]);

// Cấu hình reactive cho ApexCharts
const areaChartSeries = ref([
  {
    name: 'Doanh thu',
    data: []
  }
]);

const areaChartOptions = ref({
  chart: {
    type: 'area',
    height: 320,
    toolbar: {
      show: false
    },
    zoom: {
      enabled: false
    },
    fontFamily: 'Inter, system-ui, -apple-system, sans-serif'
  },
  colors: ['#4f46e5'],
  dataLabels: {
    enabled: false
  },
  stroke: {
    curve: 'smooth',
    width: 3
  },
  fill: {
    type: 'gradient',
    gradient: {
      shadeIntensity: 1,
      opacityFrom: 0.35,
      opacityTo: 0.05,
      stops: [0, 95]
    }
  },
  grid: {
    borderColor: '#f1f5f9',
    strokeDashArray: 4
  },
  xaxis: {
    categories: ['T1', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'T8', 'T9', 'T10', 'T11', 'T12'],
    axisBorder: {
      show: false
    },
    axisTicks: {
      show: false
    },
    labels: {
      style: {
        colors: '#64748b',
        fontSize: '11px',
        fontWeight: 500
      }
    }
  },
  yaxis: {
    labels: {
      formatter: function (value) {
        if (value >= 1e9) {
          return (value / 1e9).toFixed(1) + ' tỷ';
        } else if (value >= 1e6) {
          return (value / 1e6).toFixed(0) + ' tr';
        } else if (value >= 1e3) {
          return (value / 1e3).toFixed(0) + ' k';
        }
        return value;
      },
      style: {
        colors: '#64748b',
        fontSize: '11px',
        fontWeight: 500
      }
    }
  },
  tooltip: {
    y: {
      formatter: function (val) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
      }
    },
    theme: 'light'
  }
});

const donutChartSeries = ref([]);

const donutChartOptions = ref({
  chart: {
    type: 'donut',
    height: 300,
    fontFamily: 'Inter, system-ui, -apple-system, sans-serif'
  },
  colors: ['#4f46e5', '#06b6d4', '#10b981', '#f59e0b', '#ec4899', '#8b5cf6'],
  labels: [],
  legend: {
    position: 'bottom',
    fontSize: '12px',
    fontWeight: 500,
    labels: {
      colors: '#334155'
    },
    markers: {
      radius: 12
    }
  },
  plotOptions: {
    pie: {
      donut: {
        size: '72%',
        labels: {
          show: true,
          total: {
            show: true,
            label: 'Tổng doanh thu',
            fontSize: '13px',
            fontWeight: 600,
            color: '#64748b',
            formatter: function (w) {
              const total = w.globals.seriesTotals.reduce((a, b) => a + b, 0);
              return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(total);
            }
          },
          value: {
            show: true,
            fontSize: '16px',
            fontWeight: 700,
            color: '#1e293b',
            formatter: function (val) {
              return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
            }
          }
        }
      }
    }
  },
  dataLabels: {
    enabled: true,
    formatter: function (val) {
      return val.toFixed(1) + '%';
    },
    dropShadow: {
      enabled: false
    }
  },
  tooltip: {
    y: {
      formatter: function (val) {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
      }
    },
    theme: 'light'
  }
});

const periodOptions = [
  { title: 'Hôm nay', value: 'today' },
  { title: 'Tuần này', value: 'week' },
  { title: 'Tháng này', value: 'month' },
  { title: 'Quý này', value: 'quarter' },
  { title: 'Năm này', value: 'year' }
];

const getDateRange = () => {
  const now = new Date();
  let tuNgay = '';
  let denNgay = '';

  const formatLocalDate = (date) => {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  };

  if (selectedPeriod.value === 'today') {
    const today = new Date();
    tuNgay = formatLocalDate(today);
    denNgay = formatLocalDate(today);
  } else if (selectedPeriod.value === 'week') {
    const today = new Date();
    const day = today.getDay(); // 0 is Sunday, 1 is Monday, etc.
    const diff = today.getDate() - day + (day === 0 ? -6 : 1);
    const monday = new Date(today.setDate(diff));
    const sunday = new Date(monday);
    sunday.setDate(monday.getDate() + 6);
    
    tuNgay = formatLocalDate(monday);
    denNgay = formatLocalDate(sunday);
  } else if (selectedPeriod.value === 'month') {
    const year = selectedYear.value;
    const month = selectedMonth.value;
    const firstDay = new Date(year, month - 1, 1);
    const lastDay = new Date(year, month, 0);
    
    tuNgay = formatLocalDate(firstDay);
    denNgay = formatLocalDate(lastDay);
  } else if (selectedPeriod.value === 'quarter') {
    const currentMonth = now.getMonth();
    const quarter = Math.floor(currentMonth / 3);
    const startMonth = quarter * 3;
    const endMonth = startMonth + 2;
    
    const firstDay = new Date(selectedYear.value, startMonth, 1);
    const lastDay = new Date(selectedYear.value, endMonth + 1, 0);
    
    tuNgay = formatLocalDate(firstDay);
    denNgay = formatLocalDate(lastDay);
  } else if (selectedPeriod.value === 'year') {
    tuNgay = `${selectedYear.value}-01-01`;
    denNgay = `${selectedYear.value}-12-31`;
  }

  return { tuNgay, denNgay };
};

const loadStatistics = async () => {
  loading.value = true;
  try {
    const { tuNgay, denNgay } = getDateRange();

    const overview = await dichVuThongKe.layTongQuan(tuNgay, denNgay);

    if (overview) {
      revenueStats.value = {
        totalRevenue: overview.tongDoanhThu || 0,
        totalOrders: overview.tongDonHang || 0,
        averageOrderValue: overview.tongDonHang > 0 ? (overview.tongDoanhThu / overview.tongDonHang) : 0,
        growthRate: 0,
        donHangHoanThanh: overview.donHangHoanThanh || 0,
        donHangChoXacNhan: overview.donHangChoXacNhan || 0,
        donHangDangGiao: overview.donHangDangGiao || 0,
        donHangDaHuy: overview.donHangDaHuy || 0,
        tongKhachHang: overview.tongKhachHang || 0,
        sanPhamSapHet: overview.sanPhamSapHet || 0
      };

      if (overview.topSanPhamBanChay && overview.topSanPhamBanChay.length > 0) {
        topProducts.value = overview.topSanPhamBanChay.map(item => ({
          name: item.name,
          revenue: item.revenue || 0,
          quantity: item.quantity || 0,
          growth: item.growth || 0
        }));
      } else {
        topProducts.value = [];
      }

      if (overview.doanhThuTheoDanhMuc && overview.doanhThuTheoDanhMuc.length > 0) {
        salesByCategory.value = overview.doanhThuTheoDanhMuc.map(item => ({
          name: item.name,
          value: item.value || 0,
          percentage: item.percentage || 0
        }));
        
        // Cập nhật biểu đồ Donut
        donutChartSeries.value = salesByCategory.value.map(item => item.value);
        donutChartOptions.value = {
          ...donutChartOptions.value,
          labels: salesByCategory.value.map(item => item.name)
        };
      } else {
        salesByCategory.value = [];
        donutChartSeries.value = [];
        donutChartOptions.value = {
          ...donutChartOptions.value,
          labels: []
        };
      }
    }

    const startOfYear = `${selectedYear.value}-01-01`;
    const endOfYear = `${selectedYear.value}-12-31`;
    const dailyData = await dichVuThongKe.layDoanhThuTheoNgay(startOfYear, endOfYear);

    const months = Array.from({ length: 12 }, (_, i) => ({
      month: `T${i + 1}`,
      revenue: 0
    }));

    if (dailyData && Array.isArray(dailyData)) {
      dailyData.forEach(item => {
        if (item.ngay) {
          const parts = item.ngay.split('-');
          const m = parseInt(parts[1], 10);
          if (m >= 1 && m <= 12) {
            months[m - 1].revenue += Number(item.doanhThu || 0);
          }
        }
      });
    }
    monthlyRevenue.value = months;

    // Cập nhật biểu đồ Area
    areaChartSeries.value = [
      {
        name: 'Doanh thu',
        data: months.map(m => m.revenue)
      }
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
  <div class="pa-6 font-body thong-ke-container">
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
            <v-btn color="primary" variant="flat" @click="loadStatistics" class="px-6" height="40">
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
            <p class="text-caption text-slate-400 mt-2">Dựa trên đơn giao thành công</p>
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
              <v-icon color="warning">mdi-account-group</v-icon>
            </div>
          </div>
          <div>
            <p class="text-caption text-slate-500 font-weight-medium text-uppercase mb-1 tracking-wider">Tổng khách hàng</p>
            <p class="text-h5 font-weight-bold text-dark">{{ formatNumber(revenueStats.tongKhachHang) }}</p>
            <p class="text-caption text-slate-400 mt-2">Đăng ký trên hệ thống</p>
          </div>
        </v-card>
      </v-col>
    </v-row>

    <!-- Order Status Summary -->
    <v-row class="mb-4">
      <v-col cols="12">
        <v-card class="premium-card pa-4">
          <div class="d-flex flex-wrap align-center justify-space-around py-2" style="gap: 16px;">
            <div class="d-flex align-center">
              <v-badge color="warning" :content="formatNumber(revenueStats.donHangChoXacNhan)" inline class="mr-2"></v-badge>
              <span class="text-caption text-slate-500 font-weight-medium text-uppercase tracking-wider">Chờ xác nhận</span>
            </div>
            <v-divider vertical class="mx-2 d-none d-sm-block" style="height: 24px;"></v-divider>
            <div class="d-flex align-center">
              <v-badge color="info" :content="formatNumber(revenueStats.donHangDangGiao)" inline class="mr-2"></v-badge>
              <span class="text-caption text-slate-500 font-weight-medium text-uppercase tracking-wider">Đang giao hàng</span>
            </div>
            <v-divider vertical class="mx-2 d-none d-sm-block" style="height: 24px;"></v-divider>
            <div class="d-flex align-center">
              <v-badge color="success" :content="formatNumber(revenueStats.donHangHoanThanh)" inline class="mr-2"></v-badge>
              <span class="text-caption text-slate-500 font-weight-medium text-uppercase tracking-wider">Đã hoàn thành</span>
            </div>
            <v-divider vertical class="mx-2 d-none d-sm-block" style="height: 24px;"></v-divider>
            <div class="d-flex align-center">
              <v-badge color="error" :content="formatNumber(revenueStats.donHangDaHuy)" inline class="mr-2"></v-badge>
              <span class="text-caption text-slate-500 font-weight-medium text-uppercase tracking-wider">Đã hủy bỏ</span>
            </div>
          </div>
        </v-card>
      </v-col>
    </v-row>

    <div class="mb-4"></div>

    <!-- Row 1: Premium Interactive Charts -->
    <v-row class="mb-6">
      <!-- Area Chart: Monthly Revenue Trend -->
      <v-col cols="12" lg="8">
        <v-card class="premium-card h-100">
          <div class="card-title-bar">
            <span class="font-weight-bold text-dark text-uppercase" style="font-size: 13px; letter-spacing: 0.05em;">Xu hướng doanh thu (Năm {{ selectedYear }})</span>
            <v-icon color="slate-400">mdi-chart-areaspline</v-icon>
          </div>
          <v-card-text class="pa-4">
            <div v-if="loading" class="d-flex align-center justify-center" style="height: 320px;">
              <v-progress-circular indeterminate color="primary"></v-progress-circular>
            </div>
            <div v-else>
              <apexchart type="area" height="320" :options="areaChartOptions" :series="areaChartSeries"></apexchart>
            </div>
          </v-card-text>
        </v-card>
      </v-col>

      <!-- Donut Chart: Sales Proportion by Category -->
      <v-col cols="12" lg="4">
        <v-card class="premium-card h-100">
          <div class="card-title-bar">
            <span class="font-weight-bold text-dark text-uppercase" style="font-size: 13px; letter-spacing: 0.05em;">Tỷ trọng theo danh mục</span>
            <v-icon color="slate-400">mdi-chart-donut</v-icon>
          </div>
          <v-card-text class="pa-4 d-flex align-center justify-center">
            <div v-if="loading" class="d-flex align-center justify-center" style="height: 300px; width: 100%;">
              <v-progress-circular indeterminate color="primary"></v-progress-circular>
            </div>
            <div v-else style="width: 100%;">
              <div v-if="donutChartSeries.length === 0" class="text-center text-slate-400 py-12">
                Không có dữ liệu trong thời gian này
              </div>
              <apexchart v-else type="donut" height="300" :options="donutChartOptions" :series="donutChartSeries"></apexchart>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Row 2: Detailed Lists & Grid -->
    <v-row>
      <!-- Top Products List -->
      <v-col cols="12" lg="6">
        <v-card class="premium-card h-100">
          <div class="card-title-bar">
            <span class="font-weight-bold text-dark text-uppercase" style="font-size: 13px; letter-spacing: 0.05em;">Sản phẩm bán chạy</span>
            <v-icon color="slate-400">mdi-crown-outline</v-icon>
          </div>
          <v-list class="pa-4">
            <div v-if="loading" class="d-flex align-center justify-center py-12">
              <v-progress-circular indeterminate color="primary"></v-progress-circular>
            </div>
            <template v-else>
              <div v-if="topProducts.length === 0" class="text-center text-slate-400 py-12">
                Không có dữ liệu trong thời gian này
              </div>
              <v-list-item v-for="(product, index) in topProducts" :key="product.name" class="rounded-lg mb-2 bg-slate-50" v-else>
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
            </template>
          </v-list>
        </v-card>
      </v-col>

      <!-- Monthly Revenue Detailed Grid -->
      <v-col cols="12" lg="6">
        <v-card class="premium-card h-100">
          <div class="card-title-bar">
            <span class="font-weight-bold text-dark text-uppercase" style="font-size: 13px; letter-spacing: 0.05em;">Chi tiết doanh thu tháng (Năm {{ selectedYear }})</span>
            <v-icon color="slate-400">mdi-calendar-month</v-icon>
          </div>
          <v-card-text class="pa-4">
            <div v-if="loading" class="d-flex align-center justify-center py-12">
              <v-progress-circular indeterminate color="primary"></v-progress-circular>
            </div>
            <v-row v-else class="ma-0" style="gap: 8px 0;">
              <v-col 
                v-for="month in monthlyRevenue" 
                :key="month.month"
                cols="6" sm="4" md="4"
                class="pa-1"
              >
                 <div class="pa-3 text-center rounded-lg border bg-slate-50 hover-addr-card h-100 d-flex flex-column justify-center">
                  <div class="text-caption text-slate-500 font-weight-medium text-uppercase mb-1 tracking-wider" style="font-size: 10px !important;">{{ month.month }}</div>
                  <div class="text-subtitle-2 font-weight-bold text-dark" style="font-size: 12px !important; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">{{ formatCurrency(month.revenue) }}</div>
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

.thong-ke-container {
  height: 100%;
  overflow-y: auto !important;
}
</style>




