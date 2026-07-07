<script setup>
import { ref, computed, onMounted } from 'vue';
import { AdminBreadcrumbs, AdminFilter, AdminTable, AdminPagination } from '@/components/common';
import AppDatePicker from '@/components/common/AppDatePicker.vue';
import { dichVuThongKe } from '@/services/admin/dichVuThongKe';
import { dichVuSanPham } from '@/services/product/dichVuSanPham';
import apexchart from 'vue3-apexcharts';

const loading = ref(true);

const padDatePart = (value) => String(value).padStart(2, '0');

const formatDateInput = (date) => {
    const y = date.getFullYear();
    const m = padDatePart(date.getMonth() + 1);
    const d = padDatePart(date.getDate());
    return `${y}-${m}-${d}`;
};

const defaultEndDate = new Date();
const defaultStartDate = new Date(defaultEndDate);
defaultStartDate.setDate(defaultStartDate.getDate() - 365);
const startDate = ref(formatDateInput(defaultStartDate));
const endDate = ref(formatDateInput(defaultEndDate));

const onStartDateChange = (val) => {
    startDate.value = val ? formatDateInput(new Date(val)) : null;
};

const onEndDateChange = (val) => {
    endDate.value = val ? formatDateInput(new Date(val)) : null;
};

const selectedYear = computed(() => {
    const rangeDate = endDate.value || startDate.value;
    const year = Number(rangeDate?.slice(0, 4));
    return year || new Date().getFullYear();
});

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
    tongSanPham: 0,
    sanPhamDaBan: 0,
    doanhThuTaiQuay: 0,
    doanhThuTrucTuyen: 0,
    donTaiQuay: 0,
    donTrucTuyen: 0,
    sanPhamSapHet: 0
});

const topProducts = ref([]);
const productStats = ref([]);
const productSearchKeyword = ref('');
const productSortBy = ref('bestSelling');
const productPage = ref(1);
const productPageSize = ref(5);
const productPageSizeOptions = [
    { title: '5 dòng', value: 5 },
    { title: '10 dòng', value: 10 },
    { title: '20 dòng', value: 20 }
];
const productSortOptions = [
    { title: 'Bán chạy nhất', value: 'bestSelling' },
    { title: 'Doanh thu cao nhất', value: 'revenueDesc' }
];

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

const getDateRange = () => {
    let tuNgay = startDate.value ? startDate.value.slice(0, 10) : '';
    let denNgay = endDate.value ? endDate.value.slice(0, 10) : '';

    if (tuNgay && denNgay && tuNgay > denNgay) {
        [tuNgay, denNgay] = [denNgay, tuNgay];
    }

    return { tuNgay, denNgay };
};

const productTotalElements = ref(0);
const productTotalPages = ref(1);

const fetchProductStats = async () => {
    try {
        const { tuNgay, denNgay } = getDateRange();
        const response = await dichVuThongKe.layThongKeSanPham({
            tuNgay: tuNgay || undefined,
            denNgay: denNgay || undefined,
            keyword: productSearchKeyword.value || undefined,
            page: productPage.value - 1,
            size: productPageSize.value,
            sortBy: productSortBy.value
        });

        productStats.value = Array.isArray(response?.content) ? response.content : [];
        productTotalElements.value = response?.totalElements || 0;
        productTotalPages.value = response?.totalPages || 1;
    } catch (error) {
        console.error('Error loading product stats table:', error);
        productStats.value = [];
        productTotalElements.value = 0;
        productTotalPages.value = 1;
    }
};

