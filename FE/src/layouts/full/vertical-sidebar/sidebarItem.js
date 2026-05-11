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
  LogoutIcon,
  BoxIcon
} from 'vue-tabler-icons';
import { PATH } from '@/router/routePaths';

const sidebarItem = [
 {
    title: 'Thống kê',
    icon: ChartBarIcon,
    BgColor: 'error',
    to: PATH.THONG_KE
  },
  {
    title: 'Bán hàng',
    icon: BasketIcon,
    BgColor: 'success',
    to: PATH.BAN_HANG
  },
  {
    title: 'Quản lý hóa đơn',
    icon: ReceiptIcon,
    BgColor: 'warning',
    to: PATH.HOA_DON
  },
  {
    title: 'Quản lý sản phẩm',
    icon: PackageIcon,
    BgColor: 'info',
    children: [
      { title: 'Sản phẩm', icon: PackageIcon, to: PATH.SAN_PHAM, BgColor: 'info' },
      { title: 'Biến thể sản phẩm', icon: BoxIcon, to: PATH.BIEN_THE_SAN_PHAM, BgColor: 'success' }
    ]
  },
  {
    title: 'Quản lý thuộc tính',
    icon: ShapeIcon,
    BgColor: 'purple',
    children: [
      { title: 'Thương hiệu', icon: TagIcon, to: `${PATH.THUOC_TINH}/thuong-hieu`, BgColor: 'purple' },
      { title: 'Danh mục', icon: TagIcon, to: `${PATH.THUOC_TINH}/danh-muc`, BgColor: 'purple' },
      { title: 'Xuất xứ', icon: TagIcon, to: `${PATH.THUOC_TINH}/xuat-xu`, BgColor: 'purple' },
      { title: 'Mục đích chạy', icon: TagIcon, to: `${PATH.THUOC_TINH}/muc-dich-chay`, BgColor: 'purple' },
      { title: 'Chất liệu', icon: TagIcon, to: `${PATH.THUOC_TINH}/chat-lieu`, BgColor: 'purple' },
      { title: 'Đế giày', icon: TagIcon, to: `${PATH.THUOC_TINH}/de-giay`, BgColor: 'purple' },
      { title: 'Cổ giày', icon: TagIcon, to: `${PATH.THUOC_TINH}/co-giay`, BgColor: 'purple' },
      { title: 'Màu sắc', icon: TagIcon, to: `${PATH.THUOC_TINH}/mau-sac`, BgColor: 'purple' },
      { title: 'Kích thước', icon: TagIcon, to: `${PATH.THUOC_TINH}/kich-thuoc`, BgColor: 'purple' }
    ]
  },
  {
    title: 'Quản lý phiếu giảm giá',
    icon: TicketIcon,
    BgColor: 'pink',
    to: PATH.PHIEU_GIAM_GIA
  },
  {
    title: 'Quản lý đợt giảm giá',
    icon: TagIcon,
    BgColor: 'orange',
    to: PATH.DOT_GIAM_GIA
  },
  {
    title: 'Quản lý khách hàng',
    icon: UsersIcon,
    BgColor: 'primary',
    to: PATH.KHACH_HANG
  },
  {
    title: 'Quản lý nhân viên',
    icon: UserCheckIcon,
    BgColor: 'secondary',
    to: PATH.NHAN_VIEN
  },
  // {
  //   title: 'Thanh toán',
  //   icon: CreditCardIcon,
  //   BgColor: 'teal',
  //   to: '/thanh-toan'
  // },
  // {
  //   title: 'Quản lý file',
  //   icon: FolderIcon,
  //   BgColor: 'indigo',
  //   to: '/quan-ly-file'
  // },
  // {
  //   title: 'Đăng xuất',
  //   icon: LogoutIcon,
  //   BgColor: 'grey',
  //   to: PATH.LOGOUT
  // }
];

export default sidebarItem;
