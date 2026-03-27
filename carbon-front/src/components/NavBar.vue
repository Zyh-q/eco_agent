<template>
  <div class="nav-container">
    <nav class="glass-nav">
      <!-- Logo 区域 -->
      <div class="nav-logo">
        <i class="el-icon-leaf logo-icon"></i>
        <span class="logo-text">GreenStep</span>
      </div>

      <!-- 中间菜单 -->
      <el-menu 
        :default-active="$route.path" 
        class="nav-menu" 
        mode="horizontal"
        background-color="transparent"
        text-color="#333"
        active-text-color="#2d5a27"
      >
        <el-menu-item index="/home" @click="$router.push('/home')">首页</el-menu-item>
        <el-menu-item index="/dataInput" @click="$router.push('/dataInput')">数据录入</el-menu-item>
        <el-menu-item index="/carbonAnalysis" @click="$router.push('/carbonAnalysis')">足迹分析</el-menu-item>
        <el-menu-item index="/integral" @click="$router.push('/integral')">绿芽积分</el-menu-item>
        <el-menu-item index="/suggestion" @click="$router.push('/suggestion')">减碳小助手</el-menu-item>
      </el-menu>

      <!-- 右侧用户区 -->
      <div class="nav-user">
        <div class="user-info">
          <span class="user-name">{{ nickname }}</span>
          <i class="el-icon-edit edit-icon" @click="openEditDialog"></i>
        </div>
        <el-button type="text" class="logout-btn" @click="logout">
          <i class="el-icon-switch-button"></i>
        </el-button>
      </div>
    </nav>

    <!-- 编辑昵称对话框 -->
    <el-dialog
      title="修改昵称"
      :visible.sync="editDialogVisible"
      width="400px"
      :close-on-click-modal="false"
      append-to-body
      @close="resetEditForm"
    >
      <el-form :model="editForm" ref="editFormRef" :rules="editRules" label-width="0">
        <el-form-item prop="nickname">
          <el-input
            v-model="editForm.nickname"
            placeholder="请输入新昵称（2-20个字符）"
            clearable
            autofocus
          ></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="updateNickname" :loading="submitting">保存</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'NavBar',
  props: {
    nickname: { type: String, required: true }
  },
  data() {
    return {
      editDialogVisible: false,
      submitting: false,
      editForm: {
        nickname: ''
      },
      editRules: {
        nickname: [
          { required: true, message: '请输入昵称', trigger: 'blur' },
          { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    openEditDialog() {
      this.editForm.nickname = this.nickname
      this.editDialogVisible = true
    },
    // 重置表单
    resetEditForm() {
      this.$refs.editFormRef && this.$refs.editFormRef.resetFields()
    },
    // 更新昵称
    async updateNickname() {
      this.$refs.editFormRef.validate(async (valid) => {
        if (!valid) return
        this.submitting = true
        try {
          const userStr = localStorage.getItem('user')
          if (!userStr) {
            this.$message.error('请先登录')
            this.editDialogVisible = false
            return
          }
          const currentUser = JSON.parse(userStr)
          // 调用更新接口
          const res = await this.$http.post('/user/update', {
            id: currentUser.id,
            nickname: this.editForm.nickname
          })
          if (res.code === 200) {
            const userRes = await this.$http.get('/user/info', { params: { userId: currentUser.id } })
            if (userRes.code === 200) {
              const newUser = userRes.data
              localStorage.setItem('user', JSON.stringify(newUser))
              this.$emit('update:nickname', newUser.nickname)
              this.$message.success('修改成功')
              this.editDialogVisible = false
            } else {
              this.$message.error('获取最新用户信息失败')
            }
          } else {
            this.$message.error(res.msg || '修改失败')
          }
        } catch (error) {
          console.error('修改昵称失败', error)
          this.$message.error('网络错误，请稍后重试')
        } finally {
          this.submitting = false
        }
      })
    },
    logout() {
      localStorage.removeItem('user')
      this.$message.success('已安全退出')
      this.$router.push('/login')
    }
  }
}
</script>

<style scoped>
.nav-container {
  position: fixed;
  top: 20px;
  left: 0;
  width: 100%;
  z-index: 1000;
  display: flex;
  justify-content: center;
  pointer-events: none;
}

.glass-nav {
  pointer-events: auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 90%;
  max-width: 1200px;
  height: 64px;
  padding: 0 30px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 100px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.4);
  transition: all 0.3s ease;
}

.nav-logo {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: #2d5a27;
}
.logo-icon { font-size: 24px; margin-right: 8px; }
.logo-text { font-size: 18px; font-weight: 700; letter-spacing: 0.5px; }

.nav-menu {
  border: none !important;
  flex: 1;
  display: flex;
  justify-content: center;
  background: transparent !important;
}
.nav-menu .el-menu-item {
  height: 64px !important;
  line-height: 64px !important;
  font-size: 15px;
  font-weight: 500;
  color: #111 !important;
  padding: 0 20px !important;
  border-bottom: 2px solid transparent !important;
  transition: all 0.3s;
}
.nav-menu .el-menu-item:hover {
  background-color: transparent !important;
  color: #2d5a27 !important;
}
.nav-menu .el-menu-item.is-active {
  color: #2d5a27 !important;
  font-weight: 600;
  border-bottom: 2px solid #2d5a27 !important;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 15px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  line-height: 1.2;
}
.user-name { 
  font-size: 14px; 
  color: #333; 
  font-weight: 600; 
}
.edit-icon {
  font-size: 16px;
  color: #8c8c8c;
  cursor: pointer;
  transition: color 0.2s;
}
.edit-icon:hover {
  color: #2d5a27;
}
.logout-btn { 
  color: #111; 
  font-size: 18px; 
  padding: 5px; 
  margin-left: 5px;
}
.logout-btn:hover { 
  color: #ff4d4f; 
}

@media (max-width: 768px) {
  .nav-menu { display: none; }
  .glass-nav { width: 95%; padding: 0 15px; }
  .user-name { display: none; }
  .edit-icon { display: none; }
  .logout-btn { margin-left: 0; }
}
</style>