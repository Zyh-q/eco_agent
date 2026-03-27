<template>
  <div class="analysis-wrapper">
    <NavBar :nickname="user.nickname" @update:nickname="updateNickname" />

    <main class="analysis-content">
      <header class="stats-header">
        <div class="header-text">
          <h1 class="page-title">碳足迹分析</h1>
          <p class="page-subtitle">数据洞察，让绿色生活更清晰</p>
        </div>
        <div class="header-stats">
          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(45, 90, 39, 0.1); color: #2d5a27;">
              <i class="el-icon-s-data"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ totalEmission }} <span class="unit">kg</span></div>
              <div class="stat-label">近30天累计碳排放</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon" style="background: rgba(76, 175, 80, 0.1); color: #4caf50;">
              <i class="el-icon-refresh"></i>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ treeEquivalent }} <span class="unit">棵</span></div>
              <div class="stat-desc">相当于 {{ treeEquivalent }} 棵树生长1年所吸收</div>
            </div>
          </div>
        </div>
      </header>

      <div class="charts-grid">
        <div class="chart-panel main-chart">
          <div class="panel-header">
            <h3>排放趋势</h3>
            <span class="time-range">近 30 天</span>
          </div>
          <div id="lineChart" class="chart-container"></div>
        </div>
        <div class="chart-panel side-chart">
          <div class="panel-header">
            <h3>类型占比</h3>
          </div>
          <div id="pieChart" class="chart-container"></div>
        </div>
      </div>

      <div class="table-panel">
        <div class="panel-header">
          <h3>详细记录</h3>
          <div class="query-toolbar">
            <el-date-picker
              v-model="queryDateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="yyyy-MM-dd"
              size="mini"
            >
            </el-date-picker>

            <el-select
              v-model="queryType"
              placeholder="选择类型"
              size="mini"
              clearable
            >
              <el-option label="全部类型" value=""></el-option>
              <el-option label="交通出行" value="交通出行"></el-option>
              <el-option label="能源消耗" value="能源消耗"></el-option>
              <el-option label="饮食消费" value="饮食消费"></el-option>
              <el-option label="购物消费" value="购物消费"></el-option>
              <el-option label="垃圾处理" value="垃圾处理"></el-option>
              <el-option label="其他" value="其他"></el-option>
            </el-select>

            <el-button type="primary" size="mini" @click="getCarbonData">查询</el-button>
            <el-button type="text" class="refresh-btn" @click="resetQuery">
              <i class="el-icon-refresh"></i> 刷新
            </el-button>
          </div>
        </div>

        <el-table 
          :data="carbonList" 
          style="width: 100%" 
          class="minimal-table"
          :show-header="true"
          @row-contextmenu="handleRowContextMenu"
          v-loading="loading"
        >
          <el-table-column prop="recordTime" label="日期" width="120"></el-table-column>
          <el-table-column prop="type" label="类型" width="140">
            <template slot-scope="scope">
              <span class="type-tag" :class="scope.row.type">
                {{ scope.row.type }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="detail" label="行为详情" min-width="200"></el-table-column>
          <el-table-column prop="amount" label="数量" width="100" align="right">
            <template slot-scope="scope">
              {{ scope.row.amount }} {{ scope.row.unit || '' }}
            </template>
          </el-table-column>
          <el-table-column prop="carbonEmission" label="碳排放 (kg)" width="150" align="right">
            <template slot-scope="scope">
              <span class="emission-text">{{ (scope.row.carbonEmission || 0).toFixed(2) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template slot-scope="scope">
              <el-button type="text" size="small" @click="handleEditRow(scope.row)">
                <i class="el-icon-edit-outline"></i>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <div v-if="carbonList.length === 0 && !loading" class="empty-state">
          <i class="el-icon-document-delete"></i>
          <p>暂无数据，先去记录一下吧</p>
          <el-button type="primary" @click="$router.push('/dataInput')">去录入</el-button>
        </div>
      </div>
    </main>

    <!-- 右键菜单 -->
    <div 
      v-show="contextMenuVisible"
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
      class="custom-context-menu"
    >
      <div class="menu-option" @click="handleEdit">
        <i class="el-icon-edit"></i> 编辑
      </div>
      <div class="menu-option danger" @click="handleDelete">
        <i class="el-icon-delete"></i> 删除
      </div>
    </div>

    <!-- 编辑弹窗 -->
    <el-dialog 
      title="编辑记录" 
      :visible.sync="editDialogVisible"
      width="450px"
      class="minimal-dialog"
      :close-on-click-modal="false"
    >
      <el-form 
        :model="editForm" 
        label-width="80px" 
        ref="editFormRef"
        :rules="editFormRules"
      >
        <el-form-item label="类型" prop="type">
          <el-select v-model="editForm.type" placeholder="选择类型" style="width: 100%;">
            <el-option label="交通出行" value="交通出行"></el-option>
            <el-option label="能源消耗" value="能源消耗"></el-option>
            <el-option label="饮食消费" value="饮食消费"></el-option>
            <el-option label="购物消费" value="购物消费"></el-option>
            <el-option label="垃圾处理" value="垃圾处理"></el-option>
            <el-option label="其他" value="其他"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="详情" prop="detail">
          <el-input 
            v-model="editForm.detail" 
            placeholder="输入详情"
            style="width: 100%;"
          ></el-input>
        </el-form-item>
        <el-form-item label="数量" prop="amount">
          <el-input-number v-model="editForm.amount" :min="0" :precision="2" style="width: 100%;"></el-input-number>
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="editForm.unit" disabled placeholder="单位自动匹配" style="width: 100%;"></el-input>
        </el-form-item>
        <el-form-item label="日期" prop="recordTime">
          <el-date-picker 
            v-model="editForm.recordTime" 
            type="date" 
            placeholder="选择日期" 
            value-format="yyyy-MM-dd"
            style="width: 100%;"
          ></el-date-picker>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEditForm">保存</el-button>
      </div>
    </el-dialog>
    <EcoMascot speechText="看看你的碳足迹分布，找到减碳突破口～" />
  </div>
</template>

<script>
import NavBar from '../components/NavBar.vue'
import * as echarts from 'echarts'
import EcoMascot from '../components/EcoMascot.vue'

export default {
  name: 'CarbonAnalysis',
  components: { NavBar,EcoMascot },
  data() {
    return {
      user: JSON.parse(localStorage.getItem('user')) || {},
      carbonList: [],
      totalEmission: 0,
      treeEquivalent: 0,
      pieChart: null,
      lineChart: null,
      loading: false,
      contextMenuVisible: false,
      contextMenuX: 0,
      contextMenuY: 0,
      currentRow: null,
      editDialogVisible: false,
      queryDateRange: [],
      queryType: '',
      editForm: { 
        id: '', 
        userId: '',
        type: '', 
        detail: '', 
        amount: 0, 
        unit: '',
        recordTime: '' 
      },
      editFormRules: {
        type: [{ required: true, message: '请选择类型', trigger: 'change' }],
        detail: [{ required: true, message: '请输入详情', trigger: 'blur' }],
        amount: [{ required: true, message: '请输入数量', trigger: 'blur' }],
        recordTime: [{ required: true, message: '请选择日期', trigger: 'change' }]
      }
    }
  },
  mounted() {
    this.$nextTick(() => {
      this.initCharts()
      this.getCarbonData()
    })
    window.addEventListener('resize', this.resizeCharts)
    document.addEventListener('click', this.hideContextMenu)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts)
    document.removeEventListener('click', this.hideContextMenu)
    if (this.pieChart) this.pieChart.dispose()
    if (this.lineChart) this.lineChart.dispose()
  },
  methods: {
    initCharts() {
      this.pieChart = echarts.init(document.getElementById('pieChart'))
      this.lineChart = echarts.init(document.getElementById('lineChart'))
    },
    resizeCharts() {
      if (this.pieChart) this.pieChart.resize()
      if (this.lineChart) this.lineChart.resize()
    },
    formatDate(date) {
      const y = date.getFullYear()
      const m = String(date.getMonth() + 1).padStart(2, '0')
      const d = String(date.getDate()).padStart(2, '0')
      return `${y}-${m}-${d}`
    },
    async getCarbonData() {
      if (!this.user.id) {
        this.$message.warning('请先登录！')
        this.$router.push('/login')
        return
      }
      
      this.loading = true
      try {
        let startDate = ''
        let endDate = ''
        if (this.queryDateRange && this.queryDateRange.length === 2) {
          startDate = this.queryDateRange[0]
          endDate = this.queryDateRange[1]
        }

        const resList = await this.$http.get('/carbon/list', {
          params: {
            userId: this.user.id,
            startDate: startDate,
            endDate: endDate,
            type: this.queryType
          }
        })
        this.carbonList = resList.data || []

        this.totalEmission = this.carbonList.reduce((sum, item) => sum + (item.carbonEmission || 0), 0).toFixed(1)
        this.treeEquivalent = (this.totalEmission / 18.3).toFixed(1)

        const end = new Date()
        const dateMap = {}
        for (let i = 29; i >= 0; i--) {
          const date = new Date(end.getTime() - i * 24 * 60 * 60 * 1000)
          const dateStr = this.formatDate(date)
          dateMap[dateStr] = 0
        }
        this.carbonList.forEach(item => {
          const dateStr = item.recordTime
          if (dateStr in dateMap) {
            dateMap[dateStr] += Number(item.carbonEmission) || 0
          }
        })
        const lineX = Object.keys(dateMap)
        const lineY = Object.values(dateMap)

        const resGroup = await this.$http.get('/carbon/group', {
          params: { userId: this.user.id }
        })
        const pieX = resGroup.data.map(item => item.type)
        const pieY = resGroup.data.map(item => Number(item.carbonEmission) || 0)

        this.renderPieChart(pieX, pieY)
        this.renderLineChart(lineX, lineY)

      } catch (error) {
        console.error('获取碳数据失败：', error)
        this.$message.error('获取分析数据失败，请重试！')
      } finally {
        this.loading = false
      }
    },
    resetQuery() {
      this.queryDateRange = []
      this.queryType = ''
      this.getCarbonData()
    },
    renderPieChart(xData, yData) {
      const seriesData = xData.map((item, index) => ({
        name: item,
        value: yData[index] || 0
      }))
      
      const colors = ['#2d5a27', '#5d8aa8', '#a8c686', '#e0c686', '#c68686']
      const hasData = seriesData.some(item => item.value > 0)

      this.pieChart.setOption({
        color: colors,
        tooltip: { 
          trigger: 'item',
          backgroundColor: 'rgba(255,255,255,0.95)',
          borderColor: '#eee',
          borderWidth: 1,
          textStyle: { color: '#333', fontSize: 12 },
          formatter: '{b}: {c} kg ({d}%)'
        },
        legend: { 
          orient: 'vertical', 
          right: -5, 
          top: -5,
          textStyle: { color: '#333', fontSize: 12 },
          data: xData
        },
        series: [
          {
            name: '碳排放量',
            type: 'pie',
            radius: hasData ? ['50%', '70%'] : [0, '70%'],
            center: ['40%', '50%'],
            data: hasData ? seriesData : [{ name: '无数据', value: 1 }],
            avoidLabelOverlap: false,
            label: { show: false, position: 'center' },
            emphasis: {
              label: {
                show: hasData,
                fontSize: 18,
                fontWeight: 'bold',
                color: '#2d5a27',
                formatter: hasData ? '{d}%' : '无数据'
              }
            },
            labelLine: { show: false },
            silent: !hasData
          }
        ]
      })
    },
    updateNickname(newNickname) {
      // 从 localStorage 重新获取最新的用户信息
      const updatedUser = JSON.parse(localStorage.getItem('user'));
      if (updatedUser) {
        this.user = updatedUser;
      } else {
        this.user.nickname = newNickname;
      }
      this.$root.$emit('userInfoUpdated', this.user);
    },
    renderLineChart(xData, yData) {
      const hasData = yData.some(val => val > 0)
      this.lineChart.setOption({
        grid: { top: 30, bottom: 30, left: 50, right: 20 },
        xAxis: { 
          type: 'category', 
          data: xData, 
          axisLine: { show: false },
          axisTick: { show: false },
          axisLabel: { color: '#333', fontSize: 10, interval: 4, rotate: 30 }
        },
        yAxis: { 
          type: 'value', 
          name: 'kg CO₂',
          nameTextStyle: { color: '#333', fontSize: 10 },
          splitLine: { lineStyle: { color: '#f0f0f0', type: 'dashed' } },
          axisLabel: { color: '#333', fontSize: 10 }
        },
        tooltip: { 
          trigger: 'axis',
          backgroundColor: 'rgba(255,255,255,0.95)',
          borderColor: '#eee',
          borderWidth: 1,
          textStyle: { color: '#333', fontSize: 12 },
          formatter: '{b}: {c} kg CO₂'
        },
        series: [
          {
            name: '日碳排放量',
            type: 'line',
            data: yData,
            smooth: true,
            symbol: 'none',
            lineStyle: { color: '#2d5a27', width: 3 },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: 'rgba(45, 90, 39, 0.2)' },
                { offset: 1, color: 'rgba(45, 90, 39, 0)' }
              ])
            },
            silent: !hasData
          }
        ]
      })
    },
    handleRowContextMenu(row, column, event) {
      event.preventDefault()
      event.stopPropagation()
      this.currentRow = row
      this.contextMenuX = event.clientX
      this.contextMenuY = event.clientY
      this.contextMenuVisible = true
    },
    hideContextMenu() { this.contextMenuVisible = false },
    handleEdit() { 
      this.contextMenuVisible = false
      if (!this.currentRow) return
      this.loadEditData(this.currentRow)
    },
    handleEditRow(row) {
      this.loadEditData(row)
    },
    async loadEditData(row) {
      try {
        const res = await this.$http.get(`/carbon/get/${row.id}`)
        const record = res.data
        this.editForm = {
          id: record.id,
          userId: this.user.id,
          type: record.type,
          detail: record.detail,
          amount: record.amount,
          unit: record.unit || '',
          recordTime: record.recordTime
        }
        this.editDialogVisible = true
      } catch (error) {
        console.error('获取记录详情失败：', error)
        this.$message.error('获取记录详情失败')
      }
    },
    handleDelete() {
      this.contextMenuVisible = false
      if (!this.currentRow) return
      this.$confirm('确定要删除这条记录吗？删除后将扣减对应绿芽积分！', '温馨提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await this.$http.delete(`/carbon/delete/${this.currentRow.id}`)
          this.$message.success(res.msg || '删除成功')
          this.getCarbonData()
          this.$root.$emit('refreshIntegral')
        } catch (error) {
          console.error('删除记录失败：', error)
          this.$message.error('删除失败，请重试')
        }
      }).catch(() => {
        this.$message.info('已取消删除')
      })
    },
    submitEditForm() {
      this.$refs.editFormRef.validate(async (valid) => {
        if (valid) {
          try {
            const res = await this.$http.post('/carbon/update', this.editForm)
            this.$message.success(res.msg || '修改成功')
            this.editDialogVisible = false
            this.getCarbonData()
            this.$root.$emit('refreshIntegral')
          } catch (error) {
            console.error('修改记录失败：', error)
            this.$message.error('修改失败，请重试')
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.analysis-wrapper {
  min-height: 100vh;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  position: relative;
}
.analysis-wrapper::before {
  content: '';
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-image: url('~@/assets/CarbonAnalysis.jpg');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  z-index: -1;
}
.analysis-wrapper::after {
  content: '';
  position: fixed;
  inset: 0;
  background: rgba(255, 255, 255, 0.37);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  z-index: -1;
}
.analysis-content {
  position: relative;
  z-index: 1;
  max-width: 1200px;
  margin: 0 auto;
  padding: 120px 20px 60px;
}
.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
  flex-wrap: wrap;
  gap: 20px;
}
.header-text {
  flex: 1;
  min-width: 280px;
}
.page-title {
  font-size: 36px;
  font-weight: 800;
  color: #1a1a1a;
  margin: 0 0 8px 0;
  letter-spacing: -0.5px;
}
.page-subtitle {
  font-size: 15px;
  color: #333;
  margin: 0;
}
.header-stats {
  display: flex;
  gap: 20px;
}
.stat-card {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.6);
  padding: 15px 25px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.03);
  min-width: 180px;
  transition: all 0.3s ease;
}
.stat-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.06);
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 15px;
}
.stat-info {
  display: flex;
  flex-direction: column;
}
.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a1a;
  line-height: 1;
}
.unit {
  font-size: 14px;
  color: #333;
  font-weight: 50;
  margin-left: 2px;
}
.stat-label {
  font-size: 13px;
  color: #333;
  margin-top: 6px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.stat-desc {
  font-size: 13px;
  color: #333;
  margin-top: 4px;
  line-height: 1.2;
  white-space: nowrap;
}
.charts-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  margin-bottom: 30px;
}
.chart-panel {
  background: rgba(255, 255, 255, 0.7);
  border-radius: 20px;
  padding: 25px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
  transition: all 0.3s ease;
}
.chart-panel:hover {
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.06);
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.panel-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: '#2d5a27';
  margin: 0;
}
.time-range {
  font-size: 12px;
  color: #333;
  background: rgba(0, 0, 0, 0.05);
  padding: 4px 10px;
  border-radius: 20px;
}
.query-toolbar {
  display: flex;
  gap: 10px;
  align-items: center;
}
.chart-container {
  flex: 1;
  min-height: 300px;
  width: 100%;
}
.table-panel {
  background: rgba(255, 255, 255, 0.7);
  border-radius: 20px;
  padding: 25px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.04);
}
.refresh-btn {
  color: #333;
  font-size: 13px;
  padding: 5px 10px;
}
.refresh-btn:hover {
  color: #2d5a27;
}
.minimal-table {
  background: transparent !important;
}
:deep(.minimal-table th) {
  background: transparent !important;
  color: #333;
  font-weight: 500;
  font-size: 13px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05) !important;
  padding: 15px 10px !important;
}
:deep(.minimal-table td) {
  border-bottom: 1px solid rgba(0, 0, 0, 0.05) !important;
  color: #333;
  font-size: 14px;
  padding: 18px 10px !important;
}
:deep(.minimal-table tr:last-child td) {
  border-bottom: none !important;
}
:deep(.minimal-table tr:hover td) {
  background: rgba(45, 90, 39, 0.03) !important;
}
.type-tag {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  display: inline-block;
}
.type-tag.交通出行 { background: #e3f2fd; color: #1976d2; }
.type-tag.能源消耗 { background: #e8f5e9; color: #2e7d32; }
.type-tag.饮食消费 { background: #fff3e0; color: #ef6c00; }
.type-tag.购物消费 { background: #fce4ec; color: #c2185b; }
.type-tag.垃圾处理 { background: #f3e5f5; color: #7b1fa2; }
.type-tag.其他 { background: #e0f7fa; color: #00acc1; }
.emission-text {
  font-weight: 600;
  color: #2d5a27;
  font-family: 'Georgia', serif;
}
.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #999;
}
.empty-state i {
  font-size: 48px;
  margin-bottom: 15px;
  display: block;
  opacity: 0.3;
}
.empty-state p {
  margin: 0 0 20px 0;
  font-size: 14px;
}
.custom-context-menu {
  position: fixed;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  padding: 8px 0;
  border-radius: 12px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
  border: 1px solid rgba(0, 0, 0, 0.05);
  z-index: 9999;
  min-width: 140px;
  animation: fadeIn 0.2s ease;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}
.menu-option {
  padding: 10px 20px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 8px;
}
.menu-option:hover {
  background: rgba(45, 90, 39, 0.05);
  color: #2d5a27;
}
.menu-option i {
  font-size: 14px;
}
.menu-option.danger {
  color: #f56c6c;
}
.menu-option.danger:hover {
  background: rgba(245, 108, 108, 0.05);
}
:deep(.minimal-dialog .el-dialog) {
  border-radius: 20px;
  border: none;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}
:deep(.minimal-dialog .el-dialog__header) {
  padding: 25px 30px 10px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background: linear-gradient(90deg, rgba(45, 90, 39, 0.02), transparent);
}
:deep(.minimal-dialog .el-dialog__title) {
  font-size: 20px;
  font-weight: 600;
  color: #1a1a1a;
}
:deep(.minimal-dialog .el-dialog__body) {
  padding: 30px;
}
:deep(.el-input__inner),
:deep(.el-textarea__inner),
:deep(.el-select .el-input__inner) {
  border: 1px solid #eee;
  background: #fafafa;
  border-radius: 10px;
  padding: 10px 15px;
}
:deep(.el-input__inner:focus),
:deep(.el-textarea__inner:focus),
:deep(.el-select .el-input__inner:focus) {
  border-color: #2d5a27;
  background: #fff;
}
:deep(.dialog-footer) {
  padding: 15px 30px 25px;
  text-align: right;
}
:deep(.el-button) {
  padding: 10px 25px;
  border-radius: 10px;
  font-weight: 500;
}
:deep(.el-button--primary) {
  background: #2d5a27;
  border-color: #2d5a27;
}
:deep(.el-button--primary:hover) {
  background: #3d6a37;
  border-color: #3d6a37;
}
@media (max-width: 992px) {
  .charts-grid {
    grid-template-columns: 1fr;
  }
}
@media (max-width: 768px) {
  .stats-header {
    flex-direction: column;
    align-items: flex-start;
  }
  .header-stats {
    width: 100%;
    justify-content: space-between;
  }
  .stat-card {
    flex: 1;
    min-width: auto;
    padding: 12px 18px;
  }
  .page-title {
    font-size: 28px;
  }
  .analysis-content {
    padding: 100px 15px 40px;
  }
  .chart-panel,
  .table-panel {
    padding: 20px;
  }
  .query-toolbar {
    flex-wrap: wrap;
    justify-content: flex-end;
  }
}
@media (max-width: 480px) {
  .header-stats {
    flex-direction: column;
    gap: 12px;
  }
  .stat-card {
    width: 100%;
  }
  .stat-value {
    font-size: 20px;
  }
}
</style>