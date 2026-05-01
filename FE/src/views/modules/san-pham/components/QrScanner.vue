<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import { Html5QrcodeScanner } from 'html5-qrcode';
import { XIcon, CameraIcon } from 'vue-tabler-icons';

const props = defineProps({
  show: Boolean
});

const emit = defineEmits(['update:show', 'scan']);

const scanner = ref(null);

const onScanSuccess = (decodedText, decodedResult) => {
  emit('scan', decodedText);
  stopScanner();
  emit('update:show', false);
};

const stopScanner = () => {
    if (scanner.value) {
        scanner.value.clear().catch(err => console.error(err));
    }
};

onMounted(() => {
    if (props.show) {
        startScanner();
    }
});

const startScanner = () => {
    setTimeout(() => {
        scanner.value = new Html5QrcodeScanner(
            "qr-reader",
            { fps: 10, qrbox: { width: 250, height: 250 } },
            /* verbose= */ false
        );
        scanner.value.render(onScanSuccess, (err) => {
            // silent fail for non-detections
        });
    }, 100);
};

watch(() => props.show, (val) => {
    if (val) {
        startScanner();
    } else {
        stopScanner();
    }
});

onBeforeUnmount(() => {
    stopScanner();
});
</script>

<template>
  <v-dialog :model-value="show" @update:model-value="emit('update:show', $event)" max-width="500" transition="dialog-bottom-transition">
    <v-card class="rounded-xl overflow-hidden">
      <div class="px-6 py-4 bg-slate-900 text-white d-flex align-center justify-space-between">
        <div class="d-flex align-center gap-2">
            <CameraIcon size="20" />
            <h3 class="font-weight-bold">Quét mã QR sản phẩm</h3>
        </div>
        <v-btn icon variant="text" color="white" size="small" @click="emit('update:show', false)">
          <XIcon size="20" />
        </v-btn>
      </div>
      
      <v-card-text class="pa-0 bg-slate-100">
        <div id="qr-reader" style="width: 100%"></div>
      </v-card-text>
      
      <v-card-actions class="pa-4 bg-white border-t">
        <div class="text-caption text-slate-500 text-center w-100 font-weight-medium">
            Hướng camera về phía mã QR trên sản phẩm để quét tự động.
        </div>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<style>
#qr-reader {
    border: none !important;
}
#qr-reader__scan_region {
    background: white;
}
#qr-reader__dashboard_section_csr button {
    background: #000;
    color: white;
    padding: 8px 16px;
    border-radius: 8px;
    font-weight: bold;
    margin-top: 10px;
}
</style>

