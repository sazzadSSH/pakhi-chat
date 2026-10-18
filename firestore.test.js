const { test, before, after } = require('node:test');
const assert = require('node:assert');
const fs = require('node:fs');
const http = require('node:http');

test('Firestore rules file exists and is non-empty', () => {
  const content = fs.readFileSync('firestore.rules', 'utf8');
  assert.ok(content.includes('rules_version = \'2\';'));
  assert.ok(content.includes('match /users/{userId}'));
  assert.ok(content.includes('match /public_profiles/{pakhiId}'));
  assert.ok(content.includes('match /chats/{chatId}'));
});
