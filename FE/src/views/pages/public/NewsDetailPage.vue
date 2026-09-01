<script setup>
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MainHeader from '@/components/shared/MainHeader.vue';
import MainFooter from '@/components/shared/MainFooter.vue';
import CustomerChat from '@/components/shared/CustomerChat.vue';
import LogoClient from '@/layouts/full/logo/LogoClient.vue';
import { dichVuTinTuc } from '@/services/public/dichVuTinTuc';
import { useSeoMeta } from '@/composables/useSeoMeta';
import { useToastStore } from '@/stores/toastStore';

const route = useRoute();
const router = useRouter();
const toastStore = useToastStore();
const { setSeoMeta } = useSeoMeta();

const article = ref(null);
const relatedArticles = ref([]);
const isLoading = ref(true);

// Comment State
const commentName = ref('');
const commentText = ref('');
const isSubmittingComment = ref(false);

const fetchArticleDetail = async (id) => {
    isLoading.value = true;
    try {
        const res = await dichVuTinTuc.layChiTietTinTuc(id);
        if (res?.data) {
            article.value = res.data;
            setSeoMeta({
                title: `${article.value.title} | AeroStride News`,
                description: article.value.excerpt
            });
            fetchRelatedArticles(article.value.category, id);
        } else {
            article.value = null;
        }
    } catch (error) {
        console.error('Lỗi tải chi tiết tin tức:', error);
        article.value = null;
    } finally {
        isLoading.value = false;
    }
};

const fetchRelatedArticles = async (category, currentId) => {
    try {
        const res = await dichVuTinTuc.layDanhSachTinTuc({ category: 'Tất cả' });
        if (res?.data) {
            relatedArticles.value = res.data
                .filter((item) => String(item.id) !== String(currentId))
                .slice(0, 3);
        }
    } catch (e) {
        console.error('Lỗi tải bài viết liên quan:', e);
    }
};

const handleLike = async () => {
    if (!article.value) return;
    article.value.likes += 1;
    try {
        await dichVuTinTuc.likeTinTuc(article.value.id);
        toastStore.showToast('Đã thả tim bài viết!', 'success');
    } catch (e) {
        console.error('Lỗi thả tim bài viết:', e);
    }
};

const handleAddComment = async () => {
    if (!commentText.value.trim() || !article.value) return;
    isSubmittingComment.value = true;
    try {
        const res = await dichVuTinTuc.binhLuanTinTuc(article.value.id, {
            name: commentName.value.trim(),
            text: commentText.value.trim()
        });

        if (res?.data) {
            if (!article.value.comments) article.value.comments = [];
            article.value.comments.push(res.data);
        } else {
            if (!article.value.comments) article.value.comments = [];
            article.value.comments.push({
                name: commentName.value.trim() || 'Bạn đọc AeroStride',
                text: commentText.value.trim(),
                date: new Date().toLocaleDateString('vi-VN')
            });
        }
        commentText.value = '';
        commentName.value = '';
        toastStore.showToast('Gửi bình luận thành công!', 'success');
    } catch (e) {
        console.error('Lỗi gửi bình luận:', e);
        toastStore.showToast('Không thể gửi bình luận. Vui lòng thử lại.', 'error');
    } finally {
        isSubmittingComment.value = false;
    }
};

const copyArticleLink = () => {
    navigator.clipboard.writeText(window.location.href);
    toastStore.showToast('Đã sao chép liên kết bài viết!', 'success');
};

const goToArticle = (id) => {
    router.push(`/tin-tuc/${id}`);
};

watch(
    () => route.params.id,
    (newId) => {
        if (newId) {
            window.scrollTo({ top: 0, behavior: 'smooth' });
            fetchArticleDetail(newId);
        }
    }
);

onMounted(() => {
    window.scrollTo(0, 0);
    if (route.params.id) {
        fetchArticleDetail(route.params.id);
    }
});
</script>

