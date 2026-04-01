/**
 * HƯỚNG DẪN KẾT NỐI FRONTEND (VUE.JS) VỚI BACKEND (SPRING BOOT)
 * 
 * Quy trình gồm 3 bước chính:
 * 1. BE: Định nghĩa API trong Controller (Java)
 * 2. FE Service: Định nghĩa hàm gọi API (Javascript/Axios)
 * 3. FE Component: Gọi hàm từ Service và hiển thị dữ liệu (Vue)
 */

// --- BƯỚC 1: BACKEND (Ví dụ tại: Controller.java) ---
/*
@RestController
@RequestMapping("/api/v1/admin/...")
public class Controller {
    
    @GetMapping
    public ResponseEntity<?> getAll() {
        // Trả về danh sách từ Database
        return ResponseEntity.ok(ApiResponse.success(service.findAll()));
    }
}
*/

// --- BƯỚC 2: FRONTEND SERVICE (File này - example.js) ---
import api from './api' // 'api' là instance axios đã cấu hình baseURL

const exampleService = {
    // 1. Lấy danh sách (GET)
    // endpoint phải khớp với @RequestMapping ở Backend
    getAll: () => api.get('/admin/...'),

    // 2. Thêm mới (POST)
    create: (data) => api.post('/admin/...', data),

    // 3. Cập nhật (PUT)
    update: (id, data) => api.put(`/admin/..../${id}`, data),

    // 4. Xóa (DELETE)
    delete: (id) => api.delete(`/admin/..../${id}`)
}

export default exampleService

// --- BƯỚC 3: SỬ DỤNG TRONG VUE COMPONENT (Ví dụ: MauSac.vue) ---
/*
<script setup>
import { ref, onMounted } from 'vue'
import exampleService from '@/services/example'

const listData = ref([])

const fetchData = async () => {
    try {
        const response = await exampleService.getAll()
        // response.data thường là ApiResponse từ BE
        listData.value = response.data 
    } catch (error) {
        console.error("Lỗi kết nối BE:", error)
    }
}

onMounted(() => {
    fetchData()
})
</script>
*/
