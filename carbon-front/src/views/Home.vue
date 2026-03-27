<template>
  <div class="home-wrapper">
    <NavBar :nickname="user.nickname" @update:nickname="updateNickname" />

    <!-- 头部区域 -->
    <header class="hero-section">
      <div class="hero-content">
        <span class="hero-badge">ECO FRIENDLY LIFE</span>
        <h1 class="hero-title">记录每一次<br><span class="highlight">低碳呼吸</span></h1>
        <p class="hero-desc">用数据量化环保行为，让绿色生活触手可及。</p>
        <div class="hero-actions custom-btn-group">
          <el-button type="primary" class="btn-primary" @click="$router.push('/dataInput')">
            开始记录 <i class="el-icon-arrow-right"></i>
          </el-button>
          <el-button class="btn-secondary" @click="$router.push('/carbonAnalysis')">
            查看分析
          </el-button>
        </div>
      </div>

      <!-- 右侧装饰图/数据展示 -->
      <div class="hero-visual">
        <div class="floating-card card-1">
          <i class="el-icon-s-data"></i>
          <div>
            <div class="num-wrapper">
              <span class="num">{{ totalEmission }}</span>
              <span class="unit">kg</span>
            </div>
            <div class="label">近30天累计碳排放</div>
          </div>
        </div>
        <div class="floating-card card-2">
          <i class="el-icon-medal"></i>
          <div>
            <div class="num">{{ user.integral }}</div>
            <div class="label">当前积分</div>
          </div>
        </div>
      </div>
    </header>
    <EcoMascot />
  </div>
</template>

<script>
import NavBar from '../components/NavBar.vue'
import EcoMascot from '../components/EcoMascot.vue'
export default {
  name: 'Home',
  components: { NavBar, EcoMascot },
  data() {
    return {
      user: { nickname: 'Guest', integral: 0, id: null },
      totalEmission: 0
    }
  },
  mounted() {
    this.getUserInfo()
    this.getCarbonData()

    this.$root.$on('refreshIntegral', this.getUserInfo)
    this.$root.$on('refreshEmission', this.getCarbonData)
  },
  beforeDestroy() {
    this.$root.$off('refreshIntegral', this.getUserInfo)
    this.$root.$off('refreshEmission', this.getCarbonData)
  },
  methods: {
    formatDate(date) {
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      return `${y}-${m}-${d}`
    },
    updateNickname(newNickname) {
      // 从 localStorage 获取最新的用户信息
      const userStr = localStorage.getItem('user')
      if (userStr) {
        this.user = JSON.parse(userStr)
      } else {
        this.user.nickname = newNickname
      }
      // 刷新碳排放数据
      this.getCarbonData()
    },
    async getUserInfo() {
      try {
        const userStr = localStorage.getItem('user')
        if (!userStr) {
          this.$router.push('/login')
          return
        }
        const localUser = JSON.parse(userStr)
        const res = await this.$http.get('/user/info', {
          params: { userId: localUser.id }
        })
        if (res.code === 200) {
          this.user = res.data
          localStorage.setItem('user', JSON.stringify(res.data))
        } else {
          this.user = localUser
        }
      } catch (error) {
        console.error('获取用户信息失败：', error)
        const localUser = JSON.parse(localStorage.getItem('user')) || { nickname: 'Guest', integral: 0, id: null }
        this.user = localUser
      } finally {
        // 获取完用户信息后获取碳排放数据
        if (this.user.id) this.getCarbonData()
      }
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
        this.totalEmission = carbonList.reduce((sum, item) => sum + (item.carbonEmission || 0), 0).toFixed(1)
      } catch (error) {
        console.error('获取碳排放数据失败：', error)
        this.totalEmission = 0
      }
    }
  }
}
</script>

<style scoped>
:root {
  --primary-green: #2d5a27;
  --text-dark: #1a1a1a;
  --text-gray: #666;
}
.home-wrapper {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-image: url('~@/assets/Home.jpg');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.hero-section {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 0 0 20px; 
  padding: 140px 20px 80px 0;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  flex-wrap: wrap;
}

.hero-section::-webkit-scrollbar-track {
  background: rgba(255,255,255,0.1);
  border-radius: 10px;
}
.hero-section::-webkit-scrollbar-thumb {
  background: rgba(45, 90, 39, 0.4);
  border-radius: 10px;
}
.hero-section::-webkit-scrollbar-thumb:hover {
  background: rgba(45, 90, 39, 0.6);
}

.hero-content {
  flex: 1;
  min-width: 300px;
  text-align: left;
  margin-left: 177px;
  padding-left: 0;
}

.num-wrapper {
  display: flex;
  align-items: flex-start;
  justify-content: center; 
  line-height: 1;
}

.num {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
}

.unit {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  margin-left: 8px;
  margin-top: 8px;
}

.label {
  font-size: 12px;
  color: #333;
  margin-top: 6px;
}

.hero-badge {
  display: inline-block;
  padding: 6px 12px;
  background: #2d5a27;
  color: #fff;
  font-size: 12px;
  letter-spacing: 2px;
  border-radius: 4px;
  margin-bottom: 20px;
  font-weight: 600;
}

.hero-title {
  font-size: 56px;
  line-height: 1.1;
  color: #1a1a1a;
  margin: 0 0 20px 0;
  font-weight: 800;
}

.highlight {
  color: #2d5a27;
  position: relative;
}

.hero-desc {
  font-size: 18px;
  color: #555;
  margin-bottom: 40px;
  line-height: 1.6;
  max-width: 500px;
}

.hero-actions.custom-btn-group { 
  display: flex; 
  gap: 15px; 
  justify-content: flex-start;
  margin-left: -17px; 
}

.btn-primary {
  background: #1a1a1a;
  border: none;
  padding: 15px 35px;
  font-size: 16px;
  border-radius: 8px;
  transition: transform 0.2s;
}
.btn-primary:hover { transform: translateY(-2px); background: #333; }
.btn-secondary {
  background: rgba(255,255,255,0.5);
  border: 1px solid #ddd;
  padding: 15px 35px;
  font-size: 16px;
  border-radius: 8px;
  color: #333;
}
.btn-secondary:hover { background: #fff; }

.hero-visual {
  position: relative;
  width: 400px;
  height: 400px;
  display: none;
}
@media (min-width: 992px) { .hero-visual { display: block; } }

.floating-card {
  position: absolute;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  padding: 20px;
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0,0,0,0.08);
  display: flex;
  align-items: center;
  gap: 15px;
  animation: float 6s ease-in-out infinite;
}
.card-1 { top: 20%; right: 10%; animation-delay: 0s; }
.card-2 { bottom: 20%; left: 10%; animation-delay: 3s; }
.floating-card i { font-size: 32px; color: #2d5a27; }
.floating-card .num { font-size: 24px; font-weight: 700; color: #1a1a1a; }
.floating-card .label { font-size: 12px; color: #333; }

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

@media (max-width: 768px) {
  .hero-section {
    padding-top: 100px;
    margin-left: 10px;
  }
  .hero-title {
    font-size: 42px;
  }
  .hero-actions.custom-btn-group {
    flex-direction: column;
    gap: 10px;
    margin-left: 0;
  }
  .btn-primary, .btn-secondary {
    width: 100%;
    text-align: center;
  }
}

@media (max-width: 480px) {
  .hero-section {
    padding: 80px 16px 40px 0;
    margin-left: 10px;
  }
  .hero-title {
    font-size: 32px;
  }
  .hero-badge {
    font-size: 10px;
  }
}
</style>