<template>
    <div class="app-container bg-grey-lighten-4 font-body">
        <MainHeader />

        <main class="main-content pb-16">
            <!-- Loading State -->
            <v-container v-if="isLoading" class="py-16 text-center">
                <v-progress-circular indeterminate color="primary" size="64" class="mb-4"></v-progress-circular>
                <div class="text-slate-600 text-body-1">Đang tải bài viết...</div>
            </v-container>

            <!-- 404 Not Found State -->
            <v-container v-else-if="!article" class="py-16 text-center bg-white rounded-xl my-8 elevation-1">
                <v-icon size="80" color="grey-lighten-1" class="mb-4">mdi-file-remove-outline</v-icon>
                <h2 class="text-h4 font-weight-black text-slate-800 mb-2">Không Tìm Thấy Bài Viết</h2>
                <p class="text-slate-500 text-body-1 mb-6">Bài viết bạn tìm kiếm có thể đã bị xóa hoặc đường dẫn không đúng.</p>
                <v-btn color="primary" rounded="pill" class="font-weight-bold px-8" @click="router.push('/tin-tuc')">
                    Trở về Trang Tin Tức
                </v-btn>
            </v-container>

            <!-- Article Content View -->
            <div v-else>
                <!-- Breadcrumbs & Category Bar: Brand Blue Gradient + White Text -->
                <div class="bg-gradient-detail py-4 text-white">
                    <v-container>
                        <div class="d-flex align-center flex-wrap ga-2 text-caption text-slate-200">
                            <span class="cursor-pointer hover-text-amber" @click="router.push('/')">Trang chủ</span>
                            <v-icon size="14" color="white">mdi-chevron-right</v-icon>
                            <span class="cursor-pointer hover-text-amber" @click="router.push('/tin-tuc')">Tin tức</span>
                            <v-icon size="14" color="white">mdi-chevron-right</v-icon>
                            <span class="text-white font-weight-bold text-truncate" style="max-width: 450px">{{ article.title }}</span>
                        </div>
                    </v-container>
                </div>

                <v-container class="mt-8 max-w-900">
                    <v-card class="rounded-2xl pa-6 pa-md-10 bg-white elevation-2">
                        <!-- Header Metadata -->
                        <div class="d-flex align-center ga-3 mb-4">
                            <v-chip color="primary" size="small" class="font-weight-bold px-3 text-white">
                                {{ article.category }}
                            </v-chip>
                            <span class="text-caption text-slate-400 d-flex align-center">
                                <v-icon size="14" class="mr-1">mdi-calendar-clock</v-icon> {{ article.date }}
                            </span>
                            <span class="text-caption text-slate-400">&bull; 4 phút đọc</span>
                        </div>

                        <!-- Title -->
                        <h1 class="text-h3 font-weight-black text-slate-900 leading-snug mb-6">
                            {{ article.title }}
                        </h1>

                        <!-- Author & Share Bar -->
                        <div class="d-flex align-center justify-space-between flex-wrap ga-4 py-4 border-y mb-8">
                            <div class="d-flex align-center ga-3">
                                <v-avatar color="primary" size="44" class="elevation-1">
                                    <span class="text-white text-h6 font-weight-bold">{{ article.author?.charAt(0) }}</span>
                                </v-avatar>
                                <div>
                                    <div class="font-weight-bold text-slate-900 text-body-1">{{ article.author }}</div>
                                    <div class="text-caption text-slate-500">Ban biên tập AeroStride</div>
                                </div>
                            </div>

                            <div class="d-flex align-center ga-2">
                                <v-btn
                                    variant="tonal"
                                    color="error"
                                    rounded="pill"
                                    size="small"
                                    class="font-weight-bold px-4"
                                    prepend-icon="mdi-heart"
                                    @click="handleLike"
                                >
                                    {{ article.likes }} Yêu thích
                                </v-btn>
                                <v-btn
                                    variant="outlined"
                                    color="grey-darken-2"
                                    icon="mdi-share-variant"
                                    size="small"
                                    @click="copyArticleLink"
                                >
                                    <v-icon size="18">mdi-share-variant</v-icon>
                                    <v-tooltip activator="parent" location="top">Sao chép liên kết</v-tooltip>
                                </v-btn>
                            </div>
                        </div>

                        <!-- Excerpt Box -->
                        <div class="bg-blue-lighten-5 pa-5 rounded-xl text-slate-800 text-subtitle-1 font-italic mb-8 border-l-4 border-primary">
                            "{{ article.excerpt }}"
                        </div>

                        <!-- Featured Cover Image -->
                        <v-img :src="article.image" height="450" cover class="rounded-2xl mb-8 shadow-sm"></v-img>

                        <!-- Article Body HTML -->
                        <div class="article-detail-body text-slate-800 text-body-1 mb-12" v-html="article.content"></div>

                        <!-- Interaction Bar -->
                        <div class="d-flex align-center justify-center ga-4 py-6 border-y bg-grey-lighten-5 rounded-xl mb-10">
                            <span class="font-weight-bold text-slate-700">Bài viết có hữu ích với bạn?</span>
                            <v-btn color="error" variant="flat" rounded="pill" prepend-icon="mdi-heart" class="font-weight-bold px-6" @click="handleLike">
                                Thích bài viết ({{ article.likes }})
                            </v-btn>
                            <v-btn color="primary" variant="outlined" rounded="pill" prepend-icon="mdi-content-copy" class="font-weight-bold px-6" @click="copyArticleLink">
                                Chia sẻ
                            </v-btn>
                        </div>

                        <!-- Comments Section -->
                        <div class="comments-container pt-4">
                            <h3 class="text-h5 font-weight-black text-slate-900 mb-6 d-flex align-center">
                                <v-icon color="primary" class="mr-2">mdi-comment-text-multiple-outline</v-icon>
                                Bình Luận ({{ article.comments ? article.comments.length : 0 }})
                            </h3>

                            <!-- Comments List -->
                            <div v-if="article.comments && article.comments.length > 0" class="mb-8 ga-3 d-flex flex-column">
                                <div
                                    v-for="(c, idx) in article.comments"
                                    :key="idx"
                                    class="comment-item pa-4 bg-slate-50 rounded-xl border"
                                >
                                    <div class="d-flex align-center justify-space-between mb-2">
                                        <div class="d-flex align-center ga-2">
                                            <v-avatar color="primary-lighten" size="32">
                                                <v-icon size="18" color="primary">mdi-account</v-icon>
                                            </v-avatar>
                                            <span class="font-weight-bold text-slate-900 text-body-2">{{ c.name }}</span>
                                        </div>
                                        <span class="text-caption text-slate-400">{{ c.date }}</span>
                                    </div>
                                    <p class="text-slate-700 text-body-2 mb-0 pl-10">{{ c.text }}</p>
                                </div>
                            </div>
                            <div v-else class="text-slate-500 text-body-2 text-center py-6 bg-slate-50 rounded-xl mb-8">
                                Chưa có bình luận nào. Hãy là người đầu tiên để lại ý kiến!
                            </div>

                            <!-- Comment Input Form -->
                            <v-card class="pa-6 rounded-xl border bg-slate-50" flat>
                                <h4 class="text-subtitle-1 font-weight-bold text-slate-800 mb-3">Đóng góp ý kiến của bạn</h4>
                                <v-row dense>
                                    <v-col cols="12" sm="6" class="mb-3">
                                        <v-text-field
                                            v-model="commentName"
                                            placeholder="Họ và tên của bạn (Tùy chọn)"
                                            variant="outlined"
                                            density="comfortable"
                                            bg-color="white"
                                            hide-details
                                            class="rounded-lg"
                                        ></v-text-field>
                                    </v-col>
                                    <v-col cols="12" class="mb-3">
                                        <v-textarea
                                            v-model="commentText"
                                            placeholder="Viết nhận xét của bạn về bài viết này..."
                                            variant="outlined"
                                            rows="3"
                                            density="comfortable"
                                            bg-color="white"
                                            hide-details
                                            class="rounded-lg"
                                        ></v-textarea>
                                    </v-col>
                                    <v-col cols="12" class="text-right">
                                        <v-btn
                                            color="primary"
                                            rounded="pill"
                                            class="font-weight-bold text-none px-8"
                                            :loading="isSubmittingComment"
                                            :disabled="!commentText.trim()"
                                            @click="handleAddComment"
                                        >
                                            Gửi Bình Luận
                                        </v-btn>
                                    </v-col>
                                </v-row>
                            </v-card>
                        </div>
                    </v-card>

                    <!-- Related Articles Section -->
                    <div v-if="relatedArticles.length > 0" class="mt-12">
                        <h3 class="text-h4 font-weight-black text-slate-900 mb-6 text-center">BÀI VIẾT NỔI BẬT KHÁC</h3>
                        <v-row>
                            <v-col v-for="rel in relatedArticles" :key="rel.id" cols="12" sm="4">
                                <v-card
                                    class="h-100 rounded-xl overflow-hidden cursor-pointer elevation-2 hover-lift bg-white d-flex flex-column"
                                    @click="goToArticle(rel.id)"
                                >
                                    <v-img :src="rel.image" height="160" cover></v-img>
                                    <v-card-text class="pa-4 d-flex flex-column flex-grow-1">
                                        <div class="text-caption text-slate-400 mb-1">{{ rel.date }}</div>
                                        <h4 class="text-subtitle-1 font-weight-bold text-slate-900 line-clamp-2 mb-2 hover-text-blue">{{ rel.title }}</h4>
                                        <p class="text-caption text-slate-500 line-clamp-2 mt-auto mb-0">{{ rel.excerpt }}</p>
                                    </v-card-text>
                                </v-card>
                            </v-col>
                        </v-row>
                    </div>
                </v-container>
            </div>
        </main>

        <MainFooter />

        <CustomerChat />
    </div>
