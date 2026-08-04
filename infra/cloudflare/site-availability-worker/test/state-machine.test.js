import assert from "node:assert/strict";
import test from "node:test";

import { INITIAL_STATE, advanceState, markEffectDelivered } from "../src/state-machine.js";

test("initial availability is announced only after two successful checks", () => {
  const first = advanceState(INITIAL_STATE, true, () => "first");
  assert.equal(first.effects.length, 0);

  const second = advanceState(first.next, true, () => "first");
  assert.deepEqual(second.effects, [{ type: "RECOVERED", incidentId: "first" }]);
});

test("one failed check does not notify, but the second starts one incident", () => {
  const up = { ...INITIAL_STATE, state: "UP", availableNotified: true, incidentId: "old" };
  const first = advanceState(up, false, () => "new");
  assert.equal(first.effects.length, 0);

  const second = advanceState(first.next, false, () => "new");
  assert.deepEqual(second.effects, [{ type: "DOWN", incidentId: "new" }]);
  assert.equal(markEffectDelivered(second.next, second.effects[0]).downNotified, true);
});

test("recovery retains the same incident id and is delivered once", () => {
  const down = {
    ...INITIAL_STATE,
    state: "DOWN",
    incidentId: "incident-1",
    downNotified: true
  };
  const first = advanceState(down, true, () => "unexpected");
  const second = advanceState(first.next, true, () => "unexpected");
  assert.deepEqual(second.effects, [{ type: "RECOVERED", incidentId: "incident-1" }]);

  const delivered = markEffectDelivered(second.next, second.effects[0]);
  assert.equal(advanceState(delivered, true, () => "unexpected").effects.length, 0);
});
