<template>
  <view class="page">
    <scroll-view class="chat" scroll-y :scroll-into-view="anchor" scroll-with-animation>
      <view v-if="!messages.length" class="welcome">
        <view class="w-title">你好，我是迎新数字人</view>
        <view class="w-sub">可以点下面的热门问题，也可以直接输入你的问题</view>
        <view class="chips">
          <view v-for="h in hot" :key="h.question" class="chip" @click="sendText(h.question)">
            {{ h.question }}
          </view>
        </view>
      </view>

      <view v-for="m in messages" :id="'m' + m.id" :key="m.id" class="msg" :class="m.role">
        <view class="bubble">{{ m.content }}</view>
      </view>

      <view v-if="suggestions.length" class="suggest">
        <view class="s-title">你可能想问</view>
        <view v-for="s in suggestions" :key="s.questionId" class="chip" @click="sendText(s.question)">
          {{ s.question }}
        </view>
      </view>

      <view id="anchor-bottom" class="anchor" />
    </scroll-view>

    <view class="input-bar">
      <input
        v-model="text"
        class="input"
        placeholder="输入问题，例如：宿舍在哪里"
        confirm-type="send"
        :disabled="sending"
        @confirm="send"
      />
      <button class="send" type="primary" size="mini" :loading="sending" @click="send">发送</button>
    </view>
  </view>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { createQaSession, askQuestion, hotQuestions } from '@/api/qa'
import { QA_SESSION_KEY } from '@/api/request'
import { requireLogin } from '@/composables/requireLogin'

const hot = ref([])
const messages = ref([])
const suggestions = ref([])
const text = ref('')
const sending = ref(false)
const anchor = ref('')
let seq = 0

onLoad(async () => {
  if (!requireLogin({ relaunch: true })) return
  // 会话创建与热门问题都可能失败，不能让异常冒到生命周期外（否则页面白屏且无提示）
  try {
    await ensureSession()
  } catch (e) {
    uni.showToast({ title: '会话初始化失败，请重新进入', icon: 'none' })
  }
  await loadHot()
})

async function ensureSession() {
  let id = uni.getStorageSync(QA_SESSION_KEY)
  if (!id) {
    const data = await createQaSession()
    id = data.sessionId
    uni.setStorageSync(QA_SESSION_KEY, id)
  }
  return id
}

async function loadHot() {
  try {
    hot.value = (await hotQuestions()) || []
  } catch (e) {
    hot.value = []
  }
}

function scrollToBottom() {
  anchor.value = ''
  nextTick(() => {
    anchor.value = 'anchor-bottom'
  })
}

function sendText(q) {
  text.value = q
  send()
}

async function send() {
  const question = text.value.trim()
  if (!question || sending.value) return
  messages.value.push({ id: ++seq, role: 'user', content: question })
  text.value = ''
  suggestions.value = []
  sending.value = true
  scrollToBottom()

  try {
    const sessionId = await ensureSession()
    const res = await askQuestion({ sessionId, question })
    messages.value.push({ id: ++seq, role: 'bot', content: res.answer })
    suggestions.value = res.suggestions || []
  } catch (e) {
    // 会话可能已在服务端失效：清掉本地 id，下次提问会重新建会话
    uni.removeStorageSync(QA_SESSION_KEY)
    messages.value.push({ id: ++seq, role: 'bot', content: '这个我没能回答，请再问一次或换个说法。' })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  /* uni-app 提供的窗口高度变量，自动扣掉导航栏与 tabBar */
  height: calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px));
  background: #f5f7fa;
}
.chat {
  flex: 1;
  padding: 24rpx;
  box-sizing: border-box;
}
.welcome {
  background: #fff;
  border-radius: 16rpx;
  padding: 32rpx;
}
.w-title {
  font-size: 34rpx;
  font-weight: bold;
  color: #3a8ee6;
}
.w-sub {
  margin-top: 12rpx;
  color: #909399;
  font-size: 24rpx;
}
.chips {
  margin-top: 24rpx;
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.chip {
  font-size: 24rpx;
  color: #3a8ee6;
  background: #ecf5ff;
  border-radius: 28rpx;
  padding: 12rpx 24rpx;
}
.msg {
  display: flex;
  margin-bottom: 20rpx;
}
.msg.user {
  justify-content: flex-end;
}
.msg.bot {
  justify-content: flex-start;
}
.bubble {
  max-width: 78%;
  padding: 20rpx 24rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
  line-height: 1.6;
  word-break: break-word;
}
.msg.user .bubble {
  background: #3a8ee6;
  color: #fff;
  border-bottom-right-radius: 4rpx;
}
.msg.bot .bubble {
  background: #fff;
  color: #303133;
  border-bottom-left-radius: 4rpx;
}
.suggest {
  background: #fff;
  border-radius: 16rpx;
  padding: 20rpx 24rpx;
  margin-top: 8rpx;
}
.s-title {
  color: #909399;
  font-size: 22rpx;
  margin-bottom: 14rpx;
}
.anchor {
  height: 20rpx;
}
.input-bar {
  display: flex;
  align-items: center;
  padding: 16rpx 24rpx;
  background: #fff;
  border-top: 1rpx solid #ebeef5;
}
.input {
  flex: 1;
  height: 76rpx;
  padding: 0 20rpx;
  background: #f5f7fa;
  border-radius: 12rpx;
  font-size: 28rpx;
}
.send {
  margin-left: 16rpx;
}
</style>
