<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { getOrders } from '@/services/hoaDonService';

const router = useRouter();
const loading = ref(false);

const pageResponse = ref({
  content: [],
  totalPages: 0,
  totalElements: 0,
  number: 0,
  size: 10
});

const filters = ref({
  search: '',
  tenKhachHang: '',
  trangThai: '',
  loaiDon: '',
  ngayTao: '',
  page: 0,
  size: 10
});

const fetchData = async () => {
  loading.value = true;
  try {
    const res = await getOrders(filters.value);
    pageResponse.value = res;
  } catch (error) {
    console.error("Lỗi lấy danh sách hóa đơn:", error);
  } finally {
    loading.value = false;
  }
};

onMounted(fetchData);

const formatCurrency = (value) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value || 0);
};

const getStatusClass = (status) => {
  const map = {
    'CHO_XAC_NHAN': 'bg-blue-50 text-blue-500 border-blue-100',
    'DA_XAC_NHAN': 'bg-cyan-50 text-cyan-600 border-cyan-100',
    'DANG_XU_LI': 'bg-orange-50 text-orange-500 border-orange-100',
    'HOAN_THANH': 'bg-green-50 text-green-500 border-green-100',
    'DA_HUY': 'bg-red-50 text-red-500 border-red-100'
  };
  return map[status] || 'bg-gray-50 text-gray-400 border-gray-100';
};

const handleViewDetail = (id) => {
  router.push({ name: 'AdminOrderDetail', params: { id: id } });
};

const handleReset = () => {
  filters.value = { search: '', tenKhachHang: '', trangThai: '', loaiDon: '', ngayTao: '', page: 0, size: 10 };
  fetchData();
};

const handleFilter = () => {
  filters.value.page = 0;
  fetchData();
};

const goToPage = (p) => {
  if (p >= 0 && p < pageResponse.value.totalPages) {
    filters.value.page = p;
    fetchData();
  }
};
</script>

