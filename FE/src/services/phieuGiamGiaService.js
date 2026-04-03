import api from './api'

const ENDPOINT = '/admin/phieu-giam-gia'

export const phieuGiamGiaService = {
  getAll: (params) => api.get(`${ENDPOINT}/phan-trang`, {
    params: {
      pageNo: params.pageNo || 0,
      pageSize: params.pageSize || 5,
      keyword: params.keyword || ""
    }
  }),

  hienThi: () => api.get(`${ENDPOINT}/hien-thi`),

  detail: (ma) => api.get(`${ENDPOINT}/detail`, { params: { ma } }),

  add: (payload) => api.post(`${ENDPOINT}/add`, payload),

  update: (id, payload) => api.put(`${ENDPOINT}/update`, payload, { params: { id } }),

  delete: (id) => api.delete(`${ENDPOINT}/delete`, { params: { id } })
}

export default phieuGiamGiaService