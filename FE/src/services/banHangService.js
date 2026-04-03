import api from './api'

const AdminBanHangService = {
  getHoaDonCho: () => {
    return api.get('/admin/ban-hang')
  },
  createHoaDon: () => {
    return api.post('/admin/ban-hang')
  },
  deleteHoaDon: (id) => {
    return api.delete(`/admin/ban-hang/${id}`)
  },
  addProduct: (id, data) => {
    return api.post(`/admin/ban-hang/${id}/add-product`, data)
  },
  updateQuantity: (id, idHdct, soLuong) => {
    return api.put(`/admin/ban-hang/${id}/update-quantity/${idHdct}`, null, { params: { soLuong } })
  },
  removeProduct: (id, idHdct) => {
    return api.delete(`/admin/ban-hang/${id}/remove-product/${idHdct}`)
  },
  setKhachHang: (id, idKhachHang) => {
    return api.put(`/admin/ban-hang/${id}/khach-hang`, null, { params: { idKhachHang } })
  },
  setVoucher: (id, idVoucher) => {
    return api.put(`/admin/ban-hang/${id}/voucher`, null, { params: { idVoucher } })
  },
  checkout: (id, data) => {
    return api.post(`/admin/ban-hang/${id}/checkout`, data)
  },
  searchSanPham: (keyword) => {
    return api.get('/admin/ban-hang/search-san-pham', { params: { keyword } })
  },
  searchKhachHang: (keyword) => {
    return api.get('/admin/ban-hang/search-khach-hang', { params: { keyword } })
  },
  getVouchers: (tongTien) => {
    return api.get('/admin/ban-hang/vouchers', { params: { tongTien } })
  }
}

export default AdminBanHangService