const loadStatistics = async () => {
    loading.value = true;
    try {
        const { tuNgay, denNgay } = getDateRange();
        const startOfYear = `${selectedYear.value}-01-01`;
        const endOfYear = `${selectedYear.value}-12-31`;

        // Gọi API song song để giảm thời gian chờ
        const [overview, dailyData] = await Promise.all([
            dichVuThongKe.layTongQuan(tuNgay, denNgay),
            dichVuThongKe.layDoanhThuTheoNgay(startOfYear, endOfYear)
        ]);

        if (overview) {
            const soldProductQuantity = Array.isArray(overview.topSanPhamBanChay)
                ? overview.topSanPhamBanChay.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
                : 0;
            const completedOrderCount = Number(overview.donHangHoanThanh || 0);
            const totalRevenue = Number(overview.tongDoanhThu || 0);
            const averageOrderValue = overview.giaTriTrungBinh != null
                ? Number(overview.giaTriTrungBinh)
                : (completedOrderCount > 0 ? totalRevenue / completedOrderCount : 0);

            revenueStats.value = {
                totalRevenue,
                totalOrders: overview.tongDonHang || 0,
                averageOrderValue,
                growthRate: 0,
                donHangHoanThanh: overview.donHangHoanThanh || 0,
                donHangChoXacNhan: overview.donHangChoXacNhan || 0,
                donHangDangGiao: overview.donHangDangGiao || 0,
                donHangDaHuy: overview.donHangDaHuy || 0,
                tongKhachHang: overview.tongKhachHang || 0,
                tongSanPham: overview.tongSanPham || 0,
                sanPhamDaBan: soldProductQuantity,
                doanhThuTaiQuay: overview.doanhThuTaiQuay || 0,
                doanhThuTrucTuyen: overview.doanhThuTrucTuyen || 0,
                donTaiQuay: overview.donTaiQuay || 0,
                donTrucTuyen: overview.donTrucTuyen || 0,
                sanPhamSapHet: overview.sanPhamSapHet || 0
            };

            if (overview.topSanPhamBanChay && overview.topSanPhamBanChay.length > 0) {
                topProducts.value = overview.topSanPhamBanChay.map((item) => ({
                    maSanPham: item.maSanPham || '',
                    name: item.name,
                    thuongHieu: item.thuongHieu || '',
                    revenue: item.revenue || 0,
                    quantity: item.quantity || 0,
                    growth: item.growth || 0
                }));
            } else {
                topProducts.value = [];
            }

            const categoryShares = Array.isArray(overview.tyTrongTheoDanhMuc)
                ? overview.tyTrongTheoDanhMuc
                    .map((item) => ({
                        name: item.name || 'Khác',
                        revenue: Number(item.revenue || 0)
                    }))
                    .filter((item) => Number.isFinite(item.revenue) && item.revenue > 0)
                : [];
            donutChartSeries.value = categoryShares.map((item) => item.revenue);
            donutChartOptions.value = {
                ...donutChartOptions.value,
                labels: categoryShares.map((item) => item.name)
            };
            donutChartKey.value += 1;

        }

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

        // Tải danh sách sản phẩm
        fetchProductStats();

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

const donutChartSeries = ref([]);
const donutChartKey = ref(0);

const hasValidDonutData = computed(() => {
    const total = donutChartSeries.value.reduce((sum, value) => sum + Number(value || 0), 0);
    return donutChartSeries.value.length > 0 && Number.isFinite(total) && total > 0;
});

const donutTotalRevenue = computed(() => {
    const total = donutChartSeries.value.reduce((sum, value) => sum + Number(value || 0), 0);
    return Number.isFinite(total) ? total : 0;
});

const donutChartOptions = ref({
    chart: {
        type: 'donut',
        toolbar: {
            show: false
        },
        animations: {
            enabled: false
        },
        redrawOnParentResize: true,
        redrawOnWindowResize: true,
        fontFamily: 'Inter, system-ui, -apple-system, sans-serif'
    },
    labels: [],
    colors: ['#4f46e5', '#06b6d4', '#10b981', '#f59e0b', '#ec4899'],
    stroke: {
        show: true,
        width: 2,
        colors: ['#ffffff']
    },
    dataLabels: {
        enabled: false
    },
    plotOptions: {
        pie: {
            expandOnClick: false,
            donut: {
                size: '58%',
                labels: {
                    show: false
                }
            }
        }
    },
    legend: {
        show: true,
        position: 'bottom',
        horizontalAlign: 'center',
        fontSize: '12px',
        fontWeight: 700,
        markers: {
            width: 11,
            height: 11,
            radius: 12
        },
        itemMargin: {
            horizontal: 6,
            vertical: 4
        }
    },
    tooltip: {
        y: {
            formatter: (value) => formatCurrency(Number.isFinite(Number(value)) ? Number(value) : 0)
        }
    }
});

const refreshProductFilters = () => {
    productPage.value = 1;
    fetchProductStats();
};

const resetProductFilters = () => {
    productSearchKeyword.value = '';
    productSortBy.value = 'bestSelling';
    productPage.value = 1;
    fetchProductStats();
};

const changeProductPageSize = () => {
    productPage.value = 1;
    fetchProductStats();
};

const goToProductPage = (page) => {
    productPage.value = Math.min(Math.max(page, 1), productTotalPages.value);
    fetchProductStats();
};

const kpiCards = [
    {
        title: 'Tổng doanh thu',
        valueKey: 'totalRevenue',
        icon: 'mdi-currency-usd',
        color: 'primary',
        tone: 'blue',
        formatter: formatCurrency
    },
    {
        title: 'Tổng đơn hàng',
        valueKey: 'totalOrders',
        icon: 'mdi-shopping-outline',
        color: 'success',
        tone: 'green',
        formatter: formatNumber
    },
    {
        title: 'Sản phẩm đã bán',
        valueKey: 'sanPhamDaBan',
        icon: 'mdi-package-variant-closed',
        color: 'secondary',
        tone: 'purple',
        formatter: formatNumber
    },
    {
        title: 'Giá trị trung bình đơn',
        valueKey: 'averageOrderValue',
        icon: 'mdi-chart-line',
        color: 'info',
        tone: 'cyan',
        formatter: formatCurrency
    },
    {
        title: 'Tổng khách hàng',
        valueKey: 'tongKhachHang',
        icon: 'mdi-account-group',
        color: 'warning',
        tone: 'orange',
        formatter: formatNumber
    }
];

const salesChannelCards = [
    {
        title: 'Bán hàng tại quầy',
        revenueKey: 'doanhThuTaiQuay',
        orderKey: 'donTaiQuay',
        icon: 'mdi-storefront-outline',
        tone: 'green'
    },
    {
        title: 'Bán hàng trực tuyến',
        revenueKey: 'doanhThuTrucTuyen',
        orderKey: 'donTrucTuyen',
        icon: 'mdi-web',
        tone: 'blue'
    }
];

onMounted(() => {
    loadStatistics();
});
</script>
<template>
    <div class="pa-6 font-body thong-ke-container">
        <AdminBreadcrumbs :items="[
            { title: 'Quản lý bán hàng', disabled: false, href: '#' },
            { title: 'Thống kê', disabled: true }
        ]" />

        <section class="stats-shell mt-4">
            <div class="stats-toolbar">
                <div class="stats-filters">
                    <div class="stats-filter-field stats-filter-field-date">
                        <div class="filter-field-label">Từ ngày</div>
                        <AppDatePicker :model-value="startDate" @update:model-value="onStartDateChange"
                            :max-date="endDate" placeholder="dd/mm/yyyy" />
                    </div>
                    <div class="stats-filter-field stats-filter-field-date">
                        <div class="filter-field-label">Đến ngày</div>
                        <AppDatePicker :model-value="endDate" @update:model-value="onEndDateChange"
                            :min-date="startDate" placeholder="dd/mm/yyyy" />
                    </div>
                    <v-btn color="primary" variant="flat" class="stats-refresh-btn px-6" height="40" :loading="loading"
                        @click="loadStatistics">
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
                </article>
            </div>

            <div class="chart-grid">
                <section class="stats-panel trend-panel chart-panel-wide">
                    <div class="chart-card-heading">
                        <h2>Xu hướng doanh thu (năm {{ selectedYear }})</h2>
                    </div>
                    <div class="tab-panel revenue-tab-panel">
                        <div class="sales-channel-grid">
                            <article v-for="item in salesChannelCards" :key="item.title" class="sales-channel-card">
                                <div class="sales-channel-icon" :class="`sales-channel-icon-${item.tone}`">
                                    <v-icon size="20">{{ item.icon }}</v-icon>
                                </div>
                                <div>
                                    <span>{{ item.title }}</span>
                                    <strong>{{ formatCurrency(revenueStats[item.revenueKey]) }}</strong>
                                    <small>{{ formatNumber(revenueStats[item.orderKey]) }} đơn</small>
                                </div>
                            </article>
                        </div>
                        <div v-if="loading" class="panel-loader panel-loader-tall">
                            <v-progress-circular indeterminate color="primary" />
                        </div>
                        <apexchart v-else type="area" height="310" :options="areaChartOptions"
                            :series="areaChartSeries" />
                    </div>
                </section>

                <section class="stats-panel trend-panel chart-panel-narrow">
                    <div class="chart-card-heading">
                        <h2>Trạng thái</h2>
                    </div>
                    <div class="tab-panel">
                        <div v-if="loading" class="panel-loader panel-loader-tall">
                            <v-progress-circular indeterminate color="primary" />
                        </div>
                        <div v-else class="status-chart-wrap">
                            <div class="status-summary-row">
                                <article v-for="item in statusChartItems" :key="item.label" class="status-summary-item"
                                    :class="{ active: item.active }">
                                    <span>{{ item.label }}</span>
                                    <strong>{{ formatCurrency(item.amount) }}</strong>
                                    <small>{{ formatNumber(item.count) }} đơn</small>
                                </article>
                            </div>
                            <apexchart class="status-bar-chart" type="bar" height="320" :options="statusBarOptions"
                                :series="statusBarSeries" />
                        </div>
                    </div>
                </section>
            </div>

            <div class="split-grid">
                <section class="stats-panel monthly-detail-panel">
                    <div class="simple-card-heading">
                        <h2>Chi tiết doanh thu tháng (năm {{ selectedYear }})</h2>
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

                <section class="stats-panel category-share-panel">
                    <div class="simple-card-heading">
                        <h2>Tỷ trọng theo danh mục</h2>
                        <v-icon color="#0f172a" size="22">mdi-chart-donut</v-icon>
                    </div>
                    <div v-if="loading" class="panel-loader">
                        <v-progress-circular indeterminate color="primary" />
                    </div>
                    <div v-else-if="!hasValidDonutData" class="empty-state">Không có dữ liệu trong thời gian
                        này</div>
                    <div v-else class="category-chart-body">
                        <div class="category-donut-wrap">
                            <apexchart :key="donutChartKey" type="donut" height="330" :options="donutChartOptions"
                                :series="donutChartSeries" />
                            <div class="category-donut-center">
                                <span>Tổng doanh thu</span>
                                <strong>{{ formatCurrency(donutTotalRevenue) }}</strong>
                            </div>
                        </div>
                    </div>
                </section>
            </div>

            <section class="stats-panel product-stats-panel">
                <div class="simple-card-heading">
                    <div class="product-stats-title">
                        <v-icon color="#e11d48" size="17">mdi-cube-outline</v-icon>
                        <h2>Thống kê sản phẩm</h2>
                    </div>
                    <span class="product-count-chip">{{ formatNumber(productTotalElements) }} sản phẩm</span>
                </div>
                <div v-if="loading" class="panel-loader">
                    <v-progress-circular indeterminate color="primary" />
                </div>
                <div v-else-if="productTotalElements === 0 && !productSearchKeyword"
                    class="empty-state product-empty-state">
                    Không có dữ liệu trong thời gian này
                </div>
                <div v-else class="product-table-section">
                    <AdminFilter title="" @refresh="resetProductFilters">
                        <v-col cols="12" sm="6" md="4" class="pb-1">
                            <div class="filter-field-label">Tìm kiếm</div>
                            <v-text-field v-model="productSearchKeyword" placeholder="Mã hoặc tên sản phẩm..."
                                density="comfortable" variant="outlined" hide-details clearable
                                prepend-inner-icon="mdi-magnify" bg-color="white"
                                @input="refreshProductFilters"></v-text-field>
                        </v-col>
                        <v-col cols="12" sm="6" md="4" class="pb-1">
                            <div class="filter-field-label">Sắp xếp</div>
                            <v-select v-model="productSortBy" :items="productSortOptions" item-title="title"
                                item-value="value" density="comfortable" variant="outlined" hide-details
                                bg-color="white" @update:model-value="refreshProductFilters"></v-select>
                        </v-col>
                    </AdminFilter>
                    <AdminTable hide-toolbar :headers="[
                        { text: 'STT', align: 'center', width: '80px' },
                        { text: 'Mã sản phẩm', align: 'center' },
                        { text: 'Tên sản phẩm', align: 'start' },
                        { text: 'Thương hiệu', align: 'center' },
                        { text: 'Đã bán', align: 'center' },
                        { text: 'Doanh thu', align: 'center' }
                    ]" :items="productStats">
                        <template #row="{ item, index }">
                            <tr class="data-row" :key="`${item.maSanPham}-${item.name}`">
                                <td class="data-cell">{{ (productPage - 1) * productPageSize + index + 1 }}</td>
                                <td class="data-cell">{{ item.maSanPham || '--' }}</td>
                                <td class="data-cell text-left">
                                    <div class="product-name-cell"
                                        style="justify-content: flex-start; text-align: left;">
                                        <span class="font-weight-medium" style="color: #1e293b">{{ item.name }}</span>
                                        <small style="color: #64748b; font-size: 11px">Tổng dữ liệu</small>
                                    </div>
                                </td>
                                <td class="data-cell">
                                    <span class="brand-pill"
                                        style="display: inline-flex; padding: 4px 10px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 12px; font-weight: 600; color: #475569;">
                                        {{ item.thuongHieu || '--' }}
                                    </span>
                                </td>
                                <td class="data-cell font-weight-medium" style="color: #1e293b">{{
                                    formatNumber(item.quantity) }}</td>
                                <td class="data-cell font-weight-bold" style="color: #e11d48">{{
                                    formatCurrency(item.revenue) }}</td>
                            </tr>
                        </template>

                        <template #pagination>
                            <AdminPagination v-model="productPage" v-model:page-size="productPageSize"
                                :total-elements="productTotalElements" :total-pages="productTotalPages"
                                :current-size="productStats.length" @change="goToProductPage"
                                @update:page-size="changeProductPageSize" />
                        </template>
                    </AdminTable>
                </div>
            </section>
        </section>
    </div>
</template>
