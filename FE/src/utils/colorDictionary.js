export const COLOR_DICTIONARY = {
    // Tiếng Việt
    'đỏ': '#FF0000', 'xanh dương': '#0000FF', 'xanh lá': '#00FF00', 
    'vàng': '#FFFF00', 'đen': '#000000', 'trắng': '#FFFFFF', 
    'hồng': '#FFC0CB', 'tím': '#800080', 'cam': '#FFA500', 
    'nâu': '#8B4513', 'xám': '#808080', 'ghi': '#808080', 
    'bạc': '#C0C0C0', 'vàng đồng': '#FFD700', 'xanh ngọc': '#40E0D0', 
    'xanh mint': '#98FF98', 'xanh rêu': '#4A5D23', 'xanh navy': '#000080', 
    'be': '#F5F5DC', 'kem': '#FFFDD0', 'kem sữa': '#FFFFF0',
    'đỏ đô': '#800000', 'đỏ mận': '#800000', 'xanh coban': '#0047AB',
    'xanh tím': '#8A2BE2', 'tím nhạt': '#DDA0DD', 'hồng phấn': '#FFB6C1',
    'xanh dương nhạt': '#ADD8E6', 'xanh mạ': '#7FFF00', 'tím than': '#4B0082',
    
    // Tiếng Anh
    'red': '#FF0000', 'blue': '#0000FF', 'green': '#00FF00', 
    'yellow': '#FFFF00', 'black': '#000000', 'white': '#FFFFFF', 
    'pink': '#FFC0CB', 'purple': '#800080', 'orange': '#FFA500', 
    'brown': '#8B4513', 'gray': '#808080', 'grey': '#808080', 
    'silver': '#C0C0C0', 'gold': '#FFD700', 'turquoise': '#40E0D0', 
    'mint': '#98FF98', 'navy': '#000080', 'beige': '#F5F5DC', 
    'cream': '#FFFDD0', 'cyan': '#00FFFF', 'magenta': '#FF00FF',
    'maroon': '#800000', 'indigo': '#4B0082', 'violet': '#EE82EE'
};

const removeAccents = (str) => str.normalize('NFD').replace(/[\u0300-\u036f]/g, '');

export const getColorHexByName = (name) => {
    if (!name) return null;
    const normalizedName = name.trim().toLowerCase();
    const normalizedNoAccent = removeAccents(normalizedName);
    
    for (const [key, hex] of Object.entries(COLOR_DICTIONARY)) {
        if (normalizedName === key || normalizedNoAccent === removeAccents(key)) {
            return hex;
        }
    }
    return null;
};

export const getColorNameByHex = (hex) => {
    if (!hex) return null;
    const normalizedHex = hex.trim().toUpperCase();
    
    for (const [key, val] of Object.entries(COLOR_DICTIONARY)) {
        if (val.toUpperCase() === normalizedHex) {
            // Trả về tên viết hoa chữ cái đầu
            return key.charAt(0).toUpperCase() + key.slice(1);
        }
    }
    return null;
};
