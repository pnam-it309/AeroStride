import {
  LayoutDashboardIcon,
  BasketIcon,
  PackageIcon,
  UsersIcon,
  ReceiptIcon,
  UserCheckIcon,
  ChartBarIcon,
  TagIcon,
  TicketIcon,
  ShapeIcon,
  CreditCardIcon,
  FolderIcon,
  LoginIcon, 
  UserPlusIcon,
  LogoutIcon
} from 'vue-tabler-icons';

const sidebarItem = [
 {
    title: 'Thống kê',
    icon: ChartBarIcon,
    BgColor: 'error',
    to: '/thong-ke'
  },
  {
    title: 'Bán hàng',
    icon: BasketIcon,
    BgColor: 'success',
    to: '/ban-hang'
  },
  {
    title: 'Hóa đơn',
    icon: ReceiptIcon,
    BgColor: 'warning',
    to: '/hoa-don'
  },
  {
    title: 'Sản phẩm',
    icon: PackageIcon,
    BgColor: 'info',
    to: '/san-pham'
  },
  {
    title: 'Thuộc tính',
    icon: ShapeIcon,
    BgColor: 'purple',
    children: [
      { title: 'Thương hiệu', icon: TagIcon, to: '/thuoc-tinh/thuong-hieu', BgColor: 'purple' },
      { title: 'Danh mục', icon: TagIcon, to: '/thuoc-tinh/danh-muc', BgColor: 'purple' },
      { title: 'Xuất xứ', icon: TagIcon, to: '/thuoc-tinh/xuat-xu', BgColor: 'purple' },
      { title: 'Mục đích chạy', icon: TagIcon, to: '/thuoc-tinh/muc-dich-chay', BgColor: 'purple' },
      { title: 'Chất liệu', icon: TagIcon, to: '/thuoc-tinh/chat-lieu', BgColor: 'purple' },
      { title: 'Đế giày', icon: TagIcon, to: '/thuoc-tinh/de-giay', BgColor: 'purple' },
      { title: 'Cổ giày', icon: TagIcon, to: '/thuoc-tinh/co-giay', BgColor: 'purple' },
      { title: 'Màu sắc', icon: TagIcon, to: '/thuoc-tinh/mau-sac', BgColor: 'purple' },
      { title: 'Kích thước', icon: TagIcon, to: '/thuoc-tinh/kich-thuoc', BgColor: 'purple' }
    ]
  },
  {
    title: 'Đợt giảm giá',
    icon: TagIcon,
    BgColor: 'orange',
    to: '/dot-giam-gia'
  },
  {
    title: 'Phiếu giảm giá',
    icon: TicketIcon,
    BgColor: 'pink',
    to: '/phieu-giam-gia'
  },
  {
    title: 'Khách hàng',
    icon: UsersIcon,
    BgColor: 'primary',
    to: '/khach-hang'
  },
  {
    title: 'Nhân viên',
    icon: UserCheckIcon,
    BgColor: 'secondary',
    to: '/nhan-vien'
  },
  {
    title: 'Thanh toán',
    icon: CreditCardIcon,
    BgColor: 'teal',
    to: '/thanh-toan'
  },
  {
    title: 'Quản lý file',
    icon: FolderIcon,
    BgColor: 'indigo',
    to: '/quan-ly-file'
  },
  {
    title: 'Đăng xuất',
    icon: LogoutIcon,
    BgColor: 'grey',
    to: '/auth/logout'
  }
];

export default sidebarItem;
