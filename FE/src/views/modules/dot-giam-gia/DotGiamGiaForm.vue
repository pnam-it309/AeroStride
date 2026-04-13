<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { dichVuDotGiamGia } from '@/services/admin/dichVuDotGiamGia';
import { useNotifications } from '@/services/notificationService';
import AdminConfirm from '@/components/common/AdminConfirm.vue';
import { CalendarIcon, GiftIcon, InfoCircleIcon, TagIcon, BoxIcon } from 'vue-tabler-icons';

const route = useRoute();
const router = useRouter();
const { addNotification } = useNotifications();

const loading = ref(false);
const saving = ref(false);
const products = ref([]);
const selectedVariants = ref([]);
const isEditMode = computed(() => !!route.params.id && !route.path.includes('/detail'));
const isDetailView = computed(() => route.path.includes('/detail'));

const form = ref({
    ma: '',
    ten: '',
    moTa: '',
    loaiGiamGia: 'PHAN_TRAM',
    soTienGiam: 0,
    dieuKienGiamGia: 0,
    mucUuTien: 0,
    ngayBatDau: '',
    ngayKetThuc: '',
    trangThai: 'DANG_HOAT_DONG'
});

const init = async () => {
    // Load products to choose
    try {
        const data = await dichVuDotGiamGia.layDanhSachSanPhamApDung();
        products.value = data || [];
    } catch (e) {
        console.error('Error loading products:', e);
    }

    if (isEditMode.value || isDetailView.value) {
        loading.value = true;
        try {
            const data = await dichVuDotGiamGia.layChiTietDotGiamGia(route.params.id);
            form.value = {
                ...data,
                ngayBatDau: data.ngayBatDau ? new Date(data.ngayBatDau).toISOString().slice(0, 16) : '',
                ngayKetThuc: data.ngayKetThuc ? new Date(data.ngayKetThuc).toISOString().slice(0, 16) : ''
            };

            // Load applied variants
            const applied = await dichVuDotGiamGia.layDanhSachBienTheApDung(route.params.id);
            selectedVariants.value = applied.map((v) => v.id);
        } catch (e) {
            addNotification({ title: 'Lỗi', subtitle: 'Không thể tải thông tin', color: 'error' });
        } finally {
            loading.value = false;
        }
    }
};

const confirmDialog = ref({ show: false, title: '', message: '', color: 'primary', action: null, loading: false });

const handleSave = () => {
    if (!form.value.ten || !form.value.soTienGiam || !form.value.ngayBatDau || !form.value.ngayKetThuc) {
        addNotification({ title: 'Thiếu thông tin', subtitle: 'Vui lòng điền các trường bắt buộc', color: 'warning' });
        return;
    }
    confirmDialog.value = {
        show: true,
        title: isEditMode.value ? 'Xác nhận cập nhật' : 'Xác nhận thêm mới',
        message: isEditMode.value
            ? `Bạn có chắc muốn cập nhật chiến dịch [${form.value.ten}]?`
            : `Bạn có chắc muốn tạo chiến dịch mới [${form.value.ten}]?`,
        color: 'primary',
        action: async () => {
            confirmDialog.value.loading = true;
            saving.value = true;
            try {
                const payload = {
                    ...form.value,
                    ngayBatDau: new Date(form.value.ngayBatDau).getTime(),
                    ngayKetThuc: new Date(form.value.ngayKetThuc).getTime(),
                    listIdChiTietSanPham: selectedVariants.value
                };
                if (isEditMode.value) {
                    await dichVuDotGiamGia.capNhatDotGiamGia(route.params.id, payload);
                    addNotification({ title: 'Thành công', subtitle: 'Cập nhật chiến dịch hoàn tất', color: 'success' });
                } else {
                    await dichVuDotGiamGia.taoDotGiamGia(payload);
                    addNotification({ title: 'Thành công', subtitle: 'Đã tạo chiến dịch mới', color: 'success' });
                }
                confirmDialog.value.show = false;
                router.push('/dot-giam-gia');
            } catch (e) {
                addNotification({ title: 'Lỗi', subtitle: 'Lỗi khi lưu dữ liệu', color: 'error' });
            } finally {
                confirmDialog.value.loading = false;
                saving.value = false;
            }
        }
    };
};

