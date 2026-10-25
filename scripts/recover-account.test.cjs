const { test } = require('node:test');
const assert = require('node:assert/strict');
const { missingRows, digest } = require('./recover-account.cjs');

test('snapshot digest ignores JSON field order but detects changed visit values', () => {
  const original = [{ id: 'a', note: 'saved', revision: '2', deleted: false }];
  assert.equal(digest(original), digest([{ deleted: false, revision: '2', note: 'saved', id: 'a' }]));
  assert.notEqual(digest(original), digest([{ ...original[0], note: 'edited' }]));
  assert.notEqual(digest(original), digest([{ ...original[0], deleted: true }]));
  assert.notEqual(digest(original), digest([]));
});

test('copy preserves originals and skips deletions, duplicate IDs, and target tombstones', () => {
  const source = [
    { id: 'new', ownerUid: 'old', deleted: false, revision: '7', note: 'preserve' },
    { id: 'deleted', ownerUid: 'old', deleted: true },
    { id: 'existing', ownerUid: 'old', deleted: false },
    { id: 'target-deleted', ownerUid: 'old', deleted: false },
  ];
  const target = [{ id: 'existing' }, { id: 'target-deleted', deleted: true }];
  const before = structuredClone(source);
  const copied = missingRows(source, target, 'google');
  assert.deepEqual(copied, [{ id: 'new', ownerUid: 'google', deleted: false, revision: '7', note: 'preserve' }]);
  assert.deepEqual(source, before);
  assert.deepEqual(missingRows(source, [...target, ...copied], 'google'), []);
});
