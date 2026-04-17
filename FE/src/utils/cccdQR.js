// Tiện ích parse QR CCCD chuẩn phổ biến (có thể cần chỉnh lại theo thực tế)
export function parseCCCDQR(qrString) {
    // Ví dụ: "012345678|Nguyen Van A|01011990|Nam|123 Pho ABC, Ha Noi|0123456789"
    // Hoặc dạng chuẩn: "CIC|012345678|Nguyen Van A|01011990|Nam|123 Pho ABC, Ha Noi|0123456789"
    const parts = qrString.split('|');
    // Tùy định dạng thực tế, có thể cần điều chỉnh chỉ số
    return {
        ma: parts[1] || '',
        ten: parts[2] || '',
        ngaySinh: parts[3] || '',
        gioiTinh: parts[4]?.toLowerCase().includes('nam'),
        diaChi: parts[5] || '',
        sdt: parts[6] || ''
    };
}
