<template>
  <div class="login-page">
    <div class="login-wrapper">
      <el-card class="login-card" shadow="hover">
        <div slot="header" class="card-header">
          <div class="header-icon">
            <i class="el-icon-sunny"></i>
          </div>
          <h2 class="header-title">个人碳足迹系统 - 登录</h2>
          <p class="header-subtitle">欢迎回来，请登录您的账户</p>
        </div>
        
        <!-- 表单区域 -->
        <el-form 
          ref="loginFormRef" 
          :model="loginForm" 
          :rules="loginRules" 
          label-width="80px" 
          @submit.native.prevent="login"
          class="login-form"
        >
          <!-- 用户名输入框 -->
          <el-form-item label="用户名" prop="username" class="form-item">
            <el-input 
              v-model="loginForm.username" 
              placeholder="请输入用户名"
              class="input-item"
            >
              <i slot="prefix" class="el-input__icon el-icon-user"></i>
            </el-input>
          </el-form-item>
          
          <!-- 密码输入框 -->
          <el-form-item label="密码" prop="password" class="form-item">
            <el-input 
              v-model="loginForm.password" 
              type="password" 
              placeholder="请输入密码"
              class="input-item"
            >
              <i slot="prefix" class="el-input__icon el-icon-lock"></i>
            </el-input>
          </el-form-item>
          
          <!-- 按钮区域 -->
          <el-form-item class="button-group">
            <el-button 
              type="primary" 
              @click="login" 
              class="login-btn"
            >
              登录
            </el-button>
            <el-button 
              @click="$router.push('/register')" 
              class="register-btn"
            >
              注册
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="card-footer">
          <div class="footer-line"></div>
          <p class="footer-text">减碳，为地球“降温”；节能，为家园“加分”。</p>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Login',
  data() {
    return {
      loginForm: {
        username: '',
        password: ''
      },
      loginRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度在 3 到 20 个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    async login() {
      try {
        await this.$refs.loginFormRef.validate();
      } catch (error) {
        return;
      }

      try {
        const res = await this.$http.post('/user/login', this.loginForm)
        
        localStorage.setItem('user', JSON.stringify(res.data))
        this.$message.success('登录成功')
        this.$router.push('/home')
      } catch (error) {
        console.log('登录失败：', error);
        const errorMsg = error.response?.data?.message || '登录失败';
        
        if (errorMsg.includes('用户名不存在')) {
          this.$message.error('用户名不存在');
        } else if (errorMsg.includes('密码错误')) {
          this.$message.error('用户名或密码错误');
        } else {
          this.$message.error(errorMsg);
        }
      }
    }
  }
}
</script>

<style scoped>
.login-page {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-image: url('~@/assets/bg_login.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

.login-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 20px;
}

.login-card {
  border-radius: 16px;
  border: none;
  overflow: hidden;
  transition: all 0.3s ease;
  background-color: rgba(255, 255, 255, 0.95) !important;
}

.login-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1) !important;
}

.card-header {
  text-align: center;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.header-icon {
  margin-bottom: 16px;
}

.header-icon i {
  font-size: 42px;
  color: #34a853;
  background: linear-gradient(45deg, #34a853, #0f9d58);
  background-clip: text;
  -webkit-background-clip: text;
  -moz-background-clip: text;
  -webkit-text-fill-color: transparent;
  -moz-text-fill-color: transparent;
  color: transparent;
}

.header-title {
  margin: 0 0 8px 0;
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  letter-spacing: 0.5px;
}

.header-subtitle {
  margin: 0;
  font-size: 14px;
  color: #7f8c8d;
  font-weight: 400;
}

.login-form {
  padding: 30px 20px 10px;
}

.form-item {
  margin-bottom: 28px;
}

.form-item >>> .el-form-item__label {
  font-weight: 500;
  color: #2c3e50;
  font-size: 15px;
  padding-left: 10px;
  padding-bottom: 8px;
  display: block;
  text-align: left;
}

.input-item >>> .el-input__inner {
  height: 48px;
  border-radius: 10px;
  border: 1px solid #e0e6ed;
  padding-left: 40px;
  font-size: 15px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.input-item >>> .el-input__inner:focus {
  border-color: #34a853;
  box-shadow: 0 0 0 2px rgba(52, 168, 83, 0.1);
}

.input-item >>> .el-input__prefix {
  display: flex;
  align-items: center;
  padding-left: 12px;
}

.input-item >>> .el-input__icon {
  color: #7f8c8d;
  font-size: 18px;
}

.button-group {
  margin-top: 40px;
  text-align: center;
}

.login-btn {
  width: 45%;
  height: 44px;
  background: linear-gradient(45deg, #34a853, #0f9d58);
  border: none;
  border-radius: 10px;
  font-weight: 600;
  font-size: 16px;
  letter-spacing: 1px;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(52, 168, 83, 0.3);
}

.register-btn {
  width: 45%;
  height: 44px;
  border-radius: 10px;
  font-weight: 600;
  font-size: 16px;
  letter-spacing: 1px;
  border: 1px solid #34a853;
  color: #34a853;
  background-color: transparent;
  transition: all 0.3s ease;
}

.register-btn:hover {
  background-color: rgba(52, 168, 83, 0.1);
  transform: translateY(-2px);
}

.card-footer {
  padding: 20px 0 10px;
  text-align: center;
}

.footer-line {
  height: 1px;
  background: linear-gradient(90deg, transparent, #e0e6ed, transparent);
  margin-bottom: 16px;
}

.footer-text {
  margin: 0;
  font-size: 13px;
  color: #95a5a6;
  font-style: italic;
}
</style>