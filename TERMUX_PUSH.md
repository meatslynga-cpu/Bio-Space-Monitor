# Pushing ANS Monitor Pro v2 from Termux to GitHub

## One-time setup (skip if already done)

```bash
pkg update && pkg install git -y
git config --global user.name "destroyingmyths"
git config --global user.email "your@email.com"
```

Generate a Personal Access Token on GitHub:
GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
→ New token → check `repo` scope → copy the token

```bash
git config --global credential.helper store
# Next git push will prompt once — enter username + token, then it's saved
```

---

## First push (new repo)

1. Create the repo on GitHub: https://github.com/new
   - Name: `ANS-MONITOR-PRO`
   - Private or Public
   - **Do NOT initialize with README** (you're pushing your own files)

2. In Termux, extract the zip and push:

```bash
cd ~
unzip ANS-MONITOR-PRO-v2.zip
cd ANS-MONITOR-PRO-v2
git init
git remote add origin https://github.com/destroyingmyths/ANS-MONITOR-PRO.git
git add .
git commit -m "Initial commit: ANS Monitor Pro v2.0"
git branch -M main
git push -u origin main
```

---

## Subsequent pushes (after making edits)

```bash
cd ~/ANS-MONITOR-PRO-v2
git add .
git commit -m "describe your change here"
git push
```

---

## After pushing — get your APK

1. Go to: https://github.com/destroyingmyths/ANS-MONITOR-PRO/actions
2. Click the latest workflow run
3. Wait for green checkmark (~3–5 minutes)
4. Click **ANSMonitorPro-debug-apk** under Artifacts to download

---

## Notes

- The `gradlew` script is already executable in this package
- The workflow caches Gradle dependencies — second build is much faster
- `google-services.json` is a placeholder — Firebase is NOT used in this app
- Add your **Gemini API key** inside the app under Settings after installing
- Connect your **Y007 watch MAC** inside the app under the BIO tab
