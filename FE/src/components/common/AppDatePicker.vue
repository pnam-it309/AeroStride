<template>
  <div class="app-datepicker" :class="{ 'is-range': range }">
    <!-- Single date -->
    <template v-if="!range">
      <v-text-field
        v-model="internalText"
        :placeholder="placeholder"
        variant="outlined"
        density="compact"
        hide-details
        append-inner-icon="mdi-calendar-month-outline"
        :class="inputClass"
        v-bind="textFieldProps"
        :clearable="clearable && !!modelValue"
        @click:clear="onClear"
        @click:append-inner="open = true"
        @blur="onInputBlur"
        @keyup.enter="onInputBlur"
      />
    </template>

    <!-- Range -->
    <template v-else>
      <v-text-field
        v-model="internalText"
        :placeholder="placeholder"
        variant="outlined"
        density="compact"
        hide-details
        append-inner-icon="mdi-calendar-month-outline"
        :class="inputClass"
        v-bind="textFieldProps"
        :clearable="clearable && !!modelValue"
        @click:clear="onClear"
        @click:append-inner="open = true"
        @blur="onInputBlur"
        @keyup.enter="onInputBlur"
      />
    </template>

    <!-- Dialog Picker -->
    <v-dialog v-model="open" :max-width="range ? 680 : 380" persistent scrollable>
      <v-card rounded="xl">
        <v-card-title class="d-flex align-center justify-space-between pa-4 bg-grey-lighten-4">
          <span class="text-body-1 font-weight-bold">
            {{ range ? 'Chọn khoảng thời gian' : 'Chọn ngày' }}
          </span>
          <v-btn icon variant="text" size="small" @click="open = false">
            <v-icon>mdi-close</v-icon>
          </v-btn>
        </v-card-title>

        <v-card-text class="pa-4">
          <!-- Calendar -->
          <div class="calendar-nav d-flex align-center justify-space-between mb-4">
            <v-btn icon variant="text" size="small" @click="prevMonth">
              <v-icon>mdi-chevron-left</v-icon>
            </v-btn>
            <div class="d-flex ga-2 align-center">
              <select v-model="currentMonth" class="custom-select font-weight-bold text-body-1">
                <option v-for="m in monthOptions" :key="m.value" :value="m.value">{{ m.title }}</option>
              </select>
              <select v-model="currentYear" class="custom-select font-weight-bold text-body-1">
                <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option>
              </select>
            </div>
            <v-btn icon variant="text" size="small" @click="nextMonth">
              <v-icon>mdi-chevron-right</v-icon>
            </v-btn>
          </div>

          <div class="calendar-grid">
            <div v-for="d in dayHeaders" :key="d" class="calendar-day-header">
              {{ d }}
            </div>
            <div v-for="(day, i) in calendarDays" :key="i"
              class="calendar-day"
              :class="dayClass(day)"
              @click="selectDay(day)"
            >
              <span v-if="day">{{ day.day }}</span>
            </div>
          </div>

          <!-- Time picker -->
          <template v-if="enableTimePicker">
            <v-divider class="my-4" />
            <div class="time-picker-section">
              <div v-if="range" class="d-flex ga-4">
                <div class="flex-grow-1">
                  <label class="text-caption text-grey font-weight-bold mb-1 d-block">Từ</label>
                  <v-text-field
                    v-model="startTimeStr"
                    type="time"
                    variant="outlined"
                    density="compact"
                    hide-details
                  />
                </div>
                <div class="flex-grow-1">
                  <label class="text-caption text-grey font-weight-bold mb-1 d-block">Đến</label>
                  <v-text-field
                    v-model="endTimeStr"
                    type="time"
                    variant="outlined"
                    density="compact"
                    hide-details
                  />
                </div>
              </div>
              <div v-else>
                <label class="text-caption text-grey font-weight-bold mb-1 d-block">Giờ</label>
                <v-text-field
                  v-model="singleTimeStr"
                  type="time"
                  variant="outlined"
                  density="compact"
                  hide-details
                />
              </div>
            </div>
          </template>
        </v-card-text>

        <v-card-actions class="pa-4 pt-0 d-flex justify-end ga-3">
          <v-btn variant="outlined" rounded="pill" class="font-weight-bold text-none" @click="open = false">
            Hủy
          </v-btn>
          <v-btn color="primary" variant="flat" rounded="pill" class="font-weight-bold text-none px-6" @click="onApply">
            Áp dụng
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue';

