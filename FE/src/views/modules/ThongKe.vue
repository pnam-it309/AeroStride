<script setup>
import { ref, computed, onMounted } from 'vue';
import { AdminBreadcrumbs, AdminFilter, AdminTable, AdminPagination } from '@/components/common';
import AppDatePicker from '@/components/common/AppDatePicker.vue';
import { formatCurrency, formatNumber } from '@/utils/formatters';
import { dichVuThongKe } from '@/services/admin/dichVuThongKe';
import { dichVuHoaDon } from '@/services/admin/dichVuHoaDon';
import { dichVuThuongHieu } from '@/services/product/dichVuThuocTinh';
import { useToastStore } from '@/stores/toastStore';
import apexchart from 'vue3-apexcharts';

const toastStore = useToastStore();
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
defaultStartDate.setDate(defaultStartDate.getDate() - 7);
const startDate = ref(formatDateInput(defaultStartDate));
const endDate = ref(formatDateInput(defaultEndDate));

const onStartDateChange = (val) => {
    startDate.value = val ? formatDateInput(new Date(val)) : null;
    if (startDate.value && endDate.value && startDate.value > endDate.value) {
        endDate.value = startDate.value;
    }
    if (startDate.value && endDate.value) {
        refreshAllData(false);
    }
};

const onEndDateChange = (val) => {
    endDate.value = val ? formatDateInput(new Date(val)) : null;
    if (startDate.value && endDate.value && endDate.value < startDate.value) {
        startDate.value = endDate.value;
    }
    if (startDate.value && endDate.value) {
        refreshAllData(false);
    }
};

const selectedYear = computed(() => {
    const rangeDate = endDate.value || startDate.value;
    const year = Number(rangeDate?.slice(0, 4));
    return year || new Date().getFullYear();
});

const formatDateVN = (dateStr) => {
    if (!dateStr) return '';
    const parts = dateStr.split('-');
    if (parts.length !== 3) return dateStr;
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
};

const generateHourlyDataFromInvoices = async (dailyTotal, dateStr) => {
    if (!dateStr || !dailyTotal || Number(dailyTotal) <= 0) {
        return { revenue: Array(24).fill(0), customers: Array(24).fill(0) };
    }
    try {
        const response = await dichVuHoaDon.layHoaDonPhanTrang({
            tuNgay: dateStr,
            denNgay: dateStr,
            trangThai: 4, // HOAN_THANH
            size: 100
        });
        const invoices = Array.isArray(response) ? response : response?.data?.content || response?.data || response?.content || [];
        const hourlyRevenue = Array(24).fill(0);
        const hourlyCustomers = Array(24).fill(0);

        invoices.forEach((inv) => {
            if (inv.ngayTao) {
                const dateObj = new Date(inv.ngayTao);
                const hour = dateObj.getHours();
                if (hour >= 0 && hour < 24) {
                    const revenue = Number(inv.tongTienSauGiam || inv.tongTien || 0);
                    hourlyRevenue[hour] += revenue;
                    hourlyCustomers[hour] += 1;
                }
            }
        });
        return { revenue: hourlyRevenue, customers: hourlyCustomers };
    } catch (e) {
        console.error(`Error fetching real hourly invoices for ${dateStr}:`, e);
        return { revenue: Array(24).fill(0), customers: Array(24).fill(0) };
    }
};

const hourlyCategories = ['0h - 3h', '3h - 6h', '6h - 9h', '9h - 12h', '12h - 15h', '15h - 18h', '18h - 21h', '21h - 0h'];

const aggregateTo3HourSlots = (hourlyDataObj) => {
    const revenueSlots = Array(8).fill(0);
    const customerSlots = Array(8).fill(0);
    for (let i = 0; i < 24; i++) {
        const slotIndex = Math.floor(i / 3);
        revenueSlots[slotIndex] += hourlyDataObj.revenue[i] || 0;
        customerSlots[slotIndex] += hourlyDataObj.customers[i] || 0;
    }
    return { revenue: revenueSlots, customers: customerSlots };
};

const startHourlyChartSeries = ref([
    {
        name: 'Doanh thu',
        type: 'line',
        data: Array(24).fill(0)
    },
    {
        name: 'Khách hàng',
        type: 'line',
        data: Array(24).fill(0)
    }
]);

const endHourlyChartSeries = ref([
    {
        name: 'Doanh thu',
        type: 'line',
        data: Array(24).fill(0)
    },
    {
        name: 'Khách hàng',
        type: 'line',
        data: Array(24).fill(0)
    }
]);

const startHourlyMax = ref(0);
const endHourlyMax = ref(0);

const startDailyTotalRevenue = ref(0);
const endDailyTotalRevenue = ref(0);

const hourlyPercentageInfo = computed(() => {
    if (startDailyTotalRevenue.value === 0) {
        if (endDailyTotalRevenue.value > 0) {
            return {
                text: '100.0%',
                icon: '▲',
                color: '#22c55e'
            };
        }
        return {
            text: '0.0%',
            icon: '',
            color: '#64748b'
        };
    }
    const diff = ((endDailyTotalRevenue.value - startDailyTotalRevenue.value) / startDailyTotalRevenue.value) * 100;
    if (diff > 0) {
        return {
            text: `${diff.toFixed(1)}%`,
            icon: '▲',
            color: '#22c55e'
        };
    } else if (diff < 0) {
        return {
            text: `${Math.abs(diff).toFixed(1)}%`,
            icon: '▼',
            color: '#ef4444'
        };
    } else {
        return {
            text: '0.0%',
            icon: '',
            color: '#64748b'
        };
    }
});

