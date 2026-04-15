<template>
  <PageHeader title="Thêm sản phẩm" :backTo="{ name: 'products.list' }" />

  <div class="createLayout">
    <div class="leftColumn">
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
          <AppInput
            label="Giá nhập (VND)"
            v-model.number="form.giaNhap"
            type="number"
            placeholder="1000000"
          />
          <AppSelect
            label="Trạng thái"
            v-model="form.trangThai"
            :options="statusOptions"
          />
        </div>
      </AppCard>

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

      <AppCard title="Biến thể sản phẩm">
        <div class="variantList">
          <div
            v-for="(variant, vIdx) in form.variants"
            :key="`variant-${vIdx}`"
            class="variantCard"
          >
            <div class="variantHeader">
              <h4>Biến thể {{ vIdx + 1 }}</h4>
              <button
                v-if="form.variants.length > 1"
                type="button"
                class="textBtn danger"
                @click="removeVariant(vIdx)"
              >
                Xóa biến thể
              </button>
            </div>

            <div class="formGrid variantGrid">
              <AppSelect
                label="Màu sắc *"
                v-model="variant.idMauSac"
                :options="colorOptions"
              />
              <AppSelect
                label="Kích thước *"
                v-model="variant.idKichThuoc"
                :options="sizeOptions"
              />
              <AppInput
                label="Mã chi tiết sản phẩm"
                v-model="variant.maChiTietSanPham"
                placeholder="VD: CTSP-001"
              />
              <AppInput
                label="Số lượng *"
                v-model.number="variant.soLuong"
                type="number"
                placeholder="0"
              />
              <AppInput
                label="Giá nhập"
                v-model.number="variant.giaNhap"
                type="number"
                placeholder="0"
              />
              <AppInput
                label="Giá bán *"
                v-model.number="variant.giaBan"
                type="number"
                placeholder="0"
              />
              <AppSelect
                label="Trạng thái"
                v-model="variant.trangThai"
                :options="statusOptions"
              />
            </div>
          </div>

          <button type="button" class="addVariantBtn" @click="addVariant">
            + Thêm biến thể mới
          </button>
        </div>
      </AppCard>

      <div class="actions">
        <AppButton variant="ghost" @click="goBack">Hủy</AppButton>
        <AppButton @click="handleSubmit" :loading="isSubmitting">
          Thêm sản phẩm
        </AppButton>
      </div>
    </div>

    <div class="rightColumn">
      <AppCard title="Hình ảnh sản phẩm">
        <div class="imageSection">
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

            <AppInput
              label="URL ảnh đại diện"
              v-model="form.hinhAnh"
              placeholder="https://..."
            />
          </div>

          <div class="galleryBox">
            <div class="variantImagesHeader">
              <h5>Ảnh biến thể (gộp một nơi)</h5>
              <button type="button" class="textBtn" @click="addImageEntry">
                + Thêm ảnh
              </button>
            </div>

            <div class="imageEntries">
              <div
                v-for="(img, iIdx) in form.imageEntries"
                :key="`variant-img-${iIdx}`"
                class="variantImageItem"
              >
                <div class="variantImageGrid">
                  <AppSelect
                    label="Biến thể áp dụng"
                    v-model="img.variantIndex"
                    :options="variantBindingOptions"
                  />
                  <AppInput
                    label="Đường dẫn ảnh *"
                    v-model="img.duongDanAnh"
                    placeholder="https://..."
                  />
                  <AppInput
                    label="Mô tả"
                    v-model="img.moTa"
                    placeholder="Mô tả ảnh"
                  />
                  <AppSelect
                    label="Trạng thái"
                    v-model="img.trangThai"
                    :options="statusOptions"
                  />
                  <label class="checkLabel">
                    <span>Ảnh đại diện biến thể</span>
                    <input
                      type="checkbox"
                      :checked="img.hinhAnhDaiDien"
                      @change="setRepresentativeImage(iIdx, $event)"
                    />
                  </label>
                </div>

                <button
                  v-if="form.imageEntries.length > 1"
                  type="button"
                  class="textBtn danger"
                  @click="removeImageEntry(iIdx)"
                >
                  Xóa ảnh
                </button>
              </div>
            </div>
          </div>
        </div>
      </AppCard>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from "vue";
import { useRouter } from "vue-router";
import PageHeader from "@/components/page/PageHeader.vue";
import AppCard from "@/components/common/AppCard.vue";
import AppInput from "@/components/common/AppInput.vue";
import AppSelect from "@/components/common/AppSelect.vue";
import AppButton from "@/components/common/AppButton.vue";
import Icon from "@/components/common/Icon.vue";
import { productService } from "@/services/product.service";
import { fileService } from "@/services/file.service";
import { db } from "@/services/mock.db";

const router = useRouter();
const isSubmitting = ref(false);
const mainImageInput = ref(null);

const createEmptyVariantImage = () => ({
  duongDanAnh: "",
  moTa: "",
  hinhAnhDaiDien: false,
  trangThai: "ACTIVE",
  variantIndex: "0",
});

