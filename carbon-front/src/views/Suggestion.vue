<template>
  <div class="suggestion-page">
    <NavBar :nickname="user.nickname" @update:nickname="updateNickname" />
    
    <div class="layout-wrapper">
      <main class="main-content">
        <div v-if="loading" class="loading-container">
          <el-skeleton :rows="8" animated></el-skeleton>
        </div>
        
        <div v-else class="chat-container" ref="chatContainer">
          <div v-if="chatHistory.length === 0" class="empty-state">
            <div class="empty-icon">🌱</div>
            <h3>这里还没有对话</h3>
            <p>录入碳足迹数据，让我为你定制减碳计划</p>
            <el-button type="primary" round @click="$router.push('/dataInput')">去录入数据</el-button>
          </div>

          <div v-else class="chat-list">
            <div 
              v-for="(item, index) in chatHistory" 
              :key="`chat-${index}-${item.createTime || item.time}`"
              class="chat-item"
              :class="item.role === 'user' ? 'user-item' : 'ai-item'"
              @contextmenu.prevent="openContextMenu(index, $event)"
            >
              <div v-if="item.role === 'user'" class="user-bubble">
                <div class="chat-avatar">👤</div>
                <div class="chat-content">{{ item.content }}</div>
                <div class="chat-time">{{ formatTime(item.createTime || item.time) }}</div>
              </div>
              
              <div v-else class="ai-bubble">
                <div class="chat-avatar">🤖</div>
                <div class="chat-main">
                  <div class="chat-content">{{ item.content }}</div>
                </div>
                <div class="chat-time">{{ formatTime(item.createTime || item.time) }}</div>
              </div>
            </div>
          </div>

          <div class="chat-input-sticky-wrapper">
            <div class="chat-input-area">
              <el-input
                v-model="userInput"
                placeholder="和减碳小助手聊聊吧～"
                @keyup.enter.native="sendMessage"
                clearable
              >
                <template slot="append">
                  <el-button type="primary" @click="sendMessage" :disabled="!userInput.trim()">
                    发送
                  </el-button>
                </template>
              </el-input>
            </div>
          </div>
        </div>
      </main>
    </div>

    <div 
      v-show="contextMenuVisible"
      :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
      class="chat-context-menu"
    >
      <div class="menu-item" @click="deleteChatItem">删除此条消息</div>
    </div>

    <EcoMascot speechText="你好啊，我是你的减碳小助手^_^" />
  </div>
</template>

