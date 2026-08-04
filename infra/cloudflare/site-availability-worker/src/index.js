import { INITIAL_STATE, advanceState, markEffectDelivered } from "./state-machine.js";

const CHECK_TIMEOUT_MS = 15_000;
const MONITOR_PATH = "https://availability-monitor.internal/check";

export default {
  async fetch() {
    return Response.json({ status: "ok", monitor: "For my L availability" });
  },

  async scheduled(_controller, env, ctx) {
    ctx.waitUntil(runCheck(env));
  }
};

export class AvailabilityMonitor {
  constructor(ctx, env) {
    this.ctx = ctx;
    this.env = env;
  }

  async fetch(request) {
    if (request.method !== "POST" || new URL(request.url).pathname !== "/check") {
      return new Response("Not found", { status: 404 });
    }

    const { available } = await request.json();
    if (typeof available !== "boolean") {
      return new Response("Invalid availability payload", { status: 400 });
    }

    let state = (await this.ctx.storage.get("state")) ?? INITIAL_STATE;
    const transition = advanceState(state, available, () => crypto.randomUUID());
    state = transition.next;
    await this.ctx.storage.put("state", state);

    for (const effect of transition.effects) {
      try {
        if (effect.type === "DOWN") {
          await sendOwnerDownNotice(this.env);
        } else {
          await sendRecoveryToOwoke(this.env, effect.incidentId);
        }
        state = markEffectDelivered(state, effect);
        await this.ctx.storage.put("state", state);
      } catch (error) {
        console.error("Availability notification delivery failed", error);
      }
    }

    return Response.json({ state: state.state });
  }
}

async function runCheck(env) {
  const [frontend, api] = await Promise.all([
    fetchWithTimeout(env.FRONTEND_URL),
    fetchWithTimeout(env.API_AVAILABILITY_URL, true)
  ]);
  const available = frontend && api;
  const id = env.AVAILABILITY_MONITOR.idFromName("global");
  const stub = env.AVAILABILITY_MONITOR.get(id);
  await stub.fetch(MONITOR_PATH, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ available })
  });
}

async function fetchWithTimeout(url, requireGatewayStatus = false) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), CHECK_TIMEOUT_MS);
  try {
    const response = await fetch(url, { signal: controller.signal, redirect: "follow" });
    if (!response.ok) return false;
    if (!requireGatewayStatus) return true;
    const body = await response.json();
    return body?.status === "UP";
  } catch (error) {
    console.warn("Availability check failed", { url, error: String(error) });
    return false;
  } finally {
    clearTimeout(timeout);
  }
}

async function sendOwnerDownNotice(env) {
  const response = await fetch(`https://api.telegram.org/bot${env.TELEGRAM_BOT_TOKEN}/sendMessage`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      chat_id: env.OWNER_TELEGRAM_CHAT_ID,
      text: "⚠️ For my L временно недоступен.\n\nЯ уже заметил проблему и напишу, когда сайт снова станет доступен."
    })
  });
  if (!response.ok) throw new Error(`Telegram returned ${response.status}`);
}

async function sendRecoveryToOwoke(env, incidentId) {
  const response = await fetch(env.RECOVERY_WEBHOOK_URL, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      "X-Site-Availability-Secret": env.RECOVERY_WEBHOOK_SECRET
    },
    body: JSON.stringify({ incidentId })
  });
  if (!response.ok) throw new Error(`Owoke recovery webhook returned ${response.status}`);
}