</template>

<style scoped lang="scss">
.main-content {
    padding-top: 60px;
}

.bg-gradient-detail {
    background: linear-gradient(135deg, #1e257c 0%, #23318c 50%, #1d4ed8 100%) !important;
    box-shadow: 0 4px 16px rgba(30, 37, 124, 0.2);
}

.hover-text-amber {
    transition: color 0.2s ease;
    &:hover {
        color: #fef08a !important;
    }
}

.max-w-900 {
    max-width: 900px;
}

.hover-text-blue {
    transition: color 0.2s ease;
    &:hover {
        color: #1e257c !important;
    }
}

.hover-lift {
    transition: transform 0.3s ease, box-shadow 0.3s ease;
    &:hover {
        transform: translateY(-5px);
        box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08) !important;
    }
}

.line-clamp-2 {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.article-detail-body :deep(h3) {
    font-size: 1.35rem;
    font-weight: 800;
    color: #0f172a;
    margin-top: 1.75rem;
    margin-bottom: 0.75rem;
}

.article-detail-body :deep(p) {
    font-size: 1.05rem;
    line-height: 1.8;
    color: #334155;
    margin-bottom: 1.25rem;
}

.article-detail-body :deep(ul) {
    margin-left: 1.5rem;
    margin-bottom: 1.25rem;
    li {
        margin-bottom: 0.5rem;
        line-height: 1.7;
    }
}

.bg-blue-lighten-5 {
    background: #eff6ff;
}

.border-l-4 {
    border-left-width: 4px !important;
}
</style>