<script>
import NavBar from '../components/NavBar.vue'
import EcoMascot from '../components/EcoMascot.vue'
export default {
  name: 'Suggestion',
  components: { NavBar, EcoMascot },
  data() {
    return {
      user: JSON.parse(localStorage.getItem('user')) || {},
      chatHistory: [],
      loading: false,
      userInput: '',
      contextMenuVisible: false,
      contextMenuX: 0,
      contextMenuY: 0,
      currentDeleteIndex: -1
    }
  },
  activated() {
    this.refreshUserInfo()
    this.loadChatHistory()
  },
  mounted() {
    this.loadChatHistory()
    this.$root.$on('refreshSuggestion', this.loadChatHistory)
    window.addEventListener('storage', this.handleStorageChange)
    window.addEventListener('click', this.closeContextMenu)
  },
  beforeDestroy() {
    this.$root.$off('refreshSuggestion', this.loadChatHistory)
    window.removeEventListener('storage', this.handleStorageChange)
    window.removeEventListener('click', this.closeContextMenu)
  },
  methods: {
    scrollToBottom() {
      this.$nextTick(() => {
        const container = this.$refs.chatContainer
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    },
    updateNickname(newNickname) {
      const updatedUser = JSON.parse(localStorage.getItem('user'));
      if (updatedUser) {
        this.user = updatedUser;
      } else {
        this.user.nickname = newNickname;
      }
      this.$root.$emit('userInfoUpdated', this.user);
    },
    refreshUserInfo() {
      this.user = JSON.parse(localStorage.getItem('user')) || {}
    },
    handleStorageChange(e) {
      if (e.key === 'user' || e.key === 'chatHistory') {
        this.refreshUserInfo()
        this.loadChatHistory()
      }
    },
    formatTime(timestamp) {
      if (!timestamp) return ''
      const date = new Date(timestamp)
      return `${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
    },
    async loadChatHistory() {
      this.chatHistory = []
      this.refreshUserInfo()
      this.loading = true
      try {
        let allChat = JSON.parse(localStorage.getItem('chatHistory') || '[]')
        this.chatHistory = allChat.filter(item => item.userId === this.user.id)
        this.scrollToBottom()
      } catch (error) {
        console.error('加载对话失败：', error)
        this.chatHistory = []
      } finally {
        this.loading = false
      }
    },
    async sendMessage() {
      if (!this.userInput.trim() || !this.user.id) return
      
      const timestamp = Date.now()
      const userMsg = {
        userId: this.user.id,
        role: 'user',
        content: this.userInput.trim(),
        time: timestamp
      }
      this.chatHistory.push(userMsg)
      const input = this.userInput.trim()
      this.userInput = ''
      this.scrollToBottom()
      
      try {
        const res = await this.$http.post('/ai/chat', {
          userId: this.user.id,
          question: input
        })
        
        const aiMsg = {
          userId: this.user.id,
          role: 'ai',
          content: res.data.content,
          suggestion: res.data.suggestion || '',
          time: Date.now()
        }
        this.chatHistory.push(aiMsg)
        
        let allChat = JSON.parse(localStorage.getItem('chatHistory') || '[]')
        allChat.push(userMsg, aiMsg)
        if (allChat.length > 200) allChat = allChat.slice(-200)
        localStorage.setItem('chatHistory', JSON.stringify(allChat))
        this.scrollToBottom()
      } catch (error) {
        console.error('AI回复失败：', error)
        const errorMsg = {
          userId: this.user.id,
          role: 'ai',
          content: '网络开小差了～',
          time: Date.now()
        }
        this.chatHistory.push(errorMsg)
        let allChat = JSON.parse(localStorage.getItem('chatHistory') || '[]')
        allChat.push(errorMsg)
        localStorage.setItem('chatHistory', JSON.stringify(allChat))
      }
    },
    openContextMenu(index, e) {
      this.currentDeleteIndex = index
      this.contextMenuX = e.clientX
      this.contextMenuY = e.clientY
      this.contextMenuVisible = true
    },
    closeContextMenu() {
      this.contextMenuVisible = false
      this.currentDeleteIndex = -1
    },
    deleteChatItem() {
      if (this.currentDeleteIndex < 0) return
      const deletedItem = this.chatHistory[this.currentDeleteIndex]
      if (!deletedItem) return

      let allChat = JSON.parse(localStorage.getItem('chatHistory') || '[]')
      const globalIndex = allChat.findIndex(
        msg => msg.userId === deletedItem.userId && msg.time === deletedItem.time
      )
      if (globalIndex !== -1) {
        allChat.splice(globalIndex, 1)
        localStorage.setItem('chatHistory', JSON.stringify(allChat))
      }
      this.chatHistory.splice(this.currentDeleteIndex, 1)
      this.$message.success('删除成功')
      this.closeContextMenu()
    }
  }
}
</script>

<style scoped>
.suggestion-page {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-image: url('~@/assets/Suggestion.jpg');
  background-size: cover;
  background-position: center;
  background-attachment: fixed;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.layout-wrapper {
  flex: 1;
  display: flex;
  justify-content: center;
  overflow: hidden;
  width: 100%;
  padding: 0 24px;
  box-sizing: border-box;
  margin: 127px 0 20px 0;
}
.main-content {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 1000px;
  height: 100%;
  overflow: hidden;
}

.loading-container {
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  border-radius: 28px;
  padding: 28px;
  box-shadow: 0 12px 36px -8px rgba(0, 40, 20, 0.12);
  height: 100%;
  border: 1px solid rgba(255, 255, 255, 0.5);
}
.empty-state {
  text-align: center;
  padding: 48px 32px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(16px);
  border-radius: 28px;
  box-shadow: 0 12px 36px -8px rgba(0, 40, 20, 0.12);
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(255, 255, 255, 0.5);
}
.empty-icon { font-size: 56px; margin-bottom: 20px; opacity: 0.8; }
.empty-state h3 { font-size: 20px; font-weight: 600; color: #1e3a2a; margin: 0 0 8px 0; }
.empty-state p { color: #4b6b4b; margin: 0 0 28px 0; font-size: 15px; }
.empty-state .el-button { background: #2d5a27; border-color: #2d5a27; font-weight: 500; padding: 12px 32px; font-size: 15px; border-radius: 40px; }

.chat-container {
  flex: 1;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(16px);
  border-radius: 28px;
  padding: 24px;
  box-shadow: 0 12px 36px -8px rgba(0, 40, 20, 0.12);
  overflow-y: auto;
  border: 1px solid rgba(255, 255, 255, 0.6);
  display: flex;
  flex-direction: column;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.chat-container::-webkit-scrollbar { display: none; }

.chat-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 20px;
}

.chat-item { display: flex; width: 100%; animation: fadeIn 0.3s ease forwards; }
.user-item { justify-content: flex-end; }
.ai-item { justify-content: flex-start; }
.user-bubble { display: flex; align-items: flex-end; gap: 8px; max-width: 80%; flex-direction: row-reverse; }
.user-bubble .chat-avatar { width: 40px; height: 40px; border-radius: 40px; background: linear-gradient(145deg, #d9ecd9, #c0e0c0); display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; border: 2px solid rgba(255,255,255,0.7); }
.user-bubble .chat-content { background: #55bf48; color: white; padding: 12px 18px; border-radius: 24px 24px 8px 24px; font-size: 15px; line-height: 1.5; word-break: break-word; box-shadow: 0 6px 14px rgba(45,90,39,0.18); max-width: calc(100% - 56px); }
.user-bubble .chat-time { font-size: 12px; color: #8ba38b; margin-bottom: 8px; white-space: nowrap; }
.ai-bubble { display: flex; align-items: flex-end; gap: 8px; max-width: 80%; }
.ai-bubble .chat-avatar { width: 40px; height: 40px; border-radius: 40px; background: linear-gradient(145deg, #f0fff0, #e0f0e0); display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; border: 2px solid rgba(255,255,255,0.7); }
.ai-bubble .chat-main { flex: 1; }
.ai-bubble .chat-content { background: #ffffffd9; backdrop-filter: blur(4px); color: #1e4228; padding: 12px 18px; border-radius: 24px 24px 24px 8px; font-size: 15px; line-height: 1.5; word-break: break-word; box-shadow: 0 4px 12px rgba(0,30,0,0.04); border: 1px solid rgba(180,210,170,0.5); }
.ai-bubble .chat-time { font-size: 12px; color: #8ba38b; margin-bottom: 8px; white-space: nowrap; }

.chat-input-sticky-wrapper {
  position: sticky;
  bottom: 0;
  padding-top: 16px;
  background: linear-gradient(to top, rgba(255,255,255,0.95) 0%, rgba(255,255,255,0) 100%);
}
.chat-input-area {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  border-radius: 60px;
  padding: 6px 6px 6px 24px;
  box-shadow: 0 12px 32px -8px rgba(45, 90, 39, 0.25), 0 0 0 1px rgba(255,255,255,0.8) inset;
}

::v-deep .chat-input-area .el-input { 
  width: 100%;
  --el-input-group-append-padding: 0 4px !important;
}
::v-deep .chat-input-area .el-input__inner { 
  border: none; 
  background: transparent; 
  padding: 16px 0; 
  font-size: 16px; 
  color: #1e3a2a; 
  height: auto; 
  box-shadow: none !important; 
}
::v-deep .chat-input-area .el-input__inner::placeholder { 
  color: #9bb89b; 
  font-weight: 400; 
}
::v-deep .chat-input-area .el-input-group__append { 
  background: transparent !important; 
  border: none !important; 
  padding: 0 !important; 
  margin: 0 !important;
  position: relative;
  left: -27px !important;
}
::v-deep .chat-input-area .el-button { 
  border-radius: 40px !important; 
  padding: 12px 16px;
  background: #59c93b;
  border-color: #3cd677; 
  font-weight: 600; 
  font-size: 18px;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  height: 48px;
}
::v-deep .chat-input-area .el-button:hover { 
  background: #7c3aed; 
  border-color: #7c3aed;
  transform: scale(1.02); 
}
::v-deep .chat-input-area .el-button.is-disabled { 
  background: #d1d5db;
  border-color: #d1d5db; 
  box-shadow: none; 
  opacity: 1;
  color: #ffffff;
}

.chat-context-menu {
  position: fixed;
  z-index: 9999;
  background: #ffffff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  width: 140px;
  overflow: hidden;
}
.menu-item {
  padding: 8px 12px;
  font-size: 14px;
  color: #f56c6c;
  cursor: pointer;
  transition: background 0.2s;
}
.menu-item:hover {
  background: #f5f5f5;
}

@keyframes fadeIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
@media (max-width: 768px) {
  .layout-wrapper { padding: 0 16px; margin: 8px 0 16px 0; }
  .chat-container { padding: 16px; border-radius: 24px; }
  .chat-input-area { padding-left: 20px; }
}
</style>