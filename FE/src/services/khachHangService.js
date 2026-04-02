import axios from "axios";
const BASE_URL = "http://localhost:8080/api/v1/admin/khach-hang";

// 🔍 filter + phân trang
export const filterKhachHang = async (filters) => {
  let token = localStorage.getItem("auth_token");
  token = token?.replace(/"/g, "");

  console.log("TOKEN gửi đi:", token); // 👈 check

  const res = await axios.get(
    "http://localhost:8080/api/v1/admin/khach-hang/filter",
    {
      params: filters,
      headers: {
        Authorization: `Bearer ${token}`,
      },
    },
  );

  return res.data;
};
