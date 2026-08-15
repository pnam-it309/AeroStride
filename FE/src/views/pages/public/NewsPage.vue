<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { useSeoMeta } from '@/composables/useSeoMeta';

import { dichVuTinTuc } from '@/services/public/dichVuTinTuc';

const { setSeoMeta } = useSeoMeta();
const router = useRouter();

const searchKeyword = ref('');
const activeCategory = ref('Tất cả');
const categories = ['Tất cả', 'Giới giày', 'Sự kiện', 'Khuyến mãi', 'Bảo quản'];

const selectedArticle = ref(null);
const showDetailModal = ref(false);
const isLoadingNews = ref(false);

// Comment system state for modal
const newCommentName = ref('');
const newCommentText = ref('');
const isSubmittingComment = ref(false);

const newsList = ref([]);

const fetchNewsFromApi = async () => {
    isLoadingNews.value = true;
    try {
        const res = await dichVuTinTuc.layDanhSachTinTuc({
            keyword: searchKeyword.value,
            category: activeCategory.value
        });
        if (res?.data) {
            newsList.value = res.data;
        }
    } catch (e) {
        console.error('Lỗi khi lấy bài viết từ BE:', e);
    } finally {
        isLoadingNews.value = false;
    }
};

const filteredNews = computed(() => {
    return newsList.value.filter((n) => {
        const matchCat = activeCategory.value === 'Tất cả' || n.category === activeCategory.value;
        const matchKeyword = !searchKeyword.value.trim() ||
            n.title.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
            n.excerpt.toLowerCase().includes(searchKeyword.value.toLowerCase());
        return matchCat && matchKeyword;
    });
});

const openDetail = (news) => {
    router.push(`/tin-tuc/${news.id}`);
};

const toggleLike = async (news) => {
    news.likes += 1;
    try {
        await dichVuTinTuc.likeTinTuc(news.id);
    } catch (e) {
        console.error('Lỗi thả tim:', e);
    }
};

const handleAddComment = async () => {
    if (!newCommentText.value.trim() || !selectedArticle.value) return;
    isSubmittingComment.value = true;
    try {
        const res = await dichVuTinTuc.binhLuanTinTuc(selectedArticle.value.id, {
            name: newCommentName.value.trim(),
            text: newCommentText.value.trim()
        });
        if (res?.data) {
            selectedArticle.value.comments.push(res.data);
        } else {
            selectedArticle.value.comments.push({
                name: newCommentName.value.trim() || 'Bạn đọc AeroStride',
                text: newCommentText.value.trim(),
                date: new Date().toLocaleDateString('vi-VN')
            });
        }
        newCommentText.value = '';
        newCommentName.value = '';
    } catch (e) {
        console.error('Lỗi gửi bình luận:', e);
    } finally {
        isSubmittingComment.value = false;
    }
};

onMounted(() => {
    window.scrollTo(0, 0);
    setSeoMeta({
        title: 'Tin Tức & Khuyến Mãi | AeroStride',
        description: 'Cập nhật những xu hướng giày mới nhất, mẹo bảo quản và các chương trình khuyến mãi hấp dẫn từ AeroStride.'
    });
    fetchNewsFromApi();
});
</script>

