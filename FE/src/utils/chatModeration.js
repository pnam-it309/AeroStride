/**
 * Hệ thống kiểm duyệt ngôn từ & lọc nội dung chat AeroStride
 * Xử lý phát hiện:
 * 1. Từ ngữ khiêu dâm, đồi trụy, tình dục, mại dâm (NSFW / Adult content)
 * 2. Ngôn từ xúc phạm, chửi thề, lăng mạ thô tục (Profanity / Toxic language)
 * 3. Chuỗi gõ phím linh tinh, spam ký tự vô nghĩa (Keyboard mash / Gibberish)
 */

// Danh sách từ khóa khiêu dâm, đồi trụy, nhạy cảm
const NSFW_KEYWORDS = [
    'sex',
    'porn',
    'xxx',
    'gai goi',
    'gái gọi',
    'phim heo',
    'phim sex',
    'phim 18+',
    '18+',
    'khieu dam',
    'khiêu dâm',
    'thu dam',
    'thủ dâm',
    'dam dang',
    'dâm đãng',
    'dâm tặc',
    'dam tac',
    'lo clip',
    'lộ clip',
    'ban dam',
    'bán dâm',
    'mua dam',
    'mua dâm',
    'hap diem',
    'hấp diêm',
    'hiep dam',
    'hiếp dâm',
    'chich',
    'chịch',
    'nude',
    'khoa than',
    'khoả thân',
    'khoe hang',
    'khoe hàng',
    'vu to',
    'vú to',
    'nguc bu',
    'ngực bự',
    'buom',
    'bướm',
    'chim to',
    'thong dit',
    'thông đít',
    'oral sex',
    'bu liem',
    'bú liếm',
    'quan he tinh duc',
    'nung',
    'nứng',
    'dam duc',
    'dâm dục',
    'dit nhau',
    'địt nhau',
    'du nhau',
    'đụ nhau',
    'show hang',
    'show hàng'
];

// Danh sách từ ngữ tục tĩu, chửi thề, lăng mạ
const PROFANITY_KEYWORDS = [
    'buoi',
    'buồi',
    'cac',
    'cặc',
    'con cac',
    'con cặc',
    'lon',
    'lồn',
    'con lon',
    'con lồn',
    'du me',
    'đụ mẹ',
    'du ma',
    'đụ má',
    'duma',
    'dume',
    'dit me',
    'địt mẹ',
    'dit cu',
    'địt cụ',
    'dkm',
    'đkm',
    'dm',
    'đm',
    'vcl',
    'vkl',
    'vai lon',
    'vãi lồn',
    'vai cut',
    'vãi cứt',
    'loz',
    'clmm',
    'clgt',
    'me may',
    'mẹ mày',
    'bo may',
    'bố mày',
    'oc cho',
    'óc chó',
    'ngu lon',
    'ngu lồn',
    'con di',
    'con đĩ',
    'di me',
    'đĩ mẹ',
    'di cho',
    'đĩ chó',
    'fuck',
    'fucking',
    'bitch',
    'asshole',
    'motherfucker',
    'dick',
    'pussy',
    'slut',
    'whore',
    'cunt',
    'bastard'
];

// Mẫu bàn phím gõ linh tinh (Keyboard mash)
const GIBBERISH_PATTERNS = [
    'asdfgh',
    'sdfghj',
    'dfghjk',
    'fghjkl',
    'qwerty',
    'wertyu',
    'ertyui',
    'rtyuio',
    'tyuiop',
    'zxcvbn',
    'xcvbnm',
    '123456',
    '234567',
    '345678',
    '456789',
    'abcdef',
    'jklfds',
    'ghjkjh',
    'hjkl;'
];

