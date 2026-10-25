// Administrator-only recovery. Never ship this script or administrator credentials in the APK.
// Uses a Firebase CLI login; see docs/RECOVERY.md. No mutation runs without `apply`.
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const crypto = require('node:crypto');

const project = 'counter-f0787';
const service = `projects/${project}/locations/europe-central2/services/counter-f0787-service`;
const endpoint = `https://firebasedataconnect.googleapis.com/v1/${service}`;
const fields = 'id ownerUid type occurredAt note deleted revision';
let token;

async function authenticate() {
  const root = process.env.COUNTER_FIREBASE_TOOLS || path.join(os.homedir(), '.cache/firebase/tools/lib/node_modules/firebase-tools');
  const auth = require(path.join(root, 'lib/auth.js'));
  const account = auth.getGlobalDefaultAccount();
  if (!account) throw new Error('Run firebase login with your project administrator account first.');
  auth.setActiveAccount({}, account);
  token = (await auth.getAccessToken(account.tokens.refresh_token, ['https://www.googleapis.com/auth/cloud-platform'])).access_token;
}

async function request(url, body) {
  const response = await fetch(url, {
    method: body === undefined ? 'GET' : 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
    ...(body === undefined ? {} : { body: JSON.stringify(body) }),
    signal: AbortSignal.timeout(60000),
  });
  if (!response.ok) throw new Error(`Administrator API request failed (${response.status}). Check login, project permissions, and configuration.`);
  const result = await response.json();
  if (result.errors?.length) throw new Error(`GraphQL failed: ${result.errors.map(e => e.message).join('; ')}`);
  return result;
}

async function listVisits(ownerUid) {
  const rows = [];
  for (let offset = 0; ; offset += 500) {
    const query = ownerUid
      ? `query RecoveryRows($owner: String!, $offset: Int!) { visits(where: {ownerUid: {eq: $owner}}, orderBy: {id: ASC}, limit: 500, offset: $offset) { ${fields} } }`
      : `query RecoveryOwners($offset: Int!) { visits(orderBy: [{ownerUid: ASC}, {id: ASC}], limit: 500, offset: $offset) { id ownerUid occurredAt deleted } }`;
    const result = await request(`${endpoint}:executeGraphqlRead`, { query, variables: { offset, ...(ownerUid ? { owner: ownerUid } : {}) } });
    if (!Array.isArray(result.data?.visits)) throw new Error('Cloud response did not contain visits.');
    rows.push(...result.data.visits);
    if (result.data.visits.length < 500) return rows;
  }
}

async function googleAccount(uid) {
  const result = await request(`https://identitytoolkit.googleapis.com/v1/projects/${project}/accounts:lookup`, { localId: [uid] });
  const user = result.users?.find(u => u.localId === uid);
  if (!user || user.disabled || !user.providerUserInfo?.some(p => p.providerId === 'google.com')) {
    throw new Error('Destination must be an enabled Firebase account linked to Google. Sign in in the app first.');
  }
  return { uid, email: user.email };
}

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])]));
  }
  return value;
}

function digest(rows) {
  return crypto.createHash('sha256').update(JSON.stringify(canonical(rows))).digest('hex');
}

function missingRows(source, destination, targetUid) {
  const existing = new Set(destination.map(v => v.id));
  // Existing target rows, including tombstones, always win. Originals are never modified.
  return source.filter(v => !v.deleted && !existing.has(v.id)).map(v => ({ ...v, ownerUid: targetUid }));
}