<template>
    <div class="app-container bg-grey-lighten-4 font-body">
        <MainHeader />

        <main class="main-content pb-16">
            <!-- Header Section: Logo Blue Gradient + Crisp White Text -->
            <div class="page-header py-14 mb-10 text-white">
                <v-container>
                    <div class="d-flex justify-center mb-3">
                        <v-chip color="amber-accent-2" variant="flat" size="small" class="font-weight-black px-4 text-slate-900 shadow-sm">
                            <v-icon start size="16">mdi-newspaper-variant-outline</v-icon>
                            AEROSTRIDE MAGAZINE
                        </v-chip>
                    </div>
                    <h1 class="page-header-title text-h3 font-weight-black text-center mb-3">TIN TỨC & XU HƯỚNG AEROSTRIDE</h1>
                    <p class="page-header-subtitle text-center text-subtitle-1 max-w-600 mx-auto">
                        Cập nhật các bài viết đánh giá giày thể thao, mẹo chăm sóc sản phẩm và các sự kiện ưu đãi độc quyền mới nhất.
                    </p>

                    <!-- Search Bar -->
                    <div class="max-w-500 mx-auto mt-6">
                        <v-text-field
                            v-model="searchKeyword"
                            placeholder="Tìm kiếm bài viết, tin tức..."
                            prepend-inner-icon="mdi-magnify"
                            variant="solo"
                            bg-color="white"
                            density="comfortable"
                            rounded="pill"
                            hide-details
                            clearable
                            class="news-search-field"
                        ></v-text-field>
                    </div>
                </v-container>
            </div>

            <v-container>
                <!-- Categories -->
                <div class="d-flex justify-center flex-wrap ga-3 mb-10">
                    <v-btn
                        v-for="cat in categories"
                        :key="cat"
                        :variant="activeCategory === cat ? 'flat' : 'outlined'"
                        :color="activeCategory === cat ? 'primary' : 'grey-darken-2'"
                        rounded="pill"
                        class="text-none font-weight-bold px-6 category-btn"
                        :class="{ 'category-active-btn': activeCategory === cat }"
                        @click="activeCategory = cat"
                    >
                        {{ cat }}
                    </v-btn>
                </div>

                <!-- Empty State -->
                <div v-if="filteredNews.length === 0" class="text-center py-16 bg-white rounded-xl elevation-1">
                    <v-icon size="64" color="grey-lighten-1" class="mb-4">mdi-text-search-line</v-icon>
                    <h3 class="text-h6 font-weight-bold text-slate-700 mb-2">Không tìm thấy bài viết phù hợp</h3>
                    <p class="text-slate-500 text-body-2 mb-6">Thử thay đổi từ khóa hoặc chọn chuyên mục khác</p>
                    <v-btn color="primary" variant="outlined" rounded="pill" @click="searchKeyword = ''; activeCategory = 'Tất cả'">
                        Đặt lại bộ lọc
                    </v-btn>
                </div>

                <!-- Featured Post (Only when all selected and no active search) -->
                <v-card
                    v-else-if="activeCategory === 'Tất cả' && !searchKeyword"
                    class="featured-post rounded-xl elevation-4 mb-12 overflow-hidden cursor-pointer"
                    @click="openDetail(filteredNews[0])"
                >
                    <v-row no-gutters>
                        <v-col cols="12" md="7">
                            <v-img :src="filteredNews[0].image" height="100%" min-height="380" cover></v-img>
                        </v-col>
                        <v-col cols="12" md="5" class="d-flex flex-column justify-center pa-8 pa-md-10 bg-white">
                            <div class="d-flex align-center ga-2 mb-3">
                                <v-chip color="primary" size="small" class="font-weight-bold text-white">{{ filteredNews[0].category }}</v-chip>
                                <span class="text-caption text-slate-500">&bull; {{ filteredNews[0].date }}</span>
                            </div>
                            <h2 class="text-h4 font-weight-black text-slate-900 mb-4 line-clamp-2 title-hover">{{ filteredNews[0].title }}</h2>
                            <p class="text-body-1 text-slate-600 mb-6 line-clamp-3">{{ filteredNews[0].excerpt }}</p>
                            <div class="d-flex align-center justify-space-between mt-auto">
                                <span class="text-caption font-weight-bold text-slate-700">Tác giả: {{ filteredNews[0].author }}</span>
                                <v-btn color="primary" variant="text" append-icon="mdi-arrow-right" class="font-weight-bold text-none">
                                    Đọc bài viết
                                </v-btn>
                            </div>
                        </v-col>
                    </v-row>
                </v-card>

                <!-- News Grid -->
                <v-row v-if="filteredNews.length > 0">
                    <v-col
                        v-for="news in (activeCategory === 'Tất cả' && !searchKeyword ? filteredNews.slice(1) : filteredNews)"
                        :key="news.id"
                        cols="12"
                        sm="6"
                        md="4"
                    >
                        <v-card
                            class="h-100 rounded-xl news-card cursor-pointer d-flex flex-column elevation-2 bg-white"
                            @click="openDetail(news)"
                        >
                            <div class="image-wrapper">
                                <v-img :src="news.image" height="220" cover class="news-img"></v-img>
                                <v-chip color="primary" size="small" class="category-chip shadow-sm text-white font-weight-bold">{{ news.category }}</v-chip>
                            </div>
                            <v-card-text class="d-flex flex-column flex-grow-1 pa-5">
                                <div class="text-caption text-slate-400 mb-2 d-flex align-center justify-space-between">
                                    <span><v-icon size="14" class="mr-1">mdi-calendar</v-icon>{{ news.date }}</span>
                                    <span @click.stop="toggleLike(news)" class="cursor-pointer hover-red">
                                        <v-icon size="14" color="error" class="mr-1">mdi-heart</v-icon>{{ news.likes }}
                                    </span>
                                </div>
                                <h3 class="text-h6 font-weight-bold mb-3 text-slate-900 line-clamp-2 title-hover">{{ news.title }}</h3>
                                <p class="text-body-2 text-slate-600 line-clamp-3 mb-4">{{ news.excerpt }}</p>
                                <v-spacer></v-spacer>
                                <div class="d-flex align-center justify-space-between pt-2 border-t">
                                    <span class="text-caption text-slate-500">{{ news.author }}</span>
                                    <span class="text-primary font-weight-bold text-caption d-flex align-center">
                                        Chi tiết <v-icon size="14" class="ml-1">mdi-arrow-right</v-icon>
                                    </span>
                                </div>
                            </v-card-text>
                        </v-card>
                    </v-col>
                </v-row>
            </v-container>
        </main>

        <!-- Article Detail Modal -->
        <v-dialog v-model="showDetailModal" max-width="850" scrollable>
            <v-card v-if="selectedArticle" class="rounded-xl overflow-hidden">
                <v-card-title class="modal-news-header d-flex align-center justify-space-between px-6 py-4">
                    <div class="d-flex align-center ga-2">
                        <v-chip color="amber-accent-2" size="small" class="font-weight-black text-slate-900">{{ selectedArticle.category }}</v-chip>
                        <span class="text-caption text-slate-200">{{ selectedArticle.date }}</span>
                    </div>
                    <v-btn icon variant="text" size="small" class="modal-close-btn rounded-circle" @click="showDetailModal = false">
                        <v-icon color="#ffffff" size="20">mdi-close</v-icon>
                    </v-btn>
                </v-card-title>

                <v-card-text class="pa-6 pa-md-8 bg-white">
                    <h2 class="text-h4 font-weight-black text-slate-900 mb-4">{{ selectedArticle.title }}</h2>
                    <div class="d-flex align-center justify-space-between mb-6 pb-4 border-b">
                        <div class="d-flex align-center ga-3">
                            <v-avatar color="primary" size="36">
                                <span class="text-white text-subtitle-2 font-weight-bold">{{ selectedArticle.author.charAt(0) }}</span>
                            </v-avatar>
                            <div>
                                <div class="font-weight-bold text-slate-800 text-body-2">{{ selectedArticle.author }}</div>
                                <div class="text-caption text-slate-500">AeroStride Editorial</div>
                            </div>
                        </div>
                        <v-btn
                            variant="tonal"
                            color="error"
                            size="small"
                            rounded="pill"
                            prepend-icon="mdi-heart"
                            @click="toggleLike(selectedArticle)"
                        >
                            {{ selectedArticle.likes }} Yêu thích
                        </v-btn>
                    </div>

                    <v-img :src="selectedArticle.image" height="360" cover class="rounded-xl mb-6 shadow-sm"></v-img>

                    <div class="article-body text-slate-800 leading-relaxed mb-8" v-html="selectedArticle.content"></div>

                    <!-- Comments Section -->
                    <div class="comments-section border-t pt-6">
                        <h3 class="text-h6 font-weight-bold text-slate-900 mb-4">
                            Bình luận ({{ selectedArticle.comments.length }})
                        </h3>

                        <div v-if="selectedArticle.comments.length > 0" class="mb-6">
                            <div v-for="(c, idx) in selectedArticle.comments" :key="idx" class="bg-grey-lighten-5 pa-4 rounded-lg mb-3">
                                <div class="d-flex justify-space-between mb-1">
                                    <span class="font-weight-bold text-slate-900 text-body-2">{{ c.name }}</span>
                                    <span class="text-caption text-slate-400">{{ c.date }}</span>
                                </div>
                                <p class="text-slate-700 text-body-2 mb-0">{{ c.text }}</p>
                            </div>
                        </div>

                        <!-- Add Comment Form -->
                        <div class="bg-grey-lighten-5 pa-4 rounded-xl">
                            <div class="font-weight-bold text-slate-800 mb-3 text-body-2">Viết bình luận của bạn</div>
                            <v-row dense>
                                <v-col cols="12" sm="6" class="mb-2">
                                    <v-text-field
                                        v-model="newCommentName"
                                        placeholder="Họ tên của bạn (Tùy chọn)"
                                        variant="outlined"
                                        density="compact"
                                        bg-color="white"
                                        hide-details
                                    ></v-text-field>
                                </v-col>
                                <v-col cols="12" class="mb-2">
                                    <v-textarea
                                        v-model="newCommentText"
                                        placeholder="Chia sẻ ý kiến của bạn về bài viết..."
                                        variant="outlined"
                                        rows="2"
                                        density="compact"
                                        bg-color="white"
                                        hide-details
                                    ></v-textarea>
                                </v-col>
                                <v-col cols="12" class="text-right">
                                    <v-btn
                                        color="primary"
                                        size="small"
                                        rounded="pill"
                                        class="font-weight-bold text-none px-6"
                                        :loading="isSubmittingComment"
                                        :disabled="!newCommentText.trim()"
                                        @click="handleAddComment"
                                    >
                                        Gửi bình luận
                                    </v-btn>
                                </v-col>
                            </v-row>
                        </div>
                    </div>
                </v-card-text>
            </v-card>
        </v-dialog>

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

