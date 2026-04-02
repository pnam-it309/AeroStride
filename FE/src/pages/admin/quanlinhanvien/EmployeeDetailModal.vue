<!-- EmployeeDetailModal.vue -->
<template>
  <div
    class="fixed inset-0 bg-black/50 flex justify-center items-center z-50 p-4"
  >
    <div
      class="bg-white rounded-3xl w-full max-w-5xl p-6 relative shadow-xl overflow-auto"
      style="max-height: 90vh"
    >
      <!-- Close button -->
      <button
        @click="$emit('close')"
        class="absolute top-6 right-6 text-gray-400 hover:text-gray-600 text-xl font-bold"
      >
        ✕
      </button>

      <div
        class="flex flex-col md:flex-row space-y-6 md:space-y-0 md:space-x-8"
      >
        <!-- Avatar bên trái -->
        <div class="flex-shrink-0 flex justify-center md:justify-start">
          <img
            :src="employee?.hinhAnh || '/default-avatar.png'"
            alt="avatar"
            class="w-48 h-48 rounded-full object-cover border-2 border-gray-200 shadow-md"
          />
        </div>

        <!-- Thông tin bên phải -->
        <div class="flex-1 space-y-6">
          <div class="text-center md:text-left">
            <h2 class="text-3xl font-bold text-gray-800">
              {{ employee?.ten }}
            </h2>
            <p class="text-gray-500 text-sm">{{ employee?.phanQuyen?.ten }}</p>
          </div>

          <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div class="bg-gray-50 p-4 rounded-xl shadow-sm">
              <span class="font-semibold text-gray-600">Mã NV:</span>
              <div class="text-gray-800">{{ employee?.ma }}</div>
            </div>

            <div class="bg-gray-50 p-4 rounded-xl shadow-sm">
              <span class="font-semibold text-gray-600">Email:</span>
              <div class="text-gray-800">{{ employee?.email }}</div>
            </div>

            <div class="bg-gray-50 p-4 rounded-xl shadow-sm">
              <span class="font-semibold text-gray-600">SĐT:</span>
              <div class="text-gray-800">{{ employee?.sdt }}</div>
            </div>

            <div class="bg-gray-50 p-4 rounded-xl shadow-sm">
              <span class="font-semibold text-gray-600">Giới tính:</span>
              <div class="text-gray-800">
                {{ employee?.gioiTinh ? "Nam" : "Nữ" }}
              </div>
            </div>

            <div class="bg-gray-50 p-4 rounded-xl shadow-sm">
              <span class="font-semibold text-gray-600">Trạng thái:</span>
              <div class="text-gray-800">
                {{
                  employee?.trangThai === "DANG_HOAT_DONG"
                    ? "Hoạt động"
                    : "Ngừng hoạt động"
                }}
              </div>
            </div>

            <div class="bg-gray-50 p-4 rounded-xl shadow-sm">
              <span class="font-semibold text-gray-600">Ngày cập nhật:</span>
              <div class="text-gray-800">
                {{
                  employee?.ngayCapNhat
                    ? new Date(employee.ngayCapNhat).toLocaleDateString("vi-VN")
                    : "---"
                }}
              </div>
            </div>
          </div>

          <!-- Nút Sửa -->
          <div class="flex justify-end mt-4">
            <button
              @click="$emit('edit', employee)"
              class="bg-cyan-500 hover:bg-cyan-600 text-white font-bold py-3 px-6 rounded-2xl shadow-lg transition"
            >
              Sửa
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  employee: Object,
});
</script>

<style scoped></style>
