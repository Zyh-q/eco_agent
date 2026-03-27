<template>
  <div class="carbon-input-page">
    <div class="fixed-header-wrapper">
      <NavBar :nickname="user.nickname" class="nav-bar-glass" @update:nickname="updateNickname" />
      <div class="page-header"></div>
    </div>

    <div class="scrollable-container">
      <div class="form-card-container">
        <el-card class="form-card" shadow="never">
          <el-form :model="form" label-width="90px" ref="formRef" :rules="formRules" class="carbon-form">
            <!-- 自由描述行为 -->
            <div class="form-section">
              <div class="section-header">
                <span class="section-number">01</span>
                <h4 class="section-title">请自由描述你的行为</h4>
              </div>
              <el-form-item prop="description" class="form-item" label-width="0">
                <el-input
                  type="textarea"
                  v-model="form.description"
                  :rows="3"
                  placeholder="例如：今天开车3公里去吃了一次火锅，或者：使用空调2小时"
                  class="custom-textarea"
                ></el-input>
              </el-form-item>
            </div>

            <!-- 图片快速录入 -->
            <div class="image-upload-section">
              <div class="section-header">
                <span class="section-number">02</span>
                <h4 class="section-title">或者图片快速录入</h4>
              </div>
              <div class="upload-container">
                <el-upload
                  class="avatar-uploader"
                  action="#"
                  :show-file-list="false"
                  :before-upload="beforeUpload"
                  :on-success="handleUploadSuccess"
                  :on-error="handleUploadError"
                  :disabled="uploading"
                >
                  <img v-if="imageUrl" :src="imageUrl" class="uploaded-image" />
                  <div v-else class="upload-placeholder">
                    <i class="el-icon-plus avatar-uploader-icon"></i>
                    <p>点击上传图片</p>
                  </div>
                </el-upload>
                <el-button
                  v-if="imageUrl && !uploading"
                  type="primary"
                  @click="parseImage"
                  :loading="parsing"
                  class="parse-btn"
                >
                  <i class="el-icon-s-data"></i> 解析图片
                </el-button>
              </div>

              <!-- 图片解析结果卡片（可编辑） -->
              <div v-if="parseResult" class="parse-result-card image-result-card">
                <el-checkbox v-model="selectedImage" class="item-checkbox">
                  <div class="item-content">
                    <!-- 置信度警告 -->
                    <el-alert
                      v-if="parseResult.confidence && parseResult.confidence < 0.7"
                      title="识别可信度较低"
                      type="warning"
                      :description="`识别置信度为 ${(parseResult.confidence * 100).toFixed(0)}%，请仔细核对下方信息`"
                      show-icon
                      closable
                      style="margin-bottom: 12px"
                    ></el-alert>
                    <el-form label-width="80px" size="small">
                      <el-form-item label="行为类型">
                        <el-select v-model="parseResult.type" placeholder="请选择类型">
                          <el-option label="交通出行" value="交通出行"></el-option>
                          <el-option label="能源消耗" value="能源消耗"></el-option>
                          <el-option label="饮食消费" value="饮食消费"></el-option>
                          <el-option label="购物消费" value="购物消费"></el-option>
                          <el-option label="垃圾处理" value="垃圾处理"></el-option>
                          <el-option label="其他" value="其他"></el-option>
                        </el-select>
                      </el-form-item>
                      <el-form-item label="行为详情">
                        <el-input v-model="parseResult.detail" placeholder="请输入行为详情"></el-input>
                      </el-form-item>
                      <el-form-item label="数量">
                        <el-input-number v-model="parseResult.amount" :min="0" :precision="2" step="0.5"></el-input-number>
                      </el-form-item>
                      <el-form-item label="单位">
                        <el-select v-model="parseResult.unit" placeholder="请选择单位">
                          <el-option label="公里" value="公里"></el-option>
                          <el-option label="小时" value="小时"></el-option>
                          <el-option label="度" value="度"></el-option>
                          <el-option label="杯" value="杯"></el-option>
                          <el-option label="份" value="份"></el-option>
                          <el-option label="件" value="件"></el-option>
                          <el-option label="次" value="次"></el-option>
                        </el-select>
                      </el-form-item>
                    </el-form>
                    <div class="preview-tip">
                      <i class="el-icon-info"></i> 编辑后提交将重新计算碳排放和积分
                    </div>
                  </div>
                </el-checkbox>
              </div>
            </div>

            <!-- 文本解析结果列表（可编辑） -->
            <div v-if="textParseItems && textParseItems.length > 0" class="multi-parse-result">
              <el-divider content-position="left">解析出的多个行为（可编辑）</el-divider>
              <el-checkbox-group v-model="selectedItems">
                <el-card v-for="(item, idx) in textParseItems" :key="idx" class="parse-item-card" shadow="hover">
                  <el-checkbox :label="idx" class="item-checkbox">
                    <div class="item-content">
                      <!-- 置信度警告 -->
                      <el-alert
                        v-if="item.parseResult.confidence && item.parseResult.confidence < 0.7"
                        title="识别可信度较低"
                        type="warning"
                        :description="`识别置信度为 ${(item.parseResult.confidence * 100).toFixed(0)}%，请仔细核对`"
                        show-icon
                        closable
                        style="margin-bottom: 12px"
                      ></el-alert>
                      <el-form label-width="80px" size="small">
                        <el-form-item label="行为类型">
                          <el-select v-model="item.parseResult.type" placeholder="请选择类型">
                            <el-option label="交通出行" value="交通出行"></el-option>
                            <el-option label="能源消耗" value="能源消耗"></el-option>
                            <el-option label="饮食消费" value="饮食消费"></el-option>
                            <el-option label="购物消费" value="购物消费"></el-option>
                            <el-option label="垃圾处理" value="垃圾处理"></el-option>
                            <el-option label="其他" value="其他"></el-option>
                          </el-select>
                        </el-form-item>
                        <el-form-item label="行为详情">
                          <el-input v-model="item.parseResult.detail" placeholder="请输入行为详情"></el-input>
                        </el-form-item>
                        <el-form-item label="数量">
                          <el-input-number v-model="item.parseResult.amount" :min="0" :precision="2" step="0.5"></el-input-number>
                        </el-form-item>
                        <el-form-item label="单位">
                          <el-select v-model="item.parseResult.unit" placeholder="请选择单位">
                            <el-option label="公里" value="公里"></el-option>
                            <el-option label="小时" value="小时"></el-option>
                            <el-option label="度" value="度"></el-option>
                            <el-option label="杯" value="杯"></el-option>
                            <el-option label="份" value="份"></el-option>
                            <el-option label="件" value="件"></el-option>
                            <el-option label="次" value="次"></el-option>
                          </el-select>
                        </el-form-item>
                      </el-form>
                    </div>
                  </el-checkbox>
                </el-card>
              </el-checkbox-group>
              <div class="multi-summary">
                <p>已选中行为总碳排放：{{ selectedTotalCarbon }} kg，总积分：{{ selectedTotalIntegral }}</p>
                <p class="tip">提示：编辑后碳排放和积分将在提交时重新计算，当前显示为原始估算值</p>
              </div>
            </div>

            <!-- 记录日期 -->
            <div class="form-section">
              <div class="section-header">
                <h4 class="section-title">记录日期</h4>
              </div>
              <el-form-item prop="recordTime" class="form-item" label-width="0" style="width: 100%;">
                <el-date-picker
                  v-model="form.recordTime"
                  type="date"
                  placeholder="选择日期"
                  value-format="yyyy-MM-dd"
                  class="custom-date"
                  :default-value="new Date()"
                ></el-date-picker>
              </el-form-item>
            </div>

            <!-- 操作按钮 -->
            <div class="form-actions">
              <el-button
                type="info"
                @click="parseText"
                :loading="textParsing"
                class="action-btn parse-text-btn"
                :disabled="!form.description"
              >
                <i class="el-icon-document"></i> 解析文本
              </el-button>
              <el-button type="primary" @click="submitForm" class="action-btn submit-btn">
                <i class="el-icon-check"></i> 提交记录
              </el-button>
              <el-button @click="resetForm" class="action-btn reset-btn">
                <i class="el-icon-refresh-left"></i> 重置
              </el-button>
              <el-button @click="$router.push('/carbonAnalysis')" class="action-btn back-btn">
                返回分析
              </el-button>
            </div>
          </el-form>
        </el-card>
      </div>
    </div>
    <EcoMascot speechText="记录每一次低碳行为，让减碳更有迹可循～" />
  </div>