const getHourlyChartOptions = (color, maxVal, maxCust) => {
    const isUnder50M = !maxVal || maxVal <= 50000000;
    const safeMaxVal = Number.isFinite(maxVal) ? maxVal : 0;
    const yaxisMaxRevenue = isUnder50M ? 50000000 : Math.ceil((safeMaxVal * 1.2) / 10000000) * 10000000;
    return {
        chart: {
            type: 'line',
            height: 280,
            toolbar: {
                show: false
            },
            zoom: {
                enabled: false
            },
            fontFamily: 'Inter, system-ui, -apple-system, sans-serif'
        },
        colors: [color, '#10b981'],
        dataLabels: {
            enabled: true,
            enabledOnSeries: [0],
            formatter: function (val, opts) {
                if (!val) return '';
                const idx = opts?.dataPointIndex;
                const totalPoints = opts?.w?.config?.series?.[opts?.seriesIndex]?.data?.length || hourlyCategories.length;
                // Ẩn số ở 2 điểm ngoài cùng (điểm đầu và điểm cuối) để không bị tràn mép/vỡ chữ
                if (idx === 0 || idx === totalPoints - 1) {
                    return '';
                }
                return new Intl.NumberFormat('vi-VN').format(val) + 'đ';
            },
            style: {
                fontSize: '10px',
                fontWeight: '800',
                colors: [color]
            },
            background: {
                enabled: false
            },
            textAnchor: 'start',
            offsetX: 8,
            offsetY: -2
        },
        markers: {
            size: [5, 4],
            colors: [color, '#10b981'],
            strokeColors: '#ffffff',
            strokeWidth: 2,
            hover: {
                size: 7
            }
        },
        stroke: {
            curve: 'smooth',
            width: [3, 1.5]
        },
        grid: {
            borderColor: '#f1f5f9',
            strokeDashArray: 4,
            padding: {
                top: 15,
                right: 15
            }
        },
        xaxis: {
            categories: hourlyCategories,
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
        yaxis: [
            {
                min: 0,
                max: yaxisMaxRevenue,
                tickAmount: isUnder50M ? 5 : undefined,
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
            {
                opposite: true,
                min: 0,
                max: !maxCust || maxCust <= 100 ? 100 : undefined,
                tickAmount: 5,
                title: {
                    text: 'Khách hàng',
                    style: {
                        color: '#10b981',
                        fontSize: '11px',
                        fontWeight: 600
                    }
                },
                labels: {
                    formatter: function (value) {
                        return Math.round(value) + ' lượt';
                    },
                    style: {
                        colors: '#10b981',
                        fontSize: '11px',
                        fontWeight: 500
                    }
                }
            }
        ],
        tooltip: {
            y: [
                {
                    formatter: function (val) {
                        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
                    }
                },
                {
                    formatter: function (val) {
                        return val + ' lượt';
                    }
                }
            ],
            theme: 'light'
        }
    };
};

const startHourlyCustomerMax = ref(0);
const endHourlyCustomerMax = ref(0);

const startHourlyChartOptions = computed(() => getHourlyChartOptions('#0ea5e9', startHourlyMax.value, startHourlyCustomerMax.value));
const endHourlyChartOptions = computed(() => getHourlyChartOptions('#ef4444', endHourlyMax.value, endHourlyCustomerMax.value));

const revenueStats = ref({
    totalRevenue: 0,
    totalOrders: 0,
    averageOrderValue: 0,
    growthRate: 0,
    donHangHoanThanh: 0,
    donHangChoXacNhan: 0,
    donHangDangGiao: 0,
    donHangDaHuy: 0,
    donHangHoan: 0,
    tongKhachHang: 0,
    tongSanPham: 0,
    sanPhamDaBan: 0,
    doanhThuTaiQuay: 0,
    doanhThuTrucTuyen: 0,
    donTaiQuay: 0,
    donTrucTuyen: 0,
    sanPhamSapHet: 0,
    doanhThuChoXacNhan: 0,
    doanhThuDangGiao: 0,
    doanhThuDaHuy: 0
});

const yearlyRevenueStats = ref({
    totalRevenue: 0,
    totalOrders: 0,
    averageOrderValue: 0,
    growthRate: 0,
    donHangHoanThanh: 0,
    donHangChoXacNhan: 0,
    donHangDangGiao: 0,
    donHangDaHuy: 0,
    donHangHoan: 0,
    tongKhachHang: 0,
    tongSanPham: 0,
    sanPhamDaBan: 0,
    doanhThuTaiQuay: 0,
    doanhThuTrucTuyen: 0,
    donTaiQuay: 0,
    donTrucTuyen: 0,
    sanPhamSapHet: 0,
    doanhThuChoXacNhan: 0,
    doanhThuDangGiao: 0,
    doanhThuDaHuy: 0
});

const topProducts = ref([]);
const customerPurchaseStats = ref([]);

const customerStatsTotals = computed(() => {
    let tongChi = 0;
    let tongSanPham = 0;
    let donThanhCong = 0;
    let donHoan = 0;
    customerPurchaseStats.value.forEach((item) => {
        tongChi += item.tongChi;
        tongSanPham += item.tongSanPham;
        donThanhCong += item.donThanhCong;
        donHoan += item.donHoan;
    });
    return { tongChi, tongSanPham, donThanhCong, donHoan };
});

const employeePurchaseStats = ref([]);

const employeeStatsTotals = computed(() => {
    let tongChi = 0;
    let tongSanPham = 0;
    let tongDonHang = 0;
    employeePurchaseStats.value.forEach((item) => {
        tongChi += item.tongChi;
        tongSanPham += item.tongSanPham;
        tongDonHang += item.tongDonHang;
    });
    return { tongChi, tongSanPham, tongDonHang };
});

const revenueCycles = ref([]);

const getPercent = (value, total, symbol = '↑') => {
    if (!total || value <= 0) return '-';
    const pct = ((value / total) * 100).toFixed(2);
    return `${symbol} ${pct.replace('.', ',')}%`;
};

const productStats = ref([]);
const productSearchKeyword = ref('');
const productBrandId = ref(null);
const brandOptions = ref([]);
const productSortBy = ref('bestSelling');
const productPage = ref(1);
const productPageSize = ref(10);
const productPageSizeOptions = [
    { title: '10 dòng', value: 10 },
    { title: '20 dòng', value: 20 },
    { title: '50 dòng', value: 50 }
];

const loadBrands = async () => {
    if (brandOptions.value && brandOptions.value.length > 0) return;
    try {
        const res = await dichVuThuongHieu.layThuongHieu({ size: 100 });
        const list = Array.isArray(res?.content) ? res.content : Array.isArray(res) ? res : [];
        brandOptions.value = list.map((b) => ({ title: b.ten || b.tenThuongHieu, value: b.id }));
    } catch (e) {
        console.error('Error loading brands in ThongKe:', e);
    }
};

const toggleSoldSort = () => {
    productSortBy.value = productSortBy.value === 'bestSelling' ? 'slowSelling' : 'bestSelling';
    productPage.value = 1;
    fetchProductStats();
};

const toggleRevenueSort = () => {
    productSortBy.value = productSortBy.value === 'revenueDesc' ? 'revenueAsc' : 'revenueDesc';
    productPage.value = 1;
    fetchProductStats();
};

const monthlyRevenue = ref([]);

// Cấu hình reactive cho ApexCharts
const areaChartSeries = ref([
    {
        name: 'Doanh thu',
        type: 'line',
        data: Array(12).fill(0)
    },
    {
        name: 'Khách hàng',
        type: 'line',
        data: Array(12).fill(0)
    }
]);

const areaChartOptions = ref({
    chart: {
        type: 'line',
        height: 320,
        toolbar: {
            show: false
        },
        zoom: {
            enabled: false
        },
        fontFamily: 'Inter, system-ui, -apple-system, sans-serif'
    },
    colors: ['#4f46e5', '#10b981'],
    dataLabels: {
        enabled: false
    },
    markers: {
        size: [5, 4],
        colors: ['#4f46e5', '#10b981'],
        strokeColors: '#ffffff',
        strokeWidth: 2,
        hover: {
            size: 7
        }
    },
    stroke: {
        curve: 'smooth',
        width: [3, 1.5]
    },
    fill: {
        type: 'solid'
    },
    grid: {
        borderColor: '#f1f5f9',
        strokeDashArray: 4
    },
    xaxis: {
        categories: [
            'Tháng 1',
            'Tháng 2',
            'Tháng 3',
            'Tháng 4',
            'Tháng 5',
            'Tháng 6',
            'Tháng 7',
            'Tháng 8',
            'Tháng 9',
            'Tháng 10',
            'Tháng 11',
            'Tháng 12'
        ],
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
    yaxis: [
        {
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
        {
            opposite: true,
            min: 0,
            max: 100,
            tickAmount: 5,
            title: {
                text: 'Khách hàng',
                style: {
                    color: '#10b981',
                    fontSize: '11px',
                    fontWeight: 600
                }
            },
            labels: {
                formatter: function (value) {
                    return Math.round(value) + ' lượt';
                },
                style: {
                    colors: '#10b981',
                    fontSize: '11px',
                    fontWeight: 500
                }
            }
        }
    ],
    tooltip: {
        y: [
            {
                formatter: function (val) {
                    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val);
                }
            },
            {
                formatter: function (val) {
                    return val + ' lượt';
                }
            }
        ],
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
    colors: ['#4f939c', '#e39b32', '#16a34a', '#c9473d'],
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
            show: true,
            rotate: 0,
            rotateAlways: false,
            hideOverlappingLabels: false,
            trim: false,
            style: {
                colors: ['#4f939c', '#e39b32', '#16a34a', '#c9473d'],
                fontSize: '11px',
                fontWeight: 600
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
const productLoading = ref(false);
const isFallbackYearData = ref(false);

const fetchProductStats = async () => {
    productLoading.value = true;
    isFallbackYearData.value = false;
    try {
        const { tuNgay, denNgay } = getDateRange();
        let response = await dichVuThongKe.layThongKeSanPham({
            tuNgay: tuNgay || undefined,
            denNgay: denNgay || undefined,
            keyword: productSearchKeyword.value || undefined,
            thuongHieuId: productBrandId.value || undefined,
            page: productPage.value - 1,
            size: productPageSize.value,
            sortBy: productSortBy.value
        });

        let list = Array.isArray(response?.content) ? response.content : [];
        const hasSoldProducts = list.some((item) => Number(item.quantity || 0) > 0 || Number(item.revenue || 0) > 0);

        // Nếu trong khoảng ngày/tháng được chọn không có dữ liệu bán và không lọc thương hiệu/từ khóa, fallback năm
        if (!hasSoldProducts && (tuNgay || denNgay) && selectedYear.value && !productBrandId.value && !productSearchKeyword.value) {
            const startOfYear = `${selectedYear.value}-01-01`;
            const endOfYear = `${selectedYear.value}-12-31`;
            const fallbackResponse = await dichVuThongKe.layThongKeSanPham({
                tuNgay: startOfYear,
                denNgay: endOfYear,
                page: productPage.value - 1,
                size: productPageSize.value,
                sortBy: productSortBy.value
            });
            const fallbackList = Array.isArray(fallbackResponse?.content) ? fallbackResponse.content : [];
            if (fallbackList.length > 0) {
                response = fallbackResponse;
                list = fallbackList;
                isFallbackYearData.value = true;
            }
        }

        productStats.value = list;
        productTotalElements.value = response?.totalElements || list.length;
        productTotalPages.value = response?.totalPages || 1;
    } catch (error) {
        console.error('Error loading product stats table:', error);
        productStats.value = [];
        productTotalElements.value = 0;
        productTotalPages.value = 1;
    } finally {
        productLoading.value = false;
    }
};

const loadStatistics = async () => {
    loading.value = true;
    try {
        const { tuNgay, denNgay } = getDateRange();
        const startOfYear = `${selectedYear.value}-01-01`;
        const endOfYear = `${selectedYear.value}-12-31`;

        // Chỉ gọi layTongQuan 1 lần nếu khoảng ngày chọn chính là cả năm
        const isFullYear = tuNgay === startOfYear && denNgay === endOfYear;
        const promises = [
            dichVuThongKe.layTongQuan(tuNgay, denNgay),
            dichVuThongKe.layDoanhThuTheoNgay(startOfYear, endOfYear),
            fetchProductStats()
        ];
        if (!isFullYear) {
            promises.push(dichVuThongKe.layTongQuan(startOfYear, endOfYear));
        }

        const [overview, dailyData, prodStats, maybeYearly] = await Promise.all(promises);
        const yearlyOverview = isFullYear ? overview : maybeYearly;

        if (overview) {
            const soldProductQuantity = Array.isArray(overview.topSanPhamBanChay)
                ? overview.topSanPhamBanChay.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
                : 0;
            const completedOrderCount = Number(overview.donHangHoanThanh || 0);
            const totalRevenue = Number(overview.tongDoanhThu || 0);
            const averageOrderValue =
                overview.giaTriTrungBinh != null
                    ? Number(overview.giaTriTrungBinh)
                    : completedOrderCount > 0
                      ? totalRevenue / completedOrderCount
                      : 0;

            revenueStats.value = {
                totalRevenue,
                totalOrders: overview.tongDonHang || 0,
                averageOrderValue,
                growthRate: 0,
                donHangHoanThanh: overview.donHangHoanThanh || 0,
                donHangChoXacNhan: overview.donHangChoXacNhan || 0,
                donHangDangGiao: overview.donHangDangGiao || 0,
                donHangDaHuy: overview.donHangDaHuy || 0,
                donHangHoan: overview.donHangHoan || 0,
                tongKhachHang: overview.tongKhachHang || 0,
                tongSanPham: overview.tongSanPham || 0,
                sanPhamDaBan: soldProductQuantity,
                doanhThuTaiQuay: overview.doanhThuTaiQuay || 0,
                doanhThuTrucTuyen: overview.doanhThuTrucTuyen || 0,
                donTaiQuay: overview.donTaiQuay || 0,
                donTrucTuyen: overview.donTrucTuyen || 0,
                sanPhamSapHet: overview.sanPhamSapHet || 0,
                doanhThuChoXacNhan: overview.doanhThuChoXacNhan || 0,
                doanhThuDangGiao: overview.doanhThuDangGiao || 0,
                doanhThuDaHuy: overview.doanhThuDaHuy || 0
            };

            const effectiveTopProducts = (overview.topSanPhamBanChay && overview.topSanPhamBanChay.length > 0)
                ? overview.topSanPhamBanChay
                : (yearlyOverview?.topSanPhamBanChay || []);

            if (effectiveTopProducts.length > 0) {
                topProducts.value = effectiveTopProducts.map((item) => ({
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

            const effectiveBrandShares = (Array.isArray(overview.tyTrongTheoThuongHieu) && overview.tyTrongTheoThuongHieu.length > 0)
                ? overview.tyTrongTheoThuongHieu
                : (Array.isArray(yearlyOverview?.tyTrongTheoThuongHieu) ? yearlyOverview.tyTrongTheoThuongHieu : []);

            const brandShares = effectiveBrandShares
                .map((item) => ({
                    name: item.name || 'Khác',
                    revenue: Number(item.revenue || 0)
                }))
                .filter((item) => Number.isFinite(item.revenue) && item.revenue > 0);

            donutChartSeries.value = brandShares.map((item) => item.revenue);
            donutChartOptions.value = {
                ...donutChartOptions.value,
                labels: brandShares.map((item) => item.name)
            };
            donutChartKey.value += 1;

            const effectiveCustomers = (overview.topKhachHang && overview.topKhachHang.length > 0)
                ? overview.topKhachHang
                : (yearlyOverview?.topKhachHang || []);

            if (effectiveCustomers.length > 0) {
                customerPurchaseStats.value = effectiveCustomers.map((item) => ({
                    tenKhachHang: item.tenKhachHang || 'Khách lẻ',
                    tongChi: Number(item.tongChi || 0),
                    tongSanPham: Number(item.tongSanPham || 0),
                    donThanhCong: Number(item.donThanhCong || 0),
                    donHoan: Number(item.donHoan || 0)
                }));
            } else {
                customerPurchaseStats.value = [];
            }

            const effectiveEmployees = (overview.topNhanVien && overview.topNhanVien.length > 0)
                ? overview.topNhanVien
                : (yearlyOverview?.topNhanVien || []);

            if (effectiveEmployees.length > 0) {
                employeePurchaseStats.value = effectiveEmployees.map((item) => ({
                    maNhanVien: item.maNhanVien || '',
                    tenNhanVien: item.tenNhanVien || 'Hệ thống/Online',
                    tongChi: Number(item.tongChi || 0),
                    tongSanPham: Number(item.tongSanPham || 0),
                    tongDonHang: Number(item.tongDonHang || 0)
                }));
            } else {
                employeePurchaseStats.value = [];
            }

            if (overview.chuKyDoanhThu && overview.chuKyDoanhThu.length > 0) {
                revenueCycles.value = overview.chuKyDoanhThu.map((item) => ({
                    tenChuKy: item.tenChuKy,
                    doanhThu: Number(item.doanhThu || 0),
                    soDon: Number(item.soDon || 0),
                    trungBinhDon: Number(item.trungBinhDon || 0)
                }));
            } else {
                revenueCycles.value = [];
            }
        }

        if (yearlyOverview) {
            const yearlySoldProductQuantity = Array.isArray(yearlyOverview.topSanPhamBanChay)
                ? yearlyOverview.topSanPhamBanChay.reduce((sum, item) => sum + Number(item.quantity || 0), 0)
                : 0;
            const yearlyCompletedOrderCount = Number(yearlyOverview.donHangHoanThanh || 0);
            const yearlyTotalRevenue = Number(yearlyOverview.tongDoanhThu || 0);
            const yearlyAverageOrderValue =
                yearlyOverview.giaTriTrungBinh != null
                    ? Number(yearlyOverview.giaTriTrungBinh)
                    : yearlyCompletedOrderCount > 0
                      ? yearlyTotalRevenue / yearlyCompletedOrderCount
                      : 0;

            yearlyRevenueStats.value = {
                totalRevenue: yearlyTotalRevenue,
                totalOrders: yearlyOverview.tongDonHang || 0,
                averageOrderValue: yearlyAverageOrderValue,
                growthRate: 0,
                donHangHoanThanh: yearlyOverview.donHangHoanThanh || 0,
                donHangChoXacNhan: yearlyOverview.donHangChoXacNhan || 0,
                donHangDangGiao: yearlyOverview.donHangDangGiao || 0,
                donHangDaHuy: yearlyOverview.donHangDaHuy || 0,
                donHangHoan: yearlyOverview.donHangHoan || 0,
                tongKhachHang: yearlyOverview.tongKhachHang || 0,
                tongSanPham: yearlyOverview.tongSanPham || 0,
                sanPhamDaBan: yearlySoldProductQuantity,
                doanhThuTaiQuay: yearlyOverview.doanhThuTaiQuay || 0,
                doanhThuTrucTuyen: yearlyOverview.doanhThuTrucTuyen || 0,
                donTaiQuay: yearlyOverview.donTaiQuay || 0,
                donTrucTuyen: yearlyOverview.donTrucTuyen || 0,
                sanPhamSapHet: yearlyOverview.sanPhamSapHet || 0,
                doanhThuChoXacNhan: yearlyOverview.doanhThuChoXacNhan || 0,
                doanhThuDangGiao: yearlyOverview.doanhThuDangGiao || 0,
                doanhThuDaHuy: yearlyOverview.doanhThuDaHuy || 0
            };
        }

        const months = Array.from({ length: 12 }, (_, i) => ({
            month: `Tháng ${i + 1}`,
            revenue: 0,
            customers: 0
        }));

        if (dailyData && Array.isArray(dailyData)) {
            dailyData.forEach((item) => {
                if (item.ngay) {
                    const parts = item.ngay.split('-');
                    const m = parseInt(parts[1], 10);
                    if (m >= 1 && m <= 12) {
                        months[m - 1].revenue += Number(item.doanhThu || 0);
                        months[m - 1].customers += Number(item.soDon || 0);
                    }
                }
            });
        }
        monthlyRevenue.value = months;

        // Cập nhật biểu đồ Area
        const maxMonthlyCustomers = Math.max(...months.map((m) => m.customers));
        if (areaChartOptions.value.yaxis && areaChartOptions.value.yaxis[1]) {
            areaChartOptions.value.yaxis[1].max = maxMonthlyCustomers > 100 ? undefined : 100;
        }

        areaChartSeries.value = [
            {
                name: 'Doanh thu',
                type: 'line',
                data: months.map((m) => m.revenue)
            },
            {
                name: 'Khách hàng',
                type: 'line',
                data: months.map((m) => m.customers)
            }
        ];

        // Lấy doanh thu của startDate và endDate để làm biểu đồ theo giờ
        let startRevenue = 0;
        let endRevenue = 0;
        if (dailyData && Array.isArray(dailyData)) {
            const startItem = dailyData.find((item) => item.ngay === startDate.value);
            if (startItem) {
                startRevenue = Number(startItem.doanhThu || 0);
            }
            const endItem = dailyData.find((item) => item.ngay === endDate.value);
            if (endItem) {
                endRevenue = Number(endItem.doanhThu || 0);
            }
        }

        const startHourlyData = await generateHourlyDataFromInvoices(startRevenue, startDate.value);
        const endHourlyData = await generateHourlyDataFromInvoices(endRevenue, endDate.value);

        const startSum = startHourlyData.revenue.reduce((a, b) => a + b, 0);
        const endSum = endHourlyData.revenue.reduce((a, b) => a + b, 0);

        startDailyTotalRevenue.value = startSum > 0 ? startSum : startRevenue;
        endDailyTotalRevenue.value = endSum > 0 ? endSum : endRevenue;

        const start3HourData = aggregateTo3HourSlots(startHourlyData);
        const end3HourData = aggregateTo3HourSlots(endHourlyData);

        startHourlyMax.value = Math.max(...start3HourData.revenue);
        endHourlyMax.value = Math.max(...end3HourData.revenue);
        startHourlyCustomerMax.value = Math.max(...start3HourData.customers);
        endHourlyCustomerMax.value = Math.max(...end3HourData.customers);

        startHourlyChartSeries.value = [
            {
                name: 'Doanh thu',
                type: 'line',
                data: start3HourData.revenue
            },
            {
                name: 'Khách hàng',
                type: 'line',
                data: start3HourData.customers
            }
        ];

        endHourlyChartSeries.value = [
            {
                name: 'Doanh thu',
                type: 'line',
                data: end3HourData.revenue
            },
            {
                name: 'Khách hàng',
                type: 'line',
                data: end3HourData.customers
            }
        ];

        // Tự động làm mới key để ApexCharts vẽ lại ngay tức thì không cần reload
        startHourlyChartKey.value += 1;
        endHourlyChartKey.value += 1;
        areaChartKey.value += 1;
        statusBarChartKey.value += 1;
        donutChartKey.value += 1;
    } catch (error) {
        console.error('Error loading statistics:', error);
    } finally {
        loading.value = false;
    }
};

const refreshAllData = async (showToast = true) => {
    loading.value = true;
    try {
        await Promise.all([loadStatistics(), loadBrands()]);
        if (showToast) {
            toastStore.showToast('Cập nhật dữ liệu thống kê thành công!', 'success');
        }
    } catch (e) {
        console.error('Error refreshing all statistics:', e);
        if (showToast) {
            toastStore.showToast('Có lỗi xảy ra khi cập nhật dữ liệu!', 'error');
        }
    } finally {
        loading.value = false;
    }
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
    {
        label: 'Chờ xác nhận',
        amount: yearlyRevenueStats.value.doanhThuChoXacNhan,
        count: yearlyRevenueStats.value.donHangChoXacNhan,
        active: true
    },
    { label: 'Đang giao hàng', amount: yearlyRevenueStats.value.doanhThuDangGiao, count: yearlyRevenueStats.value.donHangDangGiao },
    { label: 'Đã hoàn thành', amount: yearlyRevenueStats.value.totalRevenue, count: yearlyRevenueStats.value.donHangHoanThanh },
    { label: 'Đã hủy bỏ', amount: yearlyRevenueStats.value.doanhThuDaHuy, count: yearlyRevenueStats.value.donHangDaHuy }
]);

const statusBarSeries = computed(() => [
    {
        name: 'Số đơn',
        data: [
            Number(yearlyRevenueStats.value.donHangChoXacNhan || 0),
            Number(yearlyRevenueStats.value.donHangDangGiao || 0),
            Number(yearlyRevenueStats.value.donHangHoanThanh || 0),
            Number(yearlyRevenueStats.value.donHangDaHuy || 0)
        ]
    }
]);

const startHourlyChartKey = ref(0);
const endHourlyChartKey = ref(0);
const areaChartKey = ref(0);
const statusBarChartKey = ref(0);
const donutChartKey = ref(0);
const donutChartSeries = ref([]);

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

let searchDebounceTimer = null;
const onSearchInput = () => {
    if (searchDebounceTimer) clearTimeout(searchDebounceTimer);
    searchDebounceTimer = setTimeout(() => {
        productPage.value = 1;
        fetchProductStats();
    }, 350);
};

const onSearchClear = () => {
    if (searchDebounceTimer) clearTimeout(searchDebounceTimer);
    productSearchKeyword.value = '';
    productPage.value = 1;
    fetchProductStats();
};

const refreshProductFilters = () => {
    productPage.value = 1;
    fetchProductStats();
};

const resetProductFilters = () => {
    if (searchDebounceTimer) clearTimeout(searchDebounceTimer);
    productSearchKeyword.value = '';
    productBrandId.value = null;
    productSortBy.value = 'bestSelling';
    productPage.value = 1;
    fetchProductStats();
};

const changeProductPageSize = (newSize) => {
    if (typeof newSize === 'number' && !isNaN(newSize)) {
        productPageSize.value = newSize;
    }
    productPage.value = 1;
    fetchProductStats();
};

const goToProductPage = (page) => {
    if (typeof page === 'number' && !isNaN(page)) {
        productPage.value = Math.min(Math.max(page, 1), productTotalPages.value);
    }
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
        title: 'Đơn hàng thành công',
        valueKey: 'donHangHoanThanh',
        icon: 'mdi-check-circle-outline',
        color: 'success',
        tone: 'green',
        formatter: formatNumber
    },
    {
        title: 'Đơn hàng hủy',
        valueKey: 'donHangDaHuy',
        icon: 'mdi-close-circle-outline',
        color: 'error',
        tone: 'red',
        formatter: formatNumber
    },
    {
        title: 'Tổng sản phẩm bán ra',
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

const exporting = ref(false);

const exportExcelStatistics = async () => {
    try {
        exporting.value = true;
        const blob = await dichVuThongKe.xuatBaoCaoExcel(startDate.value, endDate.value);
        const url = window.URL.createObjectURL(
            new Blob([blob], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
        );
        const link = document.createElement('a');
        link.href = url;
        const startStr = startDate.value || 'toan_bo';
        const endStr = endDate.value || 'hien_tai';
        link.setAttribute('download', `bao_cao_thong_ke_AeroStride_${startStr}_${endStr}.xlsx`);
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);

        import('@/stores/toastStore').then(({ useToastStore }) => {
            const toastStore = useToastStore();
            toastStore.showToast('Xuất báo cáo thống kê Excel thành công!', 'success');
        });
    } catch (error) {
        console.error('Lỗi khi xuất báo cáo thống kê Excel:', error);
        import('@/stores/toastStore').then(({ useToastStore }) => {
            const toastStore = useToastStore();
            toastStore.showToast('Có lỗi xảy ra khi xuất file Excel báo cáo thống kê!', 'error');
        });
    } finally {
        exporting.value = false;
    }
};

onMounted(async () => {
    await Promise.all([loadStatistics(), loadBrands()]);
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
                    <div class="stats-filter-field stats-filter-field-date">
                        <div class="filter-field-label">Từ ngày</div>
                        <AppDatePicker
                            :model-value="startDate"
                            @update:model-value="onStartDateChange"
                            :max-date="endDate"
                            placeholder="dd/mm/yyyy"
                        />
                    </div>
                    <div class="stats-filter-field stats-filter-field-date">
                        <div class="filter-field-label">Đến ngày</div>
                        <AppDatePicker
                            :model-value="endDate"
                            @update:model-value="onEndDateChange"
                            :min-date="startDate"
                            placeholder="dd/mm/yyyy"
                        />
                    </div>
                    <v-btn
                        color="primary"
                        variant="flat"
                        class="stats-refresh-btn px-6"
                        height="40"
                        :loading="loading"
                        @click="() => refreshAllData(true)"
                    >
                        <v-icon start size="18">mdi-refresh</v-icon>
                        Cập nhật dữ liệu
                    </v-btn>
                    <v-btn
                        color="success"
                        variant="flat"
                        class="stats-export-btn px-5 ml-2 font-weight-medium"
                        height="40"
                        style="background-color: #107c41 !important; color: #ffffff !important;"
                        :loading="exporting"
                        @click="exportExcelStatistics"
                    >
                        <v-icon start size="18" color="white" style="color: #ffffff !important;">mdi-microsoft-excel</v-icon>
                        <span style="color: #ffffff !important; font-weight: 600;">Xuất báo cáo Excel</span>
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

            <!-- Side-by-side Hourly Revenue Comparison Charts -->
            <div class="split-grid mb-4">
                <!-- Start Date Chart -->
                <section class="stats-panel trend-panel">
                    <div class="chart-card-heading d-flex align-center justify-space-between py-2" style="min-height: 64px">
                        <div>
                            <h2>Doanh thu ngày {{ formatDateVN(startDate) }}</h2>
                            <div class="mt-1 font-weight-bold" style="color: #d97706; font-size: 14px; line-height: 1.2">
                                {{ formatCurrency(startDailyTotalRevenue) }}
                            </div>
                        </div>
                        <v-icon color="#0ea5e9" size="18">mdi-chart-timeline-variant</v-icon>
                    </div>
                    <div class="tab-panel">
                        <div v-if="loading" class="panel-loader panel-loader-tall">
                            <v-progress-circular indeterminate color="primary" />
                        </div>
                        <apexchart v-else :key="startHourlyChartKey" type="line" height="280" :options="startHourlyChartOptions" :series="startHourlyChartSeries" />
                    </div>
                </section>

                <!-- End Date Chart -->
                <section class="stats-panel trend-panel">
                    <div class="chart-card-heading d-flex align-center justify-space-between py-2" style="min-height: 64px">
                        <div>
                            <h2>Doanh thu ngày {{ formatDateVN(endDate) }}</h2>
                            <div class="mt-1 d-flex align-center" style="line-height: 1.2">
                                <span class="font-weight-bold" style="color: #d97706; font-size: 14px">
                                    {{ formatCurrency(endDailyTotalRevenue) }}
                                </span>
                                <span
                                    class="text-caption font-weight-bold ml-2 d-inline-flex align-center"
                                    :style="{ color: hourlyPercentageInfo.color, fontSize: '12px' }"
                                >
                                    (<span v-if="hourlyPercentageInfo.icon" class="mr-1">{{ hourlyPercentageInfo.icon }}</span
                                    >{{ hourlyPercentageInfo.text }})
                                </span>
                            </div>
                        </div>
                        <v-icon color="#ef4444" size="18">mdi-chart-timeline-variant</v-icon>
                    </div>
                    <div class="tab-panel">
                        <div v-if="loading" class="panel-loader panel-loader-tall">
                            <v-progress-circular indeterminate color="primary" />
                        </div>
                        <apexchart v-else :key="endHourlyChartKey" type="line" height="280" :options="endHourlyChartOptions" :series="endHourlyChartSeries" />
                    </div>
                </section>
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
                                    <strong>{{ formatCurrency(yearlyRevenueStats[item.revenueKey]) }}</strong>
                                    <small>{{ formatNumber(yearlyRevenueStats[item.orderKey]) }} đơn</small>
                                </div>
                            </article>
                        </div>
                        <div v-if="loading" class="panel-loader panel-loader-tall">
                            <v-progress-circular indeterminate color="primary" />
                        </div>
                        <apexchart v-else :key="areaChartKey" type="line" height="310" :options="areaChartOptions" :series="areaChartSeries" />
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
                                <article
                                    v-for="item in statusChartItems"
                                    :key="item.label"
                                    class="status-summary-item"
                                    :class="{ active: item.active }"
                                >
                                    <span>{{ item.label }}</span>
                                    <strong>{{ formatCurrency(item.amount) }}</strong>
                                    <small>{{ formatNumber(item.count) }} đơn</small>
                                </article>
                            </div>
                            <apexchart
                                :key="statusBarChartKey"
                                class="status-bar-chart"
                                type="bar"
                                height="320"
                                :options="statusBarOptions"
                                :series="statusBarSeries"
                            />
                        </div>
                    </div>
                </section>
            </div>

            <!-- Donut Chart & Revenue Cycles Row -->
            <div class="split-grid mb-4">
                <!-- Brand Shares Donut Chart -->
                <section class="stats-panel category-share-panel">
                    <div class="simple-card-heading">
                        <h2>Tỷ trọng theo thương hiệu giày</h2>
                        <v-icon color="#0f172a" size="22">mdi-chart-donut</v-icon>
                    </div>
                    <div v-if="loading" class="panel-loader">
                        <v-progress-circular indeterminate color="primary" />
                    </div>
                    <div v-else-if="!hasValidDonutData" class="d-flex flex-column align-center justify-center py-10 px-4 w-100 animate-fade-in text-center">
                        <div
                            class="empty-state-icon-box d-flex align-center justify-center rounded-circle mb-3 mx-auto"
                            style="width: 56px; height: 56px; background: rgba(241, 245, 249, 0.8); border: 1.5px dashed #cbd5e1"
                        >
                            <v-icon icon="mdi-chart-donut" size="26" style="color: #94a3b8 !important" />
                        </div>
                        <span class="text-slate-600 font-weight-medium" style="font-size: 13.5px">Không có dữ liệu trong thời gian này</span>
                    </div>
                    <div v-else class="category-chart-body">
                        <div class="category-donut-wrap">
                            <apexchart
                                :key="donutChartKey"
                                type="donut"
                                height="330"
                                :options="donutChartOptions"
                                :series="donutChartSeries"
                            />
                            <div class="category-donut-center">
                                <span>Tổng doanh thu</span>
                                <strong>{{ formatCurrency(donutTotalRevenue) }}</strong>
                            </div>
                        </div>
                    </div>
                </section>

                <!-- Revenue Cycles Table -->
                <section class="stats-panel monthly-detail-panel cycle-stats-panel">
                    <div class="simple-card-heading">
                        <h2>Chu kỳ doanh thu</h2>
                        <v-icon color="#0f172a" size="22">mdi-calendar-clock</v-icon>
                    </div>
                    <div v-if="loading" class="panel-loader">
                        <v-progress-circular indeterminate color="primary" />
                    </div>
                    <div v-else class="cust-stats-table-container">
                        <div v-if="revenueCycles.length === 0" class="d-flex flex-column align-center justify-center py-8 px-4 w-100 animate-fade-in text-center">
                            <div
                                class="empty-state-icon-box d-flex align-center justify-center rounded-circle mb-3 mx-auto"
                                style="width: 56px; height: 56px; background: rgba(241, 245, 249, 0.8); border: 1.5px dashed #cbd5e1"
                            >
                                <v-icon icon="mdi-calendar-remove-outline" size="26" style="color: #94a3b8 !important" />
                            </div>
                            <span class="text-slate-600 font-weight-medium" style="font-size: 13.5px">Không có dữ liệu chu kỳ doanh thu</span>
                        </div>
                        <table v-else class="cust-stats-table">
                            <thead>
                                <tr>
                                    <th class="text-left" style="min-width: 120px">Chu kỳ</th>
                                    <th class="text-right">Doanh thu</th>
                                    <th class="text-right">Số đơn</th>
                                    <th class="text-right">Trung bình đơn</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="(item, index) in revenueCycles" :key="index">
                                    <td class="text-left font-weight-medium" style="color: #1e293b">
                                        {{ item.tenChuKy }}
                                    </td>
                                    <td class="text-right font-weight-semibold" style="color: #1e293b">
                                        {{ formatCurrency(item.doanhThu) }}
                                    </td>
                                    <td class="text-right" style="color: #1e293b">{{ formatNumber(item.soDon) }} đơn</td>
                                    <td class="text-right font-weight-semibold" style="color: #d97706">
                                        {{ formatCurrency(item.trungBinhDon) }}
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </section>
            </div>

            <!-- Tables Row (Customers & Employees) -->
            <div class="split-grid">
                <!-- Customer Stats Table -->
                <section class="stats-panel monthly-detail-panel">
                    <div class="simple-card-heading">
                        <h2>Tổng chi của khách hàng</h2>
                        <v-icon color="#0f172a" size="22">mdi-account-group-outline</v-icon>
                    </div>
                    <div v-if="loading" class="panel-loader">
                        <v-progress-circular indeterminate color="primary" />
                    </div>
                    <div v-else class="cust-stats-table-container">
                        <div v-if="customerPurchaseStats.length === 0" class="d-flex flex-column align-center justify-center py-8 px-4 w-100 animate-fade-in text-center">
                            <div
                                class="empty-state-icon-box d-flex align-center justify-center rounded-circle mb-3 mx-auto"
                                style="width: 56px; height: 56px; background: rgba(241, 245, 249, 0.8); border: 1.5px dashed #cbd5e1"
                            >
                                <v-icon icon="mdi-account-off-outline" size="26" style="color: #94a3b8 !important" />
                            </div>
                            <span class="text-slate-600 font-weight-medium" style="font-size: 13.5px">Không có dữ liệu của khách hàng nào</span>
                        </div>
                        <table v-else class="cust-stats-table">
                            <thead>
                                <tr>
                                    <th class="text-left" style="min-width: 140px">Tên khách hàng</th>
                                    <th class="text-right">Tổng chi</th>
                                    <th class="text-right">Tổng sản phẩm</th>
                                    <th class="text-right">Đơn thành công</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="(item, index) in customerPurchaseStats" :key="index">
                                    <td class="text-left font-weight-medium" style="color: #1e293b">
                                        {{ item.tenKhachHang }}
                                    </td>
                                    <td class="text-right">
                                        <div class="val-top font-weight-semibold" style="color: #1e293b">
                                            {{ formatCurrency(item.tongChi) }}
                                        </div>
                                        <div class="val-sub text-emerald-600">
                                            {{ getPercent(item.tongChi, customerStatsTotals.tongChi) }}
                                        </div>
                                    </td>
                                    <td class="text-right">
                                        <div class="val-top" style="color: #1e293b">{{ formatNumber(item.tongSanPham) }}</div>
                                        <div class="val-sub text-emerald-600">
                                            {{ getPercent(item.tongSanPham, customerStatsTotals.tongSanPham) }}
                                        </div>
                                    </td>
                                    <td class="text-right">
                                        <div class="val-top font-weight-semibold" style="color: #1e293b">
                                            {{ formatNumber(item.donThanhCong) }}
                                        </div>
                                        <div class="val-sub text-emerald-600">
                                            {{ getPercent(item.donThanhCong, customerStatsTotals.donThanhCong) }}
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </section>

                <!-- Employee Stats Table -->
                <section class="stats-panel monthly-detail-panel">
                    <div class="simple-card-heading">
                        <h2>Doanh thu của nhân viên trong tháng</h2>
                        <v-icon color="#0f172a" size="22">mdi-account-tie-outline</v-icon>
                    </div>
                    <div v-if="loading" class="panel-loader">
                        <v-progress-circular indeterminate color="primary" />
                    </div>
                    <div v-else class="cust-stats-table-container">
                        <div v-if="employeePurchaseStats.length === 0" class="d-flex flex-column align-center justify-center py-8 px-4 w-100 animate-fade-in text-center">
                            <div
                                class="empty-state-icon-box d-flex align-center justify-center rounded-circle mb-3 mx-auto"
                                style="width: 56px; height: 56px; background: rgba(241, 245, 249, 0.8); border: 1.5px dashed #cbd5e1"
                            >
                                <v-icon icon="mdi-account-tie-voice-off-outline" size="26" style="color: #94a3b8 !important" />
                            </div>
                            <span class="text-slate-600 font-weight-medium" style="font-size: 13.5px">Không có dữ liệu của nhân viên nào</span>
                        </div>
                        <table v-else class="cust-stats-table">
                            <thead>
                                <tr>
                                    <th class="text-left" style="min-width: 90px">Mã NV</th>
                                    <th class="text-left" style="min-width: 140px">Tên nhân viên</th>
                                    <th class="text-right">Tổng doanh thu</th>
                                    <th class="text-right">Tổng sản phẩm</th>
                                    <th class="text-right">Tổng đơn hàng</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="(item, index) in employeePurchaseStats" :key="index">
                                    <td class="text-left font-weight-medium" style="color: #64748b">
                                        {{ item.maNhanVien }}
                                    </td>
                                    <td class="text-left font-weight-medium" style="color: #1e293b">
                                        {{ item.tenNhanVien }}
                                    </td>
                                    <td class="text-right">
                                        <div class="val-top font-weight-semibold" style="color: #1e293b">
                                            {{ formatCurrency(item.tongChi) }}
                                        </div>
                                        <div class="val-sub text-emerald-600">
                                            {{ getPercent(item.tongChi, employeeStatsTotals.tongChi) }}
                                        </div>
                                    </td>
                                    <td class="text-right">
                                        <div class="val-top" style="color: #1e293b">
                                            {{ formatNumber(item.tongSanPham) }}
                                        </div>
                                        <div class="val-sub text-emerald-600">
                                            {{ getPercent(item.tongSanPham, employeeStatsTotals.tongSanPham) }}
                                        </div>
                                    </td>
                                    <td class="text-right">
                                        <div class="val-top font-weight-semibold" style="color: #1e293b">
                                            {{ formatNumber(item.tongDonHang) }}
                                        </div>
                                        <div class="val-sub text-emerald-600">
                                            {{ getPercent(item.tongDonHang, employeeStatsTotals.tongDonHang) }}
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </div>
                </section>
            </div>

            <section class="stats-panel product-stats-panel">
                <div class="simple-card-heading">
                    <div class="product-stats-title">
                        <v-icon color="#e11d48" size="17">mdi-cube-outline</v-icon>
                        <h2>Thống kê sản phẩm</h2>
                        <span v-if="isFallbackYearData" class="text-caption text-amber-800 bg-amber-50 border border-amber-200 rounded-pill px-2 py-0.5 ml-2 font-weight-medium">
                            (Dữ liệu năm {{ selectedYear }})
                        </span>
                    </div>
                    <span class="product-count-chip">{{ formatNumber(productTotalElements) }} sản phẩm</span>
                </div>
                <div v-if="loading && productStats.length === 0" class="panel-loader">
                    <v-progress-circular indeterminate color="primary" />
                </div>
                <div v-else class="product-table-section">
                    <div class="product-filter-bar d-flex flex-wrap align-center justify-space-between ga-3 mb-4 pa-2 bg-slate-50 rounded-lg border">
                        <div class="d-flex align-center ga-3 flex-grow-1" style="max-width: 680px">
                            <v-text-field
                                v-model="productSearchKeyword"
                                placeholder="Tìm theo mã SP, SKU hoặc tên sản phẩm..."
                                density="compact"
                                variant="outlined"
                                hide-details
                                clearable
                                prepend-inner-icon="mdi-magnify"
                                bg-color="white"
                                class="compact-input flex-grow-1"
                                @input="onSearchInput"
                                @click:clear="onSearchClear"
                            ></v-text-field>

                            <v-select
                                v-model="productBrandId"
                                :items="[
                                    { title: 'Tất cả thương hiệu', value: null },
                                    ...brandOptions
                                ]"
                                density="compact"
                                variant="outlined"
                                hide-details
                                bg-color="white"
                                placeholder="Thương hiệu"
                                class="compact-input"
                                style="min-width: 200px; max-width: 240px"
                                @update:model-value="() => { productPage = 1; fetchProductStats(); }"
                            ></v-select>
                        </div>
                        <div class="d-flex align-center ga-2 flex-wrap">
                            <v-btn
                                variant="outlined"
                                color="slate-600"
                                class="rounded-lg"
                                style="height: 38px; min-width: 38px; width: 38px; padding: 0"
                                @click="resetProductFilters"
                                title="Làm mới bộ lọc"
                            >
                                <v-icon size="18">mdi-refresh</v-icon>
                                <v-tooltip activator="parent" location="top">Làm mới bộ lọc</v-tooltip>
                            </v-btn>
                        </div>
                    </div>
                    <AdminTable
                        hide-toolbar
                        :loading="productLoading"
                        :headers="[
                            { text: 'STT', align: 'center', width: '60px' },
                            { text: 'Mã SP', align: 'center', width: '120px' },
                            { text: 'Mã SKU', align: 'center', width: '140px' },
                            { text: 'Tên sản phẩm', align: 'start' },
                            { text: 'Thương hiệu', align: 'center', width: '130px' },
                            { text: 'Màu sắc', align: 'center', width: '120px' },
                            { text: 'Kích thước', align: 'center', width: '100px' },
                            { text: 'Đã bán', align: 'center', width: '130px' },
                            { text: 'Doanh thu', align: 'center', width: '150px' }
                        ]"
                        :items="productStats"
                    >
                        <template #headers>
                            <tr>
                                <th class="header-cell text-center" style="width: 50px">STT</th>
                                <th class="header-cell text-center" style="width: 110px">Mã SP</th>
                                <th class="header-cell text-center" style="width: 190px; min-width: 170px">Mã SKU</th>
                                <th class="header-cell text-left" style="width: 220px; max-width: 240px">Tên sản phẩm</th>
                                <th class="header-cell text-center" style="width: 120px">Thương hiệu</th>
                                <th class="header-cell text-center" style="width: 110px">Màu sắc</th>
                                <th class="header-cell text-center" style="width: 90px">Kích thước</th>
                                <th class="header-cell text-center cursor-pointer select-none" style="width: 120px" @click="toggleSoldSort">
                                    <div class="d-inline-flex align-center justify-center ga-1 font-weight-bold">
                                        <span>Đã bán</span>
                                        <v-icon size="16" :color="(!productSortBy || productSortBy === 'bestSelling' || productSortBy === 'slowSelling') ? 'primary' : 'slate-400'">
                                            {{ productSortBy === 'slowSelling' ? 'mdi-sort-numeric-ascending' : (productSortBy === 'bestSelling' ? 'mdi-sort-numeric-descending' : 'mdi-swap-vertical') }}
                                        </v-icon>
                                    </div>
                                    <v-tooltip activator="parent" location="top">
                                        {{ productSortBy === 'bestSelling' ? 'Sắp xếp: Đã bán cao nhất (Bấm để đổi sang thấp nhất)' : (productSortBy === 'slowSelling' ? 'Sắp xếp: Đã bán thấp nhất (Bấm để đổi sang cao nhất)' : 'Bấm để sắp xếp theo Đã bán') }}
                                    </v-tooltip>
                                </th>
                                <th class="header-cell text-center cursor-pointer select-none" style="width: 140px" @click="toggleRevenueSort">
                                    <div class="d-inline-flex align-center justify-center ga-1 font-weight-bold">
                                        <span>Doanh thu</span>
                                        <v-icon size="16" :color="productSortBy && productSortBy.startsWith('revenue') ? 'primary' : 'slate-400'">
                                            {{ productSortBy === 'revenueAsc' ? 'mdi-sort-numeric-ascending' : (productSortBy === 'revenueDesc' ? 'mdi-sort-numeric-descending' : 'mdi-swap-vertical') }}
                                        </v-icon>
                                    </div>
                                    <v-tooltip activator="parent" location="top">
                                        {{ productSortBy === 'revenueDesc' ? 'Sắp xếp: Doanh thu cao nhất (Bấm để đổi sang thấp nhất)' : (productSortBy === 'revenueAsc' ? 'Sắp xếp: Doanh thu thấp nhất (Bấm để đổi sang cao nhất)' : 'Bấm để sắp xếp theo Doanh thu') }}
                                    </v-tooltip>
                                </th>
                            </tr>
                        </template>

                        <template #row="{ item, index }">
                            <tr class="data-row" :key="`${item.maSku || item.maSanPham}-${index}`">
                                <td class="data-cell text-center font-weight-medium text-slate-500">{{ (productPage - 1) * productPageSize + index + 1 }}</td>
                                <td class="data-cell text-center font-mono font-weight-bold text-primary">{{ item.maSanPham || '--' }}</td>
                                <td class="data-cell text-center font-mono font-weight-semibold text-slate-700" style="white-space: nowrap; min-width: 170px">{{ item.maSku || '--' }}</td>
                                <td class="data-cell text-left" style="max-width: 240px">
                                    <div class="product-name-cell" style="justify-content: flex-start; text-align: left; max-width: 230px">
                                        <span class="font-weight-medium text-truncate d-block" style="color: #1e293b; max-width: 220px" :title="item.name">{{ item.name }}</span>
                                        <small style="color: #64748b; font-size: 11px">{{ isFallbackYearData ? 'Dữ liệu năm ' + selectedYear : 'Thời gian chọn' }}</small>
                                    </div>
                                </td>
                                <td class="data-cell text-center">
                                    <span
                                        class="brand-pill"
                                        style="
                                            display: inline-flex;
                                            padding: 3px 8px;
                                            background: #f8fafc;
                                            border: 1px solid #e2e8f0;
                                            border-radius: 6px;
                                            font-size: 12px;
                                            font-weight: 600;
                                            color: #475569;
                                        "
                                    >
                                        {{ item.thuongHieu || '--' }}
                                    </span>
                                </td>
                                <td class="data-cell text-center">
                                    <v-chip size="small" variant="tonal" color="slate-700" class="font-weight-medium">
                                        {{ item.mauSac || '--' }}
                                    </v-chip>
                                </td>
                                <td class="data-cell text-center">
                                    <v-chip size="small" variant="outlined" color="primary" class="font-weight-bold">
                                        {{ item.kichThuoc || '--' }}
                                    </v-chip>
                                </td>
                                <td class="data-cell text-center font-weight-bold" style="color: #1e293b">{{ formatNumber(item.quantity) }}</td>
                                <td class="data-cell text-center font-weight-bold" style="color: #e11d48">{{ formatCurrency(item.revenue) }}</td>
                            </tr>
                        </template>

                        <template #pagination>
                            <AdminPagination
                                v-model="productPage"
                                v-model:page-size="productPageSize"
                                :total-elements="productTotalElements"
                                :total-pages="productTotalPages"
                                :current-size="productStats.length"
                                @change="goToProductPage"
                                @update:page-size="changeProductPageSize"
                            />
                        </template>
                    </AdminTable>
                </div>
            </section>
        </section>
    </div>
</template>

<style scoped>
.stats-export-btn,
.stats-export-btn :deep(.v-btn__content),
.stats-export-btn :deep(.v-icon) {
    color: #ffffff !important;
}
</style>

