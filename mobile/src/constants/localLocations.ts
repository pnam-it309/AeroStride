/**
 * Local Vietnamese Administrative Locations Fallback Dataset for Mobile
 * Full 63 Provinces / Cities with key districts and wards for offline/fallback reliability
 */

export interface LocationItem {
  code: number | string;
  name: string;
}

export const LOCAL_PROVINCES: LocationItem[] = [
  { code: 1, name: 'Thành phố Hà Nội' },
  { code: 79, name: 'Thành phố Hồ Chí Minh' },
  { code: 48, name: 'Thành phố Đà Nẵng' },
  { code: 31, name: 'Thành phố Hải Phòng' },
  { code: 92, name: 'Thành phố Cần Thơ' },
  { code: 89, name: 'Tỉnh An Giang' },
  { code: 77, name: 'Tỉnh Bà Rịa - Vũng Tàu' },
  { code: 24, name: 'Tỉnh Bắc Giang' },
  { code: 6, name: 'Tỉnh Bắc Kạn' },
  { code: 95, name: 'Tỉnh Bạc Liêu' },
  { code: 27, name: 'Tỉnh Bắc Ninh' },
  { code: 83, name: 'Tỉnh Bến Tre' },
  { code: 52, name: 'Tỉnh Bình Định' },
  { code: 74, name: 'Tỉnh Bình Dương' },
  { code: 70, name: 'Tỉnh Bình Phước' },
  { code: 60, name: 'Tỉnh Bình Thuận' },
  { code: 96, name: 'Tỉnh Cà Mau' },
  { code: 4, name: 'Tỉnh Cao Bằng' },
  { code: 66, name: 'Tỉnh Đắk Lắk' },
  { code: 67, name: 'Tỉnh Đắk Nông' },
  { code: 11, name: 'Tỉnh Điện Biên' },
  { code: 75, name: 'Tỉnh Đồng Nai' },
  { code: 87, name: 'Tỉnh Đồng Tháp' },
  { code: 64, name: 'Tỉnh Gia Lai' },
  { code: 2, name: 'Tỉnh Hà Giang' },
  { code: 35, name: 'Tỉnh Hà Nam' },
  { code: 42, name: 'Tỉnh Hà Tĩnh' },
  { code: 30, name: 'Tỉnh Hải Dương' },
  { code: 93, name: 'Tỉnh Hậu Giang' },
  { code: 17, name: 'Tỉnh Hòa Bình' },
  { code: 33, name: 'Tỉnh Hưng Yên' },
  { code: 56, name: 'Tỉnh Khánh Hòa' },
  { code: 91, name: 'Tỉnh Kiên Giang' },
  { code: 62, name: 'Tỉnh Kon Tum' },
  { code: 12, name: 'Tỉnh Lai Châu' },
  { code: 68, name: 'Tỉnh Lâm Đồng' },
  { code: 20, name: 'Tỉnh Lạng Sơn' },
  { code: 10, name: 'Tỉnh Lào Cai' },
  { code: 80, name: 'Tỉnh Long An' },
  { code: 36, name: 'Tỉnh Nam Định' },
  { code: 40, name: 'Tỉnh Nghệ An' },
  { code: 37, name: 'Tỉnh Ninh Bình' },
  { code: 58, name: 'Tỉnh Ninh Thuận' },
  { code: 25, name: 'Tỉnh Phú Thọ' },
  { code: 54, name: 'Tỉnh Phú Yên' },
  { code: 44, name: 'Tỉnh Quảng Bình' },
  { code: 49, name: 'Tỉnh Quảng Nam' },
  { code: 51, name: 'Tỉnh Quảng Ngãi' },
  { code: 22, name: 'Tỉnh Quảng Ninh' },
  { code: 45, name: 'Tỉnh Quảng Trị' },
  { code: 94, name: 'Tỉnh Sóc Trăng' },
  { code: 14, name: 'Tỉnh Sơn La' },
  { code: 72, name: 'Tỉnh Tây Ninh' },
  { code: 34, name: 'Tỉnh Thái Bình' },
  { code: 19, name: 'Tỉnh Thái Nguyên' },
  { code: 38, name: 'Tỉnh Thanh Hóa' },
  { code: 46, name: 'Tỉnh Thừa Thiên Huế' },
  { code: 82, name: 'Tỉnh Tiền Giang' },
  { code: 84, name: 'Tỉnh Trà Vinh' },
  { code: 8, name: 'Tỉnh Tuyên Quang' },
  { code: 86, name: 'Tỉnh Vĩnh Long' },
  { code: 26, name: 'Tỉnh Vĩnh Phúc' },
  { code: 15, name: 'Tỉnh Yên Bái' },
];

