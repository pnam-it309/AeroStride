<script setup>
import { ref, onMounted, computed, watch, onUnmounted } from 'vue'
import { Plus, Trash2, Search, User, CreditCard, Ticket, ShoppingCart, Banknote, Landmark, Printer, X, Check, Save } from 'lucide-vue-next'
import AdminBanHangService from '@/services/banHangService'
import AeroButton from '@/components/base/AeroButton.vue'
import AeroInput from '@/components/base/AeroInput.vue'
import AeroStatusBadge from '@/components/base/AeroStatusBadge.vue'
import { useLoadingStore } from '@/stores/loadingStore'

const loadingStore = useLoadingStore()

// State
const hoaDonCho = ref([])
const activeHoaDonId = ref(null)
const products = ref([])
const searchProductKey = ref('')
const customers = ref([])
const searchCustomerKey = ref('')
const vouchers = ref([])
const showVoucherList = ref(false)
const showCustomerList = ref(false)
const paymentMethod = ref('CASH') // CASH, BANK, MIXED

const checkoutData = ref({
  tienMat: 0,
  tienChuyenKhoan: 0,
  maGiaoDich: '',
  ghiChu: '',
  phiVanChuyen: 0
})

// Current Bill Data
const currentHoaDon = computed(() => 
  hoaDonCho.value.find(hd => hd.id === activeHoaDonId.value) || null
)

const changePaymentMethod = (method) => {
  paymentMethod.value = method
  if (method === 'CASH') {
    checkoutData.value.tienChuyenKhoan = 0
  } else if (method === 'BANK') {
    checkoutData.value.tienMat = 0
    checkoutData.value.tienChuyenKhoan = currentHoaDon.value?.tongTienSauGiam || 0
  }
}

// Methods
const fetchHoaDonCho = async () => {
  try {
    const res = await AdminBanHangService.getHoaDonCho()
    hoaDonCho.value = res.data
    if (hoaDonCho.value.length > 0 && !activeHoaDonId.value) {
      activeHoaDonId.value = hoaDonCho.value[0].id
    }
  } catch (error) {
    console.error('Error fetching bills:', error)
  }
}

const createHoaDon = async () => {
  try {
    const res = await AdminBanHangService.createHoaDon()
    hoaDonCho.value.push(res.data)
    activeHoaDonId.value = res.data.id
  } catch (error) {
    alert(error.response?.data?.message || 'Lỗi khi tạo hóa đơn.')
  }
}

const deleteHoaDon = async (id) => {
  if (!confirm('Bạn có chắc muốn xóa hóa đơn này?')) return
  try {
    await AdminBanHangService.deleteHoaDon(id)
    hoaDonCho.value = hoaDonCho.value.filter(hd => hd.id !== id)
    if (activeHoaDonId.value === id) {
      activeHoaDonId.value = hoaDonCho.value.length > 0 ? hoaDonCho.value[0].id : null
    }
  } catch (error) {
    console.error('Error deleting bill:', error)
  }
}

const searchProducts = async () => {
  if (!searchProductKey.value.trim()) {
    products.value = []
    return
  }
  try {
    const res = await AdminBanHangService.searchSanPham(searchProductKey.value)
    products.value = res.data
    // POS specific: If only 1 result with exact barcode match, auto add
    if (products.value.length === 1 && products.value[0].maChiTietSanPham === searchProductKey.value) {
       addToCart(products.value[0])
    }
  } catch (error) {
    console.error('Error searching products:', error)
  }
}

const addToCart = async (product) => {
  if (!activeHoaDonId.value) {
    alert('Vui lòng chọn hoặc tạo hóa đơn.')
    return
  }
  try {
    const res = await AdminBanHangService.addProduct(activeHoaDonId.value, {
      idChiTietSanPham: product.id,
      soLuong: 1
    })
    const idx = hoaDonCho.value.findIndex(hd => hd.id === activeHoaDonId.value)
    hoaDonCho.value[idx] = res.data
    searchProductKey.value = '' 
    products.value = []
  } catch (error) {
    alert(error.response?.data?.message || 'Lỗi khi thêm sản phẩm.')
  }
}