const props = defineProps({
  modelValue: {
    type: [Date, Array, String, Number, Object],
    default: null
  },
  range: {
    type: Boolean,
    default: false
  },
  enableTimePicker: {
    type: Boolean,
    default: false
  },
  placeholder: {
    type: String,
    default: 'Chọn ngày'
  },
  format: {
    type: [String, Function],
    default: null
  },
  textFieldProps: {
    type: Object,
    default: () => ({})
  },
  inputClass: {
    type: [String, Object, Array],
    default: 'compact-input date-field'
  },
  clearable: {
    type: Boolean,
    default: true
  },
  minDate: {
    type: [Date, String, Number],
    default: null
  },
  maxDate: {
    type: [Date, String, Number],
    default: null
  }
});

const emit = defineEmits(['update:modelValue']);

const open = ref(false);
const viewDate = ref(new Date());

// Internal date selection state
const tempStart = ref(null);
const tempEnd = ref(null);
const tempSingle = ref(null);
const startTimeStr = ref('00:00');
const endTimeStr = ref('12:00');
const singleTimeStr = ref('12:00');

const dayHeaders = ['T2', 'T3', 'T4', 'T5', 'T6', 'T7', 'CN'];

const monthOptions = Array.from({ length: 12 }, (_, i) => ({
  title: `Tháng ${i + 1}`,
  value: i
}));

const yearOptions = computed(() => {
  const currentYear = new Date().getFullYear();
  const years = [];
  for (let y = currentYear - 100; y <= currentYear + 10; y++) {
    years.push(y);
  }
  return years;
});

const currentMonth = computed({
  get: () => viewDate.value.getMonth(),
  set: (val) => {
    const d = new Date(viewDate.value);
    d.setMonth(val);
    viewDate.value = d;
  }
});

const currentYear = computed({
  get: () => viewDate.value.getFullYear(),
  set: (val) => {
    const d = new Date(viewDate.value);
    d.setFullYear(val);
    viewDate.value = d;
  }
});

const calendarDays = computed(() => {
  const year = viewDate.value.getFullYear();
  const month = viewDate.value.getMonth();
  const firstDay = new Date(year, month, 1);
  const lastDay = new Date(year, month + 1, 0);
  const startPad = (firstDay.getDay() + 6) % 7;

  const days = [];
  for (let i = 0; i < startPad; i++) days.push(null);
  for (let d = 1; d <= lastDay.getDate(); d++) days.push({ day: d, date: new Date(year, month, d), isCurrent: true });
  const remaining = 42 - days.length;
  for (let i = 1; i <= remaining; i++) days.push(null);
  return days;
});

const isSameDay = (a, b) => {
  if (!a || !b) return false;
  return a.getFullYear() === b.getFullYear() &&
    a.getMonth() === b.getMonth() &&
    a.getDate() === b.getDate();
};

const toDateStart = (value) => {
  if (!value) return null;
  const date = value instanceof Date ? new Date(value) : new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  date.setHours(0, 0, 0, 0);
  return date;
};

const isDateDisabled = (date) => {
  if (!date) return true;
  const current = toDateStart(date);
  const min = toDateStart(props.minDate);
  const max = toDateStart(props.maxDate);

  if (min && current < min) return true;
  if (max && current > max) return true;
  return false;
};

const isInRange = (day) => {
  if (!day || !tempStart.value) return false;
  if (!tempEnd.value) return isSameDay(day.date, tempStart.value);
  const d = day.date;
  const s = tempStart.value;
  const e = tempEnd.value;
  return d >= s && d <= e;
};

