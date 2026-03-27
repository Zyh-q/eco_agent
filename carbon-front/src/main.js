import Vue from 'vue'
import App from './App.vue'
import router from './router'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import request from './utils/request'
import 'element-ui/lib/theme-chalk/icon.css'

Vue.config.productionTip = false
Vue.use(ElementUI)

const originalMessage = ElementUI.Message
ElementUI.Message = function(options) {
  const defaultOptions = { offset: 80, duration: 2000, customClass: 'clickable-message' }
  const opts = Object.assign({}, defaultOptions, options)
  const instance = originalMessage(opts)

  const messageDom = document.querySelector('.clickable-message')
  if (messageDom) {
    messageDom.style.cursor = 'pointer'
    messageDom.onclick = function(e) {
      instance.close()
      e.stopPropagation()
    }
  }
  return instance
}

const messageTypes = ['success', 'warning', 'error', 'info']
messageTypes.forEach(function(type) {
  ElementUI.Message[type] = function(options) {
    const baseOpts = { offset: 80, duration: 2000, customClass: 'clickable-message' }
    let finalOpts = {}
    if (typeof options === 'string') {
      finalOpts = { message: options, ...baseOpts }
    } else {
      finalOpts = { ...baseOpts, ...options }
    }
    finalOpts.type = type
    return ElementUI.Message(finalOpts)
  }
})

Vue.prototype.$message = ElementUI.Message
Vue.prototype.$http = request

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')