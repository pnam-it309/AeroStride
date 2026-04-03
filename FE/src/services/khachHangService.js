import api from './api'

const ENDPOINT = '/admin/khach-hang'

export const filterKhachHang = (params) => api.get(`${ENDPOINT}/filter`, { params })

export const getKhachHangById = (id) => api.get(`${ENDPOINT}/detail/${id}`)

export const updateKhachHang = (id, payload) => api.put(`${ENDPOINT}/update/${id}`, payload)

export const createKhachHang = (payload) => api.post(`${ENDPOINT}/add`, payload)

export const doiTrangThaiKhachHang = (id, trangThai) => 
  api.patch(`${ENDPOINT}/${id}/trang-thai`, null, { params: { trangThai } })

export const deleteKhachHang = (id) => api.delete(`${ENDPOINT}/delete/${id}`)

export default {
  filterKhachHang,
  getKhachHangById,
  updateKhachHang,
  createKhachHang,
  doiTrangThaiKhachHang,
  deleteKhachHang,
}
