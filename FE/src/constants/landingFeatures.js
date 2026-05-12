export const LANDING_FEATURES = [
    { title: '3D MESH', desc: 'Vải dệt đa lớp siêu thoáng khí.', icon: 'mdi-air-filter', color: '#2962FF', size: 's' },
    { title: 'ULTRA LIGHT', desc: 'Chỉ 220g cho mỗi bước chạy.', icon: 'mdi-feather', color: '#00B0FF', size: 'm' },
    { title: 'DUAL AIR', desc: 'Đệm khí kép hấp thụ chấn động.', icon: 'mdi-atom', color: '#6200EA', size: 'l' },
    { title: 'SMART GRIP', desc: 'Đế cao su bám đường tuyệt đối.', icon: 'mdi-navigation-variant', color: '#00E676', size: 's' },
    { title: 'ECO MATERIAL', desc: '65% vật liệu tái chế bền bỉ.', icon: 'mdi-leaf', color: '#FFD600', size: 'm' },
    { title: 'HYPER-FLEX', desc: 'Co giãn theo từng nhịp chuyển động.', icon: 'mdi-cached', color: '#FF3D00', size: 's' },
    { title: 'PRO-STABILITY', desc: 'Khung gót bảo vệ cổ chân vững chãi.', icon: 'mdi-shield-check', color: '#7C4DFF', size: 'l' },
    { title: 'QUICK-DRY', desc: 'Công nghệ khô nhanh cấp tốc.', icon: 'mdi-water-off', color: '#00B8D4', size: 'm' },
    { title: 'NIGHT-GLOW', desc: 'Phản quang 360 độ an toàn ban đêm.', icon: 'mdi-brightness-6', color: '#FFAB00', size: 's' },
    { title: 'BIO-TECH', desc: 'Lót giày kháng khuẩn tự nhiên.', icon: 'mdi-bacteria-outline', color: '#C6FF00', size: 'm' },
    { title: 'AERO-LOCK', desc: 'Hệ thống thắt dây thông minh.', icon: 'mdi-lock-outline', color: '#FF1744', size: 's' },
    { title: 'ENERGY-UP', desc: 'Hoàn trả năng lượng tối đa.', icon: 'mdi-trending-up', color: '#3D5AFE', size: 'l' }
];

const buildLoop = (items, repeatCount, offset = 0) => {
    const output = [];

    for (let repeat = 0; repeat < repeatCount; repeat += 1) {
        output.push(...items.slice(offset));
        output.push(...items.slice(0, offset));
    }

    return output;
};

export const LANDING_FEATURE_COLUMNS = [
    buildLoop(LANDING_FEATURES, 2),
    buildLoop(LANDING_FEATURES.slice().reverse(), 2),
    buildLoop([...LANDING_FEATURES.slice(4), ...LANDING_FEATURES.slice(0, 4)], 2),
    buildLoop([...LANDING_FEATURES.slice(8), ...LANDING_FEATURES.slice(0, 8)], 2),
    buildLoop([...LANDING_FEATURES.slice(2), ...LANDING_FEATURES.slice(0, 2)], 2),
    buildLoop([...LANDING_FEATURES.slice(6), ...LANDING_FEATURES.slice(0, 6)], 2)
];
