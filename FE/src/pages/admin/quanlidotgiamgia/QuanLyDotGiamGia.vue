<template>
  <div class="container-fluid p-4 bg-light min-vh-100">

    <!-- HEADER -->
    <div class="row mb-4">
      <div class="col-12 d-flex justify-content-between align-items-center mb-3">
        <div>
          <h4 class="fw-bold m-0 text-uppercase">
            <i class="bi bi-tags-fill me-2 text-primary"></i>Quản Lý Đợt Giảm Giá
          </h4>
          <nav>
            <small class="text-muted">Trang chủ > Đợt giảm giá</small>
          </nav>
        </div>

        <button class="btn btn-primary px-4 fw-bold shadow-sm" @click="openDialog()">
          <i class="bi bi-plus-lg me-2"></i>TẠO ĐỢT GIẢM
        </button>
      </div>

      <!-- CARD -->
      <div class="col-md-3" v-for="c in stats" :key="c.label">
        <div class="card border-0 shadow-sm p-3 border-start border-4" :class="c.color">
          <div class="small text-muted fw-bold">{{ c.label }}</div>
          <div class="fs-4 fw-bold">{{ c.value }}</div>
        </div>
      </div>
    </div>

    <!-- FILTER -->
    <div class="card shadow-sm p-4 mb-4">
      <div class="row g-3">
        <div class="col-md-4">
          <label class="fw-bold small">Tìm kiếm</label>
          <el-input v-model="filters.keyword" placeholder="Nhập mã, tên..." clearable @input="handleFilter"/>
        </div>

        <div class="col-md-4">
          <label class="fw-bold small">Thời gian</label>
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="-"
            start-placeholder="Từ ngày"
            end-placeholder="Đến ngày"
            value-format="x"
            style="width:100%"
            @change="handleFilter"
          />
        </div>

        <div class="col-md-4 d-flex align-items-end">
          <button class="btn btn-outline-secondary w-100" @click="resetFilter">
            Làm mới
          </button>
        </div>
      </div>
    </div>

    <!-- TABLE -->
    <div class="card shadow-sm rounded-3 overflow-hidden">

      <div class="p-3 border-bottom fw-bold">
        DANH SÁCH ĐỢT GIẢM GIÁ
      </div>

      <el-table
        :data="list"
        v-loading="loading"
        stripe
        style="width:100%"
      >

        <el-table-column type="index" label="STT" width="60"/>

        <el-table-column label="Thông tin" min-width="250">
          <template #default="{ row }">
            <div class="fw-bold text-primary">{{ row.ma }}</div>
            <div class="fw-semibold">{{ row.ten }}</div>
          </template>
        </el-table-column>

        <el-table-column label="Giảm giá" width="180">
          <template #default="{ row }">
            <div class="fw-bold text-danger fs-5">
              {{ formatMoney(row.soTienGiam) }}
            </div>
            <div class="small text-muted">
              Điều kiện: {{ formatMoney(row.dieuKienGiamGia) }}
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Thời gian" width="220">
          <template #default="{ row }">
            <div class="small">
              <div>BD: <b>{{ formatDate(row.ngayBatDau) }}</b></div>
              <div>KT: <b>{{ formatDate(row.ngayKetThuc) }}</b></div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="Trạng thái" width="140">
          <template #default="{ row }">
            <span :class="getStatus(row).class">
              {{ getStatus(row).text }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="Thao tác" width="120" fixed="right">
          <template #default="{ row }">
            <button class="btn btn-sm btn-light me-2" @click="openDialog(row)">
              ✏
            </button>
            <button class="btn btn-sm btn-light text-danger" @click="remove(row.id)">
              🗑
            </button>
          </template>
        </el-table-column>

      </el-table>

      <!-- PAGINATION -->
      <div class="p-3 d-flex justify-content-between">
        <span>Hiển thị {{ list.length }} / {{ total }}</span>
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </div>

    <!-- DIALOG -->
    <el-dialog v-model="dialog" width="700px">
      <template #header>
        <b>{{ form.id ? 'CẬP NHẬT' : 'TẠO MỚI' }}</b>
      </template>

      <el-form label-position="top" class="row">

        <div class="col-md-6">
          <el-form-item label="Mã">
            <el-input v-model="form.ma"/>
          </el-form-item>
        </div>

        <div class="col-md-6">
          <el-form-item label="Tên">
            <el-input v-model="form.ten"/>
          </el-form-item>
        </div>

        <div class="col-md-6">
          <el-form-item label="Số tiền giảm">
            <el-input-number v-model="form.soTienGiam" style="width:100%"/>
          </el-form-item>
        </div>

        <div class="col-md-6">
          <el-form-item label="Điều kiện">
            <el-input-number v-model="form.dieuKienGiamGia" style="width:100%"/>
          </el-form-item>
        </div>

        <div class="col-md-12">
          <el-form-item label="Thời gian">
            <el-date-picker
              v-model="form.range"
              type="datetimerange"
              value-format="x"
              style="width:100%"
            />
          </el-form-item>
        </div>

      </el-form>

      <template #footer>
        <button class="btn btn-light" @click="dialog=false">Đóng</button>
        <button class="btn btn-primary" @click="save">Lưu</button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const loading = ref(false)
const dialog = ref(false)

const filters = reactive({
  keyword: '',
  dateRange: []
})

const form = reactive({
  id:null,
  ma:'',
  ten:'',
  soTienGiam:0,
  dieuKienGiamGia:0,
  range:[]
})

const stats = reactive([
  { label:'Tổng', value:0, color:'border-primary' },
  { label:'Đang chạy', value:0, color:'border-success' },
  { label:'Sắp diễn ra', value:0, color:'border-warning' },
  { label:'Kết thúc', value:0, color:'border-danger' }
])

const fetchData = async ()=>{
  loading.value = true
  const res = await fetch(`http://localhost:8080/api/dot-giam-gia?page=${page.value-1}&size=${size.value}`)
  const data = await res.json()

  list.value = data.content
  total.value = data.totalElements
  calcStats()

  loading.value = false
}

const save = async ()=>{
  const payload = {
    ...form,
    ngayBatDau: form.range[0],
    ngayKetThuc: form.range[1]
  }

  await fetch('http://localhost:8080/api/dot-giam-gia',{
    method: form.id ? 'PUT':'POST',
    headers:{'Content-Type':'application/json'},
    body: JSON.stringify(payload)
  })

  ElMessage.success('Lưu thành công')
  dialog.value=false
  fetchData()
}

const remove = async(id)=>{
  await fetch(`http://localhost:8080/api/dot-giam-gia/${id}`,{method:'DELETE'})
  fetchData()
}

const openDialog=(row=null)=>{
  if(row){
    Object.assign(form,row)
    form.range=[row.ngayBatDau,row.ngayKetThuc]
  }else{
    Object.assign(form,{id:null,ma:'',ten:'',range:[]})
  }
  dialog.value=true
}

const handleFilter=()=> fetchData()

const resetFilter=()=>{
  filters.keyword=''
  filters.dateRange=[]
  fetchData()
}

const formatMoney=v=> new Intl.NumberFormat('vi-VN').format(v||0)

const formatDate=t=> t? new Date(t).toLocaleString():'--'

const getStatus=(r)=>{
  const now=Date.now()
  if(now<r.ngayBatDau) return {text:'Sắp diễn ra',class:'badge bg-warning'}
  if(now>r.ngayKetThuc) return {text:'Kết thúc',class:'badge bg-secondary'}
  return {text:'Đang chạy',class:'badge bg-success'}
}

const calcStats=()=>{
  const now=Date.now()
  stats[0].value=list.value.length
  stats[1].value=list.value.filter(i=>now>=i.ngayBatDau&&now<=i.ngayKetThuc).length
  stats[2].value=list.value.filter(i=>now<i.ngayBatDau).length
  stats[3].value=list.value.filter(i=>now>i.ngayKetThuc).length
}

onMounted(fetchData)
</script>