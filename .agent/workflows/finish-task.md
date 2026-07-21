---
description: Hoàn thành task với auto PR, link Redmine, và update status
---

# /finish-task - Hoàn Thành Task & Tạo PR

## Mục đích
Workflow tự động hóa việc hoàn thành task sau khi code xong: commit code, push branch, tạo Pull Request, gắn link Redmine vào PR, gắn link PR vào Redmine, và cập nhật status trên Redmine. **Dùng khi bạn đã code xong và muốn wrap-up task.**

## Config File
API key được lưu tại: `~/.gemini/redmine-config.json`
```json
{
  "api_key": "[REDMINE_API_KEY]",
  "base_url": "https://project.flydinotech.com"
}
```

## Workflow

### Phase 1: Thu thập thông tin
1. **Kiểm tra authentication**:
   - Đọc file `~/.gemini/redmine-config.json`
   - Nếu không có: Hỏi user nhập API key và base URL → Lưu vào config file

2. **Hỏi user nhập thông tin** (nếu chưa được cung cấp):
   - **Redmine Issue ID** (bắt buộc)
   - **Branch type** (optional): `feat` hoặc `fix` - Nếu không nhập, tự detect từ Subject

3. **Fetch thông tin issue từ Redmine**:
// turbo
```bash
curl -s -H "X-Redmine-API-Key: [API_KEY]" "[BASE_URL]/issues/[ISSUE_ID].json" | python3 -c "import sys, json; data = json.load(sys.stdin); issue = data.get('issue', {}); print(f\"Subject: {issue.get('subject')}\"); print(f\"Status: {issue.get('status', {}).get('name')}\")"
```

### Phase 2: Pre-flight Check
4. **Kiểm tra trạng thái Git**:
// turbo
```bash
git status --porcelain
```

5. **Kiểm tra có thay đổi chưa commit không**:
   - Nếu **có changes**: Tiếp tục flow
   - Nếu **không có changes**: Hỏi user có muốn tiếp tục không

6. **Chạy lint check** (optional, nếu project có):
// turbo
```bash
npm run lint 2>/dev/null || echo "No lint script found"
```

### Phase 3: Git Operations
7. **Xác định Branch Info** (tự động từ Subject):
   - `[type]`: Nếu Subject chứa "Bug", "Fix", "Error", "Lỗi" → `fix`. Ngược lại → `feat`.
   - `[keyword]`: Lấy từ Subject, chuyển thành kebab-case, tối đa 5 từ
   - Ví dụ: Subject "Fix login error" → `fix/login-error-[ISSUE_ID]`

8. **Tạo branch mới** (nếu cần):
// turbo
```bash
current_branch=$(git branch --show-current)
target_branch="[type]/[keyword]-[ISSUE_ID]"
if [ "$current_branch" != "$target_branch" ]; then
  git checkout -b "$target_branch" 2>/dev/null || git checkout "$target_branch"
fi
echo "Current branch: $(git branch --show-current)"
```

9. **Stage và Commit changes**:
// turbo
```bash
git add .
git commit -m "Refs #[ISSUE_ID] [keyword]"
```

10. **Push branch lên remote**:
// turbo
```bash
git push -u origin [type]/[keyword]-[ISSUE_ID]
```

### Phase 4: Tạo Pull Request
11. **Detect GitHub URL**:
// turbo
```bash
git remote get-url origin
```

12. **Tạo Pull Request** với link Redmine trong body:
// turbo
```bash
pr_url=$(gh pr create \
  --title "[type]/[keyword]-[ISSUE_ID]" \
  --body "## Redmine Issue
- 🔗 **Link**: [BASE_URL]/issues/[ISSUE_ID]

## Changes
[Mô tả ngắn các thay đổi]

## Checklist
- [ ] Code đã được review
- [ ] Tests passed
- [ ] Lint passed")
echo "✅ Created PR: $pr_url"
```

### Phase 5: Update Redmine
13. **Gửi note lên Redmine** với PR link:
// turbo
```bash
python3 -c "
import json, urllib.request
api_key = '[API_KEY]'
issue_id = '[ISSUE_ID]'
pr_url = '[PR_URL]'  # URL từ bước trước
url = f'[BASE_URL]/issues/{issue_id}.json'
data = {'issue': {'notes': f'**Pull Request đã được tạo:**\n{pr_url}'}}
req = urllib.request.Request(url, data=json.dumps(data).encode(), headers={'Content-Type': 'application/json', 'X-Redmine-API-Key': api_key}, method='PUT')
urllib.request.urlopen(req)
print(f'✅ Added PR link to Issue #{issue_id}')
"
```

14. **Cập nhật Status Redmine** → "Waiting Review - Deploy Dev" (status_id=11):
// turbo
```bash
python3 -c "
import json, urllib.request
api_key = '[API_KEY]'
issue_id = '[ISSUE_ID]'
url = f'[BASE_URL]/issues/{issue_id}.json'
data = {'issue': {'status_id': 11}}  # Waiting Review - Deploy Dev
req = urllib.request.Request(url, data=json.dumps(data).encode(), headers={'Content-Type': 'application/json', 'X-Redmine-API-Key': api_key}, method='PUT')
urllib.request.urlopen(req)
print(f'✅ Updated Issue #{issue_id} status → Waiting Review - Deploy Dev')
"
```

### Phase 6: Final Summary
15. **Hiển thị tổng kết**:
```markdown
## ✅ Task Hoàn Thành - Issue #[ISSUE_ID]

### Git
- 🌿 Branch: `[type]/[keyword]-[ISSUE_ID]`
- 📝 Commit: `Refs #[ISSUE_ID] [keyword]`
- ⬆️ Pushed: ✅

### Pull Request
- 🔗 PR: [PR_URL]
- 📋 Title: `[type]/[keyword]-[ISSUE_ID]`
- 📎 Redmine link: ✅ Included

### Redmine
- 💬 Note: PR link đã được gửi
- 🔄 Status: Waiting Review - Deploy Dev
- 🔗 Issue: [BASE_URL]/issues/[ISSUE_ID]
```

## Tùy chọn bổ sung

### Custom Status
Nếu muốn update status khác, user có thể chỉ định status_id:
- `10` = In Progress
- `11` = Waiting Review - Deploy Dev
- `12` = Reviewing
- `13` = Done

### Skip Steps
User có thể yêu cầu skip các bước:
- `--no-lint`: Bỏ qua lint check
- `--no-status`: Không update Redmine status
- `--no-note`: Không gửi note lên Redmine

## Nguyên tắc quan trọng
- **One-click Completion**: Chỉ cần nhập Issue ID, workflow tự động hoàn thành mọi thứ
- **Bi-directional Linking**: PR link → Redmine, Redmine link → PR
- **Status Sync**: Tự động cập nhật trạng thái phù hợp
- **Safe Operations**: Kiểm tra changes trước khi commit, không force push
