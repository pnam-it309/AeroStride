<template>
  <div class="page">
    <PageHeader title="Thêm sản phẩm" :backTo="{ name: 'products.list' }" />

    <div class="createContent">
      <!-- Basic Info Card -->
      <AppCard title="Thông tin cơ bản">
        <div class="formGrid">
          <AppInput
            label="Mã sản phẩm"
            v-model="form.maSanPham"
            placeholder="VD: SH-001"
            :maxlength="50"
          />
          <AppInput
            label="Tên sản phẩm *"
            v-model="form.tenSanPham"
            placeholder="VD: Aero Runner"
            :maxlength="255"
            required
          />
          <AppInput
            label="Giá bán (VND) *"
            v-model.number="form.giaBan"
            type="number"
            placeholder="1290000"
            required
          />
          <AppSelect
            label="Trạng thái"
            v-model="form.trangThai"
            :options="statusOptions"
          />
        </div>
      </AppCard>

      <!-- Classification Card -->
      <AppCard title="Phân loại sản phẩm">
        <div class="formGrid">
          <AppSelect
            label="Danh mục *"
            v-model="form.idDanhMuc"
            :options="categoryOptions"
            required
          />
          <AppSelect
            label="Thương hiệu *"
            v-model="form.idThuongHieu"
            :options="brandOptions"
            required
          />
          <AppSelect
            label="Xuất xứ *"
            v-model="form.idXuatXu"
            :options="originOptions"
            required
          />
          <AppSelect
            label="Giới tính khách hàng *"
            v-model="form.gioiTinhKhachHang"
            :options="genderOptions"
            required
          />
        </div>
      </AppCard>

      <!-- Shoe Specifications Card -->
      <AppCard title="Thông số kỹ thuật giày">
        <div class="formGrid">
          <AppSelect
            label="Mục đích chạy *"
            v-model="form.idMucDichChay"
            :options="purposeOptions"
            required
          />
          <AppSelect
            label="Cổ giày *"
            v-model="form.idCoGiay"
            :options="shoePartOptions"
            required
          />
          <AppSelect
            label="Chất liệu *"
            v-model="form.idChatLieu"
            :options="materialOptions"
            required
          />
          <AppSelect
            label="Đế giày *"
            v-model="form.idDeGiay"
            :options="soleCostOptions"
            required
          />
        </div>
      </AppCard>

      <!-- Description Card -->
      <AppCard title="Mô tả sản phẩm">
        <div class="descContainer">
          <AppInput
            label="Mô tả ngắn"
            v-model="form.moTaNgan"
            placeholder="Mô tả ngắn gọn sản phẩm"
            :maxlength="1000"
            type="textarea"
            rows="3"
          />
          <AppInput
            label="Mô tả chi tiết"
            v-model="form.moTaChiTiet"
            placeholder="Mô tả chi tiết sản phẩm"
            type="textarea"
            rows="6"
          />
        </div>
      </AppCard>

      <!-- Image Upload Card -->
      <AppCard title="Hình ảnh sản phẩm">
        <div class="imageSection">
          <!-- Main Image -->
          <div class="mainImageBox">
            <div class="boxTitle">Ảnh đại diện chính</div>
            <div class="uploadArea" @click="triggerMainImageUpload">
              <div v-if="!form.hinhAnh" class="uploadPlaceholder">
                <Icon name="image" />
                <p>Nhấp để chọn ảnh đại diện</p>
                <span class="hint">JPG, PNG, GIF (tối đa 5MB)</span>
              </div>
              <img v-else :src="form.hinhAnh" alt="Preview" class="preview" />
              <input
                ref="mainImageInput"
                type="file"
                accept="image/*"
                hidden
                @change="onMainImageSelect"
              />
            </div>
          </div>

          <!-- Gallery Images -->
          <div class="galleryBox">
            <div class="boxTitle">Thư viện ảnh (tối đa 5 ảnh)</div>
            <div class="imageGrid">
              <div
                v-for="(img, idx) in form.thueVienAnh"
                :key="idx"
                class="imageItem"
              >
                <img :src="img" :alt="`Gallery ${idx}`" />
                <button
                  class="removeBtn"
                  @click="removeGalleryImage(idx)"
                  title="Xóa"
                >
                  <Icon name="xCircle" />
                </button>
              </div>
              <div
                v-if="form.thueVienAnh.length < 5"
                class="addImageBox"
                @click="triggerGalleryUpload"
              >
                <Icon name="plus" />
                <p>Thêm ảnh</p>
                <input
                  ref="galleryInput"
                  type="file"
                  accept="image/*"
                  hidden
                  @change="onGalleryImageSelect"
                />
              </div>
            </div>
          </div>
        </div>
      </AppCard>

      <!-- Actions -->
      <div class="actions">
        <AppButton variant="ghost" @click="goBack">Hủy</AppButton>
        <AppButton @click="handleSubmit" :loading="isSubmitting">
          Thêm sản phẩm
        </AppButton>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";
import PageHeader from "@/components/page/PageHeader.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppInput from "@/components/common/AppInput.vue";
import AppSelect from "@/components/common/AppSelect.vue";
import AppButton from "@/components/common/AppButton.vue";
import Icon from "@/components/common/Icon.vue";
import { productService } from "@/services/product.service";
import { db } from "@/services/mock.db";

