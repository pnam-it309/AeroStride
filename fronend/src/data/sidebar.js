export const sidebarItems = [
  { to: "/", label: "Thống kê", icon: "chart" },
  { to: "/sales", label: "Bán hàng", icon: "bag" },
  { to: "/orders", label: "Hóa đơn", icon: "receipt" },
  { to: "/products", label: "Sản phẩm", icon: "box" },
  {
    label: "Thuộc tính",
    icon: "sliders",
    children: [
      {
        to: { name: "attributes.list", query: { type: "brand" } },
        label: "Thương hiệu",
        icon: "tag",
      },
      {
        to: { name: "attributes.list", query: { type: "category" } },
        label: "Danh mục",
        icon: "tag",
      },
      {
        to: { name: "attributes.list", query: { type: "origin" } },
        label: "Xuất xứ",
        icon: "tag",
      },
      {
        to: { name: "attributes.list", query: { type: "purpose" } },
        label: "Mục đích chạy",
        icon: "tag",
      },
      {
        to: { name: "attributes.list", query: { type: "material" } },
        label: "Chất liệu",
        icon: "tag",
      },
      {
        to: { name: "attributes.list", query: { type: "sole" } },
        label: "Đế giày",
        icon: "tag",
      },
      {
        to: { name: "attributes.list", query: { type: "collar" } },
        label: "Cổ giày",
        icon: "tag",
      },
      {
        to: { name: "attributes.list", query: { type: "color" } },
        label: "Màu sắc",
        icon: "tag",
      },
      {
        to: { name: "attributes.list", query: { type: "size" } },
        label: "Kích thước",
        icon: "tag",
      },
    ],
  },
  { to: "/discounts", label: "Đợt giảm giá", icon: "percent" },
  { to: "/vouchers", label: "Phiếu giảm giá", icon: "ticket" },
  { to: "/customers", label: "Khách hàng", icon: "users" },
  { to: "/employees", label: "Nhân viên", icon: "user" },
  { to: "/payments", label: "Thanh toán", icon: "card" },
  { to: "/files", label: "Quản lý file", icon: "folder" },
];
