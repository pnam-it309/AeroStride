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
    BoxIcon,
    MessagesIcon,
    CalendarIcon,
    ClockIcon,
    HistoryIcon
} from 'vue-tabler-icons';
import { PATH } from '@/router/routePaths';
import { APP_ROLES } from '@/constants/appConstants';

const sidebarItem = [
    {
        title: 'Thống kê',
        icon: ChartBarIcon,
        BgColor: 'error',
        to: PATH.THONG_KE,
        roles: [APP_ROLES.ADMIN]
    },
    {
        title: 'Bán hàng',
        icon: BasketIcon,
        BgColor: 'success',
        to: PATH.BAN_HANG,
        roles: [APP_ROLES.ADMIN, APP_ROLES.STAFF]
    },
    {
        title: 'Quản lý hóa đơn',
        icon: ReceiptIcon,
        BgColor: 'warning',
        to: PATH.HOA_DON,
        roles: [APP_ROLES.ADMIN, APP_ROLES.STAFF]
    },
    {
        title: 'Quản lý sản phẩm',
        icon: PackageIcon,
        BgColor: 'info',
        roles: [APP_ROLES.ADMIN, APP_ROLES.STAFF],
        children: [
            { title: 'Sản phẩm', icon: PackageIcon, to: PATH.SAN_PHAM, BgColor: 'info', roles: [APP_ROLES.ADMIN, APP_ROLES.STAFF] },
            { title: 'Biến thể sản phẩm', icon: BoxIcon, to: PATH.BIEN_THE_SAN_PHAM, BgColor: 'success', roles: [APP_ROLES.ADMIN, APP_ROLES.STAFF] }
        ]
    },
    {
        title: 'Quản lý thuộc tính',
        icon: ShapeIcon,
        BgColor: 'purple',
        roles: [APP_ROLES.ADMIN],
        children: [
            { title: 'Thương hiệu', icon: TagIcon, to: `${PATH.THUOC_TINH}/thuong-hieu`, BgColor: 'purple', roles: [APP_ROLES.ADMIN] },
            { title: 'Xuất xứ', icon: TagIcon, to: `${PATH.THUOC_TINH}/xuat-xu`, BgColor: 'purple', roles: [APP_ROLES.ADMIN] },
            { title: 'Mục đích chạy', icon: TagIcon, to: `${PATH.THUOC_TINH}/muc-dich-chay`, BgColor: 'purple', roles: [APP_ROLES.ADMIN] },
            { title: 'Chất liệu', icon: TagIcon, to: `${PATH.THUOC_TINH}/chat-lieu`, BgColor: 'purple', roles: [APP_ROLES.ADMIN] },
            { title: 'Đế giày', icon: TagIcon, to: `${PATH.THUOC_TINH}/de-giay`, BgColor: 'purple', roles: [APP_ROLES.ADMIN] },
            { title: 'Cổ giày', icon: TagIcon, to: `${PATH.THUOC_TINH}/co-giay`, BgColor: 'purple', roles: [APP_ROLES.ADMIN] },
            { title: 'Màu sắc', icon: TagIcon, to: `${PATH.THUOC_TINH}/mau-sac`, BgColor: 'purple', roles: [APP_ROLES.ADMIN] },
            { title: 'Kích thước', icon: TagIcon, to: `${PATH.THUOC_TINH}/kich-thuoc`, BgColor: 'purple', roles: [APP_ROLES.ADMIN] }
        ]
    },
    {
        title: 'Quản lý phiếu giảm giá',
        icon: TicketIcon,
        BgColor: 'pink',
        to: PATH.PHIEU_GIAM_GIA,
        roles: [APP_ROLES.ADMIN]
    },
    {
        title: 'Quản lý đợt giảm giá',
        icon: TagIcon,
        BgColor: 'orange',
        to: PATH.DOT_GIAM_GIA,
        roles: [APP_ROLES.ADMIN]
    },
    {
        title: 'Quản lý khách hàng',
        icon: UsersIcon,
        BgColor: 'primary',
        to: PATH.KHACH_HANG,
        roles: [APP_ROLES.ADMIN, APP_ROLES.STAFF]
    },
    {
        title: 'Quản lý nhân viên',
        icon: UserCheckIcon,
        BgColor: 'secondary',
        to: PATH.NHAN_VIEN,
        roles: [APP_ROLES.ADMIN]
    },
    {
        title: 'Quản lý tin nhắn',
        icon: MessagesIcon,
        BgColor: 'indigo',
        to: PATH.QUAN_LY_CHAT,
        roles: [APP_ROLES.ADMIN, APP_ROLES.STAFF]
    },
    {
        title: 'Quản lý lịch',
        icon: CalendarIcon,
        BgColor: 'teal',
        roles: [APP_ROLES.ADMIN, APP_ROLES.STAFF],
        children: [
            { title: 'Lịch làm việc', icon: CalendarIcon, to: PATH.LICH_LAM_VIEC, BgColor: 'teal', roles: [APP_ROLES.ADMIN, APP_ROLES.STAFF] },
            { title: 'Lịch ca làm', icon: ClockIcon, to: PATH.LICH_CA_LAM, BgColor: 'teal', roles: [APP_ROLES.ADMIN, APP_ROLES.STAFF] },
            { title: 'Chấm công', icon: UserCheckIcon, to: PATH.CHAM_CONG, BgColor: 'teal', roles: [APP_ROLES.ADMIN, APP_ROLES.STAFF] },
            { title: 'Lịch sử hoạt động', icon: HistoryIcon, to: PATH.LICH_SU_HOAT_DONG, BgColor: 'teal', roles: [APP_ROLES.ADMIN, APP_ROLES.STAFF] }
        ]
    }
];

export default sidebarItem;
