<template>
  <div class="integral-page">
    <!-- 背景装饰层 -->
    <div class="ambient-light light-1"></div>
    <div class="ambient-light light-2"></div>
    <div class="noise-overlay"></div>
    <NavBar :nickname="user.nickname" class="nav-bar-glass" @update:nickname="updateNickname" />

    <div class="integral-container">
      <!-- 头部：问候与标题 -->
      <header class="page-header">
        <div class="header-content">
          <h1 class="page-title">Green Sprout</h1>
        </div>
      </header>

      <!-- 核心区域：积分资产卡片 -->
      <div class="hero-section">
        <div class="points-card">
          <div class="card-glass-effect"></div>
          <div class="card-content">
            <div class="card-label">
              <span class="label-icon">🌱</span>
              <span>当前绿芽积分</span>
            </div>
            <div class="card-value-wrapper">
              <span class="score-num">{{ userIntegral }}</span>
            </div>
            <div class="card-footer">
              <div class="rule-badge">
                <span class="dot"></span> 实时累计
              </div>
              <div class="card-arrow">
                <i class="el-icon-arrow-right"></i>
              </div>
            </div>
          </div>
          <div class="card-glow"></div>
        </div>
        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-label">
              <span class="stat-icon">☁️</span>
              近30天累计碳排放
            </div>
            <div class="stat-value">{{ totalCarbonEmission }} <small>kg</small></div>
          </div>
          <div class="stat-item">
            <div class="stat-label">
              <div class="stat-icon-box">
              <i :class="todayIntegral >= 0 ? 'el-icon-top' : 'el-icon-bottom'"></i>
            </div>
              今日积分变动
            </div>
            <div
              class="stat-value"
              :class="{
                'highlight positive': todayIntegral > 0,
                'highlight negative': todayIntegral < 0
              }"
            >
              {{ todayIntegral > 0 ? '+' : '' }}{{ todayIntegral }}
            </div>
          </div>
        </div>
      </div>

      <!-- 记录区域 -->
      <section class="record-section">
        <div class="section-header">
          <h3>变动明细</h3>
          <div class="filter-tags">
            <span class="tag active">全部</span>
          </div>
        </div>

        <div class="table-wrapper">
          <div v-if="integralList.length === 0" class="empty-state">
            <div class="empty-icon">🍃</div>
            <p>暂无积分记录</p>
            <span class="empty-tip">低碳行为将自动生成记录</span>
          </div>

          <el-table
            v-else
            :data="integralList"
            class="modern-table"
            :header-cell-style="{
              background: 'transparent',
              color: '#9ca3af',
              fontWeight: '500',
              fontSize: '13px'
            }"
            :row-style="{ background: 'transparent' }"
          >
            <el-table-column prop="createTime" label="时间" width="180">
              <template slot-scope="scope">
                <div class="time-cell">
                  <span class="date">{{ formatTime(scope.row.createTime).split(' ')[0] }}</span>
                  <span class="time">{{ formatTime(scope.row.createTime).split(' ')[1] }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="reason" label="原因" min-width="200">
              <template slot-scope="scope">
                <div class="reason-cell">
                  <div
                    class="reason-icon"
                    :class="scope.row.integral > 0 ? 'icon-income' : 'icon-expense'"
                  >
                    <i
                      :class="
                        scope.row.integral > 0 ? 'el-icon-circle-plus' : 'el-icon-remove'
                      "
                    ></i>
                  </div>
                  <span class="reason-text">{{ scope.row.reason }}</span>
                </div>
              </template>
            </el-table-column>

            <el-table-column prop="integral" label="积分变动" width="120" align="right">
              <template slot-scope="scope">
                <span
                  class="amount-text"
                  :class="scope.row.integral > 0 ? 'text-income' : 'text-expense'"
                >
                  {{ scope.row.integral > 0 ? '+' : '' }}{{ scope.row.integral }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </section>
    </div>
    <EcoMascot speechText="绿色生活，让世界拥有更多颜色。" />
  </div>
</template>

<script>
import NavBar from '../components/NavBar.vue'
import EcoMascot from '../components/EcoMascot.vue'

export default {
  name: 'Integral',
  components: { NavBar,EcoMascot },
  data() {
    return {
      user: JSON.parse(localStorage.getItem('user')) || {},
      integralList: [],
      userIntegral: 0,
      totalCarbonEmission: 0,
      todayIntegral: 0
    }
  },
  mounted() {
    this.getUserLatestInfo()
    this.getIntegralData()
    this.getCarbonData()
  },
  methods: {
    formatTime(timeStr) {
      if (!timeStr) return ''
      const date = new Date(timeStr)
      return `${date.getFullYear()}-${(date.getMonth() + 1)
        .toString()
        .padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')} ${date
        .getHours()
        .toString()
        .padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
    },

    formatDate(date) {
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      return `${y}-${m}-${d}`
    },

    async getCarbonData() {
      if (!this.user.id) return

      try {
        const endDate = new Date()
        const startDate = new Date(endDate.getTime() - 30 * 24 * 60 * 60 * 1000)

        const resList = await this.$http.get('/carbon/list', {
          params: {
            userId: this.user.id,
            startDate: this.formatDate(startDate),
            endDate: this.formatDate(endDate)
          }
        })
        const carbonList = resList.data || []
        this.totalCarbonEmission = carbonList
          .reduce((sum, item) => sum + (item.carbonEmission || 0), 0)
          .toFixed(1)
      } catch (error) {
        console.error('获取碳排放数据失败：', error)
        this.totalCarbonEmission = 0
      }
    },
    updateNickname(newNickname) {
      // 从 localStorage 重新获取最新的用户信息（NavBar 已更新）
      const updatedUser = JSON.parse(localStorage.getItem('user'));
      if (updatedUser) {
        this.user = updatedUser;
      } else {
        this.user.nickname = newNickname;
      }
      this.$root.$emit('userInfoUpdated', this.user);
    },

    calculateTodayIntegral() {
      const today = new Date().toDateString()
      this.todayIntegral = this.integralList
        .filter((item) => {
          const itemDate = new Date(item.createTime).toDateString()
          return itemDate === today
        })
        .reduce((sum, item) => sum + item.integral, 0)
    },

    async getUserLatestInfo() {
      try {
        if (!this.user.id) return

        const res = await this.$http.get('/user/info', {
          params: { userId: this.user.id }
        })
        if (res.code === 200) {
          this.userIntegral = res.data.integral || 0
          localStorage.setItem('user', JSON.stringify(res.data))
          this.user = res.data
        }
      } catch (error) {
        console.log('获取用户最新积分失败：', error)
        this.userIntegral = this.user.integral || 0
      }
    },

    async getIntegralData() {
      try {
        if (!this.user.id) return

        const res = await this.$http.get('/integral/list', {
          params: { userId: this.user.id }
        })
        this.integralList = res.data || []
        this.calculateTodayIntegral()
      } catch (error) {
        console.log('获取积分记录失败：', error)
        this.todayIntegral = 0
      }
    }
  }
}
</script>

<style scoped>
:root {
  --primary-color: #2d6a4f;
  --accent-color: #d8f3dc;
  --text-main: #1b4332;
  --text-sub: #6c757d;
  --bg-glass: rgba(255, 255, 255, 0.65);
  --border-glass: rgba(255, 255, 255, 0.4);
  --shadow-soft: 0 8px 32px 0 rgba(31, 38, 135, 0.07);
  --positive-color: #d97706;
  --negative-color: #dc2626;
}

.integral-page {
  min-height: 100vh;
  background-color: transparent;
  font-family: 'PingFang SC', 'Helvetica Neue', Helvetica, 'Microsoft YaHei', sans-serif;
  color: var(--text-main);
  position: relative;
  overflow-x: hidden;
  padding-bottom: 40px;
}

.ambient-light {
  position: fixed;
  border-radius: 50%;
  filter: blur(80px);
  z-index: 0;
  opacity: 0.6;
  animation: floatLight 10s infinite ease-in-out alternate;
}

.light-1 {
  width: 500px;
  height: 500px;
  background: #b7e4c7;
  top: -100px;
  right: -100px;
}

.light-2 {
  width: 400px;
  height: 400px;
  background: #d8f3dc;
  bottom: -50px;
  left: -100px;
  animation-delay: -5s;
}

.noise-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-image: url('~@/assets/integral.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  pointer-events: none;
  z-index: 1;
}

@keyframes floatLight {
  0% {
    transform: translate(0, 0);
  }
  100% {
    transform: translate(30px, 50px);
  }
}

.integral-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 20px 24px;
  position: relative;
  z-index: 2;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-top: 10px;
}

.page-title {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-main);
  margin: 0;
  letter-spacing: -0.5px;
}

.hero-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-bottom: 40px;
}

.points-card {
  position: relative;
  height: 220px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.4);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid var(--border-glass);
  box-shadow: var(--shadow-soft);
  overflow: hidden;
  display: flex;
  align-items: center;
  padding: 0 40px;
  transition: transform 0.3s ease;
}

.points-card:hover {
  transform: translateY(-5px);
}

.card-glass-effect {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(120deg, rgba(255, 255, 255, 0.8) 0%, rgba(255, 255, 255, 0.1) 100%);
  z-index: 1;
}

.card-glow {
  position: absolute;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(216, 243, 220, 0.6) 0%, transparent 70%);
  top: -100px;
  right: -100px;
  border-radius: 50%;
  z-index: 0;
}

.card-content {
  position: relative;
  z-index: 2;
  width: 100%;
}

.card-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--text-sub);
  font-weight: 500;
  margin-bottom: 15px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.label-icon {
  font-size: 16px;
}

.card-value-wrapper {
  display: flex;
  align-items: baseline;
  gap: 5px;
}

.score-num {
  font-size: 72px;
  font-weight: 800;
  color: transparent;
  line-height: 1;
  font-family: 'DIN Alternate', 'Roboto', sans-serif;
  letter-spacing: -2px;
  background: linear-gradient(135deg, #1b4332 0%, #2d6a4f 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.card-footer {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rule-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-sub);
  background: rgba(255, 255, 255, 0.5);
  padding: 4px 10px;
  border-radius: 20px;
}

.dot {
  width: 6px;
  height: 6px;
  background: #2d6a4f;
  border-radius: 50%;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    opacity: 1;
  }
  50% {
    opacity: 0.4;
  }
  100% {
    opacity: 1;
  }
}

.card-arrow {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: white;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--primary-color);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s;
}

.card-arrow:hover {
  transform: translateX(3px);
}

.stats-grid {
  display: grid;
  grid-template-rows: 1fr 1fr;
  gap: 20px;
}

.stat-item {
  background: white;
  border-radius: 20px;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.8);
}

.stat-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--text-sub);
  margin-bottom: 8px;
}

.stat-icon {
  font-size: 16px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-main);
}

.stat-value.highlight.positive {
  color: var(--positive-color);
}
.stat-value.highlight.negative {
  color: var(--negative-color);
}

.stat-value small {
  font-size: 14px;
  color: var(--text-sub);
  font-weight: 400;
}

.record-section {
  background: white;
  border-radius: 24px;
  padding: 30px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
}

.section-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: var(--text-main);
}

.filter-tags {
  display: flex;
  gap: 10px;
}

.tag {
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 20px;
  background: #f3f4f6;
  color: var(--text-sub);
  cursor: pointer;
  transition: all 0.2s;
}

.tag.active {
  background: var(--text-main);
  color: white;
}

.tag:hover:not(.active) {
  background: #e5e7eb;
}

.table-wrapper {
  width: 100%;
}

.modern-table {
  width: 100%;
  border: none !important;
}

:deep(.el-table) {
  background: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-header-bg-color: transparent;
  --el-table-text-color: var(--text-main);
  --el-table-header-text-color: var(--text-sub);
  --el-table-row-hover-bg-color: #f9fafb;
  --el-table-border-color: transparent;
}

:deep(.el-table th) {
  padding: 12px 0 !important;
  border-bottom: 1px solid #f3f4f6 !important;
}

:deep(.el-table td) {
  padding: 20px 0 !important;
  border-bottom: 1px solid #f9fafb !important;
  font-size: 14px;
}

:deep(.el-table__empty-block) {
  min-height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.time-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.4;
}

.time-cell .date {
  font-weight: 600;
  color: var(--text-main);
}

.time-cell .time {
  font-size: 12px;
  color: #9ca3af;
}

.reason-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.reason-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.icon-income {
  background: #ecfdf5;
  color: #059669;
}

.icon-expense {
  background: #fef2f2;
  color: #dc2626;
}

.reason-text {
  font-weight: 500;
  color: var(--text-main);
}

.amount-text {
  font-weight: 700;
  font-family: 'DIN Alternate', sans-serif;
  font-size: 15px;
}

.text-income {
  color: #059669;
}

.text-expense {
  color: #dc2626;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: var(--text-sub);
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 15px;
  opacity: 0.5;
}

.empty-tip {
  font-size: 13px;
  opacity: 0.7;
  margin-top: 5px;
  display: block;
}

@media (max-width: 768px) {
  .hero-section {
    grid-template-columns: 1fr;
  }

  .stats-grid {
    grid-template-columns: 1fr 1fr;
    grid-template-rows: auto;
  }

  .points-card {
    padding: 0 25px;
    height: 200px;
  }

  .score-num {
    font-size: 56px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }
}
</style>