onMounted(init);
</script>

<template>
    <v-container fluid class="pa-6 bg-grey-lighten-4 min-h-screen">
        <!-- Header -->
        <div class="d-flex align-center mb-6">
            <v-btn icon variant="tonal" color="secondary" class="mr-4 rounded-lg" @click="router.push('/dot-giam-gia')">
                <v-icon>mdi-arrow-left</v-icon>
            </v-btn>
            <div>
                <h1 class="text-h4 font-weight-bold">
                    {{ isDetailView ? 'Chi tiết đợt ưu đãi' : isEditMode ? 'Cập nhật chiến dịch' : 'Mở đợt giảm giá mới' }}
                </h1>
                <p class="text-subtitle-1 text-medium-emphasis">
                    {{
                        isDetailView
                            ? 'Theo dõi hiệu năng và danh sách sản phẩm đang được áp dụng'
                            : 'Thiết lập chương trình ưu đãi và giảm giá theo sự kiện'
                    }}
                </p>
            </div>
            <v-spacer></v-spacer>
            <div class="d-flex gap-2">
                <v-btn
                    v-if="!isDetailView"
                    color="#2E4E8E"
                    prepend-icon="mdi-content-save"
                    class="text-none font-weight-bold px-8 rounded-lg"
                    height="44"
                    :loading="saving"
                    @click="handleSave"
                >
                    Lưu thông tin
                </v-btn>
                <v-btn
                    v-if="isDetailView"
                    color="#2E4E8E"
                    variant="elevated"
                    prepend-icon="mdi-pencil"
                    class="text-none font-weight-bold px-10 rounded-lg"
                    height="44"
                    @click="router.push(`/dot-giam-gia/form/${route.params.id}`)"
                >
                    Chỉnh sửa ngay
                </v-btn>
            </div>
        </div>

        <v-row v-if="loading">
            <v-col cols="12" class="text-center py-16">
                <v-progress-circular indeterminate color="primary" size="64" />
                <div class="mt-4 text-h6 text-medium-emphasis">Đang tải dữ liệu chiến dịch...</div>
            </v-col>
        </v-row>

        <!-- DASHBOARD VIEW (DETAIL) -->
        <v-row v-else-if="isDetailView">
            <v-col cols="12" lg="4">
                <v-card class="rounded-xl border shadow-none mb-6 text-center pa-6">
                    <div class="d-flex justify-center mb-4">
                        <v-avatar color="primary-lighten-5" size="80" class="rounded-xl">
                            <GiftIcon size="40" class="text-primary" />
                        </v-avatar>
                    </div>
                    <h2 class="text-h5 font-weight-black mb-1 text-dark">{{ form.ten }}</h2>
                    <code class="text-caption font-weight-bold px-2 py-1 bg-grey-lighten-4 rounded mb-4 d-inline-block">{{ form.ma }}</code>

                    <div class="mt-2 mb-4">
                        <v-chip
                            :color="form.trangThai === 'DANG_HOAT_DONG' ? 'success' : 'info'"
                            variant="flat"
                            class="font-weight-bold px-6"
                        >
                            {{ form.trangThai === 'DANG_HOAT_DONG' ? 'Đang hoạt động' : 'Kết thúc / Tạm dừng' }}
                        </v-chip>
                    </div>

                    <v-divider class="my-6"></v-divider>

                    <div class="d-flex justify-space-around">
                        <div>
                            <div class="text-h5 font-weight-black text-primary">
                                {{ form.loaiGiamGia === 'PHAN_TRAM' ? `${form.soTienGiam}%` : `${form.soTienGiam / 1000}k` }}
                            </div>
                            <div class="text-caption text-uppercase font-weight-bold text-medium-emphasis">Mức giảm</div>
                        </div>
                        <v-divider vertical inset></v-divider>
                        <div>
                            <div class="text-h5 font-weight-black text-info">{{ selectedVariants.length }}</div>
                            <div class="text-caption text-uppercase font-weight-bold text-medium-emphasis">Sản phẩm</div>
                        </div>
                    </div>
                </v-card>

                <v-card class="rounded-xl border shadow-none">
                    <v-card-title class="pa-5 border-b font-weight-bold text-subtitle-1 d-flex align-center">
                        <CalendarIcon size="18" class="mr-2 text-primary" />
                        Thời gian hiệu lực
                    </v-card-title>
                    <v-card-text class="pa-6">
                        <div class="d-flex flex-column gap-6">
                            <div class="d-flex align-center">
                                <v-icon color="success" class="mr-4">mdi-clock-start</v-icon>
                                <div>
                                    <div class="text-caption text-medium-emphasis font-weight-bold">BẮT ĐẦU</div>
                                    <div class="text-subtitle-1 font-weight-black text-dark">
                                        {{ new Date(form.ngayBatDau).toLocaleString('vi-VN') }}
                                    </div>
                                </div>
                            </div>
                            <div class="d-flex align-center">
                                <v-icon color="error" class="mr-4">mdi-clock-end</v-icon>
                                <div>
                                    <div class="text-caption text-medium-emphasis font-weight-bold">KẾT THÚC</div>
                                    <div class="text-subtitle-1 font-weight-black text-dark">
                                        {{ new Date(form.ngayKetThuc).toLocaleString('vi-VN') }}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" lg="8">
                <v-card class="rounded-xl border shadow-none mb-6">
                    <v-card-title class="pa-5 border-b font-weight-bold text-subtitle-1 d-flex align-center">
                        <InfoCircleIcon size="18" class="mr-2 text-primary" />
                        Nội dung chương trình
                    </v-card-title>
                    <v-card-text class="pa-6">
                        <div class="mb-4 pb-4 border-b">
                            <div class="text-caption font-weight-bold text-medium-emphasis mb-1">MÔ TẢ CHIẾN DỊCH</div>
                            <div class="text-body-1 text-dark">{{ form.moTa || 'Không có mô tả chi tiết.' }}</div>
                        </div>
                        <div class="d-flex flex-wrap gap-8 py-2">
                            <div>
                                <div class="text-caption font-weight-bold text-medium-emphasis mb-1 uppercase">Đơn hàng tối thiểu</div>
                                <div class="text-h6 font-weight-black text-primary">
                                    {{
                                        new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(form.dieuKienGiamGia)
                                    }}
                                </div>
                            </div>
                            <div>
                                <div class="text-caption font-weight-bold text-medium-emphasis mb-1 uppercase">Mức độ ưu tiên</div>
                                <div class="text-h6 font-weight-black text-dark">Hạng {{ form.mucUuTien }}</div>
                            </div>
                        </div>
                    </v-card-text>
                </v-card>

                <v-card class="rounded-xl border shadow-none">
                    <v-card-title class="pa-5 border-b d-flex align-center justify-space-between">
                        <span class="font-weight-bold text-subtitle-1">Sản phẩm đang áp dụng</span>
                        <v-chip color="info" size="small" variant="tonal" class="font-weight-bold"
                            >{{ selectedVariants.length }} sản phẩm</v-chip
                        >
                    </v-card-title>
                    <v-card-text class="pa-0">
                        <v-table class="product-apply-table">
                            <thead>
                                <tr>
                                    <th class="text-left py-4 px-6">Sản phẩm</th>
                                    <th class="text-right py-4 px-6">Giá gốc</th>
                                    <th class="text-right py-4 px-6">Giá sau giảm</th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr v-for="vId in selectedVariants" :key="vId">
                                    <td class="py-4 px-6">
                                        <div class="font-weight-bold text-dark">
                                            {{ products.find((p) => p.id === vId)?.tenSanPhamDayDu || 'Sản phẩm ' + vId }}
                                        </div>
                                    </td>
                                    <td class="text-right py-4 px-6 text-grey text-decoration-line-through">
                                        {{ new Intl.NumberFormat('vi-VN').format(products.find((p) => p.id === vId)?.giaBan || 0) }}
                                    </td>
                                    <td class="text-right py-4 px-6 font-weight-black text-error">
                                        {{
                                            new Intl.NumberFormat('vi-VN').format(
                                                (products.find((p) => p.id === vId)?.giaBan || 0) *
                                                    (form.loaiGiamGia === 'PHAN_TRAM' ? 1 - form.soTienGiam / 100 : 1) -
                                                    (form.loaiGiamGia === 'TIEN_MAT' ? form.soTienGiam : 0)
                                            )
                                        }}
                                    </td>
                                </tr>
                            </tbody>
                        </v-table>
                        <div v-if="selectedVariants.length === 0" class="text-center py-10 text-grey">
                            <p>Không có sản phẩm nào được áp dụng cho chiến dịch này.</p>
                        </div>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <v-row v-else>
            <v-col cols="12" lg="8">
                <v-card class="rounded-xl border shadow-none mb-6">
                    <v-card-title class="pa-5 border-b d-flex align-center">
                        <InfoCircleIcon size="20" class="mr-2 text-primary" />
                        <span class="font-weight-medium">Thông tin chiến dịch</span>
                    </v-card-title>
                    <v-card-text class="pa-6">
                        <v-row>
                            <v-col cols="12" md="6">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">
                                    Mã đợt giảm giá * <span class="text-caption text-primary">(Hệ thống tự tạo)</span>
                                </div>
                                <v-text-field
                                    v-model="form.ma"
                                    readonly
                                    placeholder="Hệ thống tự tạo..."
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details
                                    class="mb-4 font-weight-bold"
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12" md="6">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Tên đợt giảm giá *</div>
                                <v-text-field
                                    v-model="form.ten"
                                    :readonly="isDetailView"
                                    placeholder="Ví dụ: Siêu Sale Hè 2024"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details
                                    class="mb-4"
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Mô tả</div>
                                <v-textarea
                                    v-model="form.moTa"
                                    :readonly="isDetailView"
                                    variant="outlined"
                                    rows="3"
                                    placeholder="Chi tiết chương trình..."
                                    hide-details
                                ></v-textarea>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <v-card class="rounded-xl border shadow-none">
                    <v-card-title class="pa-5 border-b d-flex align-center">
                        <TagIcon size="20" class="mr-2 text-primary" />
                        <span class="font-weight-medium">Cấu hình giảm giá</span>
                    </v-card-title>
                    <v-card-text class="pa-6">
                        <v-row>
                            <v-col cols="12" md="4">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Loại giảm giá *</div>
                                <v-select
                                    v-model="form.loaiGiamGia"
                                    :readonly="isDetailView"
                                    :items="[
                                        { title: 'Phần trăm (%)', value: 'PHAN_TRAM' },
                                        { title: 'Số tiền mặt (VNĐ)', value: 'TIEN_MAT' }
                                    ]"
                                    variant="outlined"
                                    density="comfortable"
                                    hide-details
                                ></v-select>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">
                                    {{ form.loaiGiamGia === 'PHAN_TRAM' ? 'Giá trị giảm (%) *' : 'Số tiền giảm (₫) *' }}
                                </div>
                                <v-text-field
                                    v-model.number="form.soTienGiam"
                                    :readonly="isDetailView"
                                    variant="outlined"
                                    density="comfortable"
                                    type="number"
                                    :suffix="form.loaiGiamGia === 'PHAN_TRAM' ? '%' : '₫'"
                                    hide-details
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Đơn tối thiểu (₫) *</div>
                                <v-text-field
                                    v-model.number="form.dieuKienGiamGia"
                                    :readonly="isDetailView"
                                    variant="outlined"
                                    density="comfortable"
                                    type="number"
                                    suffix="₫"
                                    hide-details
                                ></v-text-field>
                            </v-col>
                            <v-col cols="12" md="4">
                                <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Mức độ ưu tiên</div>
                                <v-text-field
                                    v-model.number="form.mucUuTien"
                                    :readonly="isDetailView"
                                    variant="outlined"
                                    density="comfortable"
                                    type="number"
                                    placeholder="Cao hơn sẽ ưu tiên hiển thị trước"
                                    hide-details
                                ></v-text-field>
                            </v-col>
                        </v-row>
                    </v-card-text>
                </v-card>

                <!-- Product Selection -->
                <v-card class="rounded-xl border shadow-none mt-6">
                    <v-card-title class="pa-5 border-b d-flex align-center">
                        <BoxIcon size="20" class="mr-2 text-primary" />
                        <span class="font-weight-medium">Áp dụng cho sản phẩm</span>
                    </v-card-title>
                    <v-card-text class="pa-6">
                        <div class="text-subtitle-2 font-weight-bold mb-3 ml-1 text-dark">Chọn sản phẩm / Biến thể áp dụng *</div>
                        <v-autocomplete
                            v-model="selectedVariants"
                            :items="products"
                            item-title="tenSanPhamDayDu"
                            item-value="id"
                            placeholder="Tìm kiếm sản phẩm, màu sắc, kích thước..."
                            variant="outlined"
                            multiple
                            chips
                            closable-chips
                            density="comfortable"
                            :readonly="isDetailView"
                            :hint="
                                selectedVariants.length > 0
                                    ? `Đã chọn ${selectedVariants.length} sản phẩm`
                                    : 'Trống bản chất sẽ không áp dụng cho sản phẩm nào'
                            "
                            persistent-hint
                        >
                            <template #item="{ props, item }">
                                <v-list-item
                                    v-bind="props"
                                    :subtitle="`Giá: ${item.raw.giaBan} VNĐ | Kho: ${item.raw.soLuong}`"
                                ></v-list-item>
                            </template>
                        </v-autocomplete>
                    </v-card-text>
                </v-card>
            </v-col>

            <v-col cols="12" lg="4">
                <v-card class="rounded-xl border shadow-none mb-6">
                    <v-card-title class="pa-5 border-b d-flex align-center">
                        <CalendarIcon size="20" class="mr-2 text-primary" />
                        <span class="font-weight-medium">Thời gian áp dụng</span>
                    </v-card-title>
                    <v-card-text class="pa-6">
                        <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Ngày bắt đầu *</div>
                        <v-text-field
                            v-model="form.ngayBatDau"
                            :readonly="isDetailView"
                            variant="outlined"
                            density="comfortable"
                            type="datetime-local"
                            class="mb-4"
                            hide-details
                        ></v-text-field>
                        <div class="text-subtitle-2 font-weight-bold mb-1 ml-1 text-dark">Ngày kết thúc *</div>
                        <v-text-field
                            v-model="form.ngayKetThuc"
                            :readonly="isDetailView"
                            variant="outlined"
                            density="comfortable"
                            type="datetime-local"
                            hide-details
                        ></v-text-field>
                    </v-card-text>
                </v-card>
            </v-col>
        </v-row>

        <AdminConfirm
            v-model:show="confirmDialog.show"
            :title="confirmDialog.title"
            :message="confirmDialog.message"
            :color="confirmDialog.color"
            :loading="confirmDialog.loading"
            @confirm="confirmDialog.action"
        />
    </v-container>
</template>

<style scoped>
.gap-8 {
    gap: 32px;
}
.gap-6 {
    gap: 24px;
}
.gap-2 {
    gap: 8px;
}
.product-apply-table :deep(th) {
    background: #f8fafc !important;
    font-weight: 700 !important;
    font-size: 0.75rem;
    letter-spacing: 0.05em;
    color: #64748b;
    text-transform: uppercase;
}
.font-weight-black {
    font-weight: 800 !important;
}
</style>
