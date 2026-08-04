export const INITIAL_STATE = Object.freeze({
  state: "UNKNOWN",
  failureStreak: 0,
  successStreak: 0,
  incidentId: null,
  downNotified: false,
  availableNotified: false
});

export function advanceState(previous, available, createIncidentId) {
  const next = { ...INITIAL_STATE, ...previous };
  const effects = [];

  if (available) {
    next.successStreak += 1;
    next.failureStreak = 0;

    if (next.state !== "UP" && next.successStreak >= 2) {
      next.state = "UP";
      next.incidentId ??= createIncidentId();
      next.availableNotified = false;
    }
    if (next.state === "UP" && !next.availableNotified) {
      effects.push({ type: "RECOVERED", incidentId: next.incidentId });
    }
  } else {
    next.failureStreak += 1;
    next.successStreak = 0;

    if (next.state !== "DOWN" && next.failureStreak >= 2) {
      next.state = "DOWN";
      next.incidentId = createIncidentId();
      next.downNotified = false;
      next.availableNotified = false;
    }
    if (next.state === "DOWN" && !next.downNotified) {
      effects.push({ type: "DOWN", incidentId: next.incidentId });
    }
  }

  return { next, effects };
}

export function markEffectDelivered(state, effect) {
  if (effect.type === "DOWN") {
    return { ...state, downNotified: true };
  }
  return { ...state, availableNotified: true };
}