const router = useRouter();
const isSubmitting = ref(false);
const mainImageInput = ref(null);
const galleryInput = ref(null);

const form = ref({
  maSanPham: "",
  tenSanPham: "",
  giaBan: 0,
  idDanhMuc: "",
  idThuongHieu: "",
  idXuatXu: "",
  idMucDichChay: "",
  idCoGiay: "",
  idChatLieu: "",
  idDeGiay: "",
  gioiTinhKhachHang: "",
  hinhAnh: "",
  moTaNgan: "",
  moTaChiTiet: "",
  trangThai: "ACTIVE",
  thueVienAnh: [],
});

const statusOptions = [
  { label: "Hoạt động", value: "ACTIVE" },
  { label: "Ngừng hoạt động", value: "INACTIVE" },
];

const genderOptions = [
  { label: "Nam", value: "NAM" },
  { label: "Nữ", value: "NU" },
  { label: "Unisex", value: "UNISEX" },
];

const categoryOptions = (db.categories || []).map((c) => ({
  label: c.name,
  value: c.id,
}));

const brandOptions = (db.brands || []).map((b) => ({
  label: b.name,
  value: b.id,
}));

const originOptions = (db.origins || []).map((o) => ({
  label: o.name,
  value: o.id,
}));

const purposeOptions = (db.purposes || []).map((p) => ({
  label: p.name,
  value: p.id,
}));

const shoePartOptions = (db.shoeParts || []).map((sp) => ({
  label: sp.name,
  value: sp.id,
}));

const materialOptions = (db.materials || []).map((m) => ({
  label: m.name,
  value: m.id,
}));

const soleCostOptions = (db.soleCosts || []).map((sc) => ({
  label: sc.name,
  value: sc.id,
}));

function triggerMainImageUpload() {
  mainImageInput.value?.click();
}

function triggerGalleryUpload() {
  galleryInput.value?.click();
}

function onMainImageSelect(event) {
  const file = event.target.files?.[0];
  if (file) {
    const reader = new FileReader();
    reader.onload = (e) => {
      form.value.hinhAnh = e.target?.result || "";
    };
    reader.readAsDataURL(file);
  }
}

function onGalleryImageSelect(event) {
  const file = event.target.files?.[0];
  if (file && form.value.thueVienAnh.length < 5) {
    const reader = new FileReader();
    reader.onload = (e) => {
      form.value.thueVienAnh.push(e.target?.result || "");
    };
    reader.readAsDataURL(file);
  }
}

function removeGalleryImage(idx) {
  form.value.thueVienAnh.splice(idx, 1);
}

async function handleSubmit() {
  if (!form.value.tenSanPham || !form.value.giaBan) {
    alert("Vui lòng điền các trường bắt buộc");
    return;
  }

  isSubmitting.value = true;
  try {
    await productService.create(form.value);
    router.push({ name: "products.list" });
  } finally {
    isSubmitting.value = false;
  }
}

function goBack() {
  router.back();
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.createContent {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.formGrid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

@media (max-width: 768px) {
  .formGrid {
    grid-template-columns: 1fr;
  }
}

.descContainer {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Image Section */
.imageSection {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
}

@media (max-width: 1024px) {
  .imageSection {
    grid-template-columns: 1fr;
  }
}

.mainImageBox,
.galleryBox {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.boxTitle {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  text-transform: uppercase;
}

/* Upload Area */
.uploadArea {
  position: relative;
  width: 100%;
  height: 240px;
  border: 2px dashed #cbd5e1;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
  overflow: hidden;
  background: #f8fafc;
}

.uploadArea:hover {
  border-color: #3f78d0;
  background: #f0f6ff;
}

.uploadPlaceholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  pointer-events: none;
}

.uploadPlaceholder :deep(.icon) {
  width: 48px;
  height: 48px;
  color: #cbd5e1;
}

.uploadPlaceholder p {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #3f78d0;
}

.hint {
  font-size: 12px;
  color: #6b7280;
}

.preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Gallery Grid */
.imageGrid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

@media (max-width: 600px) {
  .imageGrid {
    grid-template-columns: 1fr;
  }
}

.imageItem,
.addImageBox {
  position: relative;
  aspect-ratio: 1;
  border-radius: 8px;
  overflow: hidden;
}

.imageItem img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.removeBtn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 0;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.6);
  color: white;
  cursor: pointer;
  opacity: 0;
  transition: all 0.2s ease;
}

.removeBtn :deep(.icon) {
  width: 16px;
  height: 16px;
}

.imageItem:hover .removeBtn {
  opacity: 1;
}

.addImageBox {
  border: 2px dashed #cbd5e1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #f8fafc;
}

.addImageBox:hover {
  border-color: #3f78d0;
  background: #f0f6ff;
}

.addImageBox :deep(.icon) {
  width: 32px;
  height: 32px;
  color: #cbd5e1;
  transition: color 0.2s ease;
}

.addImageBox:hover :deep(.icon) {
  color: #3f78d0;
}

.addImageBox p {
  margin: 0;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
}

/* Actions */
.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}
</style>
