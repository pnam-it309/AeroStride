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

const loadProductStatsTable = async () => {
    try {
        const response = await dichVuSanPham.layDanhSachSanPham({
            page: 0,
            size: 1000,
            sortBy: 'ma',
            sortDirection: 'asc'
        });
        const products = Array.isArray(response?.content) ? response.content : [];
        const salesByCode = new Map(topProducts.value.map((item) => [item.maSanPham, item]));
        const salesByName = new Map(topProducts.value.map((item) => [item.name, item]));

        productStats.value = products.map((product) => {
            const salesStats = salesByCode.get(product.maSanPham) || salesByName.get(product.tenSanPham) || {};

            return {
                maSanPham: product.maSanPham || '',
                name: product.tenSanPham || 'Không có tên',
                thuongHieu: product.tenThuongHieu || '',
                quantity: salesStats.quantity || 0,
                revenue: salesStats.revenue || 0
            };
        });
        productPage.value = 1;
    } catch (error) {
        console.error('Error loading product stats table:', error);
        productStats.value = topProducts.value;
        productPage.value = 1;
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

        // Tải danh sách sản phẩm chạy ngầm, không block màn hình Dashboard
        loadProductStatsTable();

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

const productFilteredStats = computed(() => {
    const keyword = productSearchKeyword.value.trim().toLowerCase();
    const filtered = keyword
        ? productStats.value.filter((product) => {
            const searchable = [
                product.maSanPham,
                product.name,
                product.thuongHieu
            ].join(' ').toLowerCase();

            return searchable.includes(keyword);
        })
        : [...productStats.value];

    return filtered.sort((a, b) => {
        switch (productSortBy.value) {
            case 'revenueDesc':
                return (b.revenue || 0) - (a.revenue || 0);
            case 'bestSelling':
            default:
                return (b.quantity || 0) - (a.quantity || 0);
        }
    });
});

const productTotalPages = computed(() => Math.max(Math.ceil(productFilteredStats.value.length / productPageSize.value), 1));

const productPageNumbers = computed(() => {
    const total = productTotalPages.value;
    const current = Math.min(productPage.value, total);
    const maxVisible = 5;
    let start = Math.max(1, current - Math.floor(maxVisible / 2));
    let end = Math.min(total, start + maxVisible - 1);

    start = Math.max(1, end - maxVisible + 1);

    return Array.from({ length: end - start + 1 }, (_, index) => start + index);
});

const paginatedProductStats = computed(() => {
    const safePage = Math.min(productPage.value, productTotalPages.value);
    const start = (safePage - 1) * productPageSize.value;
    return productFilteredStats.value.slice(start, start + productPageSize.value);
});

const productShowingFrom = computed(() => {
    if (!productFilteredStats.value.length) return 0;
    return (Math.min(productPage.value, productTotalPages.value) - 1) * productPageSize.value + 1;
});

const productShowingTo = computed(() => {
    if (!productFilteredStats.value.length) return 0;
    return Math.min(productShowingFrom.value + productPageSize.value - 1, productFilteredStats.value.length);
});

const refreshProductFilters = () => {
    productPage.value = 1;
};

const resetProductFilters = () => {
    productSearchKeyword.value = '';
    productSortBy.value = 'bestSelling';
    productPage.value = 1;
};

const changeProductPageSize = () => {
    productPage.value = 1;
};

const goToProductPage = (page) => {
    productPage.value = Math.min(Math.max(page, 1), productTotalPages.value);
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
                        <h2>XU HƯỚNG DOANH THU (NĂM {{ selectedYear }})</h2>
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
                        <h2>TRẠNG THÁI</h2>
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

                <section class="stats-panel category-share-panel">
                    <div class="simple-card-heading">
                        <h2>TỶ TRỌNG THEO DANH MỤC</h2>
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
                        <h2>THỐNG KÊ SẢN PHẨM</h2>
                    </div>
                    <span class="product-count-chip">{{ formatNumber(productStats.length) }} sản phẩm</span>
                </div>
                <div v-if="loading" class="panel-loader">
                    <v-progress-circular indeterminate color="primary" />
                </div>
                <div v-else-if="productStats.length === 0" class="empty-state product-empty-state">
                    Không có dữ liệu trong thời gian này
                </div>
                <div v-else class="product-table-section">
                    <AdminFilter title="" @refresh="resetProductFilters">
                        <v-col cols="12" sm="6" md="4" class="pb-1">
                            <div class="filter-field-label">Tìm kiếm</div>
                            <v-text-field
                                v-model="productSearchKeyword"
                                placeholder="Mã hoặc tên sản phẩm..."
                                density="comfortable"
                                variant="outlined"
                                hide-details
                                clearable
                                prepend-inner-icon="mdi-magnify"
                                bg-color="white"
                                @input="refreshProductFilters"
                            ></v-text-field>
                        </v-col>
                        <v-col cols="12" sm="6" md="4" class="pb-1">
                            <div class="filter-field-label">Sắp xếp</div>
                            <v-select
                                v-model="productSortBy"
                                :items="productSortOptions"
                                item-title="title"
                                item-value="value"
                                density="comfortable"
                                variant="outlined"
                                hide-details
                                bg-color="white"
                                @update:model-value="refreshProductFilters"
                            ></v-select>
                        </v-col>
                    </AdminFilter>
                    <AdminTable
                        hide-toolbar
                        :headers="[
                            { text: 'STT', align: 'center', width: '80px' },
                            { text: 'MÃ SẢN PHẨM', align: 'center' },
                            { text: 'TÊN SẢN PHẨM', align: 'start' },
                            { text: 'THƯƠNG HIỆU', align: 'center' },
                            { text: 'ĐÃ BÁN', align: 'center' },
                            { text: 'DOANH THU', align: 'center' }
                        ]"
                        :items="paginatedProductStats"
                    >
                        <template #row="{ item, index }">
                            <tr class="data-row" :key="`${item.maSanPham}-${item.name}`">
                                <td class="data-cell">{{ (productPage - 1) * productPageSize + index + 1 }}</td>
                                <td class="data-cell">{{ item.maSanPham || '--' }}</td>
                                <td class="data-cell text-left">
                                    <div class="product-name-cell" style="justify-content: flex-start; text-align: left;">
                                        <span class="font-weight-medium" style="color: #1e293b">{{ item.name }}</span>
                                        <small style="color: #64748b; font-size: 11px">Tổng dữ liệu</small>
                                    </div>
                                </td>
                                <td class="data-cell">
                                    <span class="brand-pill" style="display: inline-flex; padding: 4px 10px; background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; font-size: 12px; font-weight: 600; color: #475569;">
                                        {{ item.thuongHieu || '--' }}
                                    </span>
                                </td>
                                <td class="data-cell font-weight-medium" style="color: #1e293b">{{ formatNumber(item.quantity) }}</td>
                                <td class="data-cell font-weight-bold" style="color: #e11d48">{{ formatCurrency(item.revenue) }}</td>
                            </tr>
                        </template>

                        <template #pagination>
                            <AdminPagination
                                v-model="productPage"
                                v-model:page-size="productPageSize"
                                :total-elements="productFilteredStats.length"
                                :total-pages="productTotalPages"
                                :current-size="paginatedProductStats.length"
                                @change="goToProductPage(productPage)"
                            />
                        </template>
                    </AdminTable>
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

.stats-title-tabs span+span {
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

.stats-filter-field-date {
    flex: 0 0 240px;
    width: 240px;
    max-width: 240px;
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

.stats-date-time-input :deep(.v-field) {
    min-height: 40px;
    border-radius: 8px;
    background: #ffffff;
    box-shadow: none;
}

.stats-date-time-input :deep(.v-field__outline) {
    color: #cbd5e1;
    --v-field-border-width: 1px;
}

.stats-date-time-input :deep(.v-field.v-field--focused .v-field__outline) {
    color: #2563eb;
    --v-field-border-width: 1px;
}

.stats-date-time-input :deep(.v-field__input) {
    min-height: 40px;
    padding: 0 8px 0 16px;
}

.stats-date-time-input :deep(.v-field__append-inner) {
    padding-inline-end: 8px;
    align-items: center;
}

.stats-date-time-input :deep(.v-field__append-inner .v-icon) {
    color: #c0c7d2;
    font-size: 17px;
    opacity: 1;
}

.stats-date-time-input :deep(.v-field:hover .v-field__append-inner .v-icon),
.stats-date-time-input :deep(.v-field.v-field--focused .v-field__append-inner .v-icon) {
    color: #64748b;
}

.stats-date-time-input :deep(input) {
    color: #334155;
    cursor: pointer;
    font-size: 13px;
    font-weight: 500;
    line-height: 40px;
}

.stats-date-time-input :deep(input::placeholder) {
    color: #8a94a6;
    opacity: 1;
}

.stats-date-time-input :deep(input::-webkit-calendar-picker-indicator) {
    cursor: pointer;
    margin-right: 0;
    opacity: 0.45;
    width: 18px;
    height: 18px;
    transition: opacity 0.15s ease;
}

.stats-date-time-input :deep(input::-webkit-calendar-picker-indicator:hover) {
    opacity: 0.8;
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
    grid-template-columns: repeat(5, minmax(0, 1fr));
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

.kpi-icon-purple {
    background: #f3e8ff;
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

.stats-panel {
    padding: 16px;
    border-radius: 8px;
}

.chart-grid {
    display: grid;
    grid-template-columns: minmax(0, 2fr) minmax(320px, 1fr);
    gap: 16px;
}

.trend-panel {
    border-radius: 14px;
    padding: 0;
    overflow: hidden;
}

.chart-card-heading {
    display: flex;
    align-items: center;
    min-height: 48px;
    padding: 0 22px;
    border-bottom: 1px solid #eef2f7;
    border-radius: 14px 14px 0 0;
    background: #f8fafc;
}

.chart-card-heading h2 {
    margin: 0;
    color: #0f172a;
    font-size: 13px;
    font-weight: 800;
    letter-spacing: 0.02em;
    text-transform: uppercase;
}

.tab-panel {
    padding: 16px;
}

.revenue-tab-panel {
    padding: 12px 16px 4px;
}

.revenue-tab-panel :deep(.vue-apexcharts) {
    min-height: 310px;
}

.sales-channel-grid {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
    margin-bottom: 10px;
}

.sales-channel-card span,
.sales-channel-card strong,
.sales-channel-card small,
.status-summary-item span,
.status-summary-item strong,
.status-summary-item small,
.month-cell span,
.month-cell strong,
.product-name-cell span,
.product-name-cell small {
    display: block;
}

.sales-channel-card {
    display: flex;
    min-height: 78px;
    align-items: center;
    gap: 12px;
    padding: 12px 14px;
    border: 1px solid #eef2f7;
    border-radius: 8px;
    background: #fbfcff;
}

.sales-channel-icon {
    display: grid;
    flex: 0 0 34px;
    width: 34px;
    height: 34px;
    place-items: center;
    border-radius: 8px;
}

.sales-channel-icon-green {
    background: #dcfce7;
    color: #16a34a;
}

.sales-channel-icon-blue {
    background: #eef4ff;
    color: #2563eb;
}

.sales-channel-card span {
    margin-bottom: 4px;
    color: #64748b;
    font-size: 12px;
    font-weight: 700;
}

.sales-channel-card strong {
    color: #0f172a;
    font-size: 17px;
    font-weight: 800;
    line-height: 1.2;
    overflow-wrap: anywhere;
}

.sales-channel-card small {
    margin-top: 4px;
    color: #334155;
    font-size: 12px;
    font-weight: 500;
}

.status-chart-wrap {
    border-top: 0;
}

.status-summary-row {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 8px;
    padding: 0 0 8px;
    border-bottom: 0;
}

.status-summary-item {
    position: relative;
    min-height: 58px;
    padding: 8px 10px;
    border: 1px solid #eef2f7;
    border-radius: 8px;
    background: #fbfcff;
    text-align: left;
}

.status-summary-item span {
    margin-bottom: 4px;
    color: #64748b;
    font-size: 11px;
    font-weight: 600;
}

.status-summary-item strong {
    color: #0f172a;
    font-size: 13px;
    font-weight: 800;
    line-height: 1.2;
    overflow-wrap: anywhere;
}

.status-summary-item small {
    margin-top: 3px;
    color: #334155;
    font-size: 11px;
    font-weight: 500;
}

.status-bar-chart {
    margin-top: 2px;
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

.category-share-panel,
.monthly-detail-panel,
.product-stats-panel {
    padding: 0;
    overflow: hidden;
    border: 1px solid #e2e8f0;
    border-radius: 14px;
    background: #ffffff;
}

.category-share-panel .simple-card-heading,
.monthly-detail-panel .simple-card-heading,
.product-stats-panel .simple-card-heading {
    min-height: 52px;
    margin-bottom: 0;
    padding: 0 22px;
    border-bottom: 1px solid #e7edf5;
    background: #f8fafc;
}

.category-share-panel .simple-card-heading h2,
.monthly-detail-panel .simple-card-heading h2,
.product-stats-panel .simple-card-heading h2 {
    font-size: 13px;
    letter-spacing: 0.03em;
}

.category-share-panel .panel-loader,
.category-share-panel .empty-state {
    min-height: 310px;
}

.category-share-panel .empty-state {
    color: #111827;
    font-size: 15px;
    font-weight: 500;
}

.category-chart-body {
    display: flex;
    min-height: 356px;
    align-items: center;
    justify-content: center;
    padding: 18px 14px 12px;
}

.category-chart-body :deep(.vue-apexcharts) {
    width: min(100%, 420px);
}

.category-donut-wrap {
    position: relative;
    width: min(100%, 420px);
}

.category-donut-center {
    position: absolute;
    top: 41%;
    left: 50%;
    display: grid;
    width: 150px;
    transform: translate(-50%, -50%);
    gap: 10px;
    pointer-events: none;
    text-align: center;
}

.category-donut-center span {
    color: #64748b;
    font-size: 13px;
    font-weight: 700;
}

.category-donut-center strong {
    color: #0f172a;
    font-size: 15px;
    font-weight: 900;
    line-height: 1.2;
    overflow-wrap: anywhere;
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

.month-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
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
    margin-bottom: 8px;
    color: #64748b;
    font-size: 11px;
    font-weight: 700;
    text-transform: uppercase;
}

.month-cell strong {
    color: #0f172a;
    font-size: 13px;
    font-weight: 800;
    overflow-wrap: anywhere;
}

.product-stats-title {
    display: inline-flex;
    align-items: center;
    gap: 9px;
}

.product-count-chip {
    padding: 6px 10px;
    border-radius: 999px;
    background: #f1f5f9;
    color: #64748b;
    font-size: 12px;
    font-weight: 700;
}

.product-empty-state {
    min-height: 250px;
}

.product-table-section {
    background: #ffffff;
}

.product-table-filters {
    display: grid;
    grid-template-columns: minmax(260px, 380px) minmax(220px, 280px) 86px;
    align-items: end;
    gap: 14px;
    padding: 14px 18px 6px;
}

.product-filter-group {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 0;
}

.product-filter-group span {
    color: #0f172a;
    font-size: 13px;
    font-weight: 800;
    line-height: 18px;
}

.product-search-box,
.product-filter-select,
.product-reset-btn {
    height: 40px;
    border: 1px solid #d7e1ef;
    border-radius: 8px;
    background: #ffffff;
    color: #0f172a;
    font-size: 13px;
}

.product-search-box {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 0 14px;
}

.product-search-box input {
    width: 100%;
    border: 0;
    background: transparent;
    color: #0f172a;
    font-size: 13px;
    font-weight: 600;
    outline: none;
}

.product-search-box input::placeholder {
    color: #a8b2c1;
    font-weight: 600;
}

.product-filter-select {
    width: 100%;
    padding: 0 34px 0 12px;
    appearance: none;
    background-image: url("data:image/svg+xml,%3Csvg width='12' height='8' viewBox='0 0 12 8' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1.41 0.59L6 5.17L10.59 0.59L12 2L6 8L0 2L1.41 0.59Z' fill='%23334155'/%3E%3C/svg%3E");
    background-position: right 12px center;
    background-repeat: no-repeat;
    background-size: 10px 7px;
    color: #334155;
    cursor: pointer;
    font-weight: 700;
    line-height: 40px;
    outline: none;
}

.product-reset-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    border-color: #e2e8f0;
    border-radius: 9px;
    cursor: pointer;
    font-size: 12px;
    font-weight: 800;
    transition: all 0.15s ease;
}

.product-reset-btn:hover {
    border-color: #cbd5e1;
    background: #f8fafc;
}

.product-table-wrap {
    overflow-x: auto;
    padding: 8px 18px 18px;
}

.product-stats-table {
    width: 100%;
    min-width: 820px;
    border-collapse: separate;
    border-spacing: 0 8px;
}

.product-stats-table th {
    padding: 8px 12px;
    color: #334155;
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 0.08em;
    text-align: left;
    white-space: nowrap;
}

.product-stats-table td {
    padding: 12px;
    border-top: 1px solid #edf2f7;
    border-bottom: 1px solid #edf2f7;
    background: #fbfcff;
    color: #0f172a;
    font-size: 13px;
    font-weight: 600;
}

.product-stats-table td:first-child {
    border-left: 1px solid #edf2f7;
    border-radius: 8px 0 0 8px;
    color: #64748b;
    width: 56px;
}

.product-stats-table td:last-child {
    border-right: 1px solid #edf2f7;
    border-radius: 0 8px 8px 0;
    font-weight: 800;
    text-align: right;
}

.product-stats-table th:last-child {
    text-align: right;
}

.product-name-cell span {
    font-weight: 700;
}

.product-name-cell small {
    margin-top: 3px;
    color: #64748b;
    font-size: 12px;
    font-weight: 500;
}

.brand-pill {
    display: inline-flex;
    align-items: center;
    min-height: 26px;
    padding: 4px 10px;
    border: 1px solid #e2e8f0;
    border-radius: 999px;
    background: #ffffff;
    color: #475569;
    font-size: 12px;
    font-weight: 700;
}

.product-pagination {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    padding: 18px 16px;
    border-top: 1px solid #eef2f7;
    background: #ffffff;
}

.product-page-info {
    color: #475569;
    font-size: 13px;
    font-weight: 500;
    line-height: 1.4;
}

.product-page-info strong {
    color: #0f172a;
    font-weight: 800;
}

.product-pagination-actions,
.product-page-size,
.product-page-controls {
    display: flex;
    align-items: center;
    gap: 8px;
}

.product-pagination-actions {
    gap: 16px;
}

.product-page-size span {
    color: #0f172a;
    font-size: 13px;
    font-weight: 700;
}

.product-page-size-select {
    width: 98px;
    height: 28px;
    padding: 0 30px 0 12px;
    border: 1px solid #cbd5e1;
    border-radius: 7px;
    appearance: none;
    background-color: #ffffff;
    background-image: url("data:image/svg+xml,%3Csvg width='12' height='8' viewBox='0 0 12 8' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M1.41 0.59L6 5.17L10.59 0.59L12 2L6 8L0 2L1.41 0.59Z' fill='%23000000'/%3E%3C/svg%3E");
    background-position: right 10px center;
    background-repeat: no-repeat;
    background-size: 10px 7px;
    color: #0f172a;
    cursor: pointer;
    font-size: 13px;
    font-weight: 800;
    line-height: 28px;
    outline: none;
}

.product-page-btn {
    display: grid;
    width: 32px;
    height: 32px;
    place-items: center;
    border: 1px solid #dbe4f0;
    border-radius: 8px;
    background: #ffffff;
    color: #0f172a;
    cursor: pointer;
    font-size: 13px;
    font-weight: 800;
    transition: all 0.15s ease;
}

.product-page-btn.active {
    border-color: #1e3a8a;
    background: #1e3a8a;
    color: #ffffff;
}

.product-page-btn:disabled {
    border-color: #e5edf7;
    background: #f8fafc;
    color: #cbd5e1;
    cursor: not-allowed;
}

.product-page-btn:not(:disabled):not(.active):hover {
    border-color: #94a3b8;
    background: #f8fafc;
}

@media (max-width: 1180px) {
    .kpi-grid {
        grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .chart-grid {
        grid-template-columns: 1fr;
    }

    .status-summary-row {
        grid-template-columns: repeat(4, minmax(0, 1fr));
    }

    .month-grid {
        grid-template-columns: repeat(4, minmax(0, 1fr));
    }

    .product-table-filters {
        grid-template-columns: minmax(240px, 1fr) minmax(210px, 260px) 86px;
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
    .stats-filter-field-date,
    .stats-filter,
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

    .sales-channel-grid {
        grid-template-columns: 1fr;
    }

    .status-summary-item {
        text-align: left;
    }

    .product-table-filters {
        grid-template-columns: 1fr;
    }

    .product-pagination {
        align-items: flex-start;
        flex-direction: column;
    }

    .product-pagination-actions {
        flex-wrap: wrap;
    }

}
</style>
