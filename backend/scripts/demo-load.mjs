#!/usr/bin/env node
/**
 * 演示互动脚本：模拟一批新生通过网关登录、报到打卡、提问，
 * 产生真实 MQ 事件（事件 worker 攒批落库 → 数据看板刷新）。
 *
 * 用法：node scripts/demo-load.mjs [人数] （默认 12，通过 http://localhost:8080）
 */

const GATEWAY = process.env.GATEWAY || 'http://localhost:8080';
const COUNT = Number(process.argv[2] || 12);
const PASSWORD = '123456';

const QUESTIONS = [
  '报到需要带哪些材料？',
  '宿舍在哪里？',
  '学费怎么交？',
  '军训什么时候开始？',
  '校园卡在哪里办？',
  '快递地址怎么填？',
  '绿色通道怎么申请？',
  '宿舍有热水和空调吗？',
];

const STEPS = ['PRE_REG', 'PAY', 'DORM', 'ARCHIVE', 'CERT'];

async function api(path, { method = 'GET', token, body } = {}) {
  const res = await fetch(GATEWAY + path, {
    method,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: body ? JSON.stringify(body) : undefined,
  });
  const json = await res.json().catch(() => ({}));
  return { status: res.status, json };
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

async function simulateOne(index) {
  const studentNo = `ST2026${String(index + 1).padStart(3, '0')}`;
  const tag = studentNo;

  const login = await api('/api/auth/login', {
    method: 'POST',
    body: { username: studentNo, password: PASSWORD, loginType: 1 },
  });
  if (login.json.code !== 0) {
    console.log(`${tag} 登录失败: ${login.json.message}`);
    return;
  }
  const token = login.json.data.accessToken;
  console.log(`${tag} 登录成功 role=${login.json.data.role}`);

  // 报到打卡 2-4 步
  const steps = STEPS.slice(0, 2 + (index % 3));
  for (const stepCode of steps) {
    const checkin = await api(`/api/checkin/steps/${stepCode}/checkin`, { method: 'POST', token });
    if (checkin.json.code === 0) {
      console.log(`${tag} 打卡 ${stepCode} duplicated=${checkin.json.data?.isDuplicated}`);
    }
    await sleep(80);
  }

  // 提问 1-2 次
  const askCount = 1 + (index % 2);
  for (let i = 0; i < askCount; i++) {
    const session = await api('/api/qa/sessions', { method: 'POST', token });
    if (session.json.code !== 0) continue;
    const sessionId = session.json.data.sessionId;
    const question = QUESTIONS[(index + i) % QUESTIONS.length];
    const answer = await api('/api/qa/ask', {
      method: 'POST',
      token,
      body: { sessionId, question },
    });
    if (answer.json.code === 0) {
      console.log(`${tag} 问「${question}」hitType=${answer.json.data.hitType} ${answer.json.data.costMs}ms`);
    }
    await sleep(120);
  }
}

async function main() {
  console.log(`演示互动开始：${COUNT} 名学生 → ${GATEWAY}`);
  const workers = 4;
  let next = 0;
  await Promise.all(
    Array.from({ length: workers }, async () => {
      while (next < COUNT) {
        const index = next++;
        await simulateOne(index);
      }
    }),
  );
  console.log('演示互动结束，事件已进入消息队列，几秒后看板自动刷新。');
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