export const LOCAL_DISTRICTS: Record<string | number, LocationItem[]> = {
  // Hà Nội
  1: [
    { code: 101, name: 'Quận Ba Đình' },
    { code: 102, name: 'Quận Hoàn Kiếm' },
    { code: 103, name: 'Quận Tây Hồ' },
    { code: 104, name: 'Quận Long Biên' },
    { code: 105, name: 'Quận Cầu Giấy' },
    { code: 106, name: 'Quận Đống Đa' },
    { code: 107, name: 'Quận Hai Bà Trưng' },
    { code: 108, name: 'Quận Hoàng Mai' },
    { code: 109, name: 'Quận Thanh Xuân' },
    { code: 110, name: 'Quận Nam Từ Liêm' },
    { code: 111, name: 'Quận Bắc Từ Liêm' },
    { code: 112, name: 'Quận Hà Đông' },
    { code: 113, name: 'Thị xã Sơn Tây' },
    { code: 114, name: 'Huyện Đông Anh' },
    { code: 115, name: 'Huyện Gia Lâm' },
    { code: 116, name: 'Huyện Sóc Sơn' },
    { code: 117, name: 'Huyện Thanh Trì' },
    { code: 118, name: 'Huyện Hoài Đức' },
    { code: 119, name: 'Huyện Thạch Thất' },
    { code: 120, name: 'Huyện Quốc Oai' },
    { code: 121, name: 'Huyện Chương Mỹ' },
    { code: 122, name: 'Huyện Đan Phượng' },
    { code: 123, name: 'Huyện Thường Tín' },
  ],
  // TP. Hồ Chí Minh
  79: [
    { code: 701, name: 'Quận 1' },
    { code: 702, name: 'Quận 3' },
    { code: 703, name: 'Quận 4' },
    { code: 704, name: 'Quận 5' },
    { code: 705, name: 'Quận 6' },
    { code: 706, name: 'Quận 7' },
    { code: 707, name: 'Quận 8' },
    { code: 708, name: 'Quận 10' },
    { code: 709, name: 'Quận 11' },
    { code: 710, name: 'Quận 12' },
    { code: 711, name: 'Thành phố Thủ Đức' },
    { code: 712, name: 'Quận Bình Thạnh' },
    { code: 713, name: 'Quận Gò Vấp' },
    { code: 714, name: 'Quận Phú Nhuận' },
    { code: 715, name: 'Quận Tân Bình' },
    { code: 716, name: 'Quận Tân Phú' },
    { code: 717, name: 'Quận Bình Tân' },
    { code: 718, name: 'Huyện Củ Chi' },
    { code: 719, name: 'Huyện Hóc Môn' },
    { code: 720, name: 'Huyện Bình Chánh' },
    { code: 721, name: 'Huyện Nhà Bè' },
    { code: 722, name: 'Huyện Cần Giờ' },
  ],
  // Đà Nẵng
  48: [
    { code: 481, name: 'Quận Hải Châu' },
    { code: 482, name: 'Quận Thanh Khê' },
    { code: 483, name: 'Quận Sơn Trà' },
    { code: 484, name: 'Quận Ngũ Hành Sơn' },
    { code: 485, name: 'Quận Liên Chiểu' },
    { code: 486, name: 'Quận Cẩm Lệ' },
    { code: 487, name: 'Huyện Hòa Vang' },
  ],
  // Hải Phòng
  31: [
    { code: 311, name: 'Quận Hồng Bàng' },
    { code: 312, name: 'Quận Ngô Quyền' },
    { code: 313, name: 'Quận Lê Chân' },
    { code: 314, name: 'Quận Hải An' },
    { code: 315, name: 'Quận Kiến An' },
    { code: 316, name: 'Quận Đồ Sơn' },
    { code: 317, name: 'Quận Dương Kinh' },
    { code: 318, name: 'Huyện Thủy Nguyên' },
    { code: 319, name: 'Huyện An Dương' },
  ],
  // Cần Thơ
  92: [
    { code: 921, name: 'Quận Ninh Kiều' },
    { code: 922, name: 'Quận Bình Thủy' },
    { code: 923, name: 'Quận Cái Răng' },
    { code: 924, name: 'Quận Ô Môn' },
    { code: 925, name: 'Quận Thốt Nốt' },
  ],
};

