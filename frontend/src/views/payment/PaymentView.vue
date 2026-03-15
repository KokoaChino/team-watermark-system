<template>
  <div class="payment-view">
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>点数充值</span>
          </template>
          <div class="balance-info">
            <span class="label">当前团队点数：</span>
            <span class="value">{{ teamInfo?.pointBalance || 0 }}</span>
          </div>
          <el-divider />
          <el-form label-width="100px">
            <el-form-item label="充值点数">
              <el-input-number
                v-model="points"
                :min="1"
                :max="10000"
                @change="handlePointsChange"
              />
            </el-form-item>
            <el-form-item label="应付金额">
              <span class="amount">¥{{ (points * 0.01).toFixed(2) }}</span>
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                size="large"
                :loading="loading"
                @click="handlePay"
              >
                立即充值
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="info-card">
          <template #header>
            <span>充值说明</span>
          </template>
          <ul class="info-list">
            <li>1 点 = 0.01 元</li>
            <li>每次充值最低 1 点，最高 10000 点</li>
            <li>充值成功后，点数将自动添加到团队账户</li>
            <li>使用支付宝沙箱支付完成付款</li>
          </ul>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showQrDialog" title="扫码支付" width="300px" center>
      <div class="qr-code">
        <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="支付二维码" />
        <div v-else class="loading">正在生成二维码...</div>
      </div>
      <div class="pay-amount">支付金额：¥{{ (points * 0.01).toFixed(2) }}</div>
      <template #footer>
        <el-button @click="handleCancelPay">取消</el-button>
        <el-button type="primary" @click="handleCheckPayment">我已支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTeamInfo } from '@/api/team'
import { createPaymentOrder } from '@/api/payment'
import type { TeamMemberVO } from '@/types'

const teamInfo = ref<TeamMemberVO | null>(null)
const points = ref(100)
const loading = ref(false)
const showQrDialog = ref(false)
const qrCodeUrl = ref('')
const currentOrderId = ref<number | null>(null)

async function fetchTeamInfo() {
  try {
    const res = await getTeamInfo()
    if (res.code === 200) {
      teamInfo.value = res.data
    }
  } catch (error) {
    console.error('获取团队信息失败:', error)
  }
}

function handlePointsChange() {}

async function handlePay() {
  loading.value = true
  try {
    const res = await createPaymentOrder({ points: points.value })
    if (res.code === 200) {
      currentOrderId.value = res.data.id
      qrCodeUrl.value = res.data.qrCodeBase64 || ''
      showQrDialog.value = true
    }
  } catch (error) {
    console.error('创建订单失败:', error)
  } finally {
    loading.value = false
  }
}

function handleCancelPay() {
  showQrDialog.value = false
  qrCodeUrl.value = ''
  currentOrderId.value = null
}

async function handleCheckPayment() {
  ElMessage.info('支付查询功能开发中')
  showQrDialog.value = false
}

onMounted(() => {
  fetchTeamInfo()
})
</script>

<style scoped lang="scss">
.payment-view {
  .balance-info {
    font-size: 16px;

    .label {
      color: var(--color-text-secondary);
    }

    .value {
      font-size: 24px;
      font-weight: 600;
      color: #e6a23c;
    }
  }

  .amount {
    font-size: 24px;
    font-weight: 600;
    color: #f56c6c;
  }

  .info-card {
    .info-list {
      list-style: none;
      padding: 0;
      margin: 0;
      font-size: 13px;
      color: var(--color-text-secondary);
      line-height: 2;
    }
  }

  .qr-code {
    text-align: center;
    padding: 20px;

    img {
      max-width: 200px;
    }

    .loading {
      padding: 40px;
      color: var(--color-text-secondary);
    }
  }

  .pay-amount {
    text-align: center;
    font-size: 16px;
    font-weight: 500;
    color: #f56c6c;
    margin-top: 12px;
  }
}
</style>
