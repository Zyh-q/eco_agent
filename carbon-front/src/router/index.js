import Vue from 'vue'
import VueRouter from 'vue-router'
import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import Home from '../views/Home.vue'
import DataInput from '../views/DataInput.vue'
import CarbonAnalysis from '../views/CarbonAnalysis.vue'
import Integral from '../views/Integral.vue'
import Suggestion from '../views/Suggestion.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    redirect: '/login' // 默认跳登录页
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    
  },
  {
    path: '/register',
    name: 'Register',
    component: Register
  },
  {
    path: '/home',
    name: 'Home',
    component: Home
  },
  {
    path: '/dataInput',
    name: 'DataInput',
    component: DataInput
  },
  {
    path: '/carbonAnalysis',
    name: 'CarbonAnalysis',
    component: CarbonAnalysis
  },
  {
    path: '/integral',
    name: 'Integral',
    component: Integral
  },
  {
    path: '/suggestion',
    name: 'Suggestion',
    component: Suggestion
  }
]


const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router