<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormRules, UploadUserFile } from 'element-plus'
import type { DescItem, DetailColumnDef, StatusMap } from '@/types/components'
import PageHeader from '@/components/base/PageHeader.vue'
import DescList from '@/components/base/DescList.vue'
import FileUploader from '@/components/base/FileUploader.vue'
import MasterDetailForm from '@/components/business/MasterDetailForm.vue'
import ScanInput from '@/components/business/ScanInput.vue'
import { formatDateTime, formatNumber } from '@/utils/format'

defineOptions({ name: 'FormDemo' })

// ---------- MasterDetailForm：工单 + 物料需求明细 ----------

interface OrderMaster {
  orderNo: string
  productName: string
  workshop: string
  planDate: string
  remark: string
}

interface MaterialDetail {
  materialCode: string
  materialName: string
  quantity: number | null
  unit: string | null
  needDate: string | null
}

const master = reactive<OrderMaster>({
  orderNo: 'WO20260714001',
  productName: 'AS-05 比赛级羽毛球',
  workshop: '',
  planDate: '',
  remark: '',
})

const details = ref<MaterialDetail[]>([
  { materialCode: 'M-FEATHER-01', materialName: '鹅毛片 A 级', quantity: 8000, unit: '片', needDate: '2026-07-16' },
  { materialCode: 'M-CORK-02', materialName: '软木球头', quantity: 500, unit: '个', needDate: '2026-07-16' },
])

const masterRules: FormRules = {
  orderNo: [{ required: true, message: '请输入工单号', trigger: 'blur' }],
  productName: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  workshop: [{ required: true, message: '请选择车间', trigger: 'change' }],
  planDate: [{ required: true, message: '请选择计划完成日期', trigger: 'change' }],
}

const detailColumns: DetailColumnDef<MaterialDetail>[] = [
  { prop: 'materialCode', label: '物料编码', editor: 'input', required: true, width: 180 },
  { prop: 'materialName', label: '物料名称', editor: 'input', required: true },
  { prop: 'quantity', label: '需求数量', editor: 'number', required: true, width: 160 },
  {
    prop: 'unit',
    label: '单位',
    editor: 'select',
    width: 120,
    options: [
      { label: '片', value: '片' },
      { label: '个', value: '个' },
      { label: '千克', value: '千克' },
    ],
  },
  { prop: 'needDate', label: '需求日期', editor: 'date', width: 170 },
]

// 泛型组件取不到 InstanceType，按 defineExpose 结构声明模板引用
const mdfRef = ref<{ validate: () => Promise<boolean> }>()
const saving = ref(false)

async function handleSave() {
  const ok = await mdfRef.value?.validate()
  if (!ok) return
  saving.value = true
  try {
    await new Promise((resolve) => setTimeout(resolve, 400))
    ElMessage.success(`保存成功：主表 1 条 + 明细 ${details.value.length} 行`)
  } finally {
    saving.value = false
  }
}

// ---------- DescList：详情 + 脱敏 ----------

const ORDER_STATUS: StatusMap = {
  IN_PROGRESS: { type: 'warning', text: '生产中' },
}

const detailData = {
  orderNo: 'WO20260713002',
  productName: 'AS-40 国际赛事球',
  status: 'IN_PROGRESS',
  quantity: 3500,
  workshop: '二车间',
  leader: '王工',
  leaderPhone: '13812345678',
  planDate: '2026-07-20',
  amount: 128000,
}

const descItems: DescItem<typeof detailData>[] = [
  { prop: 'orderNo', label: '工单号' },
  { prop: 'productName', label: '产品' },
  { prop: 'status', label: '状态', statusMap: ORDER_STATUS },
  { prop: 'quantity', label: '计划数量', formatter: (data) => `${formatNumber(data.quantity)} 打` },
  { prop: 'workshop', label: '车间' },
  { prop: 'leader', label: '负责人' },
  { prop: 'leaderPhone', label: '联系电话', mask: true },
  { prop: 'planDate', label: '计划完成' },
  { prop: 'amount', label: '订单金额', formatter: (data) => `¥ ${formatNumber(data.amount)}` },
]

// ---------- ScanInput ----------

interface ScanRecord {
  code: string
  time: string
}

const scanLoading = ref(false)
const scanRecords = ref<ScanRecord[]>([])

async function handleScan(code: string) {
  scanLoading.value = true
  try {
    // 模拟报工接口调用
    await new Promise((resolve) => setTimeout(resolve, 400))
    scanRecords.value.unshift({ code, time: formatDateTime(new Date()) })
    if (scanRecords.value.length > 8) scanRecords.value.pop()
    ElMessage.success(`条码 ${code} 报工成功`)
  } finally {
    scanLoading.value = false
  }
}

// ---------- FileUploader ----------

const attachFiles = ref<UploadUserFile[]>([])
const imageFiles = ref<UploadUserFile[]>([])
</script>

<template>
  <div class="page-container">
    <PageHeader
      title="主从表单与详情示例"
      description="MasterDetailForm 行内编辑明细 + DescList 脱敏详情 + ScanInput 扫码 + FileUploader 上传"
    />

    <el-card shadow="never">
      <template #header>MasterDetailForm — 工单 + 物料需求明细</template>
      <MasterDetailForm
        ref="mdfRef"
        :master-model="master"
        :rules="masterRules"
        :detail-columns="detailColumns"
        detail-title="物料需求"
        :min-details="1"
        v-model:details="details"
      >
        <template #master>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="工单号" prop="orderNo">
                <el-input v-model="master.orderNo" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="产品" prop="productName">
                <el-input v-model="master.productName" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="车间" prop="workshop">
                <el-select v-model="master.workshop" placeholder="请选择">
                  <el-option label="一车间" value="一车间" />
                  <el-option label="二车间" value="二车间" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="计划完成" prop="planDate">
                <el-date-picker
                  v-model="master.planDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择日期"
                  class="form-demo__date"
                />
              </el-form-item>
            </el-col>
            <el-col :span="16">
              <el-form-item label="备注">
                <el-input v-model="master.remark" placeholder="选填" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>
      </MasterDetailForm>
      <div class="form-demo__footer">
        <el-button type="primary" :loading="saving" @click="handleSave">保存工单</el-button>
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>DescList — 详情描述列表（含脱敏、状态、格式化）</template>
      <DescList :items="descItems" :data="detailData" :column="3" />
    </el-card>

    <el-row :gutter="16">
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="form-demo__half">
          <template #header>ScanInput — 扫码录入（支持扫码枪回车）</template>
          <ScanInput :loading="scanLoading" :min-length="6" @scan="handleScan" />
          <el-table :data="scanRecords" size="small" class="form-demo__scan-table">
            <el-table-column prop="code" label="条码" />
            <el-table-column prop="time" label="时间" width="180" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-card shadow="never" class="form-demo__half">
          <template #header>FileUploader — 附件与图片上传</template>
          <FileUploader
            v-model:file-list="attachFiles"
            accept=".pdf,.xlsx,.docx"
            :max-size-mb="20"
            :limit="3"
            tip="工艺文件/检验报告，最多 3 个，单个 ≤20MB"
          />
          <el-divider />
          <FileUploader
            v-model:file-list="imageFiles"
            accept=".jpg,.jpeg,.png"
            list-type="picture-card"
            :max-size-mb="5"
            :limit="4"
            tip="现场照片，最多 4 张，单张 ≤5MB"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.form-demo__date {
  width: 100%;
}

.form-demo__footer {
  margin-top: 16px;
  text-align: right;
}

.form-demo__scan-table {
  margin-top: 12px;
}

.form-demo__half {
  height: 100%;
}
</style>
