<script setup>
import { ref } from 'vue'

import { PlusCircle, Search, Pencil, Trash } from 'lucide-vue-next'

const tabs = [
  { id: 'danhmuc', name: 'Danh mục' },
  { id: 'thuonghieu', name: 'Thương hiệu' },
  { id: 'mausac', name: 'Màu sắc' },
  { id: 'kichthuoc', name: 'Kích thước' },
  { id: 'degiay', name: 'Đế giày' },
  { id: 'chatlieu', name: 'Chất liệu' },
  { id: 'cogiay', name: 'Cỡ giày' }
]

const activeTab = ref('danhmuc')

const mockData = ref([
  { id: 1, name: 'Nike', code: 'NK', status: 'Hoạt động' },
  { id: 2, name: 'Adidas', code: 'AD', status: 'Hoạt động' },
  { id: 3, name: 'Puma', code: 'PM', status: 'Ngừng kinh doanh' },
])
</script>

<template>
  <div class="space-y-6 animate-in fade-in duration-500">
    <div class="flex items-center justify-between">
      <div class="space-y-1">
        <h1 class="text-3xl font-display font-bold text-obsidian uppercase tracking-tight">Cấu hình Thuộc tính</h1>
        <p class="text-slate-500 text-sm font-medium">Thiết lập các thông số cơ bản cho sản phẩm.</p>
      </div>
       <button class="bg-obsidian hover:bg-obsidian/90 text-white px-6 py-3 rounded-xl font-bold flex items-center space-x-2 transition-all shadow-lg hover:shadow-glow ring-offset-2 hover:ring-2 ring-aurora/50">
        <PlusCircle class="w-5 h-5" />
        <span>Thêm mới</span>
      </button>
    </div>

    <!-- Tabs Container -->
    <div class="bg-white rounded-2xl border border-slate-100 shadow-sm overflow-hidden min-h-[600px] flex flex-col">
       <div class="flex border-b border-slate-100 overflow-x-auto scrollbar-hide bg-slate-50/50">
          <button v-for="tab in tabs" :key="tab.id" 
                  @click="activeTab = tab.id"
                  :class="`px-8 py-5 text-sm font-bold transition-all border-b-2 whitespace-nowrap ${
                    activeTab === tab.id ? 'text-aurora border-aurora bg-white' : 'text-slate-400 border-transparent hover:text-slate-600 hover:bg-white/50'
                  }`">
            {{ tab.name }}
          </button>
       </div>

       <div class="p-8 flex-1">
          <div class="flex items-center justify-between mb-8">
             <div class="relative w-96">
                <Search class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                <input type="text" :placeholder="`Tìm kiếm ${tabs.find(t => t.id === activeTab).name.toLowerCase()}...`" 
                       class="w-full pl-12 pr-4 py-3 bg-slate-50 border-transparent rounded-xl focus:ring-4 focus:ring-aurora/5 outline-none transition-all text-sm" />
             </div>
          </div>

          <table class="w-full text-left font-body">
             <thead>
                <tr class="bg-slate-50/50">
                   <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase">STT</th>
                   <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase">Mã</th>
                   <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase">Tên</th>
                   <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase">Trạng thái</th>
                   <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase text-right">Thao tác</th>
                </tr>
             </thead>
             <tbody class="divide-y divide-slate-50 text-sm">
                <tr v-for="(item, index) in mockData" :key="item.id" class="hover:bg-slate-50/30 transition-all group">
                   <td class="px-6 py-4 font-bold text-slate-400">{{ index + 1 }}</td>
                   <td class="px-6 py-4 font-bold text-obsidian">{{ item.code }}</td>
                   <td class="px-6 py-4 font-semibold text-slate-600">{{ item.name }}</td>
                   <td class="px-6 py-4">
                      <span :class="`px-3 py-1 rounded-full text-[10px] font-bold uppercase ${
                        item.status === 'Hoạt động' ? 'bg-emerald-100 text-emerald-700' : 'bg-rose-100 text-rose-700'
                      }`">{{ item.status }}</span>
                   </td>
                   <td class="px-6 py-4 text-right">
                      <div class="flex items-center justify-end space-x-2">
                        <button class="p-2 text-slate-400 hover:text-aurora hover:bg-aurora/10 rounded-lg transition-all"><Pencil class="w-5 h-5" /></button>
                        <button class="p-2 text-slate-400 hover:text-rose-500 hover:bg-rose-50 rounded-lg transition-all"><Trash class="w-5 h-5" /></button>
                      </div>
                   </td>
                </tr>
             </tbody>
          </table>
       </div>
    </div>
  </div>
</template>
