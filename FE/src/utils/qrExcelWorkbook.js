import api from '@/services/apiService';
import { API_ADMIN } from '@/constants/apiPaths';

export const triggerBlobDownload = (blob, fileName) => {
    const fileUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = fileUrl;
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(fileUrl);
};

export const exportQrImageZip = async ({ fileName, items }) => {
    try {
        const payload = {
            fileName,
            items: items.map(item => ({
                baseName: item.baseName || item.name,
                dataUrl: item.dataUrl
            }))
        };
        const response = await api.post(`${API_ADMIN.SAN_PHAM}/export/qr-zip`, payload, {
            responseType: 'blob'
        });
        triggerBlobDownload(response.data, fileName);
    } catch (error) {
        console.error('Lỗi khi xuất ZIP QR:', error);
        throw error;
    }
};

export const exportQrRowsToWorkbook = async ({ fileName, sheetName, rows, qrDataUrls }) => {
    try {
        const payload = {
            fileName,
            sheetName,
            rows: rows?.map(row => ({
                productName: row.productName,
                sku: row.sku,
                color: row.color,
                size: row.size
            })) || [],
            qrDataUrls: qrDataUrls || []
        };
        const response = await api.post(`${API_ADMIN.SAN_PHAM}/export/qr-excel`, payload, {
            responseType: 'blob'
        });
        triggerBlobDownload(response.data, fileName);
    } catch (error) {
        console.error('Lỗi khi xuất Excel QR:', error);
        throw error;
    }
};
