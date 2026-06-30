<script setup>
import { ref, onMounted } from 'vue';
import MainHeader from '@/components/shared/MainHeader.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { useSeoMeta } from '@/composables/useSeoMeta';

const { setSeoMeta } = useSeoMeta();

// Mock data since BE doesn't have News entities yet
const categories = ['Tất cả', 'Giới giày', 'Sự kiện', 'Khuyến mãi'];
const activeCategory = ref('Tất cả');

const newsList = [
    { id: 1, title: 'Xu hướng giày thể thao 2026: Lên ngôi của các tông màu Retro', category: 'Giới giày', image: 'https://images.unsplash.com/photo-1608231387042-66d1773070a5?auto=format&fit=crop&q=80&w=800', date: '15 Thg 07, 2026', excerpt: 'Năm 2026 chứng kiến sự trở lại mạnh mẽ của các thiết kế thập niên 90 với phối màu cổ điển.' },
    { id: 2, title: 'Khai trương chi nhánh thứ 50 của AeroStride tại Đà Nẵng', category: 'Sự kiện', image: 'https://images.unsplash.com/photo-1497215842964-222b430dc094?auto=format&fit=crop&q=80&w=800', date: '02 Thg 07, 2026', excerpt: 'Sự kiện đánh dấu mốc quan trọng trong quá trình phủ sóng toàn quốc của AeroStride.' },
    { id: 3, title: 'Black Friday Sớm: Giảm đến 50% toàn bộ giày chạy bộ', category: 'Khuyến mãi', image: 'https://images.unsplash.com/photo-1606107557195-0e29a4b5b4aa?auto=format&fit=crop&q=80&w=800', date: '28 Thg 06, 2026', excerpt: 'Cơ hội duy nhất trong năm để sở hữu những siêu phẩm với giá cực hời.' },
    { id: 4, title: 'Đánh giá chi tiết Air Zoom Pegasus 40: Có đáng mua?', category: 'Giới giày', image: 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&q=80&w=800', date: '20 Thg 06, 2026', excerpt: 'Dòng giày chạy quốc dân thế hệ 40 đã ra mắt với nhiều cải tiến đáng giá ở phần upper.' },
    { id: 5, title: 'Cách bảo quản giày da bò đúng cách trong mùa mưa', category: 'Giới giày', image: 'https://images.unsplash.com/photo-1512374382149-233c42b6a83b?auto=format&fit=crop&q=80&w=800', date: '10 Thg 06, 2026', excerpt: 'Đừng để cơn mưa làm hỏng đôi giày đắt tiền của bạn. Dưới đây là bí kíp bảo quản.' },
    { id: 6, title: 'Sự kiện chạy bộ từ thiện cùng AeroStride Foundation', category: 'Sự kiện', image: 'https://images.unsplash.com/photo-1552674605-15c3705e970e?auto=format&fit=crop&q=80&w=800', date: '05 Thg 06, 2026', excerpt: 'Cùng chung tay vì một cộng đồng khỏe mạnh và gắn kết.' },
];

onMounted(() => {
    window.scrollTo(0, 0);
    setSeoMeta({
        title: 'Tin Tức & Khuyến Mãi | AeroStride',
        description: 'Cập nhật những xu hướng giày mới nhất, mẹo bảo quản và các chương trình khuyến mãi hấp dẫn từ AeroStride.'
    });
});
</script>

<template>
    <div class="app-container bg-grey-lighten-4">
        <MainHeader />
        
        <main class="main-content pb-16">
            <!-- Header Section -->
            <div class="page-header py-12 bg-white mb-8 border-b">
                <v-container>
                    <h1 class="text-h3 font-weight-black text-center text-grey-darken-4 mb-4">TIN TỨC AEROSTRIDE</h1>
                    <p class="text-center text-grey-darken-1 text-subtitle-1">Cập nhật xu hướng, đánh giá sản phẩm và tin tức mới nhất.</p>
                </v-container>
            </div>

            <v-container>
                <!-- Categories -->
                <div class="d-flex justify-center flex-wrap gap-4 mb-10">
                    <v-btn 
                        v-for="cat in categories" 
                        :key="cat"
                        :variant="activeCategory === cat ? 'elevated' : 'outlined'"
                        :color="activeCategory === cat ? 'primary' : 'grey-darken-2'"
                        rounded="xl"
                        class="text-none font-weight-bold"
                        @click="activeCategory = cat"
                    >
                        {{ cat }}
                    </v-btn>
                </div>

                <!-- Featured Post (Only show if 'Tất cả' is selected) -->
                <v-card v-if="activeCategory === 'Tất cả'" class="featured-post rounded-xl elevation-5 mb-12 overflow-hidden cursor-pointer">
                    <v-row no-gutters>
                        <v-col cols="12" md="7">
                            <v-img :src="newsList[0].image" height="100%" min-height="400" cover></v-img>
                        </v-col>
                        <v-col cols="12" md="5" class="d-flex flex-column justify-center pa-8 pa-md-12 bg-white">
                            <v-chip color="error" size="small" class="font-weight-bold mb-4 align-self-start">{{ newsList[0].category }}</v-chip>
                            <h2 class="text-h4 font-weight-black text-grey-darken-4 mb-4 line-clamp-2">{{ newsList[0].title }}</h2>
                            <p class="text-body-1 text-grey-darken-1 mb-6">{{ newsList[0].excerpt }}</p>
                            <div class="d-flex align-center text-caption text-grey">
                                <v-icon size="16" class="mr-2">mdi-calendar</v-icon>
                                {{ newsList[0].date }}
                            </div>
                        </v-col>
                    </v-row>
                </v-card>

                <!-- News Grid -->
                <v-row>
                    <v-col v-for="news in newsList.filter(n => activeCategory === 'Tất cả' || n.category === activeCategory)" :key="news.id" cols="12" sm="6" md="4">
                        <v-card class="h-100 rounded-xl news-card cursor-pointer d-flex flex-column elevation-2">
                            <div class="image-wrapper">
                                <v-img :src="news.image" height="220" cover class="news-img"></v-img>
                                <v-chip color="primary" size="small" class="category-chip">{{ news.category }}</v-chip>
                            </div>
                            <v-card-text class="d-flex flex-column flex-grow-1 pa-5">
                                <div class="text-caption text-grey mb-2 d-flex align-center">
                                    <v-icon size="14" class="mr-1">mdi-clock-outline</v-icon>
                                    {{ news.date }}
                                </div>
                                <h3 class="text-h6 font-weight-bold mb-3 text-grey-darken-4 line-clamp-2 title-hover">{{ news.title }}</h3>
                                <p class="text-body-2 text-grey-darken-1 line-clamp-3 mb-4">{{ news.excerpt }}</p>
                                <v-spacer></v-spacer>
                                <div class="read-more text-primary font-weight-bold text-caption mt-auto">
                                    Đọc tiếp <v-icon size="14">mdi-arrow-right</v-icon>
                                </div>
                            </v-card-text>
                        </v-card>
                    </v-col>
                </v-row>
            </v-container>
        </main>
        
        <footer class="footer-landing py-10 text-center text-grey-darken-1 bg-white border-t">
            <LogoClient class="mb-4 d-inline-block" style="max-width: 150px" />
            <p>&copy; 2026 AeroStride All rights reserved.</p>
        </footer>
    </div>
</template>

<style scoped>
.main-content { padding-top: 60px; }
.gap-4 { gap: 16px; }
.line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}
.line-clamp-3 {
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.featured-post {
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.featured-post:hover {
    transform: translateY(-5px);
    box-shadow: 0 15px 30px rgba(0,0,0,0.1) !important;
}

.news-card {
    transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.news-card:hover {
    transform: translateY(-8px);
    box-shadow: 0 12px 24px rgba(0,0,0,0.08) !important;
}

.image-wrapper {
    position: relative;
    overflow: hidden;
}
.news-img {
    transition: transform 0.5s ease;
}
.news-card:hover .news-img {
    transform: scale(1.05);
}

.category-chip {
    position: absolute;
    top: 12px;
    left: 12px;
    font-weight: 700;
}

.title-hover {
    transition: color 0.2s ease;
}
.news-card:hover .title-hover {
    color: #2962ff !important;
}
</style>
