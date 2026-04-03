import api from './api'

export const ATTRIBUTE_ENDPOINTS = {
  DANH_MUC: '/admin/thuoc-tinh/danh-muc',
  THUONG_HIEU: '/admin/thuoc-tinh/thuong-hieu',
  XUAT_XU: '/admin/thuoc-tinh/xuat-xu',
  MUC_DICH_CHAY: '/admin/thuoc-tinh/muc-dich-chay',
  CHAT_LIEU: '/admin/thuoc-tinh/chat-lieu',
  DE_GIAY: '/admin/thuoc-tinh/de-giay',
  CO_GIAY: '/admin/thuoc-tinh/co-giay',
  MAU_SAC: '/admin/thuoc-tinh/mau-sac',
  KICH_THUOC: '/admin/thuoc-tinh/kich-thuoc',
}

export const getAttributes = (endpoint, params = {}) => api.get(endpoint, { params })

export const getAttributeDetail = (endpoint, id) => api.get(`${endpoint}/${id}`)

export const createAttribute = (endpoint, payload) => api.post(endpoint, payload)

export const updateAttribute = (endpoint, id, payload) => api.put(`${endpoint}/${id}`, payload)

export const deleteAttribute = (endpoint, id) => api.delete(`${endpoint}/${id}`)

export default {
  ATTRIBUTE_ENDPOINTS,
  getAttributes,
  getAttributeDetail,
  createAttribute,
  updateAttribute,
  deleteAttribute,
}
