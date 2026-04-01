<script setup>
import { ref } from 'vue'
import { Plus, Trash2, Edit, XCircle } from 'lucide-vue-next'
import AeroPagination from '../../base/AeroPagination.vue'
defineProps({
  columns: {
    type: Array,
    required: true,
  },
  data: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: false,
  },
  currentPage: {
    type: Number,
    default: 1,
  },
  totalPages: {
    type: Number,
    default: 1,
  },
  pageSize: {
    type: Number,
    default: 10,
  },
})

defineEmits(['update:page', 'update:size', 'add', 'delete', 'sort'])
</script>

<template>
  <div
    class="bg-gray-100/50 border border-gray-200 rounded-2xl p-6 space-y-6 flex-1 flex flex-col min-h-0"
  >
    <!-- Section 1: Actions + Table -->
    <div
      class="bg-white border border-gray-200 rounded-xl overflow-hidden shadow-sm flex-1 flex flex-col min-h-0"
    >
      <!-- Top section: Action buttons -->
      <div
        class="p-4 border-b border-gray-100 flex items-center justify-between bg-gray-50/50 flex-shrink-0"
      >
        <div class="flex items-center gap-2">
          <h3 class="text-xs font-black text-gray-400 uppercase tracking-widest">Entries</h3>
        </div>
        <div class="flex items-center gap-3">
          <!-- Add New Button -->
          <div class="relative group">
            <button
              @click="$emit('add')"
              class="p-2.5 bg-aurora text-white rounded-xl hover:shadow-lg hover:shadow-aurora/30 transition-all active:scale-95 flex items-center justify-center"
            >
              <Plus :size="18" stroke-width="3" />
            </button>
            <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block z-50">
              <div class="bg-gray-900 text-white text-[10px] font-black uppercase tracking-widest px-3 py-1.5 rounded-lg whitespace-nowrap shadow-xl">
                Add New Entry
              </div>
              <div class="w-2 h-2 bg-gray-900 rotate-45 absolute -bottom-1 left-1/2 -translate-x-1/2"></div>
            </div>
          </div>

          <!-- Bulk Delete Button -->
          <div class="relative group">
            <button
              @click="$emit('delete')"
              class="p-2.5 border border-red-100 text-red-500 rounded-xl hover:bg-red-50 hover:border-red-200 transition-all active:scale-95 flex items-center justify-center"
            >
              <Trash2 :size="18" />
            </button>
            <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover:block z-50">
              <div class="bg-red-600 text-white text-[10px] font-black uppercase tracking-widest px-3 py-1.5 rounded-lg whitespace-nowrap shadow-xl">
                Delete Selected
              </div>
              <div class="w-2 h-2 bg-red-600 rotate-45 absolute -bottom-1 left-1/2 -translate-x-1/2"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Middle section: The table (Scrollable) -->
      <div class="overflow-y-auto flex-1 min-h-[480px] h-full scrollbar-thin">
        <table class="w-full text-left border-collapse min-w-[800px]">
          <thead class="sticky top-0 z-10">
            <tr class="bg-gray-50/90 backdrop-blur-sm">
              <th
                v-for="col in columns"
                :key="col.key"
                class="px-6 py-5 text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] border-b border-gray-100"
              >
                {{ col.label }}
              </th>
              <th
                class="px-6 py-5 text-[10px] font-black text-gray-400 uppercase tracking-[0.2em] border-b border-gray-100 text-right"
              >
                Actions
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-100 relative">
            <template v-if="loading">
              <tr v-for="i in 5" :key="`skeleton-${i}`" class="animate-pulse">
                <td v-for="col in columns" :key="col.key" class="px-6 py-4 h-[80px]">
                  <div class="h-4 bg-gray-100 rounded w-2/3"></div>
                </td>
                <td class="px-6 py-5 text-right">
                  <div class="h-4 bg-gray-100 rounded w-12 ml-auto"></div>
                </td>
              </tr>
            </template>
            <template v-else>
              <tr
                v-for="(row, index) in data"
                :key="index"
                class="hover:bg-gray-50/50 transition-colors group"
              >
                <td
                  v-for="col in columns"
                  :key="col.key"
                  class="px-6 py-4 text-sm text-gray-700 font-bold h-[80px]"
                >
                  <slot :name="`cell-${col.key}`" :row="row">
                    {{ row[col.key] }}
                  </slot>
                </td>
                <td class="px-6 py-5 text-right">
                  <div
                    class="flex justify-end gap-3 opacity-0 group-hover:opacity-100 transition-opacity"
                  >
                    <!-- Edit Action -->
                    <div class="relative group/tooltip">
                      <button class="p-1.5 text-aurora hover:bg-aurora/10 rounded-lg transition-all active:scale-95">
                        <Edit :size="14" />
                      </button>
                      <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover/tooltip:block z-50">
                        <div class="bg-gray-900 text-white text-[8px] font-black uppercase tracking-widest px-2 py-1 rounded-md whitespace-nowrap">
                          Edit
                        </div>
                      </div>
                    </div>

                    <!-- Delete/Rev Action -->
                    <div class="relative group/tooltip">
                      <button class="p-1.5 text-gray-400 hover:text-red-500 hover:bg-red-50 rounded-lg transition-all active:scale-95">
                        <XCircle :size="14" />
                      </button>
                      <div class="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden group-hover/tooltip:block z-50">
                        <div class="bg-red-600 text-white text-[8px] font-black uppercase tracking-widest px-2 py-1 rounded-md whitespace-nowrap">
                          Remove
                        </div>
                      </div>
                    </div>
                  </div>
                </td>
              </tr>
              <tr v-if="!data || data.length === 0">
                <td
                  :colspan="columns.length + 1"
                  class="px-6 py-12 text-center text-gray-400 font-bold italic"
                >
                  No data available.
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Section 2: Pagination (Fixed at bottom) -->
    <AeroPagination
      :current-page="currentPage"
      :total-pages="totalPages"
      :page-size="pageSize"
      @page-change="(p) => $emit('update:page', p)"
      @size-change="(s) => $emit('update:size', s)"
    />
  </div>
</template>
