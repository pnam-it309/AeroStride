import api from './api'

const ADMIN_PRODUCT_ENDPOINT = '/admin/san-pham'

export const getProductFormOptions = () => api.get(`${ADMIN_PRODUCT_ENDPOINT}/form-options`)

export const getProducts = (params = {}) => api.get(ADMIN_PRODUCT_ENDPOINT, { params })

export const getProductDetail = (id) => api.get(`${ADMIN_PRODUCT_ENDPOINT}/${id}`)

export const createProduct = (payload) => api.post(ADMIN_PRODUCT_ENDPOINT, payload)

export const updateProduct = (id, payload) => api.put(`${ADMIN_PRODUCT_ENDPOINT}/${id}`, payload)

export const deleteProduct = (id) => api.delete(`${ADMIN_PRODUCT_ENDPOINT}/${id}`)

export const createVariant = (productId, payload) =>
  api.post(`${ADMIN_PRODUCT_ENDPOINT}/${productId}/variants`, payload)

export const updateVariant = (variantId, payload) =>
  api.put(`${ADMIN_PRODUCT_ENDPOINT}/variants/${variantId}`, payload)

export const deleteVariant = (variantId) =>
  api.delete(`${ADMIN_PRODUCT_ENDPOINT}/variants/${variantId}`)

export const createVariantImage = (variantId, payload) =>
  api.post(`${ADMIN_PRODUCT_ENDPOINT}/variants/${variantId}/images`, payload)

export const updateVariantImage = (imageId, payload) =>
  api.put(`${ADMIN_PRODUCT_ENDPOINT}/variant-images/${imageId}`, payload)

export const deleteVariantImage = (imageId) =>
  api.delete(`${ADMIN_PRODUCT_ENDPOINT}/variant-images/${imageId}`)

export const uploadProductFile = (file, folder = 'aerostride/products') => {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('folder', folder)

  return api.post('/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    skipErrorRedirect: true,
  })
}

export default {
  getProductFormOptions,
  getProducts,
  getProductDetail,
  createProduct,
  updateProduct,
  deleteProduct,
  createVariant,
  updateVariant,
  deleteVariant,
  createVariantImage,
  updateVariantImage,
  deleteVariantImage,
  uploadProductFile,
}
