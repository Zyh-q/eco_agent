module.exports = {
  publicPath: process.env.NODE_ENV === 'production' ? '/' : '/',
  
  // 开发环境代理配置
  devServer: {
    port: 8081, // 前端开发服务器端口
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 后端服务地址
        changeOrigin: true, // 开启跨域代理
        ws: true,
        pathRewrite: {} 
      }
    }
  }
}