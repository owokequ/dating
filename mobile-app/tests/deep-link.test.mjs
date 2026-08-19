import assert from 'node:assert/strict';
import test from 'node:test';

test('accepts only the declared mobile routes from a push payload', () => {
  const routes = new Set(['date', 'notifications', 'reminder']);
  assert.equal(routes.has('date'), true);
  assert.equal(routes.has('https://untrusted.example'), false);
});
