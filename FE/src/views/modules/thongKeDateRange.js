const padDatePart = (value) => String(value).padStart(2, '0');

export const formatLocalDate = (date) => {
    const year = date.getFullYear();
    const month = padDatePart(date.getMonth() + 1);
    const day = padDatePart(date.getDate());
    return `${year}-${month}-${day}`;
};

/**
 * Khoảng lũy kế năm hiện tại của dashboard. Hàm này cố ý không nhận bộ lọc
 * từ/đến ngày để hai card kênh bán hàng luôn độc lập với bộ lọc phía trên.
 */
export const getCurrentYearToDateRange = (now = new Date()) => ({
    year: now.getFullYear(),
    startDate: `${now.getFullYear()}-01-01`,
    endDate: formatLocalDate(now)
});
