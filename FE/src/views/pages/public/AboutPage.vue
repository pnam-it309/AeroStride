<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import MainFooter from '@/components/shared/MainFooter.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { useSeoMeta } from '@/composables/useSeoMeta';

import { PATH } from '@/router/routePaths';
import { dichVuGioiThieu } from '@/services/public/dichVuGioiThieu';

const router = useRouter();
const { setSeoMeta } = useSeoMeta();

const activeTab = ref('story');
const currentSlide = ref(0);

const heroSlides = [
    {
        image: 'https://images.unsplash.com/photo-1556906781-9a412961c28c?auto=format&fit=crop&q=80&w=1600',
        badge: 'EST. 2024 • AEROSTRIDE VIỆT NAM',
        title: 'BƯỚC CHÂN CỦA SỰ',
        highlight: 'ĐAM MÊ & BỨT PHÁ',
        desc: 'Hành trình mang lại chuẩn mực mua sắm giày thể thao chính hãng đỉnh cao, hiện đại và tràn đầy cảm hứng cho người Việt.'
    },
    {
        image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&q=80&w=1600',
        badge: 'CÔNG NGHỆ & TỐC ĐỘ',
        title: 'TRẢI NGHIỆM ĐỈNH CAO TỪNG',
        highlight: 'BƯỚC CHẠY',
        desc: 'Đồng hành cùng hàng triệu vận động viên và người đam mê thể thao chinh phục mọi cung đường.'
    },
    {
        image: 'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&q=80&w=1600',
        badge: '100% CHÍNH HÃNG',
        title: 'THƯƠNG HIỆU QUỐC TẾ',
        highlight: 'NIKE • ADIDAS • PUMA',
        desc: 'Tuyển chọn khắt khe những mẫu giày thời thượng, bền bỉ và hỗ trợ tối đa cho bàn chân của bạn.'
    }
];

const statsData = ref({
    totalProducts: 500,
    totalBrands: 12,
    totalStores: 50,
    satisfactionRate: '99.8%'
});

const fetchStatsFromApi = async () => {
    try {
        const res = await dichVuGioiThieu.layThongKeGioiThieu();
        if (res?.data) {
            statsData.value = { ...statsData.value, ...res.data };
        }
    } catch (e) {
        console.error('Lỗi lấy thống kê giới thiệu:', e);
    }
};

const milestones = [
    {
        year: '2024',
        period: 'Giai Đoạn Khởi Khởi Tạo',
        title: 'Khởi Đầu Đam Mê & Khai Trương Cửa Hàng Đầu Tiên',
        desc: 'AeroStride chính thức ra mắt cửa hàng Flagship tại Hà Nội với tầm nhìn mang lại những đôi giày thể thao chính hãng chất lượng đỉnh cao cho người Việt.',
        image: 'https://images.unsplash.com/photo-1556906781-9a412961c28c?auto=format&fit=crop&q=80&w=700',
        badge: 'Cột mốc khởi sự',
        icon: 'mdi-flag-variant-outline',
        stats: '10.000+ Đôi giày trao tay',
        tags: ['Hà Nội Flagship', '100% Chính Hãng', 'Đổi Trả 30 Ngày']
    },
    {
        year: '2025',
        period: 'Giai Đoạn Tăng Tốc',
        title: 'Bứt Phá Quy Mô & Hệ Thống Đa Kênh Toàn Quốc',
        desc: 'Mở rộng 20 chi nhánh tại TP.HCM, Đà Nẵng, Hải Phòng. Ra mắt website thương mại điện tử thế hệ mới cùng dịch vụ giao hàng nhanh và hỗ trợ tư vấn 24/7.',
        image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&q=80&w=700',
        badge: 'Tăng trưởng vượt bậc',
        icon: 'mdi-rocket-launch-outline',
        stats: '20 Chi nhánh toàn quốc',
        tags: ['Đa Kênh Omni-channel', 'Giao Nhanh 2H', '500.000+ Khách hàng']
    },
    {
        year: '2026',
        period: 'Giai Đoạn Đột Phá',
        title: 'Tiên Phong Công Nghệ AI & Hệ Sinh Thái Thể Thao',
        desc: 'Tích hợp trợ lý AI thông minh tư vấn chọn giày theo dáng chân, mở rộng 50+ chi nhánh toàn quốc và đồng hành tài trợ hơn 15 giải chạy Marathon lớn.',
        image: 'https://images.unsplash.com/photo-1519766304817-4f37bda74a29?auto=format&fit=crop&q=80&w=700',
        badge: 'Công nghệ tiên phong',
        icon: 'mdi-creation',
        stats: '50+ Cửa Hàng & 1M+ Khách hàng',
        tags: ['AeroStride AI Advisor', 'Đồng hành Marathon', 'Top 1 Nhà Phân Phối']
    },
    {
        year: '2027+',
        period: 'Tầm Nhìn Tương Lai',
        title: 'Vươn Tầm Khu Vực & Xuất Khẩu Thương Hiệu Thể Thao',
        desc: 'Mở rộng mạng lưới phân phối sang các quốc gia Đông Nam Á, hợp tác đồng hành cùng các vận động viên Olympic và ra mắt bộ sưu tập độc quyền AeroStride Signature.',
        image: 'https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&q=80&w=700',
        badge: 'Khát vọng vươn xa',
        icon: 'mdi-earth',
        stats: 'Mục tiêu vươn tầm Đông Nam Á',
        tags: ['Thương Hiệu Việt Nam', 'Olympic 2028', 'Thời Trang Thể Thao Bền Vững']
    }
];

