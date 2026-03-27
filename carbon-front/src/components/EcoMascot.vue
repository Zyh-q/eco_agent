<template>
  <div class="eco-mascot">
    <div class="mascot-container">
      <div class="mascot-body">
        <div class="mascot-head">
          <div class="mascot-eyes">
            <span class="eye left"></span>
            <span class="eye right"></span>
          </div>
          <div class="mascot-smile"></div>
        </div>
        <div class="mascot-leaf">
          <span class="leaf leaf-1">🌿</span>
          <span class="leaf leaf-2">🌱</span>
        </div>
      </div>
      <div class="mascot-speech">
        <p>{{ speechText }}</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'EcoMascot',
  props: {
    speechText: {
      type: String,
      default: '一起为地球减负吧！'
    }
  }
}
</script>

<style>
@keyframes mascotBounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

@keyframes blink {
  0%, 45%, 55%, 100% { transform: scaleY(1); }
  50% { transform: scaleY(0.1); }
}

@keyframes leafWave {
  0%, 100% { transform: rotate(-10deg); }
  50% { transform: rotate(10deg); }
}

@keyframes speechPop {
  0% { transform: scale(0); opacity: 0; }
  100% { transform: scale(1); opacity: 1; }
}
</style>

<style scoped>
.eco-mascot {
  position: fixed;
  bottom: 40px;
  right: 40px;
  z-index: 9999;
  pointer-events: none;
}

.mascot-container {
  position: relative;
  width: 140px;
  height: 160px;
  animation: mascotBounce 2s ease-in-out infinite;
}

.mascot-body {
  position: relative;
  width: 100%;
  height: 100%;
}

.mascot-body::before {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 80px;
  height: 90px;
  background: linear-gradient(180deg, #86efac 0%, #22c55e 100%);
  border-radius: 40px 40px 35px 35px;
  box-shadow: 0 8px 20px rgba(34, 197, 94, 0.3);
}

.mascot-head {
  position: absolute;
  top: 20px;
  left: 50%;
  transform: translateX(-50%);
  width: 70px;
  height: 70px;
  background: linear-gradient(180deg, #a7f3d0 0%, #4ade80 100%);
  border-radius: 50%;
  box-shadow: 0 4px 12px rgba(74, 222, 128, 0.4);
}

.mascot-eyes {
  position: absolute;
  top: 28px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 18px;
}

.eye {
  width: 10px;
  height: 14px;
  background: #1f2937;
  border-radius: 50%;
  animation: blink 4s ease-in-out infinite;
}

.mascot-smile {
  position: absolute;
  top: 48px;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 12px;
  border: 3px solid #1f2937;
  border-color: transparent transparent #1f2937 #1f2937;
  border-radius: 50%;
  transform: translateX(-50%) rotate(-45deg);
}

.mascot-leaf {
  position: absolute;
  top: 10px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  gap: 4px;
}

.leaf {
  font-size: 20px;
  animation: leafWave 2s ease-in-out infinite;
}

.leaf-1 {
  animation-delay: 0s;
}

.leaf-2 {
  animation-delay: 0.5s;
}

.mascot-speech {
  position: absolute;
  top: -20px;
  right: -20px;
  background: #ffffff;
  padding: 10px 16px;
  border-radius: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  animation: speechPop 0.5s ease-out;
}

.mascot-speech::before {
  content: '';
  position: absolute;
  bottom: -8px;
  left: 20px;
  width: 0;
  height: 0;
  border-left: 8px solid transparent;
  border-right: 8px solid transparent;
  border-top: 8px solid #ffffff;
}

.mascot-speech p {
  font-size: 13px;
  color: #065f46;
  margin: 0;
  font-weight: 600;
  white-space: nowrap;
  content: v-bind(speechText);
}

@media (max-width: 768px) {
  .eco-mascot {
    bottom: 10px;
    right: 10px;
  }
  .mascot-container {
    width: 100px;
    height: 120px;
  }
  .mascot-speech {
    display: none;
  }
}
</style>