const updateQuantity = async (detailId, newQty) => {
  if (newQty <= 0) {
    removeProduct(detailId)
    return
  }
  try {
    const res = await AdminBanHangService.updateQuantity(activeHoaDonId.value, detailId, newQty)
    const idx = hoaDonCho.value.findIndex(hd => hd.id === activeHoaDonId.value)
    hoaDonCho.value[idx] = res.data
  } catch (error) {
    alert(error.response?.data?.message || 'Lỗi cập nhật số lượng.')
  }
}

const removeProduct = async (detailId) => {
  try {
    await AdminBanHangService.removeProduct(activeHoaDonId.value, detailId)
    fetchHoaDonCho()
  } catch (error) {
    console.error(error)
  }
}

const searchCustomers = async () => {
  if (!searchCustomerKey.value.trim()) return
  try {
    const res = await AdminBanHangService.searchKhachHang(searchCustomerKey.value)
    customers.value = res.data
    showCustomerList.value = true
  } catch (error) {
    console.error(error)
  }
}

const selectCustomer = async (cust) => {
  try {
    const res = await AdminBanHangService.setKhachHang(activeHoaDonId.value, cust.id)
    const idx = hoaDonCho.value.findIndex(hd => hd.id === activeHoaDonId.value)
    hoaDonCho.value[idx] = res.data
    showCustomerList.value = false
    searchCustomerKey.value = ''
  } catch (error) {
    alert('Lỗi khi gán khách hàng.')
  }
}

const removeCustomer = async () => {
    try {
    const res = await AdminBanHangService.setKhachHang(activeHoaDonId.value, null)
    const idx = hoaDonCho.value.findIndex(hd => hd.id === activeHoaDonId.value)
    hoaDonCho.value[idx] = res.data
  } catch (error) {
    alert('Lỗi khi gỡ khách hàng.')
  }
}

const fetchVouchers = async () => {
  if (!currentHoaDon.value) return
  try {
    const res = await AdminBanHangService.getVouchers(currentHoaDon.value.tongTien)
    vouchers.value = res.data
    showVoucherList.value = true
  } catch (error) {
    console.error(error)
  }
}

const selectVoucher = async (v) => {
  try {
    const res = await AdminBanHangService.setVoucher(activeHoaDonId.value, v.id)
    const idx = hoaDonCho.value.findIndex(hd => hd.id === activeHoaDonId.value)
    hoaDonCho.value[idx] = res.data
    showVoucherList.value = false
  } catch (error) {
    alert('Lỗi áp dụng voucher.')
  }
}

const removeVoucher = async () => {
    try {
    const res = await AdminBanHangService.setVoucher(activeHoaDonId.value, null)
    const idx = hoaDonCho.value.findIndex(hd => hd.id === activeHoaDonId.value)
    hoaDonCho.value[idx] = res.data
  } catch (error) {
    alert('Lỗi gỡ voucher.')
  }
}

const handleCheckout = async () => {
  if (!currentHoaDon.value) return
  if (currentHoaDon.value.listsHoaDonChiTiet.length === 0) {
    alert('Hóa đơn chưa có sản phẩm.')
    return
  }

  const totalPaid = Number(checkoutData.value.tienMat) + Number(checkoutData.value.tienChuyenKhoan)
  if (totalPaid < currentHoaDon.value.tongTienSauGiam) {
    alert('Số tiền thanh toán chưa đủ.')
    return
  }

  try {
    loadingStore.startLoading()
    await AdminBanHangService.checkout(activeHoaDonId.value, {
      ...checkoutData.value,
      idKhachHang: currentHoaDon.value.idKhachHang,
      idPhieuGiamGia: currentHoaDon.value.idPhieuGiamGia,
      tongTien: currentHoaDon.value.tongTien,
      tongTienSauGiam: currentHoaDon.value.tongTienSauGiam,
      loaiDon: 'TAI_QUAY'
    })
    alert('Thanh toán thành công! Đang in hóa đơn...')
    // Simulating Print
    activeHoaDonId.value = null
    fetchHoaDonCho()
    checkoutData.value = { tienMat: 0, tienChuyenKhoan: 0, maGiaoDich: '', ghiChu: '', phiVanChuyen: 0 }
  } catch (error) {
    alert(error.response?.data?.message || 'Lỗi khi thanh toán.')
  } finally {
    loadingStore.finishLoading()
  }
}