const coreValues = [
    {
        icon: 'mdi-shield-check-outline',
        title: '100% Chính Hãng',
        desc: 'Cam kết tất cả sản phẩm đều nhập khẩu trực tiếp từ các thương hiệu hàng đầu thế giới với chứng nhận nguồn gốc rõ ràng.'
    },
    {
        icon: 'mdi-lightning-bolt-outline',
        title: 'Giao Hàng Siêu Tốc',
        desc: 'Hệ thống vận chuyển tối ưu giúp sản phẩm đến tay bạn trong thời gian ngắn nhất. Không để đam mê phải chờ đợi.'
    },
    {
        icon: 'mdi-refresh-auto',
        title: 'Đổi Trả 30 Ngày',
        desc: 'Chính sách đổi trả linh hoạt và miễn phí trong vòng 30 ngày nếu không vừa size hoặc không ưng ý.'
    },
    {
        icon: 'mdi-heart-pulse',
        title: 'Đồng Hành Thể Thao',
        desc: 'Không chỉ bán hàng, chúng tôi luôn tạo dựng cộng đồng kết nối những người yêu thích chạy bộ và vận động.'
    }
];

const teamMembers = ref([
    {
        name: 'Hoàng Phương Nam',
        role: 'Nhà Sáng Lập & Tổng Giám Đốc (CEO)',
        image: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&q=80&w=600',
        quote: 'Mỗi bước chạy của khách hàng là động lực để AeroStride hoàn thiện và vươn xa.'
    },
    {
        name: 'Phí Thu Trang',
        role: 'Giám Đốc Điều Hành & Vận Hành (COO)',
        image: 'https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&q=80&w=600',
        quote: 'Tối ưu vận hành và trải nghiệm mua sắm hoàn hảo là kim chỉ nam trong mọi hành động.'
    },
    {
        name: 'Lê Thị Thu Huyền',
        role: 'Giám Đốc Trải Nghiệm Khách Hàng (CXO)',
        image: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=600',
        quote: 'Sự hài lòng tuyệt đối của khách hàng là thước đo giá trị lớn nhất của AeroStride.'
    },
    {
        name: 'Bùi Thị Yến',
        role: 'Giám Đốc Marketing & Phát Triển (CMO)',
        image: 'https://images.unsplash.com/photo-1573497019940-1c28c88b4f3e?auto=format&fit=crop&q=80&w=600',
        quote: 'Chiến lược truyền thông và sự gắn kết khách hàng là nhịp đập của AeroStride.'
    },
    {
        name: 'Nguyễn Huy Đức',
        role: 'Giám Đốc Công Nghệ (CTO)',
        image: 'https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&q=80&w=600',
        quote: 'Ứng dụng công nghệ AI và trải nghiệm số vượt trội để phục vụ khách hàng tốt nhất.'
    }
]);

