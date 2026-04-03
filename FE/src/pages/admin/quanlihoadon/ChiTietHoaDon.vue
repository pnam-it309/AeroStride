<template>
  <div class="p-8 bg-[#f8fafc] min-h-screen font-sans text-gray-600">
    <div class="mb-6 flex justify-between items-center">
      <div>
        <h1 class="text-2xl font-bold text-slate-800 tracking-tight">Chi tiết đơn hàng</h1>
        <div class="flex gap-4 mt-1 text-[11px] text-gray-400 font-medium">
          <span>Mã hóa đơn: <span class="text-slate-700 font-bold">HD00292</span></span>
          <span>Ngày tạo: <span class="text-slate-700">16:42 23/12/2025</span></span>
        </div>
      </div>
      <button @click="$router.push('/admin/hoa-don')" class="bg-slate-500 text-white px-4 py-2 rounded-xl text-[11px] font-bold hover:bg-slate-600 transition shadow-sm">
        ← Quay lại danh sách
      </button>
    </div>

    <div class="grid grid-cols-3 gap-6">
      <div class="col-span-2 space-y-6">
        
        <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
          <div class="flex items-center gap-2 mb-8 text-slate-800 font-bold text-sm">
            <i class="fas fa-truck text-cyan-500 text-xs"></i> Trạng thái đơn hàng
          </div>
          <div class="relative flex justify-between items-start px-10">
            <div class="absolute top-4 left-20 right-20 h-[2px] bg-gray-100 z-0">
              <div class="h-full bg-cyan-400 w-1/2"></div> </div>
            
            <div v-for="(step, i) in steps" :key="i" class="relative z-10 flex flex-col items-center group">
              <div :class="i <= currentStep ? 'bg-cyan-400' : 'bg-gray-200'" class="w-8 h-8 rounded-full flex items-center justify-center text-white text-[10px] shadow-sm transition-all">
                <i :class="step.icon"></i>
              </div>
              <div class="mt-2 text-center">
                <p class="text-[10px] font-black uppercase tracking-tighter" :class="i <= currentStep ? 'text-cyan-500' : 'text-gray-300'">{{ step.name }}</p>
                <p class="text-[9px] text-gray-300 italic">{{ step.time }}</p>
              </div>
            </div>
          </div>
        </div>

        <div class="grid grid-cols-2 gap-6">
          <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm h-full">
            <div class="flex items-center gap-2 mb-4 text-slate-800 font-bold text-sm border-b border-gray-50 pb-3">
              <i class="fas fa-user text-cyan-500 text-xs"></i> Thông tin khách hàng
            </div>
            <div class="space-y-3 text-xs">
              <div class="flex justify-between"><span class="text-gray-400">Tên khách hàng:</span><span class="font-bold">Nguyễn Văn A</span></div>
              <div class="flex justify-between"><span class="text-gray-400">Số điện thoại:</span><span class="font-bold">0123456789</span></div>
              <div class="flex justify-between"><span class="text-gray-400">Email:</span><span class="font-bold">vana@example.com</span></div>
            </div>
          </div>
          <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm h-full">
            <div class="flex items-center gap-2 mb-4 text-slate-800 font-bold text-sm border-b border-gray-50 pb-3">
              <i class="fas fa-shipping-fast text-cyan-500 text-xs"></i> Thông tin giao hàng
            </div>
            <div class="space-y-3 text-xs">
              <div class="flex justify-between"><span class="text-gray-400">Địa chỉ:</span><span class="font-bold text-right w-40">123, Phương Đông Ngạc, Bắc Từ Liêm, Hà Nội</span></div>
              <div class="flex justify-between"><span class="text-gray-400">Loại đơn:</span><span class="px-2 py-0.5 rounded bg-blue-50 text-blue-500 font-bold text-[9px]">Giao hàng</span></div>
              <div class="flex justify-between"><span class="text-gray-400">Ghi chú:</span><span class="font-medium text-gray-400">---</span></div>
            </div>
          </div>
        </div>

        <div class="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
          <div class="p-6 border-b border-gray-50 flex items-center gap-2 text-slate-800 font-bold text-sm">
            <i class="fas fa-box text-cyan-500 text-xs"></i> Danh sách sản phẩm <span class="text-[10px] text-gray-300 ml-2">(2 sản phẩm)</span>
          </div>
          <table class="w-full text-left text-xs">
            <thead>
              <tr class="bg-slate-50/50 text-[10px] text-gray-400 font-bold uppercase tracking-wider">
                <th class="p-4 text-center w-12">STT</th>
                <th class="p-4">Sản phẩm</th>
                <th class="p-4">Kích cỡ/Màu</th>
                <th class="p-4 text-center">Số lượng</th>
                <th class="p-4 text-right">Đơn giá</th>
                <th class="p-4 text-right">Thành tiền</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="i in 2" :key="i" class="border-t border-gray-50 hover:bg-slate-50/30 transition">
                <td class="p-4 text-center text-gray-400">{{ i }}</td>
                <td class="p-4">
                  <div class="font-bold text-slate-700">Giày Thể Thao AeroStride X{{i}}</div>
                  <div class="text-[10px] text-gray-300 italic">Mã: CTSP{{i}}23</div>
                </td>
                <td class="p-4">
                  <span class="text-gray-500">42 / Trắng</span>
                </td>
                <td class="p-4 text-center font-bold">1</td>
                <td class="p-4 text-right text-gray-500">1.500.000 đ</td>
                <td class="p-4 text-right font-bold text-slate-700">1.500.000 đ</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="space-y-6">
        <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
          <div class="flex items-center gap-2 mb-6 text-slate-800 font-bold text-sm border-b border-gray-50 pb-3">
            <i class="fas fa-receipt text-cyan-500 text-xs"></i> Tổng kết thanh toán
          </div>
          <div class="space-y-4 text-xs font-medium">
            <div class="flex justify-between text-gray-400"><span>Tổng tiền hàng:</span><span class="text-slate-700">3.000.000 đ</span></div>
            <div class="flex justify-between text-gray-400"><span>Giảm giá:</span><span class="text-red-500">100.000 đ</span></div>
            <div class="flex justify-between text-gray-400"><span>Phí vận chuyển:</span><span class="text-slate-700">40.000 đ</span></div>
            <div class="pt-4 border-t border-dashed border-gray-100 flex justify-between items-end">
              <span class="text-slate-800 font-bold uppercase text-[10px]">Tổng tiền:</span>
              <div class="text-right">
                <div class="text-2xl font-black text-orange-500 tracking-tighter">2.940.000 đ</div>
                <div class="text-[9px] text-orange-400 font-bold uppercase mt-1">CHỜ GIAO HÀNG</div>
              </div>
            </div>
          </div>
        </div>

        <div class="bg-white p-6 rounded-2xl border border-gray-100 shadow-sm">
          <div class="flex items-center gap-2 mb-4 text-slate-800 font-bold text-sm">
            <i class="fas fa-history text-cyan-500 text-xs"></i> Lịch sử thanh toán
          </div>
          <div class="py-10 text-center border-2 border-dashed border-gray-50 rounded-xl text-gray-300 text-[11px] font-medium">
             Chưa có lịch sử thanh toán
          </div>
        </div>

        <div class="space-y-3">
          <button class="w-full bg-blue-600 text-white py-3 rounded-xl text-xs font-bold shadow-lg shadow-blue-100 hover:bg-blue-700 transition active:scale-95 flex items-center justify-center gap-2">
            <i class="fas fa-print"></i> IN HÓA ĐƠN
          </button>
          <button class="w-full bg-orange-500 text-white py-3 rounded-xl text-xs font-bold shadow-lg shadow-orange-100 hover:bg-orange-600 transition active:scale-95 flex items-center justify-center gap-2">
            <i class="fas fa-edit"></i> CHỈNH SỬA ĐƠN HÀNG
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
const currentStep = 1; // 0: Chờ xác nhận, 1: Đã xác nhận/Chờ giao hàng...
const steps = [
  { name: 'Chờ xác nhận', icon: 'fas fa-clock', time: '16:42 23/12' },
  { name: 'Chờ giao hàng', icon: 'fas fa-truck', time: '18:00 23/12' },
  { name: 'Đang vận chuyển', icon: 'fas fa-shipping-fast', time: '' },
  { name: 'Hoàn thành', icon: 'fas fa-check-double', time: '' }
];
</script>