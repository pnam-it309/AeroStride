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
    
    // Tiếng Việt bổ sung
    'xanh da trời': '#87CEEB', 'xanh biển': '#006994', 'đỏ tươi': '#EF4444', 
    'hồng đất': '#D89182', 'cam đất': '#CC7722', 'nâu đất': '#D2B48C', 
    'vàng chanh': '#FAFA33', 'vàng cát': '#E8CE8A', 'vàng nghệ': '#FFC30B',
    'hồng cánh sen': '#FF66CC', 'xám khói': '#BEBEBE', 'xanh rêu đậm': '#2E3B1C',
    'trắng kem': '#F5F5DC', 'trắng sữa': '#FDFFF5', 'vàng nhạt': '#FFFFE0', 
    'nâu tây': '#B08D6A', 'xanh pastel': '#AEC6CF', 'hồng pastel': '#FFD1DC', 
    'tím pastel': '#B39EB5', 'vàng nâu': '#D2B48C', 'xanh oliu': '#808000', 
    'cam nhạt': '#FFD580', 'xám tro': '#B2BEB5', 'xám đen': '#333333',
    'vàng hồng': '#B76E79', 'trắng tinh': '#FAFAFA', 'xanh denim': '#1560BD',
    'tím khoai môn': '#D1C4E9', 'nâu đen': '#3E2723', 'đỏ thẫm': '#8B0000',

    // Tiếng Anh
    'red': '#FF0000', 'blue': '#0000FF', 'green': '#00FF00',
    'yellow': '#FFFF00', 'black': '#000000', 'white': '#FFFFFF',
    'pink': '#FFC0CB', 'purple': '#800080', 'orange': '#FFA500',
    'brown': '#8B4513', 'gray': '#808080', 'grey': '#808080',
    'silver': '#C0C0C0', 'gold': '#FFD700', 'turquoise': '#40E0D0',
    'mint': '#98FF98', 'navy': '#000080', 'beige': '#F5F5DC',
    'cream': '#FFFDD0', 'cyan': '#00FFFF', 'magenta': '#FF00FF',
    'maroon': '#800000', 'indigo': '#4B0082', 'violet': '#EE82EE',
    
    // Tiếng Anh bổ sung
    'skyblue': '#87CEEB', 'teal': '#008080', 'olive': '#808000', 
    'coral': '#FF7F50', 'peach': '#FFE5B4', 'lavender': '#E6E6FA', 
    'burgundy': '#800020', 'mustard': '#FFDB58', 'salmon': '#FA8072', 
    'crimson': '#DC143C', 'plum': '#DDA0DD', 'khaki': '#C3B091',
    'ivory': '#FFFFF0', 'fuchsia': '#FF00FF', 'aqua': '#00FFFF',
    'tan': '#D2B48C', 'charcoal': '#36454F', 'sapphire': '#0F52BA', 
    'emerald': '#50C878', 'rose gold': '#B76E79', 'denim': '#1560BD',
    'rust': '#B7410E', 'taupe': '#483C32', 'lilac': '#C8A2C8'
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