.page-header {
    background: linear-gradient(135deg, #1e257c 0%, #23318c 50%, #1d4ed8 100%) !important;
    position: relative;
    box-shadow: 0 4px 20px rgba(30, 37, 124, 0.25);
    color: #ffffff !important;
}

.page-header-title {
    color: #ffffff !important;
    letter-spacing: -0.5px;
    font-size: 2.25rem !important;
    line-height: 1.25 !important;
}

.page-header-subtitle {
    color: rgba(255, 255, 255, 0.9) !important;
    font-weight: 500;
}

.news-search-field :deep(.v-field) {
    border-radius: 9999px !important;
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.12) !important;
}

.category-btn {
    border: 1.5px solid #cbd5e1 !important;
    background: #ffffff;
    transition: all 0.25s ease;
}

.category-active-btn {
    background: #1e257c !important;
    color: #ffffff !important;
    border-color: #1e257c !important;
    box-shadow: 0 4px 14px rgba(30, 37, 124, 0.3) !important;
}

.modal-news-header {
    background: linear-gradient(135deg, #1e257c 0%, #23318c 50%, #1d4ed8 100%) !important;
    color: #ffffff !important;
}

.modal-close-btn {
    background: rgba(255, 255, 255, 0.18) !important;
    color: #ffffff !important;
    transition: all 0.2s ease;
}

.modal-close-btn:hover {
    background: rgba(255, 255, 255, 0.35) !important;
    transform: rotate(90deg);
}

.max-w-600 {
    max-width: 600px;
}

.max-w-500 {
    max-width: 500px;
}

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

.featured-post,
.news-card {
    transition: transform 0.3s ease, box-shadow 0.3s ease;
    &:hover {
        transform: translateY(-6px);
        box-shadow: 0 14px 28px rgba(30, 37, 124, 0.12) !important;
    }
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
    background: #1e257c !important;
    color: #ffffff !important;
}

.title-hover {
    transition: color 0.2s ease;
}

.news-card:hover .title-hover {
    color: #1e257c !important;
}

.article-body :deep(h3) {
    font-size: 1.25rem;
    font-weight: 800;
    color: #0f172a;
    margin-top: 1.5rem;
    margin-bottom: 0.75rem;
}

.article-body :deep(p) {
    font-size: 1rem;
    line-height: 1.7;
    color: #334155;
    margin-bottom: 1rem;
}

.article-body :deep(ul) {
    margin-left: 1.5rem;
    margin-bottom: 1rem;
    li {
        margin-bottom: 0.5rem;
    }
}
</style>
