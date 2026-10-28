import { TasksStore } from "@/lib/tasksStore";
import type { TaskView } from "@/types";

function makeTask(id: string, status: TaskView["status"]): TaskView {
  return {
    id,
    templateId: "tpl-" + id,
    templateTitle: "模板-" + id,
    status,
    currentPhase: status === "PROCESSING" ? "GENERATING" : null,
    currentPhaseDisplay: status === "PROCESSING" ? "模拟生成" : null,
    failPhase: status === "FAILED" ? "GENERATING" : null,
    failReason: status === "FAILED" ? "模拟失败：步骤[模拟生成]被注入失败" : null,
    resultUrl: status === "SUCCESS" ? "/files/results/" + id + ".png" : null,
    createdAt: "2026-10-28T10:00:00",
    updatedAt: null,
    finishedAt: null,
    steps: [],
  };
}

/**
 * 轮询节奏说明：startTracking 先刷新一次，之后每 intervalMs 触发一轮。
 * advanceTimersByTimeAsync 会在定时器之间冲刷微任务，
 * 因此推进一个周期即可观察到“初始刷新 + 一轮轮询”的结果。
 */
describe("TasksStore", () => {
  beforeEach(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  test("多个任务状态互不串扰：只更新服务端返回的各自状态", async () => {
    const fetchTasks = jest
      .fn()
      .mockResolvedValueOnce([makeTask("t1", "PROCESSING"), makeTask("t2", "PROCESSING")])
      .mockResolvedValue([makeTask("t1", "SUCCESS"), makeTask("t2", "PROCESSING")]);
    const store = new TasksStore(fetchTasks, 1000);

    store.startTracking();
    await jest.advanceTimersByTimeAsync(2000);

    const snapshot = store.getSnapshot();
    const t1 = snapshot.tasks.find((t) => t.id === "t1")!;
    const t2 = snapshot.tasks.find((t) => t.id === "t2")!;
    // t1 已成功、t2 仍在处理中，互不影响
    expect(t1.status).toBe("SUCCESS");
    expect(t2.status).toBe("PROCESSING");
    expect(t1.failReason).toBeNull();
    store.stopTracking();
  });

  test("查询接口异常 ≠ 任务失败：保留已有任务信息且不改写任务状态", async () => {
    const fetchTasks = jest
      .fn()
      .mockResolvedValueOnce([makeTask("t1", "PROCESSING"), makeTask("t2", "FAILED")])
      .mockRejectedValueOnce(new Error("network down"))
      .mockResolvedValue([makeTask("t1", "SUCCESS"), makeTask("t2", "FAILED")]);
    const store = new TasksStore(fetchTasks, 1000);

    store.startTracking();
    // 推进一个周期 = 初始刷新 + 第一轮轮询（第 1000ms 的 tick 被包含在内）
    await jest.advanceTimersByTimeAsync(1000);
    const snapshot = store.getSnapshot();
    // 第一轮查询失败：connectionError 置位，但任务信息保留
    expect(snapshot.connectionError).toBe(true);
    expect(snapshot.tasks).toHaveLength(2);
    expect(snapshot.tasks[0].status).toBe("PROCESSING");
    expect(snapshot.tasks[1].status).toBe("FAILED");

    await jest.advanceTimersByTimeAsync(1000); // 查询恢复
    const recovered = store.getSnapshot();
    expect(recovered.connectionError).toBe(false);
    expect(recovered.tasks[0].status).toBe("SUCCESS");
    store.stopTracking();
  });

  test("存在未完成任务时轮询，全部结束后自动停止", async () => {
    const fetchTasks = jest
      .fn()
      .mockResolvedValueOnce([makeTask("t1", "PROCESSING")])
      .mockResolvedValue([makeTask("t1", "SUCCESS")]);
    const store = new TasksStore(fetchTasks, 1000);

    store.startTracking();
    // 一个周期后：任务已 SUCCESS → 轮询自动停止
    await jest.advanceTimersByTimeAsync(1000);
    expect(store.getSnapshot().tasks[0].status).toBe("SUCCESS");
    const callsAfterFinish = fetchTasks.mock.calls.length;

    await jest.advanceTimersByTimeAsync(5000); // 时间继续前进，不再查询
    expect(fetchTasks.mock.calls.length).toBe(callsAfterFinish);
  });

  test("没有待处理任务时 startTracking 不启动轮询", async () => {
    const fetchTasks = jest.fn().mockResolvedValue([makeTask("t1", "SUCCESS")]);
    const store = new TasksStore(fetchTasks, 1000);

    store.startTracking();
    await jest.advanceTimersByTimeAsync(1000);

    // 仅 startTracking 时刷新一次，之后不再轮询
    await jest.advanceTimersByTimeAsync(5000);
    expect(fetchTasks).toHaveBeenCalledTimes(1);
  });

  test("reset 清空数据并停止轮询（切换用户场景）", async () => {
    const fetchTasks = jest
      .fn()
      .mockResolvedValueOnce([makeTask("t1", "PROCESSING")])
      .mockResolvedValue([makeTask("t1", "PROCESSING")]);
    const store = new TasksStore(fetchTasks, 1000);

    store.startTracking();
    await jest.advanceTimersByTimeAsync(1000);
    expect(store.getSnapshot().tasks).toHaveLength(1);

    store.reset();
    expect(store.getSnapshot().tasks).toHaveLength(0);
    expect(store.getSnapshot().connectionError).toBe(false);

    const callsAtReset = fetchTasks.mock.calls.length;
    await jest.advanceTimersByTimeAsync(5000);
    // reset 后轮询已停止，不再发查询；过期结果也不会覆盖空状态
    expect(fetchTasks.mock.calls.length).toBe(callsAtReset);
    expect(store.getSnapshot().tasks).toHaveLength(0);
  });

  test("订阅通知：refresh 后监听器收到变更", async () => {
    const fetchTasks = jest.fn().mockResolvedValue([makeTask("t1", "SUCCESS")]);
    const store = new TasksStore(fetchTasks, 1000);
    const listener = jest.fn();
    store.subscribe(listener);

    await store.refresh();
    expect(listener).toHaveBeenCalled();
  });
});
