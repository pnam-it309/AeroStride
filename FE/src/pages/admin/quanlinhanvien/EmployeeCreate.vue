<template>
  <div class="min-h-screen p-4 bg-gray-50">
    <!-- Header -->
    <div class="mb-6 flex justify-between items-center">
      <div>
        <nav
          class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-1"
        >
          QUẢN LÝ NHÂN VIÊN / <span class="text-gray-600">THÊM MỚI</span>
        </nav>
        <h1 class="text-2xl font-bold text-gray-800">Thêm Nhân Viên</h1>
      </div>
      <button
        @click="goBack"
        class="bg-gray-200 hover:bg-gray-300 text-gray-700 px-4 py-2 rounded-xl text-sm"
      >
        Quay lại danh sách
      </button>
    </div>

    <div class="bg-white rounded-2xl shadow-md p-6 flex gap-6">
      <!-- Ảnh profile -->
      <div class="w-1/3 flex flex-col items-center gap-4">
        <div
          v-if="!previewImage"
          class="w-40 h-40 rounded-full border-2 border-dashed border-gray-300 flex items-center justify-center text-gray-400 text-sm"
        >
          Chưa có ảnh
        </div>
        <img
          v-else
          :src="previewImage"
          alt="profile"
          class="w-40 h-40 object-cover rounded-full border border-gray-200"
        />
        <input type="file" accept="image/*" @change="handleFileChange" />
      </div>

      <!-- Form thông tin -->
      <div class="w-2/3 grid grid-cols-2 gap-4">
        <!-- Mã nhân viên -->
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1">
            Mã nhân viên <span class="text-red-500">*</span>
          </label>
          <input
            v-model="form.ma"
            type="text"
            placeholder="VD: NV001"
            class="w-full border border-gray-200 rounded-xl px-3 py-2 focus:ring-2 focus:ring-cyan-400 outline-none"
            :class="errors.ma ? 'border-red-400' : ''"
          />
          <p v-if="errors.ma" class="text-red-500 text-xs mt-1">
            {{ errors.ma }}
          </p>
        </div>

        <!-- Tên nhân viên -->
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1">
            Tên nhân viên <span class="text-red-500">*</span>
          </label>
          <input
            v-model="form.ten"
            type="text"
            placeholder="Nhập họ và tên..."
            class="w-full border border-gray-200 rounded-xl px-3 py-2 focus:ring-2 focus:ring-cyan-400 outline-none"
            :class="errors.ten ? 'border-red-400' : ''"
          />
          <p v-if="errors.ten" class="text-red-500 text-xs mt-1">
            {{ errors.ten }}
          </p>
        </div>

        <!-- Email -->
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1">
            Email <span class="text-red-500">*</span>
          </label>
          <input
            v-model="form.email"
            type="email"
            placeholder="example@email.com"
            class="w-full border border-gray-200 rounded-xl px-3 py-2 focus:ring-2 focus:ring-cyan-400 outline-none"
            :class="errors.email ? 'border-red-400' : ''"
          />
          <p v-if="errors.email" class="text-red-500 text-xs mt-1">
            {{ errors.email }}
          </p>
        </div>

        <!-- Số điện thoại -->
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1"
            >Số điện thoại</label
          >
          <input
            v-model="form.sdt"
            type="text"
            placeholder="0xxxxxxxxx"
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
          <label class="block text-sm font-bold text-gray-700 mb-1">
            Tên tài khoản <span class="text-red-500">*</span>
          </label>
          <input
            v-model="form.tenTaiKhoan"
            type="text"
            placeholder="Nhập tên đăng nhập..."
            class="w-full border border-gray-200 rounded-xl px-3 py-2 focus:ring-2 focus:ring-cyan-400 outline-none"
            :class="errors.tenTaiKhoan ? 'border-red-400' : ''"
          />
          <p v-if="errors.tenTaiKhoan" class="text-red-500 text-xs mt-1">
            {{ errors.tenTaiKhoan }}
          </p>
        </div>

        <!-- Mật khẩu -->
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1">
            Mật khẩu <span class="text-red-500">*</span>
          </label>
          <input
            v-model="form.matKhau"
            type="password"
            placeholder="Nhập mật khẩu..."
            class="w-full border border-gray-200 rounded-xl px-3 py-2 focus:ring-2 focus:ring-cyan-400 outline-none"
            :class="errors.matKhau ? 'border-red-400' : ''"
          />
          <p v-if="errors.matKhau" class="text-red-500 text-xs mt-1">
            {{ errors.matKhau }}
          </p>
        </div>

        <!-- Trạng thái -->
        <div class="col-span-2">
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
        @click="handleCreate"
        :disabled="saving"
        class="bg-cyan-500 hover:bg-cyan-600 disabled:opacity-50 text-white px-6 py-2 rounded-xl text-sm"
      >
        {{ saving ? "Đang lưu..." : "Thêm nhân viên" }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import * as NhanVienService from "@/services/nhanVienService";

const router = useRouter();
const saving = ref(false);
const previewImage = ref(null);

const form = reactive({
  ma: "",
  ten: "",
  email: "",
  sdt: "",
  ngaySinh: "",
  gioiTinh: true,
  tenTaiKhoan: "",
  matKhau: "",
  trangThai: "DANG_HOAT_DONG",
});

const errors = reactive({
  ma: "",
  ten: "",
  email: "",
  tenTaiKhoan: "",
  matKhau: "",
});

// ── validate ──────────────────────────────────────────────────────────────────
const validate = () => {
  let valid = true;

  // reset errors
  Object.keys(errors).forEach((k) => (errors[k] = ""));

  if (!form.ma.trim()) {
    errors.ma = "Mã nhân viên không được để trống";
    valid = false;
  }
  if (!form.ten.trim()) {
    errors.ten = "Tên nhân viên không được để trống";
    valid = false;
  }
  if (!form.email.trim()) {
    errors.email = "Email không được để trống";
    valid = false;
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = "Email không hợp lệ";
    valid = false;
  }
  if (!form.tenTaiKhoan.trim()) {
    errors.tenTaiKhoan = "Tên tài khoản không được để trống";
    valid = false;
  }
  if (!form.matKhau.trim()) {
    errors.matKhau = "Mật khẩu không được để trống";
    valid = false;
  } else if (form.matKhau.length < 6) {
    errors.matKhau = "Mật khẩu tối thiểu 6 ký tự";
    valid = false;
  }

  return valid;
};

// ── xử lý chọn ảnh ────────────────────────────────────────────────────────────
const handleFileChange = (event) => {
  const file = event.target.files[0];
  if (file) {
    form.file = file;
    previewImage.value = URL.createObjectURL(file);
  }
};

// ── thêm mới ──────────────────────────────────────────────────────────────────
const handleCreate = async () => {
  if (!validate()) return;

  try {
    saving.value = true;

    const payload = {
      ma: form.ma,
      ten: form.ten,
      email: form.email,
      sdt: form.sdt || null,
      ngaySinh: form.ngaySinh || null,
      gioiTinh: form.gioiTinh,
      tenTaiKhoan: form.tenTaiKhoan,
      matKhau: form.matKhau,
      trangThai: form.trangThai,
    };

    await NhanVienService.createNhanVien(payload);
    alert("Thêm nhân viên thành công!");
    router.push({ name: "AdminEmployees" });
  } catch (err) {
    console.error("Lỗi thêm nhân viên:", err);
    const msg = err.response?.data?.message || "Thêm thất bại!";
    alert(msg);
  } finally {
    saving.value = false;
  }
};

const goBack = () => router.push({ name: "AdminEmployees" });
</script>