const fetchLeadershipTeam = async () => {
    try {
        const res = await dichVuGioiThieu.layDanhSachLanhDao();
        if (res?.data && Array.isArray(res.data) && res.data.length > 0) {
            teamMembers.value = res.data;
        }
    } catch (e) {
        console.error('Lỗi lấy danh sách ban lãnh đạo:', e);
    }
};

const goToProducts = () => {
    router.push(PATH.SHOES);
};

const scrollToSection = (id) => {
    const el = document.getElementById(id);
    if (el) {
        el.scrollIntoView({ behavior: 'smooth' });
    }
};

onMounted(() => {
    window.scrollTo(0, 0);
    setSeoMeta({
        title: 'Giới Thiệu | AeroStride',
        description: 'Khám phá câu chuyện thương hiệu AeroStride, sứ mệnh và hành trình mang đến những đôi giày thể thao đẳng cấp nhất cho người Việt.'
    });
    fetchStatsFromApi();
});
</script>

<template>
    <div class="app-container bg-white font-body">
        <MainHeader />

        <main class="main-content">
            <!-- Hero Banner Slideshow -->
            <section class="about-hero-carousel position-relative">
                <v-carousel
                    v-model="currentSlide"
                    cycle
                    :interval="4500"
                    height="540"
                    hide-delimiter-background
                    show-arrows="hover"
                    class="about-carousel"
                >
                    <v-carousel-item
                        v-for="(slide, index) in heroSlides"
                        :key="index"
                        :src="slide.image"
                        cover
                    >
                        <div class="carousel-overlay d-flex align-center">
                            <v-container class="text-center text-white py-12">
                                <v-chip
                                    variant="outlined"
                                    size="small"
                                    class="font-weight-bold mb-4 tracking-wider"
                                    style="color: #ffffff !important; border-color: rgba(255, 255, 255, 0.5) !important; background: rgba(0,0,0,0.3);"
                                >
                                    {{ slide.badge }}
                                </v-chip>
                                <h1 class="hero-title font-weight-black mb-4 animate-up">
                                    {{ slide.title }} <span class="text-gradient shimmer-effect">{{ slide.highlight }}</span>
                                </h1>
                                <p class="hero-subtitle mb-8 max-w-700 mx-auto">
                                    {{ slide.desc }}
                                </p>
                                <div class="d-flex justify-center flex-wrap ga-4">
                                    <v-btn
                                        color="primary"
                                        size="x-large"
                                        rounded="pill"
                                        class="font-weight-bold px-8 shadow-blue text-white"
                                        prepend-icon="mdi-arrow-down"
                                        @click="scrollToSection('story-section')"
                                    >
                                        Khám Phá Câu Chuyện
                                    </v-btn>
                                    <v-btn
                                        variant="outlined"
                                        size="x-large"
                                        rounded="pill"
                                        class="font-weight-bold px-8"
                                        style="color: #ffffff !important; border-color: rgba(255, 255, 255, 0.6) !important; background: rgba(0,0,0,0.25);"
                                        append-icon="mdi-shopping-outline"
                                        @click="goToProducts"
                                    >
                                        Xem Sản Phẩm
                                    </v-btn>
                                </div>
                            </v-container>
                        </div>
                    </v-carousel-item>
                </v-carousel>
            </section>

            <!-- Quick Navigation Tabs -->
            <section class="border-b bg-slate-50 sticky-nav-section" id="story-section">
                <v-container>
                    <div class="d-flex justify-center flex-wrap ga-2 py-4">
                        <v-btn
                            :variant="activeTab === 'story' ? 'flat' : 'text'"
                            :color="activeTab === 'story' ? 'primary' : 'default'"
                            class="rounded-pill font-weight-bold text-none px-6"
                            prepend-icon="mdi-book-open-page-variant"
                            @click="activeTab = 'story'"
                        >
                            Câu Chuyện Thương Hiệu
                        </v-btn>
                        <v-btn
                            :variant="activeTab === 'values' ? 'flat' : 'text'"
                            :color="activeTab === 'values' ? 'primary' : 'default'"
                            class="rounded-pill font-weight-bold text-none px-6"
                            prepend-icon="mdi-star-outline"
                            @click="activeTab = 'values'"
                        >
                            Giá Trị Cốt Lõi
                        </v-btn>
                        <v-btn
                            :variant="activeTab === 'timeline' ? 'flat' : 'text'"
                            :color="activeTab === 'timeline' ? 'primary' : 'default'"
                            class="rounded-pill font-weight-bold text-none px-6"
                            prepend-icon="mdi-timeline-outline"
                            @click="activeTab = 'timeline'"
                        >
                            Hành Trình Phát Triển
                        </v-btn>
                        <v-btn
                            :variant="activeTab === 'team' ? 'flat' : 'text'"
                            :color="activeTab === 'team' ? 'primary' : 'default'"
                            class="rounded-pill font-weight-bold text-none px-6"
                            prepend-icon="mdi-account-group-outline"
                            @click="activeTab = 'team'"
                        >
                            Đội Ngũ Sáng Lập
                        </v-btn>
                    </div>
                </v-container>
            </section>

            <!-- Dynamic Tab Contents -->
            <section class="py-16">
                <v-container>
                    <!-- Tab 1: Story -->
                    <div v-if="activeTab === 'story'" class="animate-fade-in">
                        <v-row align="center" class="mb-16">
                            <v-col cols="12" md="6">
                                <v-chip color="primary" variant="tonal" size="small" class="font-weight-bold mb-3">VỀ AEROSTRIDE</v-chip>
                                <h2 class="text-h3 font-weight-black text-slate-900 mb-6">Chúng Tôi Là Ai?</h2>
                                <p class="text-body-1 text-slate-700 leading-relaxed mb-4">
                                    Được khởi nguồn vào năm 2024, <strong>AeroStride</strong> sinh ra từ tình yêu mãnh liệt với các bộ môn thể thao và văn hóa Sneakers năng động. Chúng tôi đặt mục tiêu xóa bỏ mọi lo ngại về hàng giả, hàng nhái bằng việc cung cấp 100% sản phẩm chính hãng với tiêu chuẩn dịch vụ khách hàng cao cấp nhất.
                                </p>
                                <p class="text-body-1 text-slate-700 leading-relaxed mb-6">
                                    "Aero" đại diện cho sự nhẹ nhàng, bứt phá không giới hạn, còn "Stride" là từng sải bước tự tin hướng về phía trước. Mỗi đôi giày bạn mua tại AeroStride không chỉ là một phụ kiện thời trang, mà là người bạn đồng hành tin cậy trên từng chặng đường chinh phục mục tiêu cá nhân.
                                </p>
                                <div class="d-flex ga-4">
                                    <div class="border-l-4 border-primary pl-4">
                                        <div class="text-h6 font-weight-black text-primary">SỨ MỆNH</div>
                                        <div class="text-caption text-slate-600">Truyền cảm hứng và nâng tầm sức khỏe người Việt qua từng bước chạy.</div>
                                    </div>
                                    <div class="border-l-4 border-secondary pl-4">
                                        <div class="text-h6 font-weight-black text-secondary">TẦM NHÌN</div>
                                        <div class="text-caption text-slate-600">Trở thành chuỗi bán lẻ giày thể thao công nghệ hàng đầu Đông Nam Á.</div>
                                    </div>
                                </div>
                            </v-col>
                            <v-col cols="12" md="6">
                                <v-row>
                                    <v-col cols="6">
                                        <v-img
                                            src="https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&q=80&w=600"
                                            class="rounded-2xl elevation-3 hover-lift mb-4"
                                            height="260"
                                            cover
                                        ></v-img>
                                        <v-img
                                            src="https://images.unsplash.com/photo-1514989940723-e8e51635b782?auto=format&fit=crop&q=80&w=600"
                                            class="rounded-2xl elevation-3 hover-lift"
                                            height="180"
                                            cover
                                        ></v-img>
                                    </v-col>
                                    <v-col cols="6" class="pt-8">
                                        <v-img
                                            src="https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?auto=format&fit=crop&q=80&w=600"
                                            class="rounded-2xl elevation-3 hover-lift mb-4"
                                            height="180"
                                            cover
                                        ></v-img>
                                        <v-img
                                            src="https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&q=80&w=600"
                                            class="rounded-2xl elevation-3 hover-lift"
                                            height="260"
                                            cover
                                        ></v-img>
                                    </v-col>
                                </v-row>
                            </v-col>
                        </v-row>
                    </div>

                    <!-- Tab 2: Values -->
                    <div v-else-if="activeTab === 'values'" class="animate-fade-in">
                        <div class="text-center max-w-700 mx-auto mb-12">
                            <v-chip color="primary" variant="tonal" size="small" class="font-weight-bold mb-3">TIÊU CHUẨN DỊCH VỤ</v-chip>
                            <h2 class="text-h3 font-weight-black text-slate-900 mb-3">Giá Trị Cốt Lõi</h2>
                            <p class="text-slate-600">Những cam kết khẳng định đẳng cấp thương hiệu AeroStride</p>
                        </div>
                        <v-row>
                            <v-col cols="12" sm="6" md="3" v-for="(val, i) in coreValues" :key="i">
                                <v-card class="h-100 pa-6 rounded-xl border hover-lift text-center">
                                    <div class="icon-wrap mx-auto mb-4 bg-primary-lighten">
                                        <v-icon :icon="val.icon" size="36" color="primary"></v-icon>
                                    </div>
                                    <h3 class="text-h6 font-weight-bold text-slate-900 mb-3">{{ val.title }}</h3>
                                    <p class="text-body-2 text-slate-600 leading-relaxed">{{ val.desc }}</p>
                                </v-card>
                            </v-col>
                        </v-row>
                    </div>

                    <!-- Tab 3: Timeline -->
                    <div v-else-if="activeTab === 'timeline'" class="animate-fade-in">
                        <div class="text-center max-w-700 mx-auto mb-12">
                            <v-chip color="primary" variant="tonal" size="small" class="font-weight-bold mb-3">LỊCH SỬ & TẦM NHÌN</v-chip>
                            <h2 class="text-h3 font-weight-black text-slate-900 mb-3">Hành Trình Phát Triển</h2>
                            <p class="text-slate-600">Từ những bước đi đầu tiên đến hệ sinh thái giày thể thao chính hãng hàng đầu Việt Nam</p>
                        </div>
                        <v-row justify="center">
                            <v-col cols="12" lg="11">
                                <v-timeline align="start" side="end" class="timeline-premium">
                                    <v-timeline-item
                                        v-for="(item, idx) in milestones"
                                        :key="idx"
                                        size="large"
                                        dot-color="#1e257c"
                                        fill-dot
                                        class="timeline-item-premium mb-8"
                                    >
                                        <template #icon>
                                            <div class="timeline-year-dot">
                                                <v-icon size="20" color="white">{{ item.icon }}</v-icon>
                                            </div>
                                        </template>

                                        <v-card class="timeline-card-premium rounded-2xl overflow-hidden elevation-3 border">
                                            <v-row no-gutters>
                                                <!-- Image Column -->
                                                <v-col cols="12" md="4" class="position-relative">
                                                    <v-img
                                                        :src="item.image"
                                                        height="100%"
                                                        min-height="220"
                                                        cover
                                                        class="timeline-img"
                                                    >
                                                        <div class="img-gradient-overlay d-flex flex-column justify-space-between pa-4">
                                                            <v-chip size="small" color="primary" variant="flat" class="font-weight-black elevation-2 align-self-start">
                                                                NĂM {{ item.year }}
                                                            </v-chip>
                                                            <div class="timeline-stats-badge pa-2 rounded-lg text-caption font-weight-bold text-white">
                                                                <v-icon size="14" class="mr-1" color="#38bdf8">mdi-trophy-outline</v-icon>
                                                                {{ item.stats }}
                                                            </div>
                                                        </div>
                                                    </v-img>
                                                </v-col>

                                                <!-- Content Column -->
                                                <v-col cols="12" md="8" class="pa-6 d-flex flex-column justify-space-between">
                                                    <div>
                                                        <div class="d-flex align-center justify-space-between flex-wrap ga-2 mb-2">
                                                            <span class="text-caption font-weight-bold text-primary text-uppercase tracking-wider">
                                                                {{ item.period }}
                                                            </span>
                                                            <v-chip size="x-small" variant="tonal" color="success" class="font-weight-bold">
                                                                {{ item.badge }}
                                                            </v-chip>
                                                        </div>

                                                        <h3 class="text-h5 font-weight-black text-slate-900 mb-3 leading-snug">
                                                            {{ item.title }}
                                                        </h3>

                                                        <p class="text-slate-600 text-body-1 leading-relaxed mb-4">
                                                            {{ item.desc }}
                                                        </p>
                                                    </div>

                                                    <div class="d-flex flex-wrap ga-2 pt-2 border-t">
                                                        <v-chip
                                                            v-for="(tag, tIdx) in item.tags"
                                                            :key="tIdx"
                                                            size="small"
                                                            variant="flat"
                                                            color="grey-lighten-4"
                                                            class="text-slate-700 font-weight-medium"
                                                        >
                                                            <v-icon start size="12" color="#1e257c">mdi-check-circle</v-icon>
                                                            {{ tag }}
                                                        </v-chip>
                                                    </div>
                                                </v-col>
                                            </v-row>
                                        </v-card>
                                    </v-timeline-item>
                                </v-timeline>
                            </v-col>
                        </v-row>
                    </div>

                    <!-- Tab 4: Team (Dàn hàng ngang 5 Quản lý từ DB) -->
                    <div v-else-if="activeTab === 'team'" class="animate-fade-in">
                        <div class="text-center max-w-700 mx-auto mb-12">
                            <v-chip color="primary" variant="tonal" size="small" class="font-weight-bold mb-3">BAN LÃNH ĐẠO</v-chip>
                            <h2 class="text-h3 font-weight-black text-slate-900 mb-3">Đội Ngũ Quản Lý & Lãnh Đạo</h2>
                            <p class="text-slate-600">Đội ngũ quản trị viên và lãnh đạo kiến tạo nên trải nghiệm thể thao hàng đầu tại AeroStride</p>
                        </div>

                        <!-- Horizontal Row of Team Members -->
                        <div class="team-horizontal-container">
                            <div class="team-scroll-track d-flex flex-nowrap ga-5 overflow-x-auto pb-6 pt-2 px-1">
                                <v-card
                                    v-for="(member, idx) in teamMembers"
                                    :key="idx"
                                    class="team-card-horizontal flex-shrink-0 rounded-2xl overflow-hidden elevation-2 border bg-white"
                                >
                                    <div class="team-img-wrapper position-relative">
                                        <v-img :src="member.image" height="230" cover class="team-avatar-img">
                                            <template #placeholder>
                                                <div class="d-flex align-center justify-center fill-height bg-grey-lighten-3">
                                                    <v-progress-circular indeterminate color="primary" size="24"></v-progress-circular>
                                                </div>
                                            </template>
                                        </v-img>
                                        <div class="team-avatar-overlay d-flex align-end pa-3">
                                            <v-chip size="x-small" color="#1e257c" variant="flat" class="font-weight-black text-white elevation-2">
                                                QUẢN LÝ #0{{ idx + 1 }}
                                            </v-chip>
                                        </div>
                                    </div>

                                    <v-card-text class="pa-5 d-flex flex-column justify-space-between team-card-body">
                                        <div>
                                            <h3 class="text-subtitle-1 font-weight-black text-slate-900 mb-1 leading-snug">{{ member.name }}</h3>
                                            <div class="text-caption font-weight-bold text-primary mb-3">{{ member.role }}</div>
                                            <p class="text-caption text-slate-600 italic line-clamp-3 mb-0">"{{ member.quote }}"</p>
                                        </div>

                                        <div v-if="member.email || member.sdt" class="mt-3 pt-3 border-t text-caption text-slate-500 d-flex flex-column ga-1">
                                            <span v-if="member.email" class="text-truncate d-flex align-center">
                                                <v-icon size="13" class="mr-1" color="#1e257c">mdi-email-outline</v-icon>{{ member.email }}
                                            </span>
                                            <span v-if="member.sdt" class="d-flex align-center">
                                                <v-icon size="13" class="mr-1" color="#1e257c">mdi-phone-outline</v-icon>{{ member.sdt }}
                                            </span>
                                        </div>
                                    </v-card-text>
                                </v-card>
                            </div>
                        </div>
                    </div>
                </v-container>
            </section>

            <!-- Animated Stats Bar -->
            <section class="py-16 bg-slate-900 text-white">
                <v-container>
                    <v-row class="text-center">
                        <v-col cols="12" sm="3">
                            <div class="stat-num text-gradient-blue font-weight-black">{{ statsData.totalProducts }}+</div>
                            <div class="stat-txt text-grey-lighten-1 font-weight-medium">Mẫu Giày Kinh Doanh</div>
                        </v-col>
                        <v-col cols="12" sm="3">
                            <div class="stat-num text-gradient-blue font-weight-black">{{ statsData.totalBrands }}+</div>
                            <div class="stat-txt text-grey-lighten-1 font-weight-medium">Thương Hiệu Đối Tác</div>
                        </v-col>
                        <v-col cols="12" sm="3">
                            <div class="stat-num text-gradient-blue font-weight-black">{{ statsData.totalStores }}+</div>
                            <div class="stat-txt text-grey-lighten-1 font-weight-medium">Cửa Hàng Toàn Quốc</div>
                        </v-col>
                        <v-col cols="12" sm="3">
                            <div class="stat-num text-gradient-blue font-weight-black">{{ statsData.satisfactionRate }}</div>
                            <div class="stat-txt text-grey-lighten-1 font-weight-medium">Mức Độ Hài Lòng</div>
                        </v-col>
                    </v-row>
                </v-container>
            </section>

            <!-- Bottom CTA -->
            <section class="py-16 bg-blue-lighten-5 text-center">
                <v-container class="max-w-700">
                    <h2 class="text-h4 font-weight-black text-slate-900 mb-4">Sẵn Sàng Trải Nghiệm Cùng AeroStride?</h2>
                    <p class="text-body-1 text-slate-600 mb-8">Khám phá hàng ngàn mẫu giày thể thao đỉnh cao đến từ Nike, Adidas, Puma, Mizuno ngay hôm nay!</p>
                    <v-btn
                        color="primary"
                        size="x-large"
                        rounded="pill"
                        class="font-weight-bold px-10 elevation-4 text-white"
                        @click="goToProducts"
                    >
                        MUA SẮM NGAY
                    </v-btn>
                </v-container>
            </section>
        </main>

        <MainFooter />

        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.main-content {
    padding-top: 60px;
}

