# Privacy Policy · 隐私政策

_Last updated: 2026-05-12 · 最后更新：2026 年 5 月 12 日_

---

## English

**Personal Ledger** is a fully on-device bookkeeping app. It is built and
maintained by a single person (the GitHub user `visiongem`) for personal use,
and is provided as-is under the Apache License 2.0.

### What data the app stores

All of your accounts, categories, transactions, budgets, and exchange rates are
stored in a private SQLite database inside the app's sandboxed storage on your
device. No part of that data ever leaves your phone through the app itself.

If you tap **Export backup (ZIP)** in Settings, the app writes a ZIP archive to
the location you choose (typically a folder you control). What happens to that
file after that — uploading it to a cloud, sending it to yourself, etc. — is
entirely under your control.

### What network calls the app makes

The only outbound network call the app makes is to the public Frankfurter API
(`https://api.frankfurter.app`) when:

- you tap **Refresh exchange rates** in Settings, or
- the background daily job (WorkManager) runs once every 24 hours.

The request contains only the currency codes you have configured (for example
`USD`, `EUR`, `CNY`). It does not include any account names, transaction data,
device identifiers, or personally identifiable information.

### What data the app does NOT collect

- No analytics SDK (no Firebase, Google Analytics, etc.)
- No crash reporting SDK (no Crashlytics, Sentry, Bugsnag, etc.)
- No advertising SDK or trackers
- No account system, no sign-in, no remote profile
- No usage metrics of any kind

### Permissions the app requests

- `INTERNET` — required for the exchange-rate fetch above. Used for nothing else.

### Third-party services

| Service               | Purpose                  | Data sent                            |
|-----------------------|--------------------------|--------------------------------------|
| Frankfurter API       | Public exchange rates    | Currency codes you have configured   |

That's the entire list.

### Children's data

The app does not knowingly collect data from anyone. See above — it does not
collect personal data at all.

### Changes to this policy

If the policy changes (e.g. a new network feature is added in a future
release), the updated version will appear in this file in the GitHub
repository, and the "Last updated" line at the top will change.

### Contact

For privacy questions, open an issue at
[github.com/visiongem/personal-ledger/issues](https://github.com/visiongem/personal-ledger/issues).

---

## 中文

**Personal Ledger** 是一款完全本地的记账 App。由 GitHub 用户 `visiongem`
一人开发与维护，按 Apache License 2.0 协议以"原样"提供。

### 应用存储哪些数据

你的所有账户、分类、流水、预算、汇率都保存在 App 私有沙箱中的一个 SQLite
数据库里。这部分数据不会通过 App 本身离开你的设备。

如果你在 **设置 → 导出备份 (ZIP)** 里点了导出，App 会把一个 ZIP 文件写入
你选择的位置（通常是你自己控制的文件夹）。之后这个文件怎么处置——上传云盘、
发给自己等等——完全由你决定。

### App 会发起哪些网络请求

App 唯一对外的网络调用是 Frankfurter 公开 API
（`https://api.frankfurter.app`），触发时机：

- 你在设置里点 **刷新汇率**，或
- WorkManager 后台任务每 24 小时跑一次。

请求里只包含你配置的币种代码（例如 `USD`、`EUR`、`CNY`），不会包含任何账户
名、流水数据、设备标识或个人身份信息。

### App **不会** 收集哪些数据

- 不集成任何统计 SDK（无 Firebase、Google Analytics 等）
- 不集成任何崩溃上报 SDK（无 Crashlytics、Sentry、Bugsnag 等）
- 不集成任何广告 SDK 或追踪器
- 无账号体系，无登录，无远端用户档案
- 不收集任何使用数据

### App 申请的权限

- `INTERNET` —— 用于上述汇率请求，没有其它用途。

### 使用的第三方服务

| 服务名            | 用途             | 发送的数据                |
|-------------------|------------------|---------------------------|
| Frankfurter API   | 公开汇率查询     | 你已配置的币种代码        |

完整列表就这一行。

### 关于未成年人

App 本身就不收集任何个人数据，自然也不会收集未成年人的数据。

### 政策变更

如果未来版本新增了任何会改变数据流的功能，更新版的隐私政策会发布到 GitHub
仓库的这份文件里，顶部"最后更新"日期会同步变化。

### 联系方式

如有隐私相关问题，请到
[github.com/visiongem/personal-ledger/issues](https://github.com/visiongem/personal-ledger/issues)
提 issue。
