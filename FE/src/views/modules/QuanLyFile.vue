<script setup>
import { ref, onMounted } from 'vue';
import { dichVuFile } from '@/services/core/dichVuFile';

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
  <div>
    <!-- Header -->
    <div class="d-flex justify-space-between align-center mb-6">
      <div>
        <h1 class="text-h4 font-weight-bold">Quản lý file</h1>
        <p class="text-subtitle-1 text-medium-emphasis">Quản lý tài liệu và media</p>
      </div>
      <v-btn 
        color="primary" 
        size="large"
        @click="uploadDialog = true"
      >
        <v-icon start>mdi-upload</v-icon>
        Tải lên file
      </v-btn>
    </div>

    <!-- Upload Area -->
    <v-card 
      elevation="2" 
      class="mb-4"
      :class="{ 'border-primary border-2': dragging }"
      @dragover="handleDragOver"
      @dragleave="handleDragLeave"
      @drop="handleDrop"
    >
      <v-card-text class="text-center pa-8">
        <v-icon size="64" color="primary" class="mb-4">mdi-cloud-upload</v-icon>
        <h3 class="text-h5 font-weight-medium mb-2">Kéo và thả file vào đây</h3>
        <p class="text-body-2 text-medium-emphasis mb-4">Hoặc nhấn vào nút tải lên để chọn file</p>
        <v-btn color="primary" variant="tonal" @click="uploadDialog = true">
          <v-icon start>mdi-upload</v-icon>
          Chọn file
        </v-btn>
      </v-card-text>
    </v-card>

    <!-- Files Grid -->
    <v-card elevation="2">
      <v-card-text>
        <v-row v-if="loading">
          <v-col cols="12" class="text-center py-8">
            <v-progress-circular indeterminate color="primary" size="48"></v-progress-circular>
          </v-col>
        </v-row>
        
        <v-row v-else-if="files.length === 0">
          <v-col cols="12" class="text-center py-8">
            <v-icon size="64" color="grey-lighten-1" class="mb-4">mdi-folder-open</v-icon>
            <h3 class="text-h5 font-weight-medium mb-2">Chưa có file nào</h3>
            <p class="text-body-2 text-medium-emphasis">Tải lên file đầu tiên để bắt đầu</p>
          </v-col>
        </v-row>

        <v-row v-else>
          <v-col 
            v-for="file in files" 
            :key="file.id"
            cols="12" 
            sm="6" 
            md="4" 
            lg="3"
          >
            <v-card variant="tonal" class="pa-4 text-center" hover>
              <v-icon 
                :icon="getFileIcon(file.fileName)" 
                size="48" 
                color="primary"
                class="mb-3"
              ></v-icon>
              <div class="text-subtitle-1 font-weight-medium text-truncate mb-1">
                {{ file.fileName }}
              </div>
              <div class="text-caption text-medium-emphasis mb-3">
                {{ formatFileSize(file.fileSize) }}
              </div>
              <div class="d-flex justify-center gap-1">
                <v-btn
                  icon="mdi-eye"
                  variant="text"
                  color="info"
                  size="small"
                  @click="previewFile(file)"
                ></v-btn>
                <v-btn
                  icon="mdi-download"
                  variant="text"
                  color="success"
                  size="small"
                  :href="dichVuFile.layUrlFile(file.filePath)"
                  target="_blank"
                ></v-btn>
                <v-btn
                  icon="mdi-delete"
                  variant="text"
                  color="error"
                  size="small"
                  @click="deleteFile(file.id)"
                ></v-btn>
              </div>
            </v-card>
          </v-col>
        </v-row>

        <!-- Pagination -->
        <v-pagination
          v-if="pagination.total > pagination.itemsPerPage"
          v-model="pagination.page"
          :length="Math.ceil(pagination.total / pagination.itemsPerPage)"
          @update:model-value="loadFiles"
          class="mt-4"
        ></v-pagination>
      </v-card-text>
    </v-card>

    <!-- Upload Dialog -->
    <v-dialog v-model="uploadDialog" max-width="500">
      <v-card>
        <v-card-title>Tải lên file</v-card-title>
        <v-card-text>
          <v-file-input
            label="Chọn file"
            multiple
            show-size
            variant="outlined"
            @change="handleFileUpload"
          ></v-file-input>
          <p class="text-caption text-medium-emphasis">
            Hỗ trợ các định dạng: PDF, DOC, DOCX, XLS, XLSX, PPT, PPTX, JPG, PNG, GIF, MP4, MP3, ZIP
          </p>
        </v-card-text>
        <v-card-actions>
          <v-spacer></v-spacer>
          <v-btn @click="uploadDialog = false">Đóng</v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>

    <!-- Preview Dialog -->
    <v-dialog v-model="previewDialog" max-width="800">
      <v-card v-if="selectedFile">
        <v-card-title class="d-flex justify-space-between align-center">
          <span>{{ selectedFile.fileName }}</span>
          <v-btn icon="mdi-close" @click="previewDialog = false"></v-btn>
        </v-card-title>
        <v-card-text>
          <div class="text-center">
            <v-icon 
              :icon="getFileIcon(selectedFile.fileName)" 
              size="120" 
              color="primary"
              class="mb-4"
            ></v-icon>
            <p class="text-body-1 mb-2">{{ selectedFile.fileName }}</p>
            <p class="text-body-2 text-medium-emphasis mb-4">
              Kích thước: {{ formatFileSize(selectedFile.fileSize) }}
            </p>
            <v-btn 
              color="primary" 
              :href="dichVuFile.layUrlFile(selectedFile.filePath)"
              target="_blank"
            >
              <v-icon start>mdi-download</v-icon>
              Tải về
            </v-btn>
          </div>
        </v-card-text>
      </v-card>
    </v-dialog>
  </div>
</template>
