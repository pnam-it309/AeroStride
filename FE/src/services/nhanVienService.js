import axios from "axios";
const BASE_URL = "http://localhost:8080/api/v1/admin/nhan-vien";

const getToken = () => localStorage.getItem("auth_token")?.replace(/"/g, "");

const authHeader = () => ({
  Authorization: `Bearer ${getToken()}`,
});

// 🔍 filter + phân trang
export const filterNhanVien = async (filters) => {
  let token = localStorage.getItem("auth_token")?.replace(/"/g, "");

  const params = {
    page: filters.page,
    size: filters.size,
    keyword: filters.keyword || undefined,
    gioiTinh: filters.gioiTinh != null ? filters.gioiTinh : undefined,
    trangThai: filters.trangThai || undefined,
  };

  const res = await axios.get(`${BASE_URL}/filter`, {
    params,
    headers: { Authorization: `Bearer ${token}` },
  });

  return res.data;
};

export const getNhanVienById = (id) =>
  axios.get(`${BASE_URL}/detail/${id}`, {
    headers: authHeader(),
  });

export const updateNhanVien = (id, payload) =>
  axios.put(`${BASE_URL}/update/${id}`, payload, {
    headers: {
      ...authHeader(),
      "Content-Type": "application/json",
    },
  });

export const createNhanVien = (payload) =>
  axios.post(`${BASE_URL}/create`, payload, {
    headers: {
      ...authHeader(),
      "Content-Type": "application/json",
    },
  });
export const doiTrangThaiNhanVien = (id, trangThai) =>
  axios.patch(`${BASE_URL}/${id}/trang-thai`, null, {
    params: { trangThai },
    headers: authHeader(),
  });