const createEmptyVariant = () => ({
  idMauSac: "",
  idKichThuoc: "",
  maChiTietSanPham: "",
  soLuong: 0,
  giaNhap: 0,
  giaBan: 0,
  trangThai: "ACTIVE",
  images: [],
});

const form = ref({
  maSanPham: "",
  tenSanPham: "",
  giaBan: 0,
  giaNhap: 0,
  idDanhMuc: "",
  idThuongHieu: "",
  idXuatXu: "",
  idMucDichChay: "",
  idCoGiay: "",
  idChatLieu: "",
  idDeGiay: "",
  gioiTinhKhachHang: "",
  hinhAnh: "",
  mainImageName: "",
  moTaNgan: "",
  moTaChiTiet: "",
  trangThai: "ACTIVE",
  imageEntries: [createEmptyVariantImage()],
  variants: [createEmptyVariant()],
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

const defaultColorOptions = [
  { id: "MS-001", name: "Đen" },
  { id: "MS-002", name: "Trắng" },
  { id: "MS-003", name: "Xanh" },
  { id: "MS-004", name: "Đỏ" },
];

const colorOptions = (db.colors || defaultColorOptions).map((x) => ({
  label: x.name,
  value: x.id,
}));

const defaultSizeOptions = [
  { id: "SIZE-39", name: "Size 39" },
  { id: "SIZE-40", name: "Size 40" },
  { id: "SIZE-41", name: "Size 41" },
  { id: "SIZE-42", name: "Size 42" },
  { id: "SIZE-43", name: "Size 43" },
];

const sizeOptions = (
  db.attributes && db.attributes.length > 0 ? db.attributes : defaultSizeOptions
).map((x) => ({
  label: x.name,
  value: x.id,
}));

const variantBindingOptions = computed(() =>
  form.value.variants.map((variant, idx) => {
    const color = labelById(colorOptions, variant.idMauSac) || "Chưa chọn màu";
    const size =
      labelById(sizeOptions, variant.idKichThuoc) || "Chưa chọn size";
    return {
      label: `Biến thể ${idx + 1} (${color} - ${size})`,
      value: String(idx),
    };
  }),
);

function addVariant() {
  form.value.variants.push(createEmptyVariant());
}

function removeVariant(index) {
  form.value.variants.splice(index, 1);
  form.value.imageEntries = form.value.imageEntries.map((img) => {
    const currentVariant = Number(img.variantIndex || 0);
    if (currentVariant === index) {
      return { ...img, variantIndex: "0", hinhAnhDaiDien: false };
    }
    if (currentVariant > index) {
      return { ...img, variantIndex: String(currentVariant - 1) };
    }
    return img;
  });
}

function addImageEntry() {
  form.value.imageEntries.push(createEmptyVariantImage());
}

function removeImageEntry(imageIndex) {
  form.value.imageEntries.splice(imageIndex, 1);
}

function setRepresentativeImage(imageIndex, event) {
  const checked = Boolean(event.target.checked);
  const targetVariant = String(
    form.value.imageEntries[imageIndex].variantIndex || "0",
  );
  form.value.imageEntries.forEach((img, idx) => {
    if (String(img.variantIndex || "0") !== targetVariant) return;
    img.hinhAnhDaiDien = checked ? idx === imageIndex : false;
  });
}

function labelById(options, id) {
  return options.find((x) => x.value === id)?.label || "";
}

function triggerMainImageUpload() {
  mainImageInput.value?.click();
}

function onMainImageSelect(event) {
  const file = event.target.files?.[0];
  if (file) {
    form.value.mainImageName = file.name;
    const reader = new FileReader();
    reader.onload = (e) => {
      form.value.hinhAnh = e.target?.result || "";
    };
    reader.readAsDataURL(file);
  }
}

async function handleSubmit() {
  if (!form.value.tenSanPham || !form.value.giaBan) {
    alert("Vui lòng điền các trường bắt buộc");
    return;
  }

  if (!form.value.variants.length) {
    alert("Vui lòng thêm ít nhất 1 biến thể sản phẩm");
    return;
  }

  const invalidVariant = form.value.variants.find(
    (v) =>
      !v.idMauSac ||
      !v.idKichThuoc ||
      Number(v.soLuong) < 0 ||
      Number(v.giaBan) < 0,
  );
  if (invalidVariant) {
    alert(
      "Vui lòng kiểm tra lại thông tin biến thể (màu, size, số lượng, giá)",
    );
    return;
  }

  isSubmitting.value = true;
  try {
    const mappedVariants = form.value.variants.map((variant, vIdx) => ({
      idMauSac: variant.idMauSac,
      idKichThuoc: variant.idKichThuoc,
      maChiTietSanPham: String(variant.maChiTietSanPham || "").slice(0, 50),
      soLuong: Number(variant.soLuong) || 0,
      giaNhap: Number(variant.giaNhap) || 0,
      giaBan: Number(variant.giaBan) || 0,
      trangThai: variant.trangThai || "ACTIVE",
      images: (form.value.imageEntries || [])
        .filter(
          (img) =>
            String(img.variantIndex || "0") === String(vIdx) &&
            String(img.duongDanAnh || "").trim(),
        )
        .map((img) => ({
          duongDanAnh: img.duongDanAnh,
          moTa: String(img.moTa || "").slice(0, 1000),
          hinhAnhDaiDien: Boolean(img.hinhAnhDaiDien),
          trangThai: img.trangThai || "ACTIVE",
        })),
    }));

    const brandName = labelById(brandOptions, form.value.idThuongHieu);
    const categoryName = labelById(categoryOptions, form.value.idDanhMuc);
    const totalQty = mappedVariants.reduce(
      (sum, item) => sum + (Number(item.soLuong) || 0),
      0,
    );
    const firstPrice =
      Number(mappedVariants[0]?.giaBan) || Number(form.value.giaBan) || 0;

    const productPayload = {
      ...form.value,
      sku: form.value.maSanPham,
      name: form.value.tenSanPham,
      brandName,
      categoryName,
      brand: brandName,
      category: categoryName,
      qty: totalQty,
      price: firstPrice,
      status: form.value.trangThai,
      giaBan: firstPrice,
      giaNhap: Number(form.value.giaNhap) || 0,
      variants: mappedVariants,
    };

    const createdProduct = await productService.create(productPayload);

    if (form.value.hinhAnh) {
      const fileName =
        form.value.mainImageName ||
        `${createdProduct.maSanPham || createdProduct.id}-cover.png`;

      await fileService.create({
        code: `F-${String(createdProduct.id).padStart(3, "0")}`,
        name: fileName,
        url: `/uploads/products/${fileName}`,
        status: "ACTIVE",
        createdAt: new Date().toISOString().slice(0, 10),
        relatedEntityType: "PRODUCT",
        relatedEntityId: String(createdProduct.id),
        relatedEntityCode:
          createdProduct.maSanPham || createdProduct.code || "",
        relatedEntityName:
          createdProduct.tenSanPham || createdProduct.name || "",
        fileType: "MAIN_IMAGE",
      });
    }

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

.createLayout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 420px;
  gap: 18px;
  align-items: start;
}

.leftColumn,
.rightColumn {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.rightColumn {
  position: sticky;
  top: 16px;
}

.formGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
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

.imageSection {
  display: flex;
  flex-direction: column;
  gap: 16px;
  align-items: stretch;
}

.mainImageBox {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.galleryBox {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.boxTitle {
  font-size: 13px;
  font-weight: 700;
  color: #243043;
  letter-spacing: 0.02em;
}

.uploadArea {
  height: 280px;
  border: 1.5px dashed #cbd5e1;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  background: linear-gradient(180deg, #f8fbff 0%, #f3f7fd 100%);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
  transition: all 0.2s ease;
}

.uploadArea:hover {
  transform: translateY(-1px);
  border-color: #3f78d0;
  box-shadow: 0 10px 24px rgba(63, 120, 208, 0.08);
}

.uploadPlaceholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  text-align: center;
  padding: 20px;
}

.uploadPlaceholder :deep(.icon) {
  width: 44px;
  height: 44px;
  color: #94a3b8;
}

.uploadPlaceholder p {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  color: #1f3b68;
}

.hint {
  font-size: 12px;
  color: #64748b;
}

.preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

@media (max-width: 600px) {
  .createLayout {
    grid-template-columns: 1fr;
  }

  .rightColumn {
    position: static;
  }

  .uploadArea {
    min-height: 240px;
    height: 240px;
  }
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}

.variantList {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.variantCard {
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #fff;
}

.variantHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.variantHeader h4 {
  margin: 0;
  font-size: 15px;
  color: #243043;
}

.variantGrid {
  gap: 12px;
}

.variantImagesHeader {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.imageEntries {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.variantImagesHeader h5 {
  margin: 0;
  font-size: 13px;
  color: #334155;
}

.variantImageItem {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.variantImageGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.checkLabel {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 40px;
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  padding: 0 12px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.textBtn {
  border: 0;
  background: transparent;
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.textBtn.danger {
  color: #dc2626;
}

.addVariantBtn {
  border: 1px dashed #b6c7e4;
  background: #f8fbff;
  border-radius: 10px;
  height: 40px;
  color: #1d4ed8;
  font-weight: 700;
  cursor: pointer;
}

.addVariantBtn:hover {
  background: #eef5ff;
}

@media (max-width: 1200px) {
  .createLayout {
    grid-template-columns: minmax(0, 1fr) 360px;
  }
}

@media (max-width: 1024px) {
  .createLayout {
    grid-template-columns: 1fr;
  }

  .rightColumn {
    position: static;
  }

  .variantImageGrid {
    grid-template-columns: 1fr;
  }
}
</style>
