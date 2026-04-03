const API_BASE = "http://localhost:8080/api/v1/admin/phieu-giam-gia";

export const phieuGiamGiaService = {
    getAll: async (params) => {
        const url = new URL(`${API_BASE}/phan-trang`);
        // Đảm bảo không gửi giá trị undefined lên server
        url.searchParams.append("pageNo", params.pageNo || 0);
        url.searchParams.append("pageSize", params.pageSize || 5);
        url.searchParams.append("keyword", params.keyword || "");

        const res = await fetch(url);
        if (!res.ok) throw new Error("Lỗi tải danh sách");
        return res.json();
    },

    add: async (data) => {
        const res = await fetch(`${API_BASE}/add`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        if (!res.ok) {
            const msg = await res.text();
            throw new Error(msg || "Lỗi khi thêm");
        }
        return res;
    },

    delete: async (id) => {
        const res = await fetch(`${API_BASE}/delete?id=${id}`, {
            method: 'DELETE'
        });
        if (!res.ok) throw new Error("Xóa thất bại");
        return res;
    }
};