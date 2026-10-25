// Configures the existing Android app using Firebase CLI's official provisioning API.
const fs = require('node:fs');
const path = require('node:path');
const os = require('node:os');
const { authenticate, request } = require('./recover-account.cjs');

async function main() {
  const root = process.env.COUNTER_FIREBASE_TOOLS || path.join(os.homedir(), '.cache/firebase/tools/lib/node_modules/firebase-tools');
  const auth = require(path.join(root, 'lib/auth.js'));
  const { provisionFirebaseApp } = require(path.join(root, 'lib/management/provisioning/provision.js'));
  const { AppPlatform } = require(path.join(root, 'lib/management/apps.js'));
  await authenticate();
  const supportEmail = auth.getGlobalDefaultAccount().user.email;
  const appId = '1:863793754472:android:e132c286f1a186effae6dd';
  const options = {
    project: { parent: { type: 'existing_project', projectId: 'counter-f0787' } },
    app: {
      platform: AppPlatform.ANDROID, appId,
      sha1Hashes: ['06ACA7725D68C9C396ED2495767A9BAFBAFB30F0'],
      sha256Hashes: ['5EBC79221646C69F7F471ADF1B4CA59F7BCAB4FD07B95440E21F0F645D1E9FED'],
    },
    features: { firebaseAuthInput: {
      anonymousAuthProviderMode: 'PROVIDER_ENABLED',
      googleSigninProviderMode: 'PROVIDER_ENABLED',
      googleSigninProviderConfig: { publicDisplayName: 'counter', customerSupportEmail: supportEmail },
    } },
  };
  console.log(JSON.stringify(options, null, 2));
  if (!process.argv.includes('--apply')) {
    console.log('Preview only. Pass --apply to configure Google sign-in and download updated Android configuration.');
    return;
  }
  await provisionFirebaseApp(options);
  const shaUrl = `https://firebase.googleapis.com/v1beta1/projects/counter-f0787/androidApps/${appId}/sha`;
  const existing = await request(shaUrl);
  for (const [certType, hashes] of [['SHA_1', options.app.sha1Hashes], ['SHA_256', options.app.sha256Hashes]]) {
    for (const shaHash of hashes) {
      if (!existing.certificates?.some(c => c.shaHash.replaceAll(':', '').toUpperCase() === shaHash)) {
        await request(shaUrl, { certType, shaHash });
      }
    }
  }
  const result = await request(`https://firebase.googleapis.com/v1beta1/projects/counter-f0787/androidApps/${appId}/config`);
  const config = JSON.parse(Buffer.from(result.configFileContents, 'base64').toString('utf8'));
  if (!config.client?.some(c => c.oauth_client?.some(o => o.client_type === 3) && c.oauth_client?.some(o => o.client_type === 1))) {
    throw new Error('Google OAuth client not present yet. Download a fresh google-services.json from Firebase before building.');
  }
  fs.writeFileSync(path.join(__dirname, '../app/google-services.json'), JSON.stringify(config, null, 2) + '\n');
  console.log('Google sign-in configured and Android configuration updated.');
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
