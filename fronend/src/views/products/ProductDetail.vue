<template>
  <div class="page">
    <PageHeader title="Chi tiết sản phẩm" :backTo="{ name: 'products.list' }">
      <AppButton variant="ghost" @click="goBack">Quay lại</AppButton>
      <AppButton @click="goEdit">Sửa</AppButton>
    </PageHeader>

    <div v-if="loaded" class="content">
      <div class="topGrid">
        <AppCard title="Hình ảnh sản phẩm" class="imageCard">
          <div class="mainImageWrap">
            <img
              v-if="normalized.hinhAnh"
              :src="normalized.hinhAnh"
              alt="Ảnh sản phẩm"
              class="mainImage"
            />
            <div v-else class="imagePlaceholder">Chưa có ảnh đại diện</div>
          </div>

          <div class="thumbList" v-if="variantImages.length">
            <div
              v-for="(img, idx) in variantImages"
              :key="`thumb-${idx}`"
              class="thumbItem"
            >
              <img :src="img.duongDanAnh" :alt="img.moTa || 'Ảnh biến thể'" />
            </div>
          </div>
        </AppCard>

        <AppCard title="Thông tin chính" class="summaryCard">
          <div class="summaryTitle">
            <h2>{{ normalized.tenSanPham || "-" }}</h2>
            <span class="code">{{ normalized.maSanPham || "-" }}</span>
          </div>

          <div class="statusRow">
            <StatusBadge
              :text="displayStatus(normalized.trangThai)"
              :tone="statusTone(normalized.trangThai)"
            />
          </div>

          <div class="infoGrid">
            <div class="infoItem">
              <span class="label">Danh mục</span>
              <b>{{ normalized.tenDanhMuc || "-" }}</b>
            </div>
            <div class="infoItem">
              <span class="label">Thương hiệu</span>
              <b>{{ normalized.tenThuongHieu || "-" }}</b>
            </div>
            <div class="infoItem">
              <span class="label">Xuất xứ</span>
              <b>{{ normalized.tenXuatXu || "-" }}</b>
            </div>
            <div class="infoItem">
              <span class="label">Giới tính khách hàng</span>
              <b>{{ displayGender(normalized.gioiTinhKhachHang) }}</b>
            </div>
            <div class="infoItem">
              <span class="label">Mục đích chạy</span>
              <b>{{ normalized.tenMucDichChay || "-" }}</b>
            </div>
            <div class="infoItem">
              <span class="label">Cổ giày</span>
              <b>{{ normalized.tenCoGiay || "-" }}</b>
            </div>
            <div class="infoItem">
              <span class="label">Chất liệu</span>
              <b>{{ normalized.tenChatLieu || "-" }}</b>
            </div>
            <div class="infoItem">
              <span class="label">Đế giày</span>
              <b>{{ normalized.tenDeGiay || "-" }}</b>
            </div>
          </div>
        </AppCard>
      </div>

      <AppCard title="Mô tả">
        <div class="descBlock">
          <div class="descItem">
            <span class="label">Mô tả ngắn</span>
            <p>{{ normalized.moTaNgan || "-" }}</p>
          </div>
          <div class="descItem">
            <span class="label">Mô tả chi tiết</span>
            <p>{{ normalized.moTaChiTiet || "-" }}</p>
          </div>
        </div>
      </AppCard>

      <AppCard title="Biến thể sản phẩm">
        <div v-if="normalized.variants.length" class="variantList">
          <div
            v-for="(variant, idx) in normalized.variants"
            :key="`variant-${idx}`"
            class="variantCard"
          >
            <div class="variantHead">
              <h4>Biến thể {{ idx + 1 }}</h4>
              <StatusBadge
                :text="displayStatus(variant.trangThai)"
                :tone="statusTone(variant.trangThai)"
              />
            </div>

            <div class="variantGrid">
              <div class="infoItem">
                <span class="label">Màu sắc</span>
                <b>{{ displayColor(variant.idMauSac) }}</b>
              </div>
              <div class="infoItem">
                <span class="label">Kích thước</span>
                <b>{{ displaySize(variant.idKichThuoc) }}</b>
              </div>
              <div class="infoItem">
                <span class="label">Mã chi tiết sản phẩm</span>
                <b>{{ variant.maChiTietSanPham || "-" }}</b>
              </div>
              <div class="infoItem">
                <span class="label">Số lượng</span>
                <b>{{ variant.soLuong ?? 0 }}</b>
              </div>
              <div class="infoItem">
                <span class="label">Giá nhập</span>
                <b>{{ formatCurrency(variant.giaNhap) }}</b>
              </div>
              <div class="infoItem">
                <span class="label">Giá bán</span>
                <b>{{ formatCurrency(variant.giaBan) }}</b>
              </div>
            </div>

            <div class="variantImages" v-if="variant.images?.length">
              <div
                v-for="(img, iIdx) in variant.images"
                :key="`variant-${idx}-img-${iIdx}`"
                class="variantImage"
              >
                <img
                  v-if="img.duongDanAnh"
                  :src="img.duongDanAnh"
                  alt="Ảnh biến thể"
                />
                <div class="imageMeta">
                  <span>{{ img.moTa || "Không có mô tả" }}</span>
                  <small v-if="img.hinhAnhDaiDien">Ảnh đại diện</small>
                </div>
              </div>
            </div>
          </div>
        </div>
        <div v-else class="emptyText">Chưa có biến thể.</div>
      </AppCard>

      <AppCard title="Thông tin quản trị">
        <div class="infoGrid">
          <div class="infoItem">
            <span class="label">Người tạo</span>
            <b>{{ normalized.nguoiTao || "-" }}</b>
          </div>
          <div class="infoItem">
            <span class="label">Ngày tạo</span>
            <b>{{ formatDateTime(normalized.ngayTao) }}</b>
          </div>
          <div class="infoItem">
            <span class="label">Người cập nhật</span>
            <b>{{ normalized.nguoiCapNhat || "-" }}</b>
          </div>
          <div class="infoItem">
            <span class="label">Ngày cập nhật</span>
            <b>{{ formatDateTime(normalized.ngayCapNhat) }}</b>
          </div>
        </div>
      </AppCard>
    </div>

    <AppCard v-else>
      <span class="emptyText">Đang tải dữ liệu sản phẩm...</span>
    </AppCard>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import PageHeader from "@/components/page/PageHeader.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppButton from "@/components/common/AppButton.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import { productService } from "@/services/product.service";
