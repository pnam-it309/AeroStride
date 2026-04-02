import axios from "axios";
const BASE_URL = "http://localhost:8080/api/v1/admin/khach-hang";

// 🔍 filter + phân trang
export const filterKhachHang = async (filters) => {
  let token = localStorage.getItem("auth_token");
  token = token?.replace(/"/g, "");

  //kỉem tra token đã được lấy đúng chưa
  console.log("TOKEN gửi đi:", token);

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
