// Tiện ích parse QR CCCD chuẩn của Việt Nam
// Định dạng chuẩn: CCCD_ID|Old_ID|FullName|DOB|Gender|Address|IssueDate
export function parseCCCDQR(qrString) {
    if (!qrString) return null;
    
    const parts = qrString.split('|');
    
    // Phải có ít nhất 5 trường để coi là QR CCCD hợp lệ
    if (parts.length < 5) return null;

    // Chuyển đổi ngày sinh DDMMYYYY -> YYYY-MM-DD
    const rawDob = parts[3] || '';
    let formattedDob = '';
    if (rawDob.length === 8) {
        const day = rawDob.substring(0, 2);
        const month = rawDob.substring(2, 4);
        const year = rawDob.substring(4, 8);
        formattedDob = `${year}-${month}-${day}`;
    }

    // Tìm trường nào giống số điện thoại (Nếu có - thường CCCD không có nhưng hỗ trợ nếu QR có thêm)
    let detectedSdt = '';
    parts.forEach(p => {
        const cleaned = p.trim();
        if (/^0\d{9}$/.test(cleaned)) {
            detectedSdt = cleaned;
        }
    });

    return {
        ma: '', // Người dùng yêu cầu không lấy mã CCCD làm mã định danh
        ten: parts[2] || '',      // Họ và tên
        ngaySinh: formattedDob,   // Ngày sinh chuẩn ISO
        gioiTinh: (parts[4] === 'Nam' || parts[4]?.toLowerCase().includes('nam')), // Giới tính
        diaChi: parts[5] || '',   // Địa chỉ thường trú
        sdt: detectedSdt,         // Số điện thoại nếu tìm thấy
        cccd: parts[0] || '',     // Vẫn giữ trường cccd để dùng nếu cần nhưng không gán vào 'ma'
    };
}
