<template>
  <div class="register-page">
    <div class="register-wrapper">
      <el-card class="register-card" shadow="hover">
        <div slot="header" class="card-header">
          <div class="header-icon">
            <i class="el-icon-user"></i>
          </div>
          <h2 class="header-title">个人碳足迹系统 - 注册</h2>
          <p class="header-subtitle">创建账户，开始您的环保之旅</p>
        </div>
        
        <el-form 
          ref="registerForm" 
          :model="registerForm" 
          :rules="registerRules" 
          label-width="80px" 
          @submit.native.prevent="register"
          class="register-form"
        >
          <el-form-item label="用户名" class="form-item" prop="username">
            <el-input 
              v-model="registerForm.username" 
              placeholder="请输入用户名"
              class="input-item"
            >
              <i slot="prefix" class="el-input__icon el-icon-user"></i>
            </el-input>
          </el-form-item>
          
          <el-form-item label="密码" class="form-item" prop="password">
            <el-input 
              v-model="registerForm.password" 
              type="password" 
              placeholder="请输入密码"
              class="input-item"
            >
              <i slot="prefix" class="el-input__icon el-icon-lock"></i>
            </el-input>
          </el-form-item>
          
          <el-form-item label="昵称" class="form-item" prop="nickname">
            <el-input 
              v-model="registerForm.nickname" 
              placeholder="请输入昵称"
              class="input-item"
            >
              <i slot="prefix" class="el-input__icon el-icon-edit"></i>
            </el-input>
          </el-form-item>
          
          <el-form-item class="button-group">
            <el-button 
              type="primary" 
              @click="register" 
              class="register-btn"
            >
              注册
            </el-button>
            <el-button 
              @click="$router.push('/login')" 
              class="back-btn"
            >
              返回登录
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Register',
  data() {
    return {
      registerForm: {
        username: '',
        password: '',
        nickname: ''
      },
      registerRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 20, message: '用户名长度需在3-20个字符之间', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度需在6-20个字符之间', trigger: 'blur' }
        ],
        nickname: [
          { required: true, message: '请输入昵称', trigger: 'blur' },
          { min: 2, max: 10, message: '昵称长度需在2-10个字符之间', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    async register() {
      try {
        await this.$refs.registerForm.validate();
      } catch (error) {
        return;
      }

      try {
        const res = await this.$http.post('/user/register', this.registerForm);
        if (res.code == 200) {  
          this.$message.success(res.msg);
          this.$router.push('/login');
        } else {
          this.$message.error(res.msg || '注册失败');
        }
      } catch (error) {
        if (error.response) {
          this.$message.error(error.response.data?.msg || '注册失败');
        } else if (error.request) {
          this.$message.error('网络连接失败，请检查网络');
        } else {
          this.$message.error('注册失败，请稍后重试');
        }
      }
    }
  }
}
</script>

<style scoped>
.register-page {
  height: 100vh !important;
  width: 100vw !important;
  display: flex;
  align-items: center; 
  justify-content: center; 
  position: fixed;
  top: 0;
  left: 0;
  background-image: url('~@/assets/bg_login.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  background-attachment: fixed;
  overflow: hidden !important;
  padding: 0;
  margin: 0;
}

.register-wrapper {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 20px;
  margin: 0;
}

.register-card {
  border-radius: 16px;
  border: none;
  overflow: hidden;
  transition: all 0.3s ease;
  background-color: rgba(255, 255, 255, 0.95) !important;
  box-sizing: border-box;
}

.register-card:hover {
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
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
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

.register-form {
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

.register-btn {
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

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 12px rgba(52, 168, 83, 0.3);
}

.back-btn {
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

.back-btn:hover {
  background-color: rgba(52, 168, 83, 0.1);
  transform: translateY(-2px);
}
</style>