async function main(args) {
  const [command, from, to, output] = args;
  if (!['status', 'owners', 'backup', 'plan', 'apply'].includes(command)) {
    throw new Error('Usage: node scripts/recover-account.cjs status | owners | backup OLD_UID FILE | plan OLD_UID GOOGLE_UID PLAN_FILE | apply PLAN_FILE');
  }
  await authenticate();
  if (command === 'status') {
    try {
      const provider = await request(`https://identitytoolkit.googleapis.com/admin/v2/projects/${project}/defaultSupportedIdpConfigs/google.com`);
      console.log(JSON.stringify({ googleEnabled: !!provider.enabled, hasWebClientId: !!provider.clientId }));
    } catch (error) { console.log(error.message); process.exitCode = 1; }
  } else if (command === 'owners') {
    const owners = new Map();
    for (const row of await listVisits()) {
      const info = owners.get(row.ownerUid) || { ownerUid: row.ownerUid, active: 0, deleted: 0, first: row.occurredAt, last: row.occurredAt };
      info[row.deleted ? 'deleted' : 'active']++;
      if (row.occurredAt < info.first) info.first = row.occurredAt;
      if (row.occurredAt > info.last) info.last = row.occurredAt;
      owners.set(row.ownerUid, info);
    }
    console.table([...owners.values()]);
  } else if (command === 'backup') {
    if (!from || !to) throw new Error('Supply the source UID and a new private backup filename.');
    const source = await listVisits(from);
    if (!source.length) throw new Error('Source UID has no SQL records.');
    fs.writeFileSync(to, JSON.stringify({ version: 1, project, service, sourceUid: from, source, sourceDigest: digest(source) }, null, 2), { flag: 'wx', mode: 0o600 });
    console.log(`Saved ${source.length} cloud rows to the private backup. No cloud data changed.`);
  } else if (command === 'plan') {
    if (!from || !to || !output || from === to) throw new Error('Supply different source/destination UIDs and a new private plan filename.');
    const account = await googleAccount(to);
    const source = await listVisits(from);
    if (!source.length) throw new Error('Source UID has no SQL records.');
    const destination = await listVisits(to);
    const plan = { version: 1, project, service, sourceUid: from, destination: account, createdAt: new Date().toISOString(), source, sourceDigest: digest(source) };
    fs.writeFileSync(output, JSON.stringify(plan, null, 2), { flag: 'wx', mode: 0o600 });
    console.log(`Plan saved. Copy ${missingRows(source, destination, to).length} missing active visits to ${account.email}. Original records and existing destination records will be preserved.`);
  } else {
    if (!from) throw new Error('Supply the reviewed private plan filename.');
    const plan = JSON.parse(fs.readFileSync(from, 'utf8'));
    if (plan.version !== 1 || plan.project !== project || plan.service !== service || plan.sourceUid === plan.destination?.uid || digest(plan.source) !== plan.sourceDigest) {
      throw new Error('Invalid recovery plan. Generate a fresh plan.');
    }
    const account = await googleAccount(plan.destination.uid);
    if (account.email !== plan.destination.email) throw new Error('Destination account changed. Generate a fresh plan.');
    const source = await listVisits(plan.sourceUid);
    if (digest(source) !== plan.sourceDigest) throw new Error('Source records changed after planning. Generate a fresh plan.');
    const destination = await listVisits(account.uid);
    const rows = missingRows(source, destination, account.uid);
    for (let index = 0; index < rows.length; index += 100) {
      await request(`${endpoint}:executeGraphql`, {
        query: 'mutation RecoverVisits($rows: [Visit_Data!]!) @transaction { visit_insertMany(data: $rows) }',
        variables: { rows: rows.slice(index, index + 100) },
      });
    }
    const restored = await listVisits(account.uid);
    const ids = new Set(restored.map(v => v.id));
    if (rows.some(v => !ids.has(v.id))) throw new Error('Post-copy verification failed. Keep the plan and inspect the database.');
    console.log(`Verified ${rows.length} copied visits for ${account.email}. Tap Restore from cloud in the app. Rerunning this plan skips existing IDs.`);
  }
}

module.exports = { missingRows, digest, authenticate, request };
if (require.main === module) main(process.argv.slice(2)).catch(error => { console.error(error.message); process.exitCode = 1; });