<template>
  <div class="p-8 bg-[#f8fafc] min-h-screen font-sans text-gray-600">
    <div class="mb-6 flex justify-between items-center">
      <h1 class="text-2xl font-bold text-slate-800 tracking-tight">Quản lý hóa đơn</h1>
      <div class="text-[11px] text-gray-400 font-medium italic">
        Tổng số hóa đơn: <span class="font-bold text-slate-800">{{ pageResponse.totalElements }}</span>
      </div>
    </div>

    <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm mb-6">
      <div class="flex items-center gap-2 mb-6 text-slate-800 font-bold text-sm">
        <i class="fas fa-filter text-xs text-cyan-500"></i> Bộ Lọc Tìm Kiếm
      </div>
      
      <div class="grid grid-cols-4 gap-4 mb-4">
        <div class="space-y-2">
          <label class="text-[11px] font-semibold text-gray-400 ml-1">Mã hóa đơn</label>
          <input v-model="filters.search" type="text" placeholder="Nhập mã hóa đơn..." class="w-full px-4 py-2.5 bg-white border border-gray-200 rounded-xl focus:outline-none focus:border-cyan-400 text-xs transition" />
        </div>
        <div class="space-y-2">
          <label class="text-[11px] font-semibold text-gray-400 ml-1">Khách hàng</label>
          <input v-model="filters.tenKhachHang" type="text" placeholder="Tên khách hàng..." class="w-full px-4 py-2.5 bg-white border border-gray-200 rounded-xl focus:outline-none focus:border-cyan-400 text-xs transition" />
        </div>
        <div class="space-y-2">
          <label class="text-[11px] font-semibold text-gray-400 ml-1">Trạng thái hóa đơn</label>
          <select v-model="filters.trangThai" class="w-full px-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:outline-none cursor-pointer hover:border-cyan-400 transition">
            <option value="">Tất cả trạng thái</option>
            <option value="CHO_XAC_NHAN">Chờ xác nhận</option>
            <option value="DA_XAC_NHAN">Đã xác nhận</option>
            <option value="DANG_XU_LI">Đang xử lí</option>
            <option value="HOAN_THANH">Hoàn thành</option>
            <option value="DA_HUY">Đã hủy</option>
          </select>
        </div>
        <div class="space-y-2">
          <label class="text-[11px] font-semibold text-gray-400 ml-1">Loại hóa đơn</label>
          <select v-model="filters.loaiDon" class="w-full px-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:outline-none cursor-pointer hover:border-cyan-400 transition">
            <option value="">Tất cả loại</option>
            <option value="ONLINE">ONLINE</option>
            <option value="TAI_QUAY">TẠI QUẦY</option>
          </select>
        </div>
      </div>

      <div class="flex justify-between items-end mt-6">
        <div class="space-y-2 w-1/4">
          <label class="text-[11px] font-semibold text-gray-400 ml-1">Ngày tạo</label>
          <input v-model="filters.ngayTao" type="date" class="w-full px-4 py-2.5 bg-white border border-gray-200 rounded-xl text-xs focus:outline-none focus:border-cyan-400" />
        </div>
        <button @click="handleFilter" class="bg-cyan-500 text-white px-6 py-2.5 rounded-xl text-xs font-bold flex items-center gap-2 hover:bg-cyan-600 transition active:scale-95 shadow-lg shadow-cyan-100 mr-2">
          <i class="fas fa-search text-[10px]"></i> Áp dụng
        </button>
        <button @click="handleReset" class="bg-slate-700 text-white px-6 py-2.5 rounded-xl text-xs font-bold flex items-center gap-2 hover:bg-black transition active:scale-95 shadow-lg shadow-slate-100">
          <i class="fas fa-sync-alt text-[10px]"></i> Làm mới bộ lọc
        </button>
      </div>
    </div>

    <div class="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
      <div v-if="loading" class="p-20 text-center text-gray-400">
        <i class="fas fa-spinner fa-spin mr-2"></i> Đang tải dữ liệu...
      </div>
      <div v-else class="overflow-x-auto">
        <table class="w-full text-left border-collapse">
          <thead>
            <tr class="bg-slate-50 text-[10px] text-gray-400 font-bold uppercase tracking-widest border-b border-gray-100">
              <th class="p-5 text-center w-16">STT</th>
              <th class="p-5">Mã đơn</th>
              <th class="p-5">Nhân viên</th>
              <th class="p-5">Khách hàng</th>
              <th class="p-5">Ngày tạo</th>
              <th class="p-5 text-right">Tổng tiền</th>
              <th class="p-5 text-center">Loại</th>
              <th class="p-5 text-center">Trạng thái</th>
              <th class="p-5 text-center w-24">Hành động</th>
            </tr>
          </thead>
          <tbody class="text-xs font-medium text-slate-600">
            <tr v-for="(item, index) in pageResponse.content" :key="item.id" class="border-t border-gray-50 hover:bg-slate-50/50 transition">
              <td class="p-5 text-center text-gray-300 font-normal">{{ index + 1 + (pageResponse.number * pageResponse.size) }}</td>
              <td class="p-5 font-bold text-cyan-600 underline underline-offset-4 decoration-cyan-100">{{ item.id }}</td>
              <td class="p-5 text-slate-500">{{ item.nhanVien }}</td>
              <td class="p-5 font-bold text-slate-700">{{ item.khachHang }}</td>
              <td class="p-5 text-gray-400 font-normal">{{ item.ngayTao }}</td>
              <td class="p-5 text-right font-black text-orange-500 text-sm tracking-tighter italic">
                {{ typeof item.tongTien === 'number' ? formatCurrency(item.tongTien) : item.tongTien }}
              </td>
              <td class="p-5 text-center">
                <span :class="item.loaiDon === 'ONLINE' ? 'bg-indigo-50 text-indigo-500 border-indigo-100' : 'bg-amber-50 text-amber-600 border-amber-100'"
                      class="px-2 py-1 rounded text-[9px] font-black border italic uppercase">
                  {{ item.loaiDon }}
                </span>
              </td>
              <td class="p-5 text-center">
                <span class="px-3 py-1 rounded-full text-[10px] font-bold border inline-block min-w-[110px] text-center transition-all" 
                      :class="getStatusClass(item.trangThai)">
                  {{ item.trangThai }}
                </span>
              </td>
              <td class="p-5 text-center">
                <button @click="handleViewDetail(item.id)" class="w-9 h-9 rounded-xl bg-[#1e293b] text-white flex items-center justify-center hover:bg-cyan-500 transition shadow-md active:scale-90">
                  <i class="fas fa-eye text-[10px]"></i>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="px-6 py-5 bg-white border-t border-gray-50 flex items-center justify-between">
        <div class="text-[11px] text-gray-400 font-medium italic">
          Trang <span class="text-slate-700 font-bold">{{ pageResponse.number + 1 }}</span> 
          / <span class="text-slate-700 font-bold">{{ pageResponse.totalPages }}</span>
        </div>

        <div class="flex items-center gap-3">
          <button 
            @click="goToPage(pageResponse.number - 1)" 
            :disabled="pageResponse.number === 0"
            class="w-10 h-10 flex items-center justify-center rounded-xl border border-gray-200 bg-white text-slate-600 shadow-sm hover:border-cyan-400 hover:text-cyan-500 hover:shadow-md disabled:opacity-20 disabled:shadow-none transition-all duration-200 cursor-pointer"
          >
            <i class="fas fa-chevron-left text-xs"></i>
          </button>

          <div class="flex gap-1.5">
            <button 
              v-for="p in pageResponse.totalPages" 
              :key="p" 
              @click="goToPage(p - 1)"
              :class="pageResponse.number === p - 1 
                ? 'bg-[#1e293b] text-white scale-110 shadow-lg shadow-slate-300 z-10' 
                : 'bg-slate-50/50 text-gray-400 hover:bg-slate-100 hover:text-slate-600 border border-transparent'"
              class="w-9 h-9 rounded-xl text-[11px] font-bold transition-all duration-200 cursor-pointer"
            >
              {{ p }}
            </button>
          </div>

          <button 
            @click="goToPage(pageResponse.number + 1)" 
            :disabled="pageResponse.number === pageResponse.totalPages - 1"
            class="w-10 h-10 flex items-center justify-center rounded-xl border border-gray-200 bg-white text-slate-600 shadow-sm hover:border-cyan-400 hover:text-cyan-500 hover:shadow-md disabled:opacity-20 disabled:shadow-none transition-all duration-200 cursor-pointer"
          >
            <i class="fas fa-chevron-right text-xs"></i>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>