export const LOCAL_WARDS: Record<string | number, LocationItem[]> = {
  // Cầu Giấy (105)
  105: [
    { code: 10501, name: 'Phường Dịch Vọng' },
    { code: 10502, name: 'Phường Dịch Vọng Hậu' },
    { code: 10503, name: 'Phường Mai Dịch' },
    { code: 10504, name: 'Phường Nghĩa Đô' },
    { code: 10505, name: 'Phường Nghĩa Tân' },
    { code: 10506, name: 'Phường Quan Hoa' },
    { code: 10507, name: 'Phường Trung Hòa' },
    { code: 10508, name: 'Phường Yên Hòa' },
  ],
  // Đống Đa (106)
  106: [
    { code: 10601, name: 'Phường Cát Linh' },
    { code: 10602, name: 'Phường Hàng Bột' },
    { code: 10603, name: 'Phường Khâm Thiên' },
    { code: 10604, name: 'Phường Láng Hạ' },
    { code: 10605, name: 'Phường Láng Thượng' },
    { code: 10606, name: 'Phường Ô Chợ Dừa' },
    { code: 10607, name: 'Phường Quang Trung' },
    { code: 10608, name: 'Phường Quốc Tử Giám' },
  ],
  // Quận 1 (701)
  701: [
    { code: 70101, name: 'Phường Bến Nghé' },
    { code: 70102, name: 'Phường Bến Thành' },
    { code: 70103, name: 'Phường Cầu Kho' },
    { code: 70104, name: 'Phường Cầu Ông Lãnh' },
    { code: 70105, name: 'Phường Cô Giang' },
    { code: 70106, name: 'Phường Đa Kao' },
    { code: 70107, name: 'Phường Nguyễn Cư Trinh' },
    { code: 70108, name: 'Phường Nguyễn Thái Bình' },
    { code: 70109, name: 'Phường Phạm Ngũ Lão' },
    { code: 70110, name: 'Phường Tân Định' },
  ],
  // TP Thủ Đức (711)
  711: [
    { code: 71101, name: 'Phường An Khánh' },
    { code: 71102, name: 'Phường An Lợi Đông' },
    { code: 71103, name: 'Phường An Phú' },
    { code: 71104, name: 'Phường Bình Chiểu' },
    { code: 71105, name: 'Phường Bình Thọ' },
    { code: 71106, name: 'Phường Hiệp Bình Chánh' },
    { code: 71107, name: 'Phường Hiệp Bình Phước' },
    { code: 71108, name: 'Phường Linh Chiểu' },
    { code: 71109, name: 'Phường Linh Đông' },
    { code: 71110, name: 'Phường Linh Tây' },
    { code: 71111, name: 'Phường Linh Trung' },
    { code: 71112, name: 'Phường Linh Xuân' },
    { code: 71113, name: 'Phường Thảo Điền' },
    { code: 71114, name: 'Phường Thủ Thiêm' },
  ],
};

export const getFallbackDistricts = (provinceName?: string): LocationItem[] => {
  return [
    { code: 'D_CENTRAL', name: `Trung tâm ${provinceName || 'Thành phố/Thị xã'}` },
    { code: 'D_OTHER', name: 'Khu vực khác (Nhập chi tiết)' },
  ];
};

export const getFallbackWards = (districtName?: string): LocationItem[] => {
  return [
    { code: 'W_CENTRAL', name: `Phường/Xã trung tâm ${districtName || ''}` },
    { code: 'W_OTHER', name: 'Phường/Xã khác (Nhập chi tiết)' },
  ];
};