.about-hero-carousel {
    overflow: hidden;
}

.carousel-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, rgba(15, 23, 42, 0.75) 0%, rgba(15, 23, 42, 0.85) 100%);
    backdrop-filter: blur(2px);
}

.about-hero {
    min-height: 480px;
    background: linear-gradient(135deg, #0f172a 0%, #1e293b 50%, #1e3a8a 100%);
    background-size: cover;
    background-position: center;
    display: flex;
    align-items: center;
}

.hero-overlay {
    position: absolute;
    inset: 0;
    background: radial-gradient(circle at 30% 50%, rgba(37, 99, 235, 0.25) 0%, transparent 60%);
}

.hero-title {
    font-size: clamp(1.8rem, 2.8vw, 2.6rem) !important;
    line-height: 1.25;
    color: #ffffff !important;
    white-space: nowrap;
    text-shadow: 0 4px 16px rgba(0, 0, 0, 0.4);
}

@media (max-width: 600px) {
    .hero-title {
        white-space: normal;
    }
}

.hero-subtitle {
    color: rgba(255, 255, 255, 0.88) !important;
    font-size: 1.15rem;
    line-height: 1.6;
}

.text-gradient {
    background: linear-gradient(90deg, #60a5fa 0%, #38bdf8 35%, #93c5fd 50%, #60a5fa 75%, #38bdf8 100%);
    background-size: 200% auto;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    animation: textShine 4s linear infinite;
    display: inline-block;
    filter: drop-shadow(0 2px 8px rgba(56, 189, 248, 0.4));
}

@keyframes textShine {
    to {
        background-position: 200% center;
    }
}

.text-gradient-blue {
    background: linear-gradient(90deg, #38bdf8, #60a5fa);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
}

.stat-num {
    font-size: 3rem;
    line-height: 1;
    margin-bottom: 8px;
}

.icon-wrap {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

.bg-primary-lighten {
    background: #eff6ff;
}

.hover-lift {
    transition: transform 0.3s ease, box-shadow 0.3s ease;
    &:hover {
        transform: translateY(-6px);
        box-shadow: 0 12px 24px rgba(0, 0, 0, 0.08) !important;
    }
}

.shadow-blue {
    box-shadow: 0 10px 20px rgba(37, 99, 235, 0.3) !important;
}

.max-w-700 {
    max-width: 700px;
}

/* ── Timeline Premium Styles ── */
.timeline-premium {
    :deep(.v-timeline-divider__dot) {
        background: #1e257c !important;
        box-shadow: 0 0 0 6px rgba(30, 37, 124, 0.15);
    }
    :deep(.v-timeline-divider__line) {
        background: linear-gradient(180deg, #1e257c 0%, #3b82f6 100%) !important;
        width: 3px !important;
    }
}

.timeline-year-dot {
    width: 38px;
    height: 38px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #1e257c 0%, #2563eb 100%);
    box-shadow: 0 4px 12px rgba(30, 37, 124, 0.4);
}

.timeline-card-premium {
    background: #ffffff;
    border: 1px solid rgba(226, 232, 240, 0.9);
    transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
        transform: translateY(-6px) scale(1.01);
        box-shadow: 0 20px 40px rgba(30, 37, 124, 0.12) !important;
        border-color: #93c5fd;

        .timeline-img :deep(img) {
            transform: scale(1.08);
        }
    }
}

.timeline-img {
    transition: transform 0.5s ease;
    :deep(img) {
        transition: transform 0.5s ease;
    }
}

.img-gradient-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, rgba(15, 23, 42, 0.3) 0%, rgba(15, 23, 42, 0.85) 100%);
}

.timeline-stats-badge {
    background: rgba(15, 23, 42, 0.75);
    backdrop-filter: blur(4px);
    border: 1px solid rgba(255, 255, 255, 0.2);
}

/* ── Horizontal Team Card Styles ── */
.team-horizontal-container {
    width: 100%;
}

.team-scroll-track {
    scrollbar-width: thin;
    scrollbar-color: #cbd5e1 transparent;
    &::-webkit-scrollbar {
        height: 6px;
    }
    &::-webkit-scrollbar-thumb {
        background: #cbd5e1;
        border-radius: 4px;
    }
}

.team-card-horizontal {
    width: 245px;
    min-width: 245px;
    transition: all 0.35s cubic-bezier(0.16, 1, 0.3, 1);

    &:hover {
        transform: translateY(-8px);
        box-shadow: 0 18px 36px rgba(30, 37, 124, 0.12) !important;
        border-color: #93c5fd;

        .team-avatar-img :deep(img) {
            transform: scale(1.08);
        }
    }
}

.team-avatar-img {
    transition: transform 0.4s ease;
    :deep(img) {
        transition: transform 0.4s ease;
    }
}

.team-avatar-overlay {
    position: absolute;
    inset: 0;
    background: linear-gradient(180deg, transparent 50%, rgba(15, 23, 42, 0.7) 100%);
}

.team-card-body {
    min-height: 180px;
}
</style>