</template>

<script>
import EcoMascot from '../components/EcoMascot.vue'
import NavBar from '../components/NavBar.vue'

export default {
  name: 'DataInputPage',
  components: { EcoMascot, NavBar },
  data() {
    return {
      user: JSON.parse(localStorage.getItem('user')) || {},
      form: {
        userId: 0,
        description: '',
        recordTime: new Date().toISOString().split('T')[0]
      },
      formRules: {
        description: [
          { required: true, message: '请描述你的行为', trigger: 'blur' },
          { min: 3, message: '描述不能少于3个字符', trigger: 'blur' }
        ],
        recordTime: [{ required: true, message: '请选择记录日期', trigger: 'change' }]
      },
      // 图片相关
      imageUrl: '',
      uploading: false,
      parsing: false,
      parseResult: null,
      file: null,
      selectedImage: true,
      // 文本解析相关
      textParsing: false,
      textParseItems: [],
      selectedItems: [],
      totalCarbon: 0,
      totalIntegral: 0,
      warmChat: null,
      carbonAmount: '',
      imageIntegral: 0,
      // 地理位置
      latitude: null,
      longitude: null,
      locationError: null
    }
  },
  computed: {
    selectedTotalCarbon() {
      let total = 0
      if (this.parseResult && this.selectedImage) {
        total += this.carbonAmount || 0
      }
      if (this.textParseItems.length && this.selectedItems.length) {
        this.selectedItems.forEach(idx => {
          total += this.textParseItems[idx]?.carbonAmount || 0
        })
      }
      return total.toFixed(2)
    },
    selectedTotalIntegral() {
      let total = 0
      if (this.parseResult && this.selectedImage) {
        total += this.imageIntegral || 0
      }
      if (this.textParseItems.length && this.selectedItems.length) {
        this.selectedItems.forEach(idx => {
          total += this.textParseItems[idx]?.integral || 0
        })
      }
      return total
    }
  },
  mounted() {
    this.checkLoginStatus()
    this.getLocation()
  },
  methods: {
    checkLoginStatus() {
      const userStr = localStorage.getItem('user')
      if (!userStr) {
        this.$message.warning('请先登录后再录入碳足迹数据！')
        this.$router.push('/login')
        return
      }
      const user = JSON.parse(userStr)
      if (!user.id) {
        this.$message.warning('登录信息失效，请重新登录！')
        this.$router.push('/login')
        return
      }
      this.form.userId = user.id
      this.user = user
    },

    getLocation() {
      if (!navigator.geolocation) {
        this.locationError = '浏览器不支持地理定位'
        return
      }
      navigator.geolocation.getCurrentPosition(
        (position) => {
          this.latitude = position.coords.latitude
          this.longitude = position.coords.longitude
        },
        (error) => {
          console.warn('定位失败：', error.message)
        },
        { timeout: 5000 }
      )
    },

    updateNickname(newNickname) {
      const updatedUser = JSON.parse(localStorage.getItem('user'))
      if (updatedUser) {
        this.user = updatedUser
      } else {
        this.user.nickname = newNickname
      }
      this.$root.$emit('userInfoUpdated', this.user)
    },

    saveWarmChatToHistory(warmChat) {
      if (!warmChat || !this.form.userId) return
      const aiMsg = {
        userId: this.form.userId,
        role: 'ai',
        content: warmChat.content,
        suggestion: warmChat.suggestion || '',
        carbonReduce: warmChat.carbonAmount || 0,
        time: new Date().getTime()
      }
      let allChat = JSON.parse(localStorage.getItem('chatHistory') || '[]')
      allChat = allChat.filter(item => item.userId === this.form.userId)
      allChat.push(aiMsg)
      if (allChat.length > 20) allChat.shift()
      localStorage.setItem('chatHistory', JSON.stringify(allChat))
      this.$root.$emit('refreshSuggestion')
    },

    async parseText() {
      if (!this.form.description) {
        this.$message.warning('请输入行为描述')
        return
      }
      this.textParsing = true
      try {
        const res = await this.$http.post('/carbon/parseText', {
          userId: this.form.userId,
          description: this.form.description,
          recordTime: this.form.recordTime
        })
        if (res.code === 200) {
          this.textParseItems = res.data.items
          this.totalCarbon = res.data.totalCarbon
          this.totalIntegral = res.data.totalIntegral
          this.warmChat = res.data.warmChat
          this.selectedItems = this.textParseItems.map((_, index) => index)
          this.$message.success('文本解析成功')
        }
      } catch (error) {
        console.error(error)
        this.$message.error('文本解析失败')
      } finally {
        this.textParsing = false
      }
    },

    async parseImage() {
      if (!this.file) {
        this.$message.warning('请先上传图片！')
        return
      }
      this.parsing = true
      try {
        const formData = new FormData()
        formData.append('file', this.file)
        formData.append('userId', this.form.userId)

        const res = await this.$http.post('/image/upload', formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        })
        if (res.code === 200) {
          this.parseResult = res.data.parseResult
          this.carbonAmount = res.data.carbonAmount
          this.imageIntegral = res.data.integral
          this.warmChat = res.data.warmChat
          this.selectedImage = true
          this.$message.success('图片解析成功')
        }
      } catch (error) {
        console.error(error)
        this.$message.error('解析图片失败')
      } finally {
        this.parsing = false
      }
    },

    async submitForm() {
      const items = []

      // 添加图片结果
      if (this.parseResult && this.selectedImage) {
        items.push({
          type: this.parseResult.type,
          detail: this.parseResult.detail,
          amount: this.parseResult.amount,
          unit: this.parseResult.unit,
          confidence: this.parseResult.confidence,
          emissionFactor: this.parseResult.emissionFactor,
          powerKw: this.parseResult.powerKw
        })
      }

      // 添加选中的文本结果
      if (this.textParseItems.length > 0 && this.selectedItems.length > 0) {
        this.selectedItems.forEach(index => {
          const item = this.textParseItems[index].parseResult
          items.push({
            type: item.type,
            detail: item.detail,
            amount: item.amount,
            unit: item.unit,
            confidence: item.confidence,
            emissionFactor: item.emissionFactor,
            powerKw: item.powerKw
          })
        })
      }

      if (items.length === 0) {
        this.$message.warning('请至少选择一个行为')
        return
      }

      const submitData = {
        userId: this.form.userId,
        recordTime: this.form.recordTime,
        items: items,
        latitude: this.latitude,
        longitude: this.longitude
      }

      try {
        const res = await this.$http.post('/carbon/confirmBatch', submitData)
        if (res.code === 200) {
          this.$message.success(res.msg)
          this.saveWarmChatToHistory(res.data.warmChat)
          const userRes = await this.$http.get('/user/info', { params: { userId: this.form.userId } })
          if (userRes.code === 200) {
            localStorage.setItem('user', JSON.stringify(userRes.data))
            this.user = userRes.data
          }
          this.resetForm()
          setTimeout(() => this.$router.push('/carbonAnalysis'), 1000)
        } else {
          this.$message.error(res.msg || '提交失败')
        }
      } catch (error) {
        console.error('提交失败', error)
        this.$message.error('提交失败，请重试')
      }
    },

    resetForm() {
      this.$refs.formRef.resetFields()
      this.form = {
        userId: this.form.userId,
        description: '',
        recordTime: new Date().toISOString().split('T')[0]
      }
      this.imageUrl = ''
      this.parseResult = null
      this.file = null
      this.textParseItems = []
      this.selectedItems = []
      this.totalCarbon = 0
      this.totalIntegral = 0
      this.carbonAmount = ''
      this.imageIntegral = 0
      this.selectedImage = true
    },

    beforeUpload(file) {
      const isImage = file.type.startsWith('image/')
      if (!isImage) {
        this.$message.error('只能上传图片！')
        return false
      }
      if (file.size / 1024 / 1024 > 10) {
        this.$message.error('图片不能超过10MB')
        return false
      }
      this.uploading = true
      this.file = file
      const reader = new FileReader()
      reader.onload = (e) => {
        this.imageUrl = e.target.result
        this.uploading = false
      }
      reader.readAsDataURL(file)
      return false
    },

    handleUploadSuccess() {
      this.uploading = false
    },
    handleUploadError() {
      this.uploading = false
      this.$message.error('图片上传失败！')
    }
  }
}
</script>
<style scoped>
.multi-parse-result {
  margin-top: 20px;
}
.parse-item-card {
  margin-bottom: 15px;
  padding: 10px;
}
.item-checkbox {
  display: flex;
  align-items: flex-start;
  width: 100%;
}
.item-content {
  margin-left: 10px;
  flex: 1;
}
.item-content p {
  margin: 5px 0;
}
.multi-summary {
  margin-top: 10px;
  font-weight: bold;
  text-align: right;
}
.carbon-input-page {
  --primary-color: #10b981;
  --primary-hover: #059669;
  --primary-light: #d1fae5;
  --text-main: #1f2937;
  --text-sub: #1f2022;
  --border-color: #e5e7eb;
  --bg-input: #f9fafb;
  --shadow-card: 0 10px 30px -5px rgba(0, 0, 0, 0.1);
  --radius-lg: 16px;
  --radius-md: 8px;
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  flex-direction: column;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  background: linear-gradient(135deg, #f0fdf4 0%, #d1fae5 30%, #ecfdf5 60%, #f0fdf4 100%);
  background-image: 
    linear-gradient(135deg, #f0fdf4 0%, #d1fae5 30%, #ecfdf5 60%, #f0fdf4 100%),
    repeating-linear-gradient(45deg, rgba(16, 185, 129, 0.03) 0px, rgba(16, 185, 129, 0.03) 2px, transparent 2px, transparent 10px);
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  padding: 0;
  box-sizing: border-box;
  color: var(--text-main);
}
.fixed-header-wrapper {
  flex-shrink: 0;
  padding: 0 20px;
  padding-top: 80px;
  box-sizing: border-box;
}
.page-header {
  text-align: center;
  margin: 15px 0 20px 0;
  animation: fadeInDown 0.5s ease-out;
}

.scrollable-container {
  flex: 1;
  padding: 0 20px 20px 20px;
  overflow-y: auto;
}
.form-card-container {
  width: 100%;
  max-width: 1000px;
  margin: 0 auto;
  height: auto;
  padding-right: 5px;
  animation: fadeInUp 0.5s ease-out 0.1s backwards;
}
.form-card {
  border: none;
  border-radius: var(--radius-lg);
  background: rgba(255, 255, 255, 0.9) !important;
  backdrop-filter: blur(8px);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.carbon-form {
  padding: 30px 40px;
}
.form-section {
  margin-bottom: 30px;
}
.section-header {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-color);
}
.section-number {
  font-size: 12px;
  font-weight: 700;
  color: var(--primary-color);
  background: var(--primary-light);
  padding: 2px 8px;
  border-radius: 4px;
  margin-right: 10px;
}
.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0;
}
.form-row {
  display: block !important;
  width: 100%;
}
.form-item {
  margin-bottom: 0 !important;
  width: 100% !important;
}
::v-deep .el-form-item__label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-main);
  line-height: 36px;
  padding-right: 4px !important;
}
::v-deep .custom-input .el-input__inner,
::v-deep .custom-select .el-input__inner {
  height: 36px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background-color: var(--bg-input);
  font-size: 13px;
  color: var(--text-main);
  transition: all 0.3s ease;
  padding: 0 12px;
}
::v-deep .custom-date,
::v-deep .custom-textarea {
  width: 100% !important;
}

::v-deep .el-form-item__content {
  margin-left: 0 !important;
  padding-left: 0 !important;
  width: 100% !important;
}

.image-upload-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px dashed var(--border-color);
}
.upload-container {
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
  padding: 20px;
  background: var(--bg-input);
  border-radius: var(--radius-md);
  margin-top: 10px;
}
.avatar-uploader {
  width: 120px;
  height: 120px;
  border: 1px dashed var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  background: #fff;
}
.avatar-uploader:hover {
  border-color: var(--primary-color);
}
.uploaded-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.upload-placeholder {
  display: flex;
  flex-direction:column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-sub);
}
.parse-btn {
  height: 36px;
  padding: 0 20px;
  border-radius: var(--radius-md);
}
.parse-result-card {
  margin-top: 15px;
}
.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px dashed var(--border-color);
}
.action-btn {
  height: 38px;
  padding: 0 24px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 600;
  transition: all 0.3s;
}
.submit-btn {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-hover));
  box-shadow: 0 2px 8px rgba(16, 185, 129, 0.25);
}
@keyframes fadeInDown {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(10px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>