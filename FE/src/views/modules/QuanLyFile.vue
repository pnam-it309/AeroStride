<script setup>
import { ref, onMounted } from 'vue';
import { dichVuFile } from '@/services/core/dichVuFile';
import AdminBreadcrumbs from '@/components/common/AdminBreadcrumbs.vue';

const loading = ref(false);
const files = ref([]);
const selectedFile = ref(null);
const uploadDialog = ref(false);
const previewDialog = ref(false);
const dragging = ref(false);

const pagination = ref({
  page: 1,
  itemsPerPage: 12,
  total: 0
});

const loadFiles = async () => {
  loading.value = true;
  try {
    const params = {
      page: pagination.value.page,
      size: pagination.value.itemsPerPage
    };
    const response = await dichVuFile.layDanhSachFile(params);
    files.value = response.content || response;
    pagination.value.total = response.totalElements || response.length;
  } catch (error) {
    console.error('Error loading files:', error);
  } finally {
    loading.value = false;
  }
};

const handleFileUpload = async (event) => {
  const uploadedFiles = event.target.files;
  if (!uploadedFiles.length) return;

  loading.value = true;
  try {
    for (const file of uploadedFiles) {
      await dichVuFile.taiLenFile(file);
    }
    await loadFiles();
    uploadDialog.value = false;
  } catch (error) {
    console.error('Error uploading files:', error);
  } finally {
    loading.value = false;
  }
};

const deleteFile = async (fileId) => {
  try {
    await dichVuFile.xoaFile(fileId);
    files.value = files.value.filter(file => file.id !== fileId);
  } catch (error) {
    console.error('Error deleting file:', error);
  }
};

const previewFile = (file) => {
  selectedFile.value = file;
  previewDialog.value = true;
};

const formatFileSize = (bytes) => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

const getFileIcon = (fileName) => {
  const extension = fileName.split('.').pop().toLowerCase();
  const iconMap = {
    'pdf': 'mdi-file-pdf',
    'doc': 'mdi-file-word',
    'docx': 'mdi-file-word',
    'xls': 'mdi-file-excel',
    'xlsx': 'mdi-file-excel',
    'ppt': 'mdi-file-powerpoint',
    'pptx': 'mdi-file-powerpoint',
    'jpg': 'mdi-file-image',
    'jpeg': 'mdi-file-image',
    'png': 'mdi-file-image',
    'gif': 'mdi-file-image',
    'mp4': 'mdi-file-video',
    'avi': 'mdi-file-video',
    'mp3': 'mdi-file-music',
    'wav': 'mdi-file-music',
    'zip': 'mdi-file-zip',
    'rar': 'mdi-file-zip'
  };
  return iconMap[extension] || 'mdi-file';
};

const handleDragOver = (event) => {
  event.preventDefault();
  dragging.value = true;
};

const handleDragLeave = (event) => {
  event.preventDefault();
  dragging.value = false;
};

const handleDrop = async (event) => {
  event.preventDefault();
  dragging.value = false;
  
  const droppedFiles = event.dataTransfer.files;
  if (!droppedFiles.length) return;

  loading.value = true;
  try {
    for (const file of droppedFiles) {
      await dichVuFile.taiLenFile(file);
    }
    await loadFiles();
  } catch (error) {
    console.error('Error uploading files:', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadFiles();
});
</script>
<template>
  <div class="pa-6 font-body">
    <!-- Breadcrumbs -->
    <AdminBreadcrumbs
        :items="[
            { title: 'Quản lý hệ thống', disabled: false, href: '#' },
            { title: 'Quản lý File', disabled: true }
        ]"
    />

    <div class="mb-4"></div>

    <!-- Header -->
    <div class="header-section mb-6">
      <div>
        <h1 class="text-h4 font-black text-dark mb-1">Quản lý Tài nguyên</h1>
        <p class="text-subtitle-1 text-slate-500 font-bold">Lưu trữ và quản lý tệp tin hệ thống</p>
      </div>
      <v-btn 
        color="primary" 
        size="large"
        class="font-bold rounded-lg px-6"
        @click="uploadDialog = true"
      >
        <v-icon start size="20">mdi-upload</v-icon>
        Tải lên tài liệu
      </v-btn>
    </div>

    <!-- Upload Area -->
    <v-card 
      class="premium-card mb-6 overflow-hidden"
      :class="{ 'border-primary border-2': dragging }"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDrop"
    >
      <v-card-text class="text-center pa-12 bg-slate-50">
        <div class="icon-blob bg-primary-light mx-auto mb-4" style="width: 80px; height: 80px;">
          <v-icon size="40" color="primary">mdi-cloud-upload</v-icon>
        </div>
        <h3 class="text-h5 font-black text-dark mb-2">Kéo & Thả tệp tin vào đây</h3>
        <p class="text-body-1 text-slate-500 font-bold mb-6">Hệ thống tự động phân loại và lưu trữ an toàn</p>
        <v-btn color="primary" variant="tonal" class="font-bold px-8" height="44" @click="uploadDialog = true">
          <v-icon start size="18">mdi-folder-open</v-icon>
          Duyệt từ máy tính
        </v-btn>
      </v-card-text>
    </v-card>

    <!-- Files Grid -->
    <v-card class="premium-card">
      <div class="card-title-bar">
        <span class="font-bold text-dark text-uppercase" style="font-size: 13px; letter-spacing: 0.05em;">Danh sách tệp tin</span>
        <v-icon color="slate-400">mdi-file-multiple</v-icon>
      </div>
      <v-card-text class="pa-6">
        <v-row v-if="loading">
          <v-col cols="12" class="text-center py-12">
            <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
          </v-col>
        </v-row>
        
        <v-row v-else-if="files.length === 0">
          <v-col cols="12" class="text-center py-12">
            <v-icon size="64" color="slate-200" class="mb-4">mdi-folder-open</v-icon>
            <h3 class="text-h5 font-black text-slate-300">Thư mục trống</h3>
            <p class="text-body-2 text-slate-400 font-bold">Bắt đầu bằng việc tải lên tệp tin mới</p>
          </v-col>
        </v-row>

        <v-row v-else>
          <v-col v-for="file in files" :key="file.id" cols="12" sm="6" md="4" lg="3">
            <v-card class="pa-5 text-center border rounded-xl hover-addr-card h-100 bg-white">
              <div class="icon-blob bg-slate-50 mx-auto mb-4" style="width: 64px; height: 64px;">
                <v-icon :icon="getFileIcon(file.fileName)" size="32" color="primary"></v-icon>
              </div>
              <div class="text-subtitle-1 font-black text-dark text-truncate mb-1 px-2" :title="file.fileName">
                {{ file.fileName }}
              </div>
              <div class="text-caption text-slate-400 font-bold mb-4">
                {{ formatFileSize(file.fileSize) }}
              </div>
              <div class="d-flex justify-center action-controls">
                <v-btn icon class="action-icon-btn" color="info" @click="previewFile(file)">
                  <v-icon size="18">mdi-eye</v-icon>
                </v-btn>
                <v-btn icon class="action-icon-btn" color="success" :href="dichVuFile.layUrlFile(file.filePath)" target="_blank">
                  <v-icon size="18">mdi-download</v-icon>
                </v-btn>
                <v-btn icon class="action-icon-btn" color="error" @click="deleteFile(file.id)">
                  <v-icon size="18">mdi-delete</v-icon>
                </v-btn>
              </div>
            </v-card>
          </v-col>
        </v-row>

        <!-- Pagination -->
        <div class="mt-8 d-flex justify-center">
          <v-pagination
            v-if="pagination.total > pagination.itemsPerPage"
            v-model="pagination.page"
            :length="Math.ceil(pagination.total / pagination.itemsPerPage)"
            @update:model-value="loadFiles"
            density="compact"
            active-color="primary"
          ></v-pagination>
        </div>
      </v-card-text>
    </v-card>

    <!-- Upload Dialog -->
    <v-dialog v-model="uploadDialog" max-width="500" transition="dialog-bottom-transition">
      <v-card class="rounded-xl pa-2">
        <v-card-title class="font-black text-dark text-h5 pa-6">Tải lên dữ liệu</v-card-title>
        <v-card-text class="pa-6 pt-0">
          <v-file-input
            label="Chọn tệp tin"
            multiple
            show-size
            variant="outlined"
            density="comfortable"
            prepend-icon=""
            prepend-inner-icon="mdi-paperclip"
            class="rounded-lg"
            @change="handleFileUpload"
          ></v-file-input>
          <div class="bg-slate-50 pa-4 rounded-lg mt-4 border">
            <p class="text-caption text-slate-500 font-bold leading-relaxed">
              <v-icon size="14" class="mr-1">mdi-information-outline</v-icon>
              Hệ thống tối ưu cho PDF, Word, Excel, Hình ảnh (JPG, PNG) và các tệp nén (ZIP, RAR).
            </p>
          </div>
        </v-card-text>
        <v-card-actions class="pa-6">
          <v-spacer></v-spacer>
          <v-btn variant="text" class="font-bold px-6" @click="uploadDialog = false">Hủy bỏ</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Preview Dialog -->
    <v-dialog v-model="previewDialog" max-width="600" transition="dialog-bottom-transition">
      <v-card v-if="selectedFile" class="rounded-xl pa-2">
        <v-card-title class="d-flex justify-space-between align-center pa-6">
          <span class="font-black text-dark">{{ selectedFile.fileName }}</span>
          <v-btn icon="mdi-close" variant="text" @click="previewDialog = false"></v-btn>
        </v-card-title>
        <v-card-text class="pa-8 text-center pt-0">
          <div class="icon-blob bg-primary-light mx-auto mb-6" style="width: 120px; height: 120px;">
            <v-icon :icon="getFileIcon(selectedFile.fileName)" size="64" color="primary"></v-icon>
          </div>
          <p class="text-h6 font-black text-dark mb-1">{{ selectedFile.fileName }}</p>
          <p class="text-body-1 text-slate-400 font-bold mb-8">
            Dung lượng: {{ formatFileSize(selectedFile.fileSize) }}
          </p>
          <v-btn 
            color="primary" 
            size="large"
            block
            class="font-bold rounded-lg"
            :href="dichVuFile.layUrlFile(selectedFile.filePath)"
            target="_blank"
          >
            <v-icon start>mdi-download</v-icon>
            Tải về ngay
          </v-btn>
        </v-card-text>
      </v-card>
    </v-dialog>
  </div>
</template>

<style scoped>
.bg-primary-light { background: #eff6ff; }
.border-primary { border-color: #3b82f6 !important; }
.border-2 { border-width: 2px !important; }
</style>