import { db } from "@/services/mock.db";

const route = useRoute();
const router = useRouter();

const loaded = ref(false);
const raw = ref({});

const byId = (arr, id) =>
  arr.find((x) => String(x.id) === String(id))?.name || "";

const normalized = computed(() => {
  const x = raw.value || {};

  const idDanhMuc = x.idDanhMuc || "";
  const idThuongHieu = x.idThuongHieu || "";
  const idXuatXu = x.idXuatXu || "";
  const idMucDichChay = x.idMucDichChay || "";
  const idCoGiay = x.idCoGiay || "";
  const idChatLieu = x.idChatLieu || "";
  const idDeGiay = x.idDeGiay || "";

  return {
    id: x.id || "",
    maSanPham: x.maSanPham || x.sku || "",
    tenSanPham: x.tenSanPham || x.name || "",
    idDanhMuc,
    tenDanhMuc:
      x.tenDanhMuc ||
      x.categoryName ||
      x.category ||
      byId(db.categories || [], idDanhMuc),
    idThuongHieu,
    tenThuongHieu:
      x.tenThuongHieu ||
      x.brandName ||
      x.brand ||
      byId(db.brands || [], idThuongHieu),
    idXuatXu,
    tenXuatXu: x.tenXuatXu || byId(db.origins || [], idXuatXu),
    idMucDichChay,
    tenMucDichChay: x.tenMucDichChay || byId(db.purposes || [], idMucDichChay),
    idCoGiay,
    tenCoGiay: x.tenCoGiay || byId(db.shoeParts || [], idCoGiay),
    idChatLieu,
    tenChatLieu: x.tenChatLieu || byId(db.materials || [], idChatLieu),
    idDeGiay,
    tenDeGiay: x.tenDeGiay || byId(db.soleCosts || [], idDeGiay),
    gioiTinhKhachHang: x.gioiTinhKhachHang || "",
    moTaNgan: x.moTaNgan || "",
    moTaChiTiet: x.moTaChiTiet || "",
    hinhAnh: x.hinhAnh || "",
    trangThai: x.trangThai || x.status || "",
    ngayTao: x.ngayTao || x.createdAt || "",
    nguoiTao: x.nguoiTao || "",
    ngayCapNhat: x.ngayCapNhat || x.updatedAt || "",
    nguoiCapNhat: x.nguoiCapNhat || "",
    variants: Array.isArray(x.variants) ? x.variants : [],
  };
});

