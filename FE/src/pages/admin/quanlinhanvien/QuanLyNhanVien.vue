<template>
  <div class="min-h-screen bg-gray-50 p-2">
    <!-- Header -->
    <div class="mb-2">
      <nav
        class="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-1"
      >
        QUẢN LÝ NHÂN VIÊN / <span class="text-gray-600">DANH SÁCH</span>
      </nav>
      <h1 class="text-2xl font-bold text-gray-800">Quản Lý Nhân Viên</h1>
    </div>

    <!-- Filter -->
    <div class="bg-white rounded-2xl shadow-sm p-2 mb-6 border border-gray-100">
      <div class="grid grid-cols-12 gap-4 items-end">
        <!-- Keyword -->
        <div class="col-span-6">
          <label class="block text-sm font-bold text-gray-700 mb-2"
            >Tìm kiếm</label
          >
          <div class="relative">
            <span
              class="absolute inset-y-0 left-0 pl-3 flex items-center text-gray-400"
            >
              <i class="fas fa-search"></i>
            </span>
            <input
              v-model="filters.keyword"
              type="text"
              placeholder="Mã, tên, email, SDT, tài khoản..."
              class="w-full pl-10 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-xl focus:ring-2 focus:ring-cyan-400 outline-none text-sm"
            />
          </div>
        </div>

        <!-- Giới tính -->
        <div class="col-span-3">
          <label class="block text-sm font-bold text-gray-700 mb-2"
            >Giới tính</label
          >
          <select
            v-model="filters.gioiTinh"
            class="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-xl text-sm"
          >
            <option :value="null">Tất cả giới tính</option>
            <option :value="true">Nam</option>
            <option :value="false">Nữ</option>
          </select>
        </div>

        <!-- Trạng thái -->
        <div class="col-span-3">
          <label class="block text-sm font-bold text-gray-700 mb-2"
            >Trạng thái</label
          >
          <select
            v-model="filters.trangThai"
            class="w-full px-4 py-2 bg-gray-50 border border-gray-200 rounded-xl text-sm"
          >
            <option :value="null">Tất cả trạng thái</option>
            <option value="DANG_HOAT_DONG">Hoạt động</option>
            <option value="KHONG_HOAT_DONG">Ngừng hoạt động</option>
          </select>
        </div>
      </div>

      <!-- Buttons -->
      <div class="flex justify-between items-center mt-6">
        <div class="space-x-3">
          <button
            @click="handleFilter"
            class="bg-cyan-500 hover:bg-cyan-600 text-white font-bold py-2 px-6 rounded-xl transition shadow-sm text-sm"
          >
            Áp dụng bộ lọc
          </button>
          <button
            @click="handleReset"
            class="bg-gray-100 hover:bg-gray-200 text-gray-600 font-bold py-2 px-6 rounded-xl transition text-sm"
          >
            Làm mới
          </button>
        </div>
        <div class="flex items-center space-x-4">
          <button
            class="bg-gray-900 hover:bg-black text-white font-bold py-2 px-6 rounded-xl flex items-center space-x-2 transition shadow-lg text-sm"
          >
            <span>+ Thêm nhân viên</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Table -->
    <div
      class="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden"
    >
      <div class="max-h-[500px] overflow-auto">
        <div class="p-2 border-b border-gray-50">
          <h2 class="font-bold text-gray-800">Danh sách nhân viên</h2>
        </div>
        <table class="w-full text-sm">
          <thead
            class="sticky top-0 bg-gray-50/90 backdrop-blur text-[10px] font-bold text-gray-400 uppercase tracking-widest"
          >
            <tr>
              <td class="px-2 py-2">STT</td>
              <td class="px-2 py-2">Mã NV</td>
              <td class="px-2 py-2">Tên nhân viên</td>
              <td class="px-6 py-2">Liên hệ</td>
              <td class="px-2 py-2 text-center">Giới tính</td>
              <td class="px-6 py-2">Tên tài khoản</td>
              <td class="px-6 py-2 text-center">Trạng thái</td>
              <td class="px-6 py-2">Ngày cập nhật</td>
              <td class="px-8 py-2 text-right">Thao tác</td>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-50 text-sm">
            <tr
              v-for="(item, index) in employeeList"
              :key="item?.id"
              class="hover:bg-gray-50/80 transition group"
            >
              <td class="px-4 py-2">
                <div
                  class="rounded-full flex items-center text-gray-500 font-bold text-xs"
                >
                  {{ index + 1 + filters.page * filters.size }}
                </div>
              </td>
              <td class="px-2 py-2">
                <div class="font-mono text-gray-400">{{ item.ma }}</div>
              </td>
              <td class="px-2 py-4">
                <div class="flex items-center space-x-3">
                  <div>
                    <div class="font-bold text-gray-800">{{ item.ten }}</div>
                    <div class="text-xs text-gray-400">
                      {{ item.phanQuyen?.ten }}
                    </div>
                  </div>
                </div>
              </td>
              <td class="px-6 py-4">
                <div class="text-gray-600">{{ item.email }}</div>
                <div class="text-xs text-gray-400">{{ item.sdt }}</div>
              </td>
              <td class="px-2 py-2 text-center">
                <span
                  :class="item.gioiTinh ? 'text-blue-500' : 'text-pink-500'"
                  class="font-semibold"
                >
                  {{ item.gioiTinh ? "Nam" : "Nữ" }}
                </span>
              </td>
              <td class="px-6 py-4">
                <div class="text-gray-600">{{ item.tenTaiKhoan }}</div>
              </td>

              <td class="px-6 py-4 text-center">
                <span
                  :class="
                    item.trangThai === 'DANG_HOAT_DONG'
                      ? 'bg-emerald-50 text-emerald-600 border-emerald-100'
                      : 'bg-gray-50 text-gray-400 border-gray-100'
                  "
                  class="px-3 py-1 rounded-full text-[10px] font-bold border"
                >
                  {{
                    item.trangThai === "DANG_HOAT_DONG"
                      ? "HOẠT ĐỘNG"
                      : "NGỪNG HOẠT ĐỘNG"
                  }}
                </span>
              </td>
              <td class="px-6 py-4 text-xs text-gray-400">
                {{ formatDate(item.ngayCapNhat) }}
              </td>
              <td class="px-6 py-4 text-right space-x-2">
                <!-- Detail -->
                <button
                  @click="handleDetail(item)"
                  class="p-2 text-gray-400 hover:text-blue-500 transition"
                  title="Xem chi tiết"
                >
                  <i class="fas fa-eye"></i>
                </button>

                <!-- Edit / Thay đổi trạng thái -->
                <button
                  @click="handleEdit(item)"
                  class="p-2 text-gray-400 hover:text-cyan-500 transition"
                  title="Sửa"
                >
                  <i class="fas fa-edit"></i>
                </button>
              </td>
            </tr>
            <tr v-if="employeeList.length === 0">
              <td colspan="9" class="py-20 text-center text-gray-400 italic">
                Chưa có nhân viên nào phù hợp bộ lọc.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Pagination -->
    <div
      class="p-6 bg-gray-50/30 border-t border-gray-50 flex justify-between items-center"
    >
      <div class="flex items-center space-x-2 text-xs font-bold text-gray-400">
        <span>Số trang:</span>
        <span class="text-cyan-500">{{ filters.page + 1 }}</span>
        <span class="ml-4">Tổng: {{ totalPages }}</span>
        <span class="ml-4">Số dòng:</span>
        <select
          v-model="filters.size"
          @change="handleFilter"
          class="bg-transparent outline-none text-gray-600"
        >
          <option :value="5">5 dòng</option>
          <option :value="10">10 dòng</option>
          <option :value="20">20 dòng</option>
        </select>
      </div>
      <div class="flex space-x-1">
        <button
          @click="changePage(filters.page - 1)"
          :disabled="filters.page === 0"
          class="p-2 rounded-lg hover:bg-white disabled:opacity-30 text-gray-400"
        >
          <i class="fas fa-chevron-left"></i>
        </button>
        <button
          @click="changePage(filters.page + 1)"
          :disabled="filters.page >= totalPages - 1"
          class="p-2 rounded-lg hover:bg-white disabled:opacity-30 text-gray-400"
        >
          <i class="fas fa-chevron-right"></i>
        </button>
      </div>
    </div>
  </div>
  <EmployeeDetailModal
    v-if="showDetailModal"
    :employee="selectedEmployee"
    @close="showDetailModal = false"
    @edit="handleEdit"
  />
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
const router = useRouter();
import * as NhanVienService from "@/services/nhanVienService";
import EmployeeDetailModal from "./EmployeeDetailModal.vue";