// Shortcuts helper
const handleKeydown = (e) => {
  if (e.key === 'F9') { // F9 for New bill
    createHoaDon()
  }
  if (e.key === 'F10') { // F10 for Checkout
    handleCheckout()
  }
}

// Watcher for search product
watch(searchProductKey, () => {
  searchProducts()
})

onMounted(() => {
  fetchHoaDonCho()
  window.addEventListener('keydown', handleKeydown)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleKeydown)
})

const formatCurrency = (val) => {
  if (!val) return '0 ₫'
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(val)
}
</script>

<template>
  <div class="h-full flex flex-col gap-6 overflow-hidden select-none">
    <!-- Header: Bill Tabs -->
    <div class="bg-white p-4 rounded-3xl shadow-sm border border-gray-100 flex items-center justify-between overflow-x-auto whitespace-nowrap scrollbar-none">
      <div class="flex items-center gap-4">
          <div
            v-for="(hd, index) in hoaDonCho"
            :key="hd.id"
            @click="activeHoaDonId = hd.id"
            class="relative px-6 py-3 rounded-2xl cursor-pointer transition-all duration-300 border flex items-center gap-4 group"
            :class="activeHoaDonId === hd.id ? 'bg-aurora text-white border-aurora shadow-lg translate-y-[-2px]' : 'bg-gray-50 border-gray-100 text-gray-500 hover:bg-white hover:border-aurora hover:text-aurora'"
          >
            <div class="flex flex-col">
              <span class="font-black text-[10px] uppercase tracking-widest leading-none mb-1">Đơn {{ index + 1 }}</span>
              <span class="text-[12px] font-bold">{{ hd.maHoaDon }}</span>
            </div>
            <button 
              v-if="hoaDonCho.length > 1"
              @click.stop="deleteHoaDon(hd.id)" 
              class="hover:bg-red-500 rounded-full p-1 transition-colors"
            >
              <X class="w-3 h-3" />
            </button>
          </div>
          <button
            v-if="hoaDonCho.length < 5"
            @click="createHoaDon"
            class="w-14 h-14 flex items-center justify-center bg-gray-50 border border-dashed border-gray-200 rounded-2xl text-gray-400 hover:border-aurora hover:text-aurora hover:bg-white transition-all hover:scale-105"
          >
            <Plus class="w-6 h-6" />
          </button>
      </div>

      <div class="flex items-center gap-4 px-4 text-gray-400">
          <div class="flex flex-col items-end">
            <span class="text-[10px] font-black uppercase tracking-widest text-aurora">Hệ thống AeroStride POS</span>
            <span class="text-[12px] font-medium italic">Standard Edition v1.0</span>
          </div>
          <div class="w-10 h-10 bg-aurora/10 rounded-xl flex items-center justify-center text-aurora">
            <Printer class="w-5 h-5" />
          </div>
      </div>
    </div>

    <!-- Main Content -->
    <div v-if="currentHoaDon" class="flex-1 grid grid-cols-12 gap-6 overflow-hidden min-h-0">
      
      <!-- Left: Products & Cart (col-8) -->
      <div class="col-span-8 flex flex-col gap-6 min-h-0">
        
        <!-- Search Products -->
        <div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 flex flex-col gap-4 relative">
          <div class="flex items-center gap-4">
            <div class="flex-1 relative group">
              <Search class="absolute left-5 top-1/2 -translate-y-1/2 w-6 h-6 text-gray-400 group-focus-within:text-aurora transition-colors" />
              <input
                v-model="searchProductKey"
                type="text"
                placeholder="Tìm sản phẩm / Quét mã vạch (Barcode)..."
                class="w-full pl-14 pr-6 py-5 bg-gray-50 border-none rounded-2xl focus:ring-4 focus:ring-aurora/10 text-lg font-bold placeholder:font-normal transition-all"
              />
            </div>
          </div>

          <!-- Dropdown Search Results -->
          <transition enter-active-class="animate-in fade-in slide-in-from-top-2 duration-300">
              <div v-if="products.length > 0" class="absolute top-[105%] left-6 right-6 bg-white border border-gray-100 rounded-3xl shadow-[0_20px_50px_rgba(0,0,0,0.1)] z-[100] max-h-[500px] overflow-y-auto custom-scrollbar">
                <div 
                  v-for="p in products" 
                  :key="p.id" 
                  @click="addToCart(p)"
                  class="flex items-center gap-6 p-5 hover:bg-aurora/[0.02] cursor-pointer border-b border-gray-50 last:border-0 group/p"
                >
                  <div class="w-16 h-16 bg-gray-50 rounded-2xl flex items-center justify-center text-aurora font-black text-xl group-hover/p:bg-aurora group-hover/p:text-white transition-all">{{ p.tenSanPham.charAt(0) }}</div>
                  <div class="flex-1">
                    <div class="font-black text-gray-900 text-lg mb-1">{{ p.tenSanPham }}</div>
                    <div class="flex items-center gap-2">
                        <span class="px-2 py-0.5 bg-gray-100 rounded-md text-[10px] font-black uppercase tracking-wider text-gray-500">{{ p.tenMauSac }}</span>
                        <span class="px-2 py-0.5 bg-gray-100 rounded-md text-[10px] font-black uppercase tracking-wider text-gray-500">{{ p.tenKichThuoc }}</span>
                        <span class="text-xs text-gray-400 ml-2">SKU: {{ p.maChiTietSanPham }}</span>
                    </div>
                  </div>
                  <div class="text-right">
                    <div class="font-black text-2xl text-aurora">{{ formatCurrency(p.giaBan) }}</div>
                    <div class="text-[12px] font-bold text-gray-400">Trong kho: {{ p.soLuongTon }}</div>
                  </div>
                </div>
              </div>
          </transition>
        </div>

        <!-- Cart Table -->
        <div class="flex-1 bg-white rounded-3xl shadow-sm border border-gray-100 flex flex-col min-h-0 relative overflow-hidden">
          <div class="p-6 border-b border-gray-50 flex items-center justify-between bg-white z-10">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-xl bg-aurora/10 flex items-center justify-center text-aurora">
                 <ShoppingCart class="w-5 h-5" />
              </div>
              <span class="font-black text-gray-900 uppercase tracking-widest text-sm">Giỏ hàng thanh toán</span>
            </div>
            <AeroStatusBadge :status="currentHoaDon.listsHoaDonChiTiet.length > 0 ? 'DANG_HOAT_DONG' : 'KHONG_HOAT_DONG'">
                {{ currentHoaDon.listsHoaDonChiTiet.length }} sản phẩm
            </AeroStatusBadge>
          </div>
          
          <div class="flex-1 overflow-y-auto p-2 custom-scrollbar">
            <template v-if="currentHoaDon.listsHoaDonChiTiet.length > 0">
              <table class="w-full border-separate border-spacing-y-2 px-4">
                <thead class="sticky top-0 bg-white z-10">
                  <tr class="text-left text-[11px] font-black uppercase tracking-[0.2em] text-gray-400">
                    <th class="px-6 py-4">Sản Phẩm</th>
                    <th class="px-6 py-4">Đơn giá</th>
                    <th class="px-6 py-4">Số Lượng</th>
                    <th class="px-6 py-4 text-right">Tổng cộng</th>
                    <th class="px-6 py-4"></th>
                  </tr>
                </thead>
                <tbody class="space-y-4">
                  <tr v-for="item in currentHoaDon.listsHoaDonChiTiet" :key="item.id" class="group hover:scale-[1.01] transition-all duration-300">
                    <td class="px-6 py-6 bg-gray-50 rounded-l-3xl">
                      <div class="font-black text-gray-900 text-md">{{ item.tenSanPham }}</div>
                      <div class="text-[11px] font-bold text-gray-400 uppercase tracking-widest mt-1">{{ item.tenMauSac }} • {{ item.tenKichThuoc }}</div>
                    </td>
                    <td class="px-6 py-6 bg-gray-50 font-bold text-gray-600">{{ formatCurrency(item.donGia) }}</td>
                    <td class="px-6 py-6 bg-gray-50">
                      <div class="flex items-center gap-4 bg-white p-1 rounded-2xl border border-gray-100 shadow-sm w-fit">
                        <button @click="updateQuantity(item.id, item.soLuong - 1)" class="w-8 h-8 rounded-xl bg-gray-50 hover:bg-red-500 hover:text-white transition-all">-</button>
                        <span class="w-6 text-center font-black text-gray-900">{{ item.soLuong }}</span>
                        <button @click="updateQuantity(item.id, item.soLuong + 1)" class="w-8 h-8 rounded-xl bg-gray-50 hover:bg-aurora hover:text-white transition-all">+</button>
                      </div>
                    </td>
                    <td class="px-6 py-6 bg-gray-50 text-right font-black text-aurora text-lg">{{ formatCurrency(item.thanhTien) }}</td>
                    <td class="px-6 py-6 bg-gray-50 rounded-r-3xl">
                      <button @click="removeProduct(item.id)" class="w-10 h-10 rounded-xl flex items-center justify-center text-gray-300 hover:bg-red-50 hover:text-red-500 transition-all">
                        <Trash2 class="w-5 h-5" />
                      </button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </template>
            <div v-else class="h-full flex flex-col items-center justify-center opacity-30 select-none pointer-events-none">
              <div class="w-32 h-32 bg-gray-50 rounded-3xl flex items-center justify-center mb-6">
                 <ShoppingCart class="w-16 h-16 text-gray-300" />
              </div>
              <div class="font-black uppercase tracking-widest text-lg">Giỏ hàng đang trống</div>
              <p class="text-sm mt-2">Vui lòng quét mã hoặc tìm sản phẩm để bắt đầu</p>
            </div>
          </div>
        </div>
      </div>

      <!-- Right: Checkout Sidebar (col-4) -->
      <div class="col-span-4 flex flex-col gap-6 min-h-0">
        
        <!-- Customer & Voucher Panel -->
        <div class="bg-white p-6 rounded-3xl shadow-sm border border-gray-100 flex flex-col gap-6">
          
          <!-- Customer -->
          <div>
              <div class="flex items-center justify-between mb-3 px-1">
                <div class="flex items-center gap-3">
                  <User class="text-aurora w-5 h-5" />
                  <span class="font-black text-gray-900 uppercase tracking-widest text-[11px]">Thông tin khách hàng</span>
                </div>
                <button v-if="currentHoaDon.idKhachHang" @click="removeCustomer" class="text-xs font-bold text-red-500 hover:underline">Gỡ</button>
              </div>
              
              <div v-if="currentHoaDon.idKhachHang" class="p-4 bg-aurora/5 border border-aurora/20 rounded-2xl flex items-center gap-4 animate-in zoom-in-95 duration-200">
                <div class="w-12 h-12 bg-aurora rounded-2xl flex items-center justify-center text-white font-black text-xl shadow-lg shadow-aurora/30">
                  {{ (currentHoaDon.tenKhachHang || 'K').charAt(0) }}
                </div>
                <div class="flex-1 truncate">
                  <div class="text-md font-black text-gray-900 truncate">{{ currentHoaDon.tenKhachHang }}</div>
                  <div class="text-[12px] font-bold text-aurora">{{ currentHoaDon.sdtKhachHang }}</div>
                </div>
                <Check class="w-5 h-5 text-aurora" />
              </div>
              
              <div v-else class="relative space-y-3">
                  <div class="relative group">
                    <Search class="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400 group-focus-within:text-aurora" />
                    <input 
                      v-model="searchCustomerKey"
                      @keyup.enter="searchCustomers"
                      placeholder="Tìm theo tên / SĐT khách hàng..." 
                      class="w-full pl-10 pr-4 py-3 bg-gray-50 border-none rounded-xl text-sm focus:ring-2 focus:ring-aurora transition-all"
                    />
                  </div>
                  
                  <transition enter-active-class="animate-in fade-in slide-in-from-top-2">
                      <div v-if="showCustomerList && customers.length > 0" class="absolute top-[105%] left-0 right-0 bg-white border border-gray-100 rounded-2xl shadow-xl z-50 p-2 space-y-1">
                        <div 
                          v-for="c in customers" 
                          :key="c.id"
                          @click="selectCustomer(c)"
                          class="p-3 hover:bg-gray-50 rounded-xl cursor-pointer flex items-center gap-3 group"
                        >
                          <div class="w-8 h-8 rounded-lg bg-gray-100 flex items-center justify-center text-gray-400 group-hover:bg-aurora group-hover:text-white transition-all uppercase font-bold">{{ c.tenKhachHang.charAt(0) }}</div>
                          <div class="flex-1">
                            <div class="text-xs font-bold text-gray-900">{{ c.tenKhachHang }}</div>
                            <div class="text-[10px] text-gray-400">{{ c.sdt }}</div>
                          </div>
                        </div>
                      </div>
                  </transition>
              </div>
          </div>

          <!-- Voucher -->
          <div class="border-t border-gray-50 pt-6">
              <div class="flex items-center justify-between mb-3 px-1">
                <div class="flex items-center gap-3">
                  <Ticket class="text-aurora w-5 h-5" />
                  <span class="font-black text-gray-900 uppercase tracking-widest text-[11px]">Phiếu giảm giá</span>
                </div>
                <button v-if="currentHoaDon.idPhieuGiamGia" @click="removeVoucher" class="text-xs font-bold text-red-500 hover:underline">Gỡ bỏ</button>
              </div>

              <div v-if="currentHoaDon.idPhieuGiamGia" class="px-4 py-3 bg-red-50 border border-red-100 rounded-2xl flex items-center gap-4">
                  <Ticket class="w-6 h-6 text-red-500" />
                  <div class="flex-1">
                    <span class="text-xs font-black text-red-500 uppercase tracking-widest">Đã áp dụng mã giảm giá</span>
                  </div>
                  <div class="font-black text-red-600 text-sm">
                      - {{ formatCurrency(currentHoaDon.tongTien - currentHoaDon.tongTienSauGiam) }}
                  </div>
              </div>

              <AeroButton v-else variant="outline" class="w-full justify-start px-4 h-12 rounded-xl group/v" @click="fetchVouchers">
                  <Plus class="w-4 h-4 mr-2 text-gray-400 group-hover/v:text-aurora" />
                  <span class="text-xs font-bold text-gray-400 group-hover/v:text-gray-900">Chọn mã giảm giá khả dụng</span>
              </AeroButton>

              <!-- Voucher Selection Bottom Modal Style -->
              <transition enter-active-class="animate-in fade-in zoom-in-95 duration-200">
                  <div v-if="showVoucherList" class="fixed inset-0 bg-black/20 backdrop-blur-sm z-[200] flex items-center justify-center p-4">
                    <div class="bg-white w-full max-w-md rounded-[40px] shadow-2xl p-8 flex flex-col max-h-[80vh]">
                        <div class="flex items-center justify-between mb-8">
                            <span class="text-xl font-black uppercase tracking-[0.2em] text-gray-900">Mã giảm giá</span>
                            <button @click="showVoucherList = false" class="p-2 hover:bg-gray-100 rounded-2xl transition-all"><X class="w-6 h-6"/></button>
                        </div>
                        <div class="flex-1 overflow-y-auto space-y-4 custom-scrollbar pr-2">
                            <div v-for="v in vouchers" :key="v.id" @click="selectVoucher(v)" class="p-6 border-2 border-gray-50 hover:border-aurora hover:bg-aurora/[0.02] rounded-3xl cursor-pointer transition-all flex items-center gap-6 group/vi">
                                <div class="w-14 h-14 bg-red-50 rounded-2xl flex items-center justify-center text-red-500 group-hover/vi:bg-red-500 group-hover/vi:text-white transition-all shadow-sm">
                                    <Ticket class="w-7 h-7" />
                                </div>
                                <div class="flex-1">
                                    <div class="font-black text-gray-900 uppercase tracking-widest text-sm mb-1">{{ v.maPhieuGiamGia }}</div>
                                    <div class="text-xs font-bold text-red-500">{{ v.loaiPhieu === 'PERCENT' ? 'Giảm ' + v.phanTramGiamGia + '%' : 'Giảm ' + formatCurrency(v.soTienGiam) }}</div>
                                    <div class="text-[10px] text-gray-400 mt-2 uppercase tracking-tighter">Đơn tối thiểu: {{ formatCurrency(v.donHangToiThieu) }}</div>
                                </div>
                                <div class="w-8 h-8 rounded-full border-2 border-gray-100 flex items-center justify-center group-hover/vi:border-aurora group-hover/vi:bg-aurora transition-all">
                                    <div class="w-2 h-2 rounded-full bg-white opacity-0 group-hover/vi:opacity-100"></div>
                                </div>
                            </div>
                            <div v-if="vouchers.length === 0" class="py-12 text-center text-gray-400 font-bold">Không có mã giảm giá nào khả dụng</div>
                        </div>
                    </div>
                  </div>
              </transition>
          </div>
        </div>

        <!-- Payment & Checkout Panel -->
        <div class="flex-1 bg-white rounded-3xl shadow-sm border border-gray-100 flex flex-col min-h-0 relative">
          <div class="p-8 pb-4 flex items-center gap-4">
             <div class="w-12 h-12 bg-aurora rounded-2xl flex items-center justify-center text-white shadow-lg shadow-aurora/30">
               <CreditCard class="w-6 h-6" />
             </div>
             <div class="flex flex-col">
               <span class="text-[10px] font-black uppercase tracking-[0.3em] text-gray-400">Bước cuối cùng</span>
               <span class="text-xl font-black text-gray-900 tracking-tight">Thanh toán hóa đơn</span>
             </div>
          </div>

          <div class="flex-1 overflow-y-auto px-8 py-4 space-y-6 custom-scrollbar">
            <!-- Price Summary -->
            <div class="bg-gray-50 rounded-[32px] p-6 space-y-4">
                <div class="flex justify-between items-center text-gray-500">
                  <span class="text-sm font-bold">Tổng tiền hàng</span>
                  <span class="font-black text-gray-900">{{ formatCurrency(currentHoaDon.tongTien) }}</span>
                </div>
                <div class="flex justify-between items-center text-gray-500">
                  <span class="text-sm font-bold">Giảm giá</span>
                  <span class="font-black text-red-500">- {{ formatCurrency(currentHoaDon.tongTien - currentHoaDon.tongTienSauGiam) }}</span>
                </div>
                <div class="h-0.5 bg-gray-200/50 w-full"></div>
                <div class="flex justify-between items-center">
                  <span class="text-sm font-black uppercase tracking-widest text-gray-400">Thực thu</span>
                  <span class="font-black text-3xl text-aurora">{{ formatCurrency(currentHoaDon.tongTienSauGiam) }}</span>
                </div>
            </div>

            <!-- Payment Methods Toggle -->
            <div class="space-y-4">
               <div class="text-[10px] font-black uppercase tracking-[0.2em] text-gray-400 px-1">Hình thức thanh toán</div>
               <div class="flex p-1 bg-gray-50 rounded-2xl gap-1">
                  <button 
                    @click="changePaymentMethod('CASH')"
                    class="flex-1 flex items-center justify-center gap-2 py-3 rounded-xl transition-all font-black text-[11px] uppercase tracking-widest"
                    :class="paymentMethod === 'CASH' ? 'bg-white text-aurora shadow-sm' : 'text-gray-400 hover:text-gray-600'"
                  >
                    <Banknote class="w-4 h-4" /> Tiền mặt
                  </button>
                  <button 
                     @click="changePaymentMethod('BANK')"
                    class="flex-1 flex items-center justify-center gap-2 py-3 rounded-xl transition-all font-black text-[11px] uppercase tracking-widest"
                    :class="paymentMethod === 'BANK' ? 'bg-white text-aurora shadow-sm' : 'text-gray-400 hover:text-gray-600'"
                  >
                    <Landmark class="w-4 h-4" /> Chuyển khoản
                  </button>
                  <button 
                     @click="changePaymentMethod('MIXED')"
                    class="flex-1 flex items-center justify-center gap-2 py-3 rounded-xl transition-all font-black text-[11px] uppercase tracking-widest text-nowrap"
                    :class="paymentMethod === 'MIXED' ? 'bg-white text-aurora shadow-sm' : 'text-gray-400 hover:text-gray-600'"
                  >
                    <CreditCard class="w-4 h-4" /> Kết hợp
                  </button>
               </div>
            </div>

            <!-- Payment Inputs based on method -->
            <div class="space-y-4 animate-in fade-in duration-300">
               <div v-if="paymentMethod === 'CASH' || paymentMethod === 'MIXED'" class="space-y-2">
                  <label class="text-[10px] font-black uppercase text-gray-400 px-1">Tiền mặt khách đưa</label>
                  <AeroInput v-model="checkoutData.tienMat" type="number" placeholder="0 ₫" class="text-xl font-black"/>
                  <div class="flex justify-between px-1">
                    <span class="text-xs font-bold text-gray-400 italic">Tiền thừa trả khách:</span>
                    <span class="text-sm font-black text-emerald-500">{{ formatCurrency(Math.max(0, (Number(checkoutData.tienMat) + Number(checkoutData.tienChuyenKhoan)) - currentHoaDon.tongTienSauGiam)) }}</span>
                  </div>
               </div>

               <div v-if="paymentMethod === 'BANK' || paymentMethod === 'MIXED'" class="space-y-2">
                  <label class="text-[10px] font-black uppercase text-gray-400 px-1">Số tiền chuyển khoản</label>
                  <AeroInput v-model="checkoutData.tienChuyenKhoan" type="number" placeholder="0 ₫" class="text-xl font-black"/>
                  <AeroInput v-model="checkoutData.maGiaoDich" placeholder="Mã giao dịch / Nội dung chuyển khoản..." size="sm" />
               </div>

               <div class="pt-2">
                 <label class="text-[10px] font-black uppercase text-gray-400 px-1">Ghi chú đơn hàng</label>
                 <textarea v-model="checkoutData.ghiChu" class="w-full mt-1 p-4 bg-gray-50 border-none rounded-2xl text-sm focus:ring-2 focus:ring-aurora placeholder:italic" rows="2" placeholder="Ví dụ: Khách hẹn lấy sau..."></textarea>
               </div>
            </div>
          </div>

          <!-- Final Checkout Button -->
          <div class="p-8 bg-white border-t border-gray-50 rounded-b-3xl">
              <AeroButton 
                @click="handleCheckout"
                class="w-full h-20 rounded-[32px] text-xl font-black uppercase tracking-[0.3em] shadow-[0_20px_40px_rgba(6,182,212,0.4)] group overflow-hidden relative"
              >
                 <div class="absolute inset-0 bg-white/10 translate-x-[-100%] group-hover:translate-x-[100%] transition-transform duration-1000 skew-x-[-20deg]"></div>
                 Xác nhận & In (F10)
              </AeroButton>
              <div class="mt-6 flex items-center justify-center gap-6">
                  <div class="flex items-center gap-2 text-gray-400">
                    <kbd class="px-2 py-1 bg-gray-100 rounded text-[10px] font-black">F9</kbd>
                    <span class="text-[10px] font-bold uppercase italic">Mới</span>
                  </div>
                  <div class="flex items-center gap-2 text-gray-400">
                    <kbd class="px-2 py-1 bg-gray-100 rounded text-[10px] font-black">ESC</kbd>
                    <span class="text-[10px] font-bold uppercase italic">Hủy</span>
                  </div>
                  <div class="flex items-center gap-2 text-gray-400">
                    <kbd class="px-2 py-1 bg-gray-100 rounded text-[10px] font-black">F10</kbd>
                    <span class="text-[10px] font-bold uppercase italic">Chốt</span>
                  </div>
              </div>
          </div>
        </div>

      </div>
    </div>

    <!-- Empty State if no bills -->
    <div v-else class="flex-1 flex flex-col items-center justify-center opacity-40 select-none">
       <div class="w-64 h-64 bg-gray-50 rounded-[80px] flex items-center justify-center mb-12 shadow-inner">
         <ShoppingCart class="w-32 h-32 text-gray-200" />
       </div>
       <div class="text-4xl font-black uppercase tracking-[0.4em] text-gray-300">AeroStride POS</div>
       <p class="text-gray-400 mt-6 max-w-sm text-center font-medium">Bắt đầu phiên bán hàng của bạn bằng cách thêm một hóa đơn chờ mới với phím tắt <strong>F9</strong>.</p>
       <AeroButton @click="createHoaDon" class="mt-12 px-16 h-16 rounded-[30px] font-black uppercase tracking-widest text-lg shadow-xl translate-y-0 active:translate-y-1 transition-all">Tạo hóa đơn mới</AeroButton>
    </div>
  </div>
</template>

<style scoped>
.shadow-aurora {
  box-shadow: 0 15px 35px -5px rgba(6, 182, 212, 0.4);
}
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
  background: transparent;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.04);
  border-radius: 20px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.1);
}
.scrollbar-none::-webkit-scrollbar {
  display: none;
}
@keyframes slide-up {
  from { transform: translateY(10px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
.animate-slide-up {
  animation: slide-up 0.4s ease-out;
}
</style>
