<script setup>
import { ref, computed, onMounted } from 'vue';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';
import { dichVuThongKe } from '@/services/admin/dichVuThongKe';
import apexchart from 'vue3-apexcharts';

const loading = ref(false);
const selectedPeriod = ref('month');
const selectedYear = ref(new Date().getFullYear());
const selectedMonth = ref(new Date().getMonth() + 1);
const selectedChartTab = ref('revenue');

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

const statusBarOptions = ref({
    chart: {
        type: 'bar',
        height: 320,
        toolbar: {
            show: false
        },
        fontFamily: 'Inter, system-ui, -apple-system, sans-serif'
    },
    colors: ['#4f939c', '#e39b32', '#6fba83', '#c9473d'],
    plotOptions: {
        bar: {
            distributed: true,
            borderRadius: 4,
            columnWidth: '48%'
        }
    },
    dataLabels: {
        enabled: false
    },
    legend: {
        show: false
    },
    grid: {
        borderColor: '#e5e7eb',
        strokeDashArray: 4
    },
    xaxis: {
        categories: ['Chờ xác nhận', 'Đang giao hàng', 'Đã hoàn thành', 'Đã hủy bỏ'],
        axisBorder: {
            show: true,
            color: '#e5e7eb'
        },
        axisTicks: {
            show: false
        },
        labels: {
            style: {
                colors: '#334155',
                fontSize: '12px',
                fontWeight: 500
            }
        }
    },
    yaxis: {
        min: 0,
        labels: {
            formatter: function (value) {
                return new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 0 }).format(value);
            },
            style: {
                colors: '#64748b',
                fontSize: '12px',
                fontWeight: 500
            }
        }
    },
    tooltip: {
        y: {
            formatter: function (val) {
                return `${new Intl.NumberFormat('vi-VN').format(val)} đơn`;
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
                averageOrderValue: overview.tongDonHang > 0 ? overview.tongDoanhThu / overview.tongDonHang : 0,
                growthRate: 0,
                donHangHoanThanh: overview.donHangHoanThanh || 0,
                donHangChoXacNhan: overview.donHangChoXacNhan || 0,
                donHangDangGiao: overview.donHangDangGiao || 0,
                donHangDaHuy: overview.donHangDaHuy || 0,
                tongKhachHang: overview.tongKhachHang || 0,
                sanPhamSapHet: overview.sanPhamSapHet || 0
            };

            if (overview.topSanPhamBanChay && overview.topSanPhamBanChay.length > 0) {
                topProducts.value = overview.topSanPhamBanChay.map((item) => ({
                    name: item.name,
                    revenue: item.revenue || 0,
                    quantity: item.quantity || 0,
                    growth: item.growth || 0
                }));
            } else {
                topProducts.value = [];
            }

            if (overview.doanhThuTheoDanhMuc && overview.doanhThuTheoDanhMuc.length > 0) {
                salesByCategory.value = overview.doanhThuTheoDanhMuc.map((item) => ({
                    name: item.name,
                    value: item.value || 0,
                    percentage: item.percentage || 0
                }));

                // Cập nhật biểu đồ Donut
                donutChartSeries.value = salesByCategory.value.map((item) => item.value);
                donutChartOptions.value = {
                    ...donutChartOptions.value,
                    labels: salesByCategory.value.map((item) => item.name)
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
            dailyData.forEach((item) => {
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
                data: months.map((m) => m.revenue)
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

const statusItems = [
    { label: 'Chờ xác nhận', valueKey: 'donHangChoXacNhan', icon: 'mdi-clock-outline', color: 'warning' },
    { label: 'Đang giao hàng', valueKey: 'donHangDangGiao', icon: 'mdi-truck-fast-outline', color: 'info' },
    { label: 'Đã hoàn thành', valueKey: 'donHangHoanThanh', icon: 'mdi-check-circle-outline', color: 'success' },
    { label: 'Đã hủy bỏ', valueKey: 'donHangDaHuy', icon: 'mdi-close-circle-outline', color: 'error' }
];

const statusChartItems = computed(() => [
    { label: 'Chờ xác nhận', amount: 0, count: revenueStats.value.donHangChoXacNhan, active: true },
    { label: 'Đang giao hàng', amount: 0, count: revenueStats.value.donHangDangGiao },
    { label: 'Đã hoàn thành', amount: revenueStats.value.totalRevenue, count: revenueStats.value.donHangHoanThanh },
    { label: 'Đã hủy bỏ', amount: 0, count: revenueStats.value.donHangDaHuy }
]);

const statusBarSeries = computed(() => [
    {
        name: 'Số đơn',
        data: [
            revenueStats.value.donHangChoXacNhan,
            revenueStats.value.donHangDangGiao,
            revenueStats.value.donHangHoanThanh,
            revenueStats.value.donHangDaHuy
        ]
    }
]);

const kpiCards = [
    {
        title: 'Tổng doanh thu',
        valueKey: 'totalRevenue',
        icon: 'mdi-currency-usd',
        color: 'primary',
        tone: 'blue',
        formatter: formatCurrency,
        description: 'Dựa trên đơn giao thành công'
    },
    {
        title: 'Tổng đơn hàng',
        valueKey: 'totalOrders',
        icon: 'mdi-shopping-outline',
        color: 'success',
        tone: 'green',
        formatter: formatNumber,
        description: 'Trong thời gian này'
    },
    {
        title: 'Giá trị trung bình',
        valueKey: 'averageOrderValue',
        icon: 'mdi-chart-line',
        color: 'info',
        tone: 'cyan',
        formatter: formatCurrency,
        description: 'Mỗi đơn hàng thành công'
    },
    {
        title: 'Tổng khách hàng',
        valueKey: 'tongKhachHang',
        icon: 'mdi-account-group',
        color: 'warning',
        tone: 'orange',
        formatter: formatNumber,
        description: 'Đăng ký trên hệ thống'
    }
];

onMounted(() => {
    loadStatistics();
});
</script>
<template>
    <div class="pa-6 font-body thong-ke-container">
        <AdminBreadcrumbs
            :items="[
                { title: 'Quản lý bán hàng', disabled: false, href: '#' },
                { title: 'Thống kê', disabled: true }
            ]"
        />

        <section class="stats-shell mt-4">
            <div class="stats-toolbar">
                <div class="stats-filters">
                    <div class="stats-filter-field">
                        <div class="filter-field-label">Kỳ thống kê</div>
                        <v-select
                            v-model="selectedPeriod"
                            :items="periodOptions"
                            variant="outlined"
                            density="compact"
                            hide-details
                            class="stats-filter"
                            @update:model-value="loadStatistics"
                        />
                    </div>
                    <div class="stats-filter-field stats-filter-field-year">
                        <div class="filter-field-label">Năm</div>
                        <v-select
                            v-model="selectedYear"
                            :items="Array.from({ length: 5 }, (_, i) => new Date().getFullYear() - i)"
                            variant="outlined"
                            density="compact"
                            hide-details
                            class="stats-filter stats-filter-year"
                            @update:model-value="loadStatistics"
                        />
                    </div>
                    <div v-if="selectedPeriod === 'month'" class="stats-filter-field stats-filter-field-year">
                        <div class="filter-field-label">Tháng</div>
                        <v-select
                            v-model="selectedMonth"
                            :items="Array.from({ length: 12 }, (_, i) => ({ title: `Tháng ${i + 1}`, value: i + 1 }))"
                            variant="outlined"
                            density="compact"
                            hide-details
                            class="stats-filter stats-filter-year"
                            @update:model-value="loadStatistics"
                        />
                    </div>
                    <v-btn color="primary" variant="flat" class="stats-refresh-btn px-6" height="40" :loading="loading" @click="loadStatistics">
                        <v-icon start size="18">mdi-refresh</v-icon>
                        Cập nhật dữ liệu
                    </v-btn>
                </div>
            </div>

            <div class="kpi-grid">
                <article v-for="item in kpiCards" :key="item.valueKey" class="kpi-card">
                    <div class="kpi-icon" :class="`kpi-icon-${item.tone}`">
                        <v-icon :color="item.color" size="22">{{ item.icon }}</v-icon>
                    </div>
                    <p>{{ item.title }}</p>
                    <strong>{{ item.formatter(revenueStats[item.valueKey]) }}</strong>
                    <small>{{ item.description }}</small>
                </article>
            </div>

            <section class="stats-panel trend-panel">
                <div class="stats-tabs">
                    <button type="button" :class="{ active: selectedChartTab === 'revenue' }" @click="selectedChartTab = 'revenue'">
                        XU HƯỚNG DOANH THU (NĂM {{ selectedYear }})
                    </button>
                    <button type="button" :class="{ active: selectedChartTab === 'status' }" @click="selectedChartTab = 'status'">
                        Trạng thái
                    </button>
                </div>

                <div v-if="selectedChartTab === 'revenue'" class="tab-panel">
                    <div v-if="loading" class="panel-loader panel-loader-tall">
                        <v-progress-circular indeterminate color="primary" />
                    </div>
                    <apexchart v-else type="area" height="320" :options="areaChartOptions" :series="areaChartSeries" />
                </div>

                <div v-else class="tab-panel">
                    <div v-if="loading" class="panel-loader panel-loader-tall">
                        <v-progress-circular indeterminate color="primary" />
                    </div>
                    <div v-else class="status-chart-wrap">
                        <div class="status-summary-row">
                            <article v-for="item in statusChartItems" :key="item.label" class="status-summary-item" :class="{ active: item.active }">
                                <span>{{ item.label }}</span>
                                <strong>{{ formatCurrency(item.amount) }}</strong>
                                <small>{{ formatNumber(item.count) }} đơn</small>
                            </article>
                        </div>
                        <apexchart type="bar" height="320" :options="statusBarOptions" :series="statusBarSeries" />
                    </div>
                </div>
            </section>

            <div class="split-grid">
                <section class="stats-panel top-products-panel">
                    <div class="simple-card-heading">
                        <h2>SẢN PHẨM BÁN CHẠY</h2>
                        <v-icon color="#0f172a" size="22">mdi-crown-outline</v-icon>
                    </div>
                    <div v-if="loading" class="panel-loader">
                        <v-progress-circular indeterminate color="primary" />
                    </div>
                    <div v-else-if="topProducts.length === 0" class="empty-state">Không có dữ liệu trong thời gian này</div>
                    <div v-else class="top-products">
                        <div v-for="(product, index) in topProducts" :key="product.name" class="product-row">
                            <span class="rank">{{ index + 1 }}</span>
                            <div class="product-info">
                                <strong>{{ product.name }}</strong>
                                <small>{{ formatNumber(product.quantity) }} sản phẩm · {{ formatCurrency(product.revenue) }}</small>
                            </div>
                            <v-chip size="x-small" :color="getGrowthColor(product.growth)" variant="tonal">
                                <v-icon start size="14">{{ getGrowthIcon(product.growth) }}</v-icon>
                                {{ product.growth >= 0 ? '+' : '' }}{{ product.growth }}%
                            </v-chip>
                        </div>
                    </div>
                </section>

                <section class="stats-panel category-share-panel">
                    <div class="simple-card-heading">
                        <h2>TỶ TRỌNG THEO DANH MỤC</h2>
                        <v-icon color="#0f172a" size="22">mdi-chart-donut</v-icon>
                    </div>
                    <div v-if="loading" class="panel-loader">
                        <v-progress-circular indeterminate color="primary" />
                    </div>
                    <div v-else-if="donutChartSeries.length === 0" class="empty-state">Không có dữ liệu trong thời gian này</div>
                    <div v-else class="category-chart-body">
                        <apexchart type="donut" height="300" :options="donutChartOptions" :series="donutChartSeries" />
                    </div>
                </section>
            </div>

            <section class="stats-panel monthly-detail-panel">
                <div class="simple-card-heading">
                    <h2>CHI TIẾT DOANH THU THÁNG (NĂM {{ selectedYear }})</h2>
                    <v-icon color="#0f172a" size="22">mdi-calendar-month</v-icon>
                </div>
                <div v-if="loading" class="panel-loader">
                    <v-progress-circular indeterminate color="primary" />
                </div>
                <div v-else class="month-grid">
                    <div v-for="month in monthlyRevenue" :key="month.month" class="month-cell">
                        <span>{{ month.month }}</span>
                        <strong>{{ formatCurrency(month.revenue) }}</strong>
                    </div>
                </div>
            </section>
        </section>
    </div>
</template>

<style scoped>
.thong-ke-container {
    height: 100%;
    overflow-y: auto !important;
    background: #f6f7fb;
}

.stats-shell {
    display: flex;
    flex-direction: column;
    gap: 24px;
}

.stats-toolbar,
.kpi-card,
.stats-panel {
    border: 1px solid #e4e8f0;
    background: #ffffff;
    box-shadow: 0 14px 35px rgba(15, 23, 42, 0.06);
}

.stats-toolbar {
    display: flex;
    align-items: flex-end;
    padding: 14px 18px;
    border-radius: 12px;
}

.stats-title-tabs,
.section-label {
    display: inline-flex;
    align-items: center;
    width: fit-content;
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    background: #ffffff;
    color: #0f172a;
    font-size: 13px;
    font-weight: 800;
    letter-spacing: 0.05em;
    text-transform: uppercase;
}

.stats-title-tabs span,
.section-label {
    padding: 11px 14px;
}

.stats-title-tabs span + span {
    border-left: 1px solid #e2e8f0;
}

.stats-filters {
    display: flex;
    align-items: flex-end;
    justify-content: flex-start;
    gap: 16px;
    flex-wrap: wrap;
    width: 100%;
}

.stats-filter-field {
    flex: 0 0 252px;
    width: 252px;
    max-width: 252px;
}

.stats-filter-field-year {
    flex: 0 0 164px;
    width: 164px;
    max-width: 164px;
}

.filter-field-label {
    margin-bottom: 6px;
    color: #475569;
    font-size: 12px;
    font-weight: 700;
}

.stats-filter {
    width: 100%;
}

.stats-filter-year {
    width: 100%;
}

.stats-refresh-btn {
    margin-left: auto;
    border-radius: 12px;
    font-size: 13px;
    font-weight: 700;
    letter-spacing: 0;
    min-width: 168px;
}

.kpi-grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 14px;
}

.kpi-card {
    min-height: 120px;
    padding: 20px 22px;
    border-radius: 12px;
}

.kpi-icon {
    display: grid;
    width: 34px;
    height: 34px;
    margin-bottom: 18px;
    place-items: center;
    border-radius: 50%;
    background: #f3f6fb;
}

.kpi-icon-blue {
    background: #eef4ff;
}

.kpi-icon-green {
    background: #dcfce7;
}

.kpi-icon-cyan {
    background: #e0f7ff;
}

.kpi-icon-orange {
    background: #fff3df;
}

.kpi-card p {
    margin: 0 0 8px;
    color: #111827;
    font-size: 11px;
    font-weight: 700;
    line-height: 1.25;
    letter-spacing: 0.02em;
    text-transform: uppercase;
}

.kpi-card strong {
    display: block;
    color: #0f172a;
    font-size: 21px;
    font-weight: 800;
    line-height: 1.2;
    overflow-wrap: anywhere;
}

.kpi-card small {
    display: block;
    margin-top: 8px;
    color: #111827;
    font-size: 11px;
    font-weight: 500;
    line-height: 1.35;
}

.stats-panel {
    padding: 16px;
    border-radius: 8px;
}

.trend-panel {
    padding: 0;
    overflow: hidden;
}

.stats-tabs {
    display: flex;
    align-items: center;
    border-bottom: 1px solid #eef2f7;
    background: #f8fafc;
}

.stats-tabs button {
    min-height: 48px;
    padding: 0 22px;
    border: 0;
    border-right: 1px solid #eef2f7;
    background: transparent;
    color: #475569;
    cursor: pointer;
    font-size: 13px;
    font-weight: 800;
    letter-spacing: 0.02em;
    text-transform: uppercase;
}

.stats-tabs button.active {
    background: #ffffff;
    color: #0f172a;
    box-shadow: inset 0 -2px 0 #4f46e5;
}

.tab-panel {
    padding: 16px;
}

.status-chart-wrap {
    border-top: 0;
}

.status-summary-row {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: 12px;
    padding: 14px 16px 16px;
    border-bottom: 0;
}

.status-summary-item {
    position: relative;
    min-height: 86px;
    padding: 14px 16px;
    border: 1px solid #eef2f7;
    border-radius: 12px;
    background: #fbfcff;
    text-align: right;
}

.status-summary-item span {
    display: block;
    margin-bottom: 8px;
    color: #64748b;
    font-size: 12px;
    font-weight: 600;
}

.status-summary-item strong {
    display: block;
    color: #0f172a;
    font-size: 18px;
    font-weight: 800;
    line-height: 1.2;
    overflow-wrap: anywhere;
}

.status-summary-item small {
    display: block;
    margin-top: 6px;
    color: #334155;
    font-size: 13px;
    font-weight: 500;
}

.panel-heading {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 16px;
    margin: 16px 0 10px;
}

.panel-heading.compact {
    align-items: center;
}

.panel-heading h2 {
    margin: 0;
    color: #0f172a;
    font-size: 16px;
    font-weight: 800;
}

.panel-heading p {
    margin: 4px 0 0;
    color: #64748b;
    font-size: 13px;
}

.simple-card-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 16px;
}

.simple-card-heading h2 {
    margin: 0;
    color: #0f172a;
    font-size: 15px;
    font-weight: 800;
    letter-spacing: 0.04em;
}

.top-products-panel,
.category-share-panel,
.monthly-detail-panel {
    padding: 0;
    overflow: hidden;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    background: #ffffff;
}

.top-products-panel .simple-card-heading,
.category-share-panel .simple-card-heading,
.monthly-detail-panel .simple-card-heading {
    min-height: 52px;
    margin-bottom: 0;
    padding: 0 22px;
    border-bottom: 1px solid #e7edf5;
    background: #f8fafc;
}

.top-products-panel .simple-card-heading h2,
.category-share-panel .simple-card-heading h2,
.monthly-detail-panel .simple-card-heading h2 {
    font-size: 13px;
    letter-spacing: 0.03em;
}

.top-products-panel .panel-loader,
.top-products-panel .empty-state,
.top-products-panel .top-products,
.category-share-panel .panel-loader,
.category-share-panel .empty-state {
    min-height: 310px;
}

.top-products-panel .empty-state,
.category-share-panel .empty-state {
    color: #111827;
    font-size: 15px;
    font-weight: 500;
}

.top-products-panel .top-products {
    padding: 18px 20px;
}

.category-chart-body {
    min-height: 310px;
    padding: 18px 16px 0;
}

.monthly-detail-panel .panel-loader {
    min-height: 260px;
}

.panel-loader {
    display: flex;
    min-height: 240px;
    align-items: center;
    justify-content: center;
}

.panel-loader-tall {
    min-height: 320px;
}

.split-grid {
    display: grid;
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
    gap: 24px;
}

.empty-state {
    display: grid;
    min-height: 220px;
    place-items: center;
    color: #94a3b8;
    font-weight: 700;
    text-align: center;
}

.top-products {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.product-row {
    display: grid;
    grid-template-columns: 34px minmax(0, 1fr) auto;
    align-items: center;
    gap: 12px;
    padding: 12px;
    border: 1px solid #eef2f7;
    border-radius: 8px;
    background: #fbfcff;
}

.rank {
    display: grid;
    width: 34px;
    height: 34px;
    place-items: center;
    border-radius: 8px;
    background: #eef2ff;
    color: #4f46e5;
    font-weight: 800;
}

.product-info {
    min-width: 0;
}

.product-info strong,
.product-info small {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.product-info strong {
    color: #0f172a;
    font-size: 14px;
}

.product-info small {
    color: #64748b;
    font-size: 12px;
}

.month-grid {
    display: grid;
    grid-template-columns: repeat(6, minmax(0, 1fr));
    gap: 10px;
    padding: 16px;
}

.month-cell {
    min-height: 78px;
    padding: 12px;
    border: 1px solid #eef2f7;
    border-radius: 8px;
    background: #fbfcff;
}

.month-cell span {
    display: block;
    margin-bottom: 8px;
    color: #64748b;
    font-size: 11px;
    font-weight: 700;
    text-transform: uppercase;
}

.month-cell strong {
    display: block;
    color: #0f172a;
    font-size: 13px;
    font-weight: 800;
    overflow-wrap: anywhere;
}

@media (max-width: 1180px) {
    .kpi-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .status-summary-row {
        grid-template-columns: repeat(3, minmax(0, 1fr));
    }

    .month-grid {
        grid-template-columns: repeat(4, minmax(0, 1fr));
    }
}

@media (max-width: 780px) {
    .thong-ke-container {
        padding: 16px !important;
    }

    .stats-toolbar,
    .split-grid {
        grid-template-columns: 1fr;
    }

    .stats-toolbar {
        align-items: stretch;
        flex-direction: column;
    }

    .stats-filters,
    .stats-filter-field,
    .stats-filter-field-year,
    .stats-filter,
    .stats-filter-year,
    .stats-refresh-btn {
        width: 100%;
    }

    .stats-refresh-btn {
        margin-left: 0;
    }

    .kpi-grid,
    .split-grid,
    .month-grid {
        grid-template-columns: 1fr;
    }

    .status-summary-row {
        grid-template-columns: 1fr;
    }

    .status-summary-item {
        text-align: left;
    }

    .stats-tabs {
        overflow-x: auto;
    }

    .stats-tabs button {
        flex: 0 0 auto;
        padding: 0 16px;
        white-space: nowrap;
    }

    .product-row {
        grid-template-columns: 34px minmax(0, 1fr);
    }

    .product-row .v-chip {
        grid-column: 2;
        width: fit-content;
    }
}
</style>
