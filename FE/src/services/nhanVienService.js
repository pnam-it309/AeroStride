import api from './api'

const ENDPOINT = '/admin/nhan-vien'

export const filterNhanVien = (params) => api.get(`${ENDPOINT}/filter`, { params })

export const getNhanVienById = (id) => api.get(`${ENDPOINT}/detail/${id}`)

export const updateNhanVien = (id, payload) => api.put(`${ENDPOINT}/update/${id}`, payload)

export const createNhanVien = (payload) => api.post(`${ENDPOINT}/add`, payload)

export const doiTrangThaiNhanVien = (id, trangThai) => 
  api.patch(`${ENDPOINT}/${id}/trang-thai`, null, { params: { trangThai } })

export const deleteNhanVien = (id) => api.delete(`${ENDPOINT}/delete/${id}`)

export default {
  filterNhanVien,
  getNhanVienById,
  updateNhanVien,
  createNhanVien,
  doiTrangThaiNhanVien,
  deleteNhanVien,
}