const variantImages = computed(() =>
  normalized.value.variants
    .flatMap((variant) => variant.images || [])
    .filter((img) => String(img.duongDanAnh || "").trim()),
);

onMounted(async () => {
  raw.value = await productService.get(route.params.id);
  loaded.value = true;
});

function goBack() {
  router.push({ name: "products.list" });
}

function goEdit() {
  router.push({ name: "products.edit", params: { id: route.params.id } });
}

function displayGender(value) {
  const key = String(value || "").toUpperCase();
  if (key === "NAM") return "Nam";
  if (key === "NU") return "Nữ";
  if (key === "UNISEX") return "Unisex";
  return value || "-";
}

function displayStatus(value) {
  const key = String(value || "").toUpperCase();
  const map = {
    ACTIVE: "Hoạt động",
    INACTIVE: "Ngừng hoạt động",
  };
  return map[key] || value || "-";
}

function statusTone(value) {
  return String(value || "").toUpperCase() === "ACTIVE" ? "blue" : "inactive";
}

function displayColor(id) {
  const colors = db.colors || [
    { id: "MS-001", name: "Đen" },
    { id: "MS-002", name: "Trắng" },
    { id: "MS-003", name: "Xanh" },
    { id: "MS-004", name: "Đỏ" },
  ];
  return byId(colors, id) || id || "-";
}

function displaySize(id) {
  const sizes = db.attributes || [];
  return byId(sizes, id) || id || "-";
}

function formatCurrency(value) {
  const amount = Number(value);
  if (!Number.isFinite(amount)) return "-";
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(amount);
}

function formatDateTime(value) {
  if (value == null || value === "") return "-";
  const rawValue = String(value).trim();
  const numeric = Number(rawValue);

  const date =
    Number.isFinite(numeric) && /^\d+$/.test(rawValue)
      ? new Date(numeric)
      : new Date(rawValue);

  if (Number.isNaN(date.getTime())) return rawValue;

  const pad = (n) => String(n).padStart(2, "0");
  return `${pad(date.getHours())}:${pad(date.getMinutes())} ${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.topGrid {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 14px;
}

.mainImageWrap {
  width: 100%;
  height: 320px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #dbe3ef;
  background: #f8fafc;
}

.mainImage {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.imagePlaceholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  font-size: 14px;
}

.thumbList {
  margin-top: 12px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.thumbItem {
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #e2e8f0;
  aspect-ratio: 1;
}

.thumbItem img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.summaryTitle {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;
}

.summaryTitle h2 {
  margin: 0;
  font-size: 24px;
  color: #0f172a;
}

.code {
  font-size: 14px;
  color: #64748b;
  font-weight: 700;
}

.statusRow {
  margin-bottom: 12px;
}

.infoGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.infoItem {
  display: flex;
  flex-direction: column;
  gap: 4px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px;
}

.label {
  font-size: 12px;
  text-transform: uppercase;
  color: #64748b;
  font-weight: 700;
}

.descBlock {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.descItem {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px;
}

.descItem p {
  margin: 6px 0 0;
  color: #334155;
  line-height: 1.5;
}

.variantList {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.variantCard {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.variantHead {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.variantHead h4 {
  margin: 0;
  font-size: 15px;
}

.variantGrid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.variantImages {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 10px;
}

.variantImage {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
}

.variantImage img {
  width: 100%;
  height: 140px;
  object-fit: cover;
  background: #f8fafc;
}

.imageMeta {
  padding: 8px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #334155;
  font-size: 13px;
}

.imageMeta small {
  color: #1d4ed8;
  font-weight: 700;
}

.emptyText {
  color: #64748b;
  font-size: 14px;
}

@media (max-width: 1100px) {
  .topGrid {
    grid-template-columns: 1fr;
  }

  .infoGrid,
  .variantGrid {
    grid-template-columns: 1fr;
  }
}
</style>
