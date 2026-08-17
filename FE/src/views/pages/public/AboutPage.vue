<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { useSeoMeta } from '@/composables/useSeoMeta';

import { dichVuGioiThieu } from '@/services/public/dichVuGioiThieu';

const router = useRouter();
const { setSeoMeta } = useSeoMeta();

const activeTab = ref('story');
const statsData = ref({
    totalProducts: 500,
    totalBrands: 12,
    totalShowrooms: 50,
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
        title: 'Khởi Đầu Đam Mê',
        desc: 'AeroStride chính thức ra mắt cửa hàng đầu tiên tại Hà Nội với tầm nhìn mang lại những đôi giày thể thao chính hãng chất lượng cao nhất.'
    },
    {
        year: '2025',
        title: 'Bứt Phá Quy Mô',
        desc: 'Mở rộng 20 chi nhánh trên các tỉnh thành lớn, ra mắt hệ thống mua sắm trực tuyến thông minh và dịch vụ CSKH 24/7.'
    },
    {
        year: '2026',
        title: 'Tiên Phong Công Nghệ AI',
        desc: 'Tích hợp công nghệ tư vấn AI thông minh, mở rộng mạng lưới 50+ chi nhánh toàn quốc và đạt mốc 1.000.000+ khách hàng tin tưởng.'
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

const teamMembers = [
    {
        name: 'Hoàng Phương Nam',
        role: 'Founder & CEO',
        image: 'https://images.unsplash.com/photo-1560250097-0b93528c311a?auto=format&fit=crop&q=80&w=400',
        quote: 'Mỗi đôi giày là một bước tiến đưa bạn vượt qua giới hạn của chính mình.'
    },
    {
        name: 'Phí Thu Trang',
        role: 'Co-Founder & Product Director',
        image: 'https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&q=80&w=400',
        quote: 'Chúng tôi tỉ mỉ lựa chọn từng mẫu thiết kế đạt tiêu chuẩn êm ái và thẩm mỹ cao nhất.'
    },
    {
        name: 'Lê Thị Thu Huyền',
        role: 'Head of Customer Success',
        image: 'https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&q=80&w=400',
        quote: 'Sự hài lòng tuyệt đối của khách hàng là thước đo giá trị lớn nhất của AeroStride.'
    }
];

const goToProducts = () => {
    router.push('/san-pham');
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
            <!-- Hero Banner -->
            <section class="about-hero position-relative">
                <div class="hero-overlay"></div>
                <v-container class="position-relative z-index-2 text-center text-white py-16">
                    <v-chip
                        variant="outlined"
                        size="small"
                        class="font-weight-bold mb-4 tracking-wider"
                        style="color: #ffffff !important; border-color: rgba(255, 255, 255, 0.4) !important;"
                    >
                        EST. 2024 &bull; AEROSTRIDE VIỆT NAM
                    </v-chip>
                    <h1 class="hero-title font-weight-black mb-4 animate-up">
                        BƯỚC CHÂN CỦA SỰ <span class="text-gradient">ĐAM MÊ & THÀNH CÔNG</span>
                    </h1>
                    <p class="hero-subtitle mb-8 max-w-700 mx-auto">
                        Hành trình mang lại chuẩn mực mua sắm giày thể thao chính hãng đỉnh cao, hiện đại và tràn đầy cảm hứng cho người Việt.
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
                            style="color: #ffffff !important; border-color: rgba(255, 255, 255, 0.4) !important;"
                            append-icon="mdi-shopping-outline"
                            @click="goToProducts"
                        >
                            Xem Sản Phẩm
                        </v-btn>
                    </div>
                </v-container>
            </section>

            <!-- Quick Navigation Tabs -->
            <section id="story-section" class="py-8 bg-grey-lighten-5 border-b">
                <v-container>
                    <div class="d-flex justify-center flex-wrap ga-3">
                        <v-btn
                            :variant="activeTab === 'story' ? 'flat' : 'text'"
                            :color="activeTab === 'story' ? 'primary' : 'grey-darken-1'"
                            rounded="pill"
                            class="font-weight-bold px-6"
                            @click="activeTab = 'story'"
                        >
                            <v-icon class="mr-2">mdi-book-open-variant</v-icon>
                            Câu Chuyện Thương Hiệu
                        </v-btn>
                        <v-btn
                            :variant="activeTab === 'values' ? 'flat' : 'text'"
                            :color="activeTab === 'values' ? 'primary' : 'grey-darken-1'"
                            rounded="pill"
                            class="font-weight-bold px-6"
                            @click="activeTab = 'values'"
                        >
                            <v-icon class="mr-2">mdi-star-outline</v-icon>
                            Giá Trị Cốt Lõi
                        </v-btn>
                        <v-btn
                            :variant="activeTab === 'timeline' ? 'flat' : 'text'"
                            :color="activeTab === 'timeline' ? 'primary' : 'grey-darken-1'"
                            rounded="pill"
                            class="font-weight-bold px-6"
                            @click="activeTab = 'timeline'"
                        >
                            <v-icon class="mr-2">mdi-timeline-text-outline</v-icon>
                            Hành Trình Phát Triển
                        </v-btn>
                        <v-btn
                            :variant="activeTab === 'team' ? 'flat' : 'text'"
                            :color="activeTab === 'team' ? 'primary' : 'grey-darken-1'"
                            rounded="pill"
                            class="font-weight-bold px-6"
                            @click="activeTab = 'team'"
                        >
                            <v-icon class="mr-2">mdi-account-group-outline</v-icon>
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
                        <v-row align="center">
                            <v-col cols="12" md="6" class="pr-md-8">
                                <v-chip color="primary" variant="tonal" size="small" class="font-weight-bold mb-3">VỀ AEROSTRIDE</v-chip>
                                <h2 class="text-h3 font-weight-black text-slate-900 mb-6">Chúng Tôi Là Ai?</h2>
                                <p class="text-body-1 text-slate-700 leading-relaxed mb-4">
                                    Được khởi nguồn vào năm 2024, <strong>AeroStride</strong> sinh ra từ tình yêu mãnh liệt với các bộ môn thể thao và văn hóa Sneakers năng động. Chúng tôi đặt mục tiêu xóa bỏ mọi lo lắng về hàng giả, mang đến không gian mua sắm đáng tin cậy 100%.
                                </p>
                                <p class="text-body-1 text-slate-700 leading-relaxed mb-6">
                                    Mỗi đôi giày tại AeroStride không chỉ sở hữu thiết kế thời thượng mà còn được ứng dụng những công nghệ đệm tối tân giúp từng bước chạy trở nên nhẹ nhàng, bứt phá giới hạn.
                                </p>
                                <v-alert border="start" border-color="primary" class="bg-blue-lighten-5 text-slate-800 rounded-lg">
                                    <template #title>
                                        <div class="font-weight-bold text-primary">Sứ mệnh cốt lõi</div>
                                    </template>
                                    "Truyền cảm hứng và cung cấp trang thiết bị chạy bộ chính hãng tốt nhất để mọi người tự tin chinh phục mọi ước mơ."
                                </v-alert>
                            </v-col>
                            <v-col cols="12" md="6" class="mt-6 mt-md-0">
                                <v-row dense>
                                    <v-col cols="6">
                                        <v-img src="https://images.unsplash.com/photo-1556906781-9a412961c28c?auto=format&fit=crop&q=80&w=600" height="260" cover class="rounded-xl shadow-sm mb-4"></v-img>
                                        <v-img src="https://images.unsplash.com/photo-1514989940723-e8e51635b782?auto=format&fit=crop&q=80&w=600" height="180" cover class="rounded-xl shadow-sm"></v-img>
                                    </v-col>
                                    <v-col cols="6">
                                        <v-img src="https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&q=80&w=600" height="455" cover class="rounded-xl shadow-md"></v-img>
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
                            <v-chip color="primary" variant="tonal" size="small" class="font-weight-bold mb-3">LỊCH SỬ</v-chip>
                            <h2 class="text-h3 font-weight-black text-slate-900 mb-3">Hành Trình Phát Triển</h2>
                        </div>
                        <v-row justify="center">
                            <v-col cols="12" md="10">
                                <v-timeline align="start" dot-color="primary">
                                    <v-timeline-item
                                        v-for="(item, idx) in milestones"
                                        :key="idx"
                                        size="large"
                                    >
                                        <template #icon>
                                            <div class="font-weight-black text-white text-caption">{{ item.year }}</div>
                                        </template>
                                        <v-card class="pa-6 rounded-xl elevation-2 border">
                                            <div class="text-h6 font-weight-bold text-primary mb-2">{{ item.title }}</div>
                                            <p class="text-slate-700 text-body-1 leading-relaxed mb-0">{{ item.desc }}</p>
                                        </v-card>
                                    </v-timeline-item>
                                </v-timeline>
                            </v-col>
                        </v-row>
                    </div>

                    <!-- Tab 4: Team -->
                    <div v-else-if="activeTab === 'team'" class="animate-fade-in">
                        <div class="text-center max-w-700 mx-auto mb-12">
                            <v-chip color="primary" variant="tonal" size="small" class="font-weight-bold mb-3">CON NGƯỜI</v-chip>
                            <h2 class="text-h3 font-weight-black text-slate-900 mb-3">Đội Ngũ Ban Lãnh Đạo</h2>
                            <p class="text-slate-600">Những người kiến tạo nên hành trình tuyệt vời của AeroStride</p>
                        </div>
                        <v-row>
                            <v-col cols="12" md="4" v-for="(member, idx) in teamMembers" :key="idx">
                                <v-card class="h-100 rounded-xl overflow-hidden elevation-3 border">
                                    <v-img :src="member.image" height="280" cover></v-img>
                                    <v-card-text class="pa-6">
                                        <h3 class="text-h6 font-weight-bold text-slate-900 mb-1">{{ member.name }}</h3>
                                        <div class="text-caption font-weight-bold text-primary mb-4">{{ member.role }}</div>
                                        <p class="text-body-2 text-slate-600 italic">"{{ member.quote }}"</p>
                                    </v-card-text>
                                </v-card>
                            </v-col>
                        </v-row>
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
                            <div class="stat-num text-gradient-blue font-weight-black">{{ statsData.totalShowrooms }}+</div>
                            <div class="stat-txt text-grey-lighten-1 font-weight-medium">Chi Nhánh Showroom</div>
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
                        class="font-weight-bold px-10 elevation-4"
                        @click="goToProducts"
                    >
                        MUA SẮM NGAY
                    </v-btn>
                </v-container>
            </section>
        </main>

        <footer class="footer-landing py-10 text-center text-grey-darken-1 bg-white border-t">
            <LogoClient class="mb-4 d-inline-block" style="max-width: 150px" />
            <p>&copy; 2026 AeroStride All rights reserved.</p>
        </footer>

        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.main-content {
    padding-top: 60px;
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
    line-height: 1.2;
    color: #ffffff !important;
    white-space: nowrap;
}

@media (max-width: 600px) {
    .hero-title {
        white-space: normal;
    }
}

.hero-subtitle {
    color: rgba(255, 255, 255, 0.85) !important;
    font-size: 1.15rem;
    line-height: 1.6;
}

.text-gradient {
    background: linear-gradient(90deg, #60a5fa, #3b82f6, #93c5fd);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
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
</style>
