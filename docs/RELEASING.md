# Releasing

Releases are automated with GitHub Actions. Running the **Create Release** workflow will:

1. Pick the version: the one you type, or the next one computed from the commit messages
2. Update `CHANGELOG.md`, commit it and tag the release (e.g. `v1.2.0`)
3. Create the GitHub Release with the changelog as its notes
4. Run the unit tests and build a signed app bundle (`.aab`) and a signed APK
5. Upload the bundle to Google Play (production track by default) and, in parallel,
   attach the APK to the GitHub Release for sideloading

The tag `v1.2.3` becomes `versionName` 1.2.3 and `versionCode` 10203, so you never edit
version numbers by hand.

Computing the version relies on commit messages following Conventional Commits, see
[CONTRIBUTING.md](../CONTRIBUTING.md).

## One-time setup

### 1. Confirm your keystore passwords

The upload keystore (`DerDieDasKeyStore`, key alias `key0`) has two passwords: one for the
keystore file and one for the key inside it. Android Studio's wizard often uses the same
password for both, so try that first.

To check them locally, fill in `keystore.properties` in the project root (it is gitignored):

```properties
storeFile=C:/Users/okano/Desktop/DerDieDasKeyStore
storePassword=...
keyAlias=key0
keyPassword=...
```

Then run:

```bash
./gradlew bundleRelease
```

- `Keystore was tampered with, or password was incorrect` means `storePassword` is wrong
- `Cannot recover key` means `keyPassword` is wrong
- `BUILD SUCCESSFUL` means both are right

If you can't recover them: with Play App Signing enabled (Play Console > Test and release >
App integrity > App signing), you can request an **upload key reset**. You generate a new
upload key and Google switches to it after a short waiting period. The key Google uses to sign
the app for users does not change.

### 2. Create a Google Play service account

This lets GitHub upload to Play Console on your behalf.

1. Open [Google Cloud Console](https://console.cloud.google.com/) and create or select a project
2. Enable the **Google Play Android Developer API** (APIs & Services > Library)
3. Go to IAM & Admin > Service accounts > **Create service account**. It needs no Cloud roles
4. Open the new service account > Keys > Add key > **Create new key** > JSON. A `.json` file downloads
5. In [Play Console](https://play.google.com/console), go to **Users and permissions** > Invite new users
6. Paste the service account email (it looks like `name@project.iam.gserviceaccount.com`)
7. Under App permissions, add Der Die Das and grant **Release apps to testing tracks**
   (also grant **Release to production** if you want to publish to production from CI)
8. Send the invite. Permissions can take a while to become active

### 3. Add the GitHub secrets

In the GitHub repository, go to Settings > Secrets and variables > Actions > **New repository secret**
and add:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | The keystore file, base64 encoded (see below) |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore password |
| `ANDROID_KEY_ALIAS` | `key0` |
| `ANDROID_KEY_PASSWORD` | Key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | Full contents of the service account `.json` file |

To copy the base64-encoded keystore to the clipboard, run this in Git Bash:

```bash
base64 -w 0 ~/Desktop/DerDieDasKeyStore | clip
```

Then paste it as the secret value. Afterwards, delete the downloaded service account `.json`
file or keep it somewhere safe: it grants access to your Play Console.

### 4. Choose the Play track (optional)

By default, releases go to **production**, fully rolled out once Google approves the review.
The service account needs the **Release to production** permission for this. To release to a
testing track instead, add a repository **variable** (Settings > Secrets and variables > Actions >
Variables) named `PLAY_TRACK` with the value `internal`, `alpha` or `beta`.

## Making a release

1. Open the repository's **Actions** tab and select **Create Release**
2. Click **Run workflow** (on the `main` branch)
3. Leave **Version** empty to compute it from the commits, or type one such as `1.3.0`
4. Click **Run workflow** and watch the progress

The computed version follows the commits since the last release:

| Commits since the last release | Next version (from 1.2.0) |
| --- | --- |
| Any `feat!:` or `BREAKING CHANGE:` | 2.0.0 |
| Any `feat:` | 1.3.0 |
| Any `fix:` or `perf:` | 1.2.1 |
| Only `ci:`, `docs:`, `chore:`, `test:`, ... | No release: the run ends with "nothing to release" |

A typed version always creates a release (even with only `ci:` or `docs:` commits), and must be
higher than the latest release. When the workflow finishes, the
bundle is in Play Console and the APK is on the repository's **Releases** page.

If publishing fails after the release was created (for example, a missing secret), fix the
problem and run the **Build and Publish** workflow manually with the existing tag, e.g. `v1.2.0`.

## Notes

- **Keystore safety:** the keystore is never committed. CI restores it from the secret into a
  temporary file and deletes it after the build.
- **Sideloaded APK signature:** the APK is signed with the upload key. If Google signs Play
  installs with a different key (Play App Signing), people who installed from Play can't update
  with the sideloaded APK (or the other way round) without uninstalling first.
- **Draft apps:** if the app has never been published on Play, the upload step must use
  `status: draft` in `.github/workflows/release.yml`.
