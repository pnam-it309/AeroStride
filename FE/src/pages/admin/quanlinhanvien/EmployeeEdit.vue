<template>
  <div class="min-h-screen p-4 bg-gray-50">
    <!-- Header -->
    <div class="mb-6 flex justify-between items-center">
      <div>
        <nav
          class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-1"
        >
          QUẢN LÝ NHÂN VIÊN / <span class="text-gray-600">CẬP NHẬT</span>
        </nav>
        <h1 class="text-2xl font-bold text-gray-800">Sửa Nhân Viên</h1>
      </div>
      <button
        @click="goBack"
        class="bg-gray-200 hover:bg-gray-300 text-gray-700 px-4 py-2 rounded-xl text-sm"
      >
        Quay lại danh sách
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="flex justify-center items-center py-20">
      <div class="text-gray-400 text-sm">Đang tải dữ liệu...</div>
    </div>

    <div v-else class="bg-white rounded-2xl shadow-md p-6 flex gap-6">
      <!-- Ảnh profile -->
      <div class="w-1/3 flex flex-col items-center gap-4">
        <img
          v-if="previewImage || form.hinhAnh"
          :src="previewImage || form.hinhAnh"
          alt="profile"
          class="w-40 h-40 object-cover rounded-full border border-gray-200"
        />
        <div
          v-else
          class="w-40 h-40 rounded-full border-2 border-dashed border-gray-300 flex items-center justify-center text-gray-400 text-sm"
        >
          Chưa có ảnh
        </div>
        <input type="file" accept="image/*" @change="handleFileChange" />
      </div>

      <!-- Thông tin nhân viên -->
      <div class="w-2/3 grid grid-cols-2 gap-4">
        <!-- Mã nhân viên (readonly) -->
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1"
            >Mã nhân viên</label
          >
          <input
            :value="form.ma"
            type="text"
            disabled
            class="w-full border border-gray-200 rounded-xl px-3 py-2 bg-gray-100 text-gray-500"
          />
        </div>

        <!-- Tên -->
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1"
            >Tên nhân viên</label
          >
          <input
            v-model="form.ten"
            type="text"
            class="w-full border border-gray-200 rounded-xl px-3 py-2 focus:ring-2 focus:ring-cyan-400 outline-none"
          />
        </div>

        <!-- Email -->
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1"
            >Email</label
          >
          <input
            v-model="form.email"
            type="email"
            class="w-full border border-gray-200 rounded-xl px-3 py-2 focus:ring-2 focus:ring-cyan-400 outline-none"
          />
        </div>

        <!-- Số điện thoại -->
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1"
            >Số điện thoại</label
          >
          <input
            v-model="form.sdt"
            type="text"
            class="w-full border border-gray-200 rounded-xl px-3 py-2 focus:ring-2 focus:ring-cyan-400 outline-none"
          />
        </div>

        <!-- Ngày sinh -->
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1"
            >Ngày sinh</label
          >
          <input
            v-model="form.ngaySinh"
            type="date"
            class="w-full border border-gray-200 rounded-xl px-3 py-2 focus:ring-2 focus:ring-cyan-400 outline-none"
          />
        </div>

        <!-- Giới tính -->
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1"
            >Giới tính</label
          >
          <select
            v-model="form.gioiTinh"
            class="w-full border border-gray-200 rounded-xl px-3 py-2"
          >
            <option :value="true">Nam</option>
            <option :value="false">Nữ</option>
          </select>
        </div>

        <!-- Tên tài khoản -->
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1"
            >Tên tài khoản</label
          >
          <input
            v-model="form.tenTaiKhoan"
            type="text"
            class="w-full border border-gray-200 rounded-xl px-3 py-2 focus:ring-2 focus:ring-cyan-400 outline-none"
          />
        </div>

        <!-- Trạng thái -->
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1"
            >Trạng thái</label
          >
          <select
            v-model="form.trangThai"
            class="w-full border border-gray-200 rounded-xl px-3 py-2"
          >
            <option value="DANG_HOAT_DONG">Hoạt động</option>
            <option value="KHONG_HOAT_DONG">Ngừng hoạt động</option>
          </select>
        </div>

        <!-- Mật khẩu mới (không bắt buộc) -->
        <div class="col-span-2">
          <label class="block text-sm font-bold text-gray-700 mb-1">
            Mật khẩu mới
            <span class="text-gray-400 font-normal"
              >(để trống nếu không đổi)</span
            >
          </label>
          <input
            v-model="form.matKhau"
            type="password"
            placeholder="Nhập mật khẩu mới nếu muốn đổi..."
            class="w-full border border-gray-200 rounded-xl px-3 py-2 focus:ring-2 focus:ring-cyan-400 outline-none"
          />
        </div>
      </div>
    </div>

    <!-- Nút lưu -->
    <div class="mt-6 flex justify-end gap-4">
      <button
        @click="goBack"
        class="bg-gray-200 hover:bg-gray-300 text-gray-700 px-6 py-2 rounded-xl text-sm"
      >
        Hủy
      </button>
      <button
        @click="handleUpdate"
        :disabled="saving"
        class="bg-cyan-500 hover:bg-cyan-600 disabled:opacity-50 text-white px-6 py-2 rounded-xl text-sm"
      >
        {{ saving ? "Đang lưu..." : "Cập nhật" }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import * as NhanVienService from "@/services/nhanVienService";

const route = useRoute();
const router = useRouter();

// ── lấy id từ query param ──────────────────────────────────────────────────
const id = route.query.id;

const loading = ref(true);
const saving = ref(false);

const form = reactive({
  ma: "",
  ten: "",
  email: "",
  sdt: "",
  ngaySinh: "",
  gioiTinh: true,
  tenTaiKhoan: "",
  trangThai: "DANG_HOAT_DONG",
  matKhau: "",
  hinhAnh: null,
  file: null,
});

const previewImage = ref(null);

// ── load dữ liệu khi vào trang ─────────────────────────────────────────────
const fetchNhanVien = async () => {
  if (!id) {
    alert("Không tìm thấy ID nhân viên!");
    router.push({ name: "AdminEmployees" });
    return;
  }
  try {
    loading.value = true;
    const res = await NhanVienService.getNhanVienById(id);

    const data = res;

    form.ma = data.ma || "";
    form.ten = data.ten || "";
    form.email = data.email || "";
    form.sdt = data.sdt || "";
    form.ngaySinh = data.ngaySinh || "";
    form.gioiTinh = data.gioiTinh ?? true;
    form.tenTaiKhoan = data.tenTaiKhoan || "";
    form.trangThai = data.trangThai || "DANG_HOAT_DONG";
    form.hinhAnh = data.hinhAnh || null;

    previewImage.value = data.hinhAnh || null;
  } catch (error) {
    console.error("Lỗi lấy dữ liệu nhân viên:", error);
    alert("Không thể tải dữ liệu nhân viên!");
  } finally {
    loading.value = false;
  }
};

onMounted(() => fetchNhanVien());

// ── xử lý chọn ảnh ────────────────────────────────────────────────────────
const handleFileChange = (event) => {
  const file = event.target.files[0];
  if (file) {
    form.file = file;
    previewImage.value = URL.createObjectURL(file);
  }
};

// ── cập nhật ──────────────────────────────────────────────────────────────
const handleUpdate = async () => {
  try {
    saving.value = true;

    // Gửi JSON (không dùng FormData vì BE nhận @RequestBody)
    const payload = {
      ma: form.ma,
      ten: form.ten,
      email: form.email,
      sdt: form.sdt,
      ngaySinh: form.ngaySinh || null,
      gioiTinh: form.gioiTinh,
      tenTaiKhoan: form.tenTaiKhoan,
      trangThai: form.trangThai,
      matKhau: form.matKhau || null,
    };

    await NhanVienService.updateNhanVien(id, payload);
    alert("Cập nhật nhân viên thành công!");
    router.push({ name: "AdminEmployees" });
  } catch (err) {
    console.error("Lỗi cập nhật:", err);
    const msg = err.response?.data?.message || "Cập nhật thất bại!";
    alert(msg);
  } finally {
    saving.value = false;
  }
};

const goBack = () => router.push({ name: "AdminEmployees" });
</script>