// Chuẩn hóa văn bản tiếng Việt để dễ so khớp
const normalizeText = (str) => {
    if (!str) return '';
    let normalized = str.toLowerCase().trim();

    // Loại bỏ các ký tự phân tách cố tình chèn vào giữa từ (ví dụ: d.m, d*m, s_e_x, l-o-n)
    normalized = normalized.replace(/[\.\,\-\_\*\#\@\$\%\^\&\(\)\!\?\+\=\/\\\|\<\>]/g, '');

    // Chuẩn hóa dấu tiếng Việt thành không dấu
    const withoutAccents = normalized
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/đ/g, 'd')
        .replace(/Đ/g, 'd');

    return {
        raw: str.toLowerCase().trim(),
        cleanRaw: normalized,
        noAccents: withoutAccents
    };
};

/**
 * Kiểm tra xem tin nhắn có chứa nội dung không hợp lệ hay không
 * @param {string} text - Tin nhắn cần kiểm tra
 * @returns {object} { isValid: boolean, reason?: string, type?: 'PROFANITY' | 'NSFW' | 'GIBBERISH' | 'SPAM' }
 */
export function validateChatMessage(text) {
    if (!text || typeof text !== 'string') {
        return { isValid: false, reason: 'Tin nhắn không được để trống.', type: 'EMPTY' };
    }

    const trimmed = text.trim();
    if (trimmed.length === 0) {
        return { isValid: false, reason: 'Tin nhắn không được để trống.', type: 'EMPTY' };
    }

    if (trimmed.length > 1000) {
        return { isValid: false, reason: 'Tin nhắn quá dài (tối đa 1000 ký tự).', type: 'SPAM' };
    }

    const { cleanRaw, noAccents } = normalizeText(trimmed);

    // 1. Kiểm tra lặp ký tự vô nghĩa liên tiếp (ví dụ: aaaaaaaa, 11111111, .........)
    const repeatedCharRegex = /(.)\1{5,}/;
    if (repeatedCharRegex.test(trimmed)) {
        return {
            isValid: false,
            reason: 'Tin nhắn chứa chuỗi ký tự lặp vô nghĩa hoặc spam phím. Vui lòng nhập nội dung rõ ràng.',
            type: 'GIBBERISH'
        };
    }

    // 2. Kiểm tra chuỗi phím lộn xộn (Keyboard mash)
    for (const pattern of GIBBERISH_PATTERNS) {
        if (cleanRaw.includes(pattern) || noAccents.includes(pattern)) {
            return {
                isValid: false,
                reason: 'Tin nhắn có dấu hiệu gõ phím linh tinh vô nghĩa. Vui lòng nhập câu hỏi hoặc yêu cầu cụ thể.',
                type: 'GIBBERISH'
            };
        }
    }

    // 3. Kiểm tra từ ngữ khiêu dâm, đồi trụy (NSFW)
    for (const keyword of NSFW_KEYWORDS) {
        const kwNorm = keyword.toLowerCase();
        // Kiểm tra nguyên từ hoặc xuất hiện trong văn bản
        const regex = new RegExp(`(^|\\s|\\b)${kwNorm.replace(/ /g, '\\s+')}($|\\s|\\b)`, 'i');
        if (regex.test(trimmed) || regex.test(noAccents) || cleanRaw.includes(kwNorm.replace(/\s+/g, ''))) {
            return {
                isValid: false,
                reason: 'Tin nhắn của bạn chứa nội dung đồi trụy hoặc nhạy cảm không phù hợp với tiêu chuẩn cộng đồng AeroStride. Vui lòng sử dụng ngôn từ đúng mực.',
                type: 'NSFW'
            };
        }
    }

    // 4. Kiểm tra từ ngữ tục tĩu, chửi thề, xúc phạm (Profanity)
    for (const keyword of PROFANITY_KEYWORDS) {
        const kwNorm = keyword.toLowerCase();
        const regex = new RegExp(`(^|\\s|\\b)${kwNorm.replace(/ /g, '\\s+')}($|\\s|\\b)`, 'i');
        if (regex.test(trimmed) || regex.test(noAccents) || cleanRaw.includes(kwNorm.replace(/\s+/g, ''))) {
            return {
                isValid: false,
                reason: 'Tin nhắn của bạn chứa ngôn từ thô tục hoặc thiếu tôn trọng. Vui lòng sử dụng ngôn từ lịch sự để được trợ lý AI và nhân viên hỗ trợ.',
                type: 'PROFANITY'
            };
        }
    }

    // 5. Kiểm tra chuỗi toàn phụ âm vô nghĩa dài (trên 7 phụ âm liên tiếp không có nguyên âm)
    const longConsonantRegex = /[bcdfghjklmnpqrstvwxz]{7,}/i;
    if (longConsonantRegex.test(noAccents.replace(/\s+/g, ''))) {
        return {
            isValid: false,
            reason: 'Nội dung nhập không rõ nghĩa hoặc chứa ký tự spam. Vui lòng kiểm tra lại câu hỏi của bạn.',
            type: 'GIBBERISH'
        };
    }

    return { isValid: true };
}