const dayClass = (day) => {
  if (!day) return 'disabled';
  const d = day.date;
  const disabled = isDateDisabled(d);
  const today = new Date();
  const isToday = isSameDay(d, today);
  const isStart = tempStart.value && isSameDay(d, tempStart.value);
  const isEnd = tempEnd.value && isSameDay(d, tempEnd.value);
  const inRange = isInRange(day);
  const isSelected = (!props.range && tempSingle.value && isSameDay(d, tempSingle.value)) || isStart || isEnd;
  return {
    'disabled': disabled,
    'today': isToday,
    'selected': !!isSelected,
    'in-range': inRange && !isStart && !isEnd,
    'range-start': !!isStart,
    'range-end': !!isEnd
  };
};

const formatDateDisplay = (date) => {
  if (!date) return '';
  const d = date.getDate().toString().padStart(2, '0');
  const m = (date.getMonth() + 1).toString().padStart(2, '0');
  const y = date.getFullYear();
  if (props.enableTimePicker) {
    const h = date.getHours().toString().padStart(2, '0');
    const min = date.getMinutes().toString().padStart(2, '0');
    return `${d}/${m}/${y} ${h}:${min}`;
  }
  return `${d}/${m}/${y}`;
};

const applyTimeToDate = (date, timeStr) => {
  if (!date || !timeStr) return date;
  const [h, m] = timeStr.split(':').map(Number);
  const d = new Date(date);
  d.setHours(h || 0, m || 0, 0, 0);
  return d;
};

const displayText = computed(() => {
  if (!props.modelValue) return '';
  if (props.range && Array.isArray(props.modelValue)) {
    const [s, e] = props.modelValue;
    if (!s) return '';
    const startStr = formatDateDisplay(s);
    if (!e) return startStr;
    return `${startStr} - ${formatDateDisplay(e)}`;
  }
  const val = props.modelValue;
  if (val instanceof Date) {
    return formatDateDisplay(val);
  }
  if (typeof val === 'string' || typeof val === 'number') {
    const d = new Date(val);
    if (!isNaN(d.getTime())) return formatDateDisplay(d);
  }
  return '';
});

const internalText = ref('');

watch(displayText, (val) => {
  internalText.value = val;
}, { immediate: true });

const parseDateString = (str) => {
  if (!str) return null;
  const parts = str.trim().split(' ');
  const datePart = parts[0];
  const timePart = parts[1] || '00:00';
  
  if (!datePart) return null;
  const dateParts = datePart.split('/');
  if (dateParts.length !== 3) return null;
  
  const d = parseInt(dateParts[0], 10);
  const m = parseInt(dateParts[1], 10) - 1;
  const y = parseInt(dateParts[2], 10);
  
  if (isNaN(d) || isNaN(m) || isNaN(y)) return null;
  
  const [h, min] = timePart.split(':').map(Number);
  
  const date = new Date(y, m, d, h || 0, min || 0, 0, 0);
  if (isNaN(date.getTime())) return null;
  return date;
};

const onInputBlur = () => {
  const val = internalText.value;
  if (!val || !val.trim()) {
    emit('update:modelValue', props.range ? [null, null] : null);
    return;
  }
  
  if (props.range) {
    const parts = val.split('-');
    if (parts.length === 2) {
      const start = parseDateString(parts[0]);
      const end = parseDateString(parts[1]);
      if (start && end) {
        emit('update:modelValue', [start, end]);
        return;
      }
    }
  } else {
    const d = parseDateString(val);
    if (d && !isDateDisabled(d)) {
      emit('update:modelValue', d);
      return;
    }
  }
  // Reset if invalid
  internalText.value = displayText.value;
};

const prevMonth = () => {
  const d = new Date(viewDate.value);
  d.setMonth(d.getMonth() - 1);
  viewDate.value = d;
};

const nextMonth = () => {
  const d = new Date(viewDate.value);
  d.setMonth(d.getMonth() + 1);
  viewDate.value = d;
};

// Select a day
const selectDay = (day) => {
  if (!day) return;
  const d = day.date;
  if (isDateDisabled(d)) return;

  if (props.range) {
    if (!tempStart.value || (tempStart.value && tempEnd.value)) {
      tempStart.value = d;
      tempEnd.value = null;
    } else {
      if (d < tempStart.value) {
        tempStart.value = d;
      } else {
        tempEnd.value = d;
      }
    }
  } else {
    tempSingle.value = d;
  }
};

