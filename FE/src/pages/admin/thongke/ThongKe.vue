<script setup>
import { ref } from 'vue'

import { 
  Currency, 
  ShoppingCart, 
  Users, 
  AlertCircle, 
  PlusCircle, 
  CalendarPlus, 
  FileText,
  ChevronRight
} from 'lucide-vue-next'

const stats = ref([
  { name: 'Tổng doanh thu', value: '128.430.000đ', change: '+12.5%', icon: Currency, color: 'text-emerald-500' },
  { name: 'Đơn hàng mới', value: '42', change: '+18.2%', icon: ShoppingCart, color: 'text-aurora' },
  { name: 'Khách hàng mới', value: '12', change: '+4.3%', icon: Users, color: 'text-purple-500' },
  { name: 'Sản phẩm sắp hết', value: '8', change: '-2.1%', icon: AlertCircle, color: 'text-amber-500' },
])

const recentOrders = ref([
  { id: 'HD001', customer: 'Nguyễn Văn A', date: '2024-03-20', total: '1.200.000đ', status: 'Chờ xác nhận' },
  { id: 'HD002', customer: 'Trần Thị B', date: '2024-03-19', total: '850.000đ', status: 'Đang giao' },
  { id: 'HD003', customer: 'Lê Văn C', date: '2024-03-19', total: '2.340.000đ', status: 'Đã hoàn thành' },
  { id: 'HD004', customer: 'Phạm Minh D', date: '2024-03-18', total: '450.000đ', status: 'Đã hủy' },
])
</script>

<template>
  <div class="space-y-8 animate-in fade-in duration-500">
    <div class="flex flex-col space-y-2">
      <h1 class="text-3xl font-display font-bold text-obsidian">Thống Kê Tổng Quan</h1>
      <p class="text-slate-500">Chào mừng trở lại! Dưới đây là tóm tắt hoạt động kinh doanh của AeroStride.</p>
    </div>

    <!-- Stats Grid -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
      <div v-for="item in stats" :key="item.name" 
           class="bg-white p-6 rounded-2xl border border-slate-100 shadow-sm hover:shadow-md transition-all duration-300 group cursor-pointer">
        <div class="flex items-center justify-between mb-4">
          <div :class="`p-3 rounded-xl bg-slate-50 group-hover:bg-aurora/10 transition-colors ${item.color}`">
             <component :is="item.icon" class="w-6 h-6" />
          </div>
          <span :class="`text-sm font-medium ${item.change.startsWith('+') ? 'text-emerald-500' : 'text-rose-500'}`">
            {{ item.change }}
          </span>
        </div>
        <div class="space-y-1">
          <p class="text-slate-500 text-sm font-medium">{{ item.name }}</p>
          <p class="text-2xl font-bold text-obsidian font-display">{{ item.value }}</p>
        </div>
      </div>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
      <!-- Recent Orders Table -->
      <div class="lg:col-span-2 bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden">
        <div class="p-6 border-b border-slate-50 flex items-center justify-between">
          <h2 class="text-xl font-display font-bold text-obsidian">Đơn hàng gần đây</h2>
          <button class="text-aurora hover:text-aurora/80 text-sm font-semibold transition-colors">Xem tất cả</button>
        </div>
        <div class="overflow-x-auto">
          <table class="w-full text-left">
            <thead>
              <tr class="bg-slate-50">
                <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Mã HD</th>
                <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Khách hàng</th>
                <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Ngày đặt</th>
                <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Tổng tiền</th>
                <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Trạng thái</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-slate-50">
              <tr v-for="order in recentOrders" :key="order.id" class="hover:bg-slate-50/50 transition-colors cursor-pointer group">
                <td class="px-6 py-4 text-sm font-semibold text-aurora group-hover:underline underline-offset-4">{{ order.id }}</td>
                <td class="px-6 py-4">
                  <div class="text-sm font-medium text-obsidian">{{ order.customer }}</div>
                </td>
                <td class="px-6 py-4 text-sm text-slate-500">{{ order.date }}</td>
                <td class="px-6 py-4 text-sm font-bold text-obsidian">{{ order.total }}</td>
                <td class="px-6 py-4">
                  <span :class="`px-3 py-1 rounded-full text-xs font-bold ${
                    order.status === 'Đã hoàn thành' ? 'bg-emerald-100 text-emerald-700' : 
                    order.status === 'Chờ xác nhận' ? 'bg-amber-100 text-amber-700' :
                    order.status === 'Đang giao' ? 'bg-blue-100 text-blue-700' : 'bg-rose-100 text-rose-700'
                  }`">
                    {{ order.status }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="space-y-6">
        <div class="bg-obsidian rounded-2xl p-6 text-white shadow-lg overflow-hidden relative group">
          <div class="absolute -right-8 -top-8 w-32 h-32 bg-aurora/20 rounded-full blur-3xl group-hover:bg-aurora/30 transition-all duration-500"></div>
          <h3 class="text-lg font-bold mb-4 relative z-10">Thao tác nhanh</h3>
          <div class="space-y-3 relative z-10">
            <button class="w-full flex items-center justify-between p-4 bg-white/10 hover:bg-white/20 rounded-xl transition-all border border-white/5 group ring-1 ring-white/10">
              <span class="flex items-center space-x-3">
                <PlusCircle class="w-5 h-5 text-aurora" />
                <span class="font-medium">Thêm sản phẩm mới</span>
              </span>
              <ChevronRight class="w-4 h-4" />
            </button>
             <button class="w-full flex items-center justify-between p-4 bg-white/10 hover:bg-white/20 rounded-xl transition-all border border-white/5 group ring-1 ring-white/10">
              <span class="flex items-center space-x-3">
                <CalendarPlus class="w-5 h-5 text-aurora" />
                <span class="font-medium">Tạo đợt giảm giá</span>
              </span>
              <ChevronRight class="w-4 h-4" />
            </button>
             <button class="w-full flex items-center justify-between p-4 bg-white/10 hover:bg-white/20 rounded-xl transition-all border border-white/5 group ring-1 ring-white/10">
              <span class="flex items-center space-x-3">
                <FileText class="w-5 h-5 text-aurora" />
                <span class="font-medium">Xuất báo cáo bán hàng</span>
              </span>
              <ChevronRight class="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