const employeeList = ref([]);
const totalPages = ref(0);

const filters = reactive({
  keyword: "",
  trangThai: null,
  gioiTinh: null,
  page: 0,
  size: 5,
});

const fetchData = async () => {
  try {
    const res = await NhanVienService.filterNhanVien(filters);

    console.log("Response Backend:", res);

    // ✔ Lấy đúng dữ liệu từ backend wrapper
    const pageData = res.data; // data nằm trong res.data
    employeeList.value = pageData?.content || [];
    totalPages.value = pageData?.totalPages || 0;
  } catch (error) {
    console.error("Lỗi hiển thị nhân viên:", error);
  }
};

const changePage = (newPage) => {
  filters.page = newPage;
  fetchData();
};

const handleFilter = () => {
  filters.page = 0;
  fetchData();
};

const handleReset = () => {
  filters.keyword = "";
  filters.trangThai = null;
  filters.gioiTinh = null;
  filters.page = 0;
  fetchData();
};

const formatDate = (date) => {
  if (!date) return "---";
  return new Date(date).toLocaleDateString("vi-VN");
};

onMounted(() => {
  fetchData();
});

const handleEdit = (item) => {
  router.push({
    name: "EmployeeEdit",
    query: { id: item.id },
  });
};

const showDetailModal = ref(false);
const selectedEmployee = ref(null);

const handleDetail = (item) => {
  selectedEmployee.value = item; // gán nhân viên cần xem
  showDetailModal.value = true; // bật modal
};
</script>

<style scoped></style>
