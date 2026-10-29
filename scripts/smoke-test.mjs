#!/usr/bin/env node
/**
 * 端到端冒烟测试：登录 → 上传素材 → 提交任务 → 轮询直到结束 → 校验失败注入
 * 用法：node scripts/smoke-test.mjs [后端地址] [失败注入阶段]
 * 默认：node scripts/smoke-test.mjs http://localhost:8080
 */
const BASE = process.argv[2] || "http://localhost:8080";
const FAIL_PHASE = process.argv[3] || "";

// 1×1 透明 PNG
const PNG = Buffer.from(
  "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==",
  "base64"
);

function check(cond, label) {
  if (!cond) {
    console.error(`❌ ${label}`);
    process.exit(1);
  }
  console.log(`✅ ${label}`);
}

async function json(res, label) {
  if (!res.ok) {
    console.error(`❌ ${label}: HTTP ${res.status} ${await res.text()}`);
    process.exit(1);
  }
  return res.json();
}

async function pollTask(token, taskId, maxSeconds = 60) {
  for (let i = 1; i <= maxSeconds; i++) {
    const task = await json(
      await fetch(`${BASE}/api/tasks/${taskId}`, { headers: { "X-Auth-Token": token } }),
      "查询任务"
    );
    console.log(`  [${i}] status=${task.status} phase=${task.currentPhaseDisplay ?? "—"}`);
    if (task.status === "SUCCESS" || task.status === "FAILED") return task;
    await new Promise((r) => setTimeout(r, 1000));
  }
  console.error("❌ 任务超时未结束");
  process.exit(1);
}

async function main() {
  console.log(`目标后端: ${BASE}`);

  console.log("== 1. 模板列表 ==");
  const templates = await json(await fetch(`${BASE}/api/templates`), "模板列表");
  check(Array.isArray(templates) && templates.length > 0, "模板列表非空");
  const template = templates[0];
  console.log(`模板: ${template.id}，需要 ${template.requiredImageCount} 张素材`);

  console.log("== 2. 登录 ==");
  const login = await json(
    await fetch(`${BASE}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: "smoke-tester" }),
    }),
    "登录"
  );
  check(Boolean(login.token), `登录成功 token=${login.token.slice(0, 8)}…`);
  const auth = { "X-Auth-Token": login.token, "Content-Type": "application/json" };

  console.log("== 3. 上传素材 ==");
  const materialIds = [];
  for (let i = 0; i < template.requiredImageCount; i++) {
    const form = new FormData();
    form.append("templateId", template.id);
    form.append("file", new Blob([PNG], { type: "image/png" }), `pixel${i}.png`);
    const material = await json(
      await fetch(`${BASE}/api/materials`, {
        method: "POST",
        headers: { "X-Auth-Token": login.token },
        body: form,
      }),
      "上传素材"
    );
    materialIds.push(material.id);
    console.log(`  素材 ${i + 1}: id=${material.id}`);
  }

  console.log("== 4. 提交任务 ==");
  const submit = await json(
    await fetch(`${BASE}/api/tasks`, {
      method: "POST",
      headers: auth,
      body: JSON.stringify({ templateId: template.id, materialIds }),
    }),
    "提交任务"
  );
  check(submit.status === "WAITING", `任务受理立即返回: ${submit.taskId} (${submit.status})`);

  console.log("== 5. 轮询任务状态 ==");
  const task = await pollTask(login.token, submit.taskId);
  check(task.status === "SUCCESS", "任务最终成功");
  const img = await fetch(`${BASE}${task.resultUrl}`);
  check(img.ok, `结果可访问: ${task.resultUrl}`);
  await img.arrayBuffer(); // 完整读取响应体，避免中断下载在服务端产生连接中断噪音

  if (FAIL_PHASE) {
    console.log(`== 6. 失败注入验证（阶段：${FAIL_PHASE}）==`);
    await json(
      await fetch(`${BASE}/api/dev/simulation`, {
        method: "POST",
        headers: auth,
        body: JSON.stringify({ failPhase: FAIL_PHASE }),
      }),
      "设置失败注入"
    );
    const submit2 = await json(
      await fetch(`${BASE}/api/tasks`, {
        method: "POST",
        headers: auth,
        body: JSON.stringify({ templateId: template.id, materialIds }),
      }),
      "提交第二个任务"
    );
    const failed = await pollTask(login.token, submit2.taskId);
    check(failed.status === "FAILED", "注入后任务失败");
    check(failed.failPhase === FAIL_PHASE && Boolean(failed.failReason), `失败原因: ${failed.failReason}`);
    // 还原
    await fetch(`${BASE}/api/dev/simulation`, {
      method: "POST",
      headers: auth,
      body: JSON.stringify({ failPhase: "" }),
    });
  }

  console.log("== 全部通过 ==");
}

main().catch((e) => {
  console.error(`❌ 冒烟异常: ${e.message}`);
  process.exit(1);
});