const onApply = () => {
  if (props.range) {
    const start = tempStart.value ? applyTimeToDate(tempStart.value, startTimeStr.value) : null;
    const end = tempEnd.value ? applyTimeToDate(tempEnd.value, endTimeStr.value) : null;
    emit('update:modelValue', [start, end]);
  } else {
    const d = tempSingle.value ? applyTimeToDate(tempSingle.value, singleTimeStr.value) : null;
    emit('update:modelValue', d);
  }
  open.value = false;
};

const onClear = () => {
  emit('update:modelValue', props.range ? [null, null] : null);
};

// Sync internal state with modelValue
watch(() => props.modelValue, (val) => {
  if (props.range && Array.isArray(val)) {
    tempStart.value = val[0] || null;
    tempEnd.value = val[1] || null;
    if (val[0]) {
      startTimeStr.value = `${val[0].getHours().toString().padStart(2, '0')}:${val[0].getMinutes().toString().padStart(2, '0')}`;
      viewDate.value = new Date(val[0]);
    }
    if (val[1]) {
      endTimeStr.value = `${val[1].getHours().toString().padStart(2, '0')}:${val[1].getMinutes().toString().padStart(2, '0')}`;
    }
  } else if (!props.range && val) {
    const d = val instanceof Date ? val : new Date(val);
    if (!isNaN(d.getTime())) {
      tempSingle.value = d;
      singleTimeStr.value = `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
      viewDate.value = new Date(d);
    }
  }
}, { immediate: true });

watch(open, (val) => {
  if (val) {
    // Reset to current model value when opening
    const v = props.modelValue;
    if (props.range && Array.isArray(v)) {
      tempStart.value = v[0] || null;
      tempEnd.value = v[1] || null;
      if (v[0]) startTimeStr.value = `${v[0].getHours().toString().padStart(2, '0')}:${v[0].getMinutes().toString().padStart(2, '0')}`;
      if (v[1]) endTimeStr.value = `${v[1].getHours().toString().padStart(2, '0')}:${v[1].getMinutes().toString().padStart(2, '0')}`;
    } else if (!props.range && v) {
      const d = v instanceof Date ? v : new Date(v);
      if (!isNaN(d.getTime())) {
        tempSingle.value = d;
        singleTimeStr.value = `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`;
      }
    }
  }
});
</script>

<style scoped>
.app-datepicker {
  position: relative;
}

.calendar-nav {
  user-select: none;
}

.custom-select {
  border: none;
  background: transparent;
  color: #333;
  outline: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  text-align: center;
  appearance: none;
}

.custom-select:hover {
  background: #f0f1ff;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 2px;
  text-align: center;
}

.calendar-day-header {
  font-size: 0.75rem;
  font-weight: 700;
  color: #9e9e9e;
  padding: 8px 0;
  text-transform: uppercase;
}

.calendar-day {
  padding: 6px 0;
  font-size: 0.85rem;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
  color: #333;
  min-height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.calendar-day:hover:not(.disabled) {
  background: #f0f1ff;
}

.calendar-day.disabled {
  cursor: default;
  color: #cbd5e1;
  pointer-events: none;
}

.calendar-day.today {
  font-weight: 700;
  color: #1e257c;
  position: relative;
}

.calendar-day.today::after {
  content: '';
  position: absolute;
  bottom: 4px;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #1e257c;
}

.calendar-day.selected {
  background: #1e257c !important;
  color: #fff !important;
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(30, 37, 124, 0.2);
}

.calendar-day.in-range {
  background: #e8eaff;
  border-radius: 0;
}

.calendar-day.range-start {
  background: #1e257c !important;
  color: #fff !important;
  border-radius: 8px 0 0 8px;
}

.calendar-day.range-end {
  background: #1e257c !important;
  color: #fff !important;
  border-radius: 0 8px 8px 0;
}

.calendar-day.disabled,
.calendar-day.disabled.today,
.calendar-day.disabled.selected {
  background: transparent !important;
  color: #cbd5e1 !important;
  box-shadow: none;
}

.time-picker-section {
  input[type="time"] {
    font-family: inherit;
  }
}
</style>
