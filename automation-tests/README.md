# 🚀 AeroStride Premium Automation Testing Framework

A modern, high-performance end-to-end (E2E) and API integration test suite designed for **AeroStride**. Built with **Playwright**, this framework tests both the Frontend (FE) browser behaviors and the Backend (BE) REST API endpoints against the deployed live environment: `https://aerostride.me/`.

---

## 🛠️ Tech Stack & Key Features

* **Automation Tool**: Playwright (JS/Node)
* **Testing Coverage**:
  * **Frontend UI**: Navigation, layout, brand elements, and E2E Customer Chat Widget flows.
  * **Backend API**: Public product listing, landing contents, and OAuth/Authentication flows.
* **Browsers Tested**: Chromium, Firefox, WebKit (Safari).
* **Built-in Quality Controls**: Auto-waiting, screenshot capture on failure, video recording, trace viewer.

---

## 📂 Project Structure

```text
automation-tests/
├── playwright.config.js       # Playwright runner configuration (target, browsers, timeouts)
├── package.json               # Test script execution, dependencies
├── README.md                  # Beautiful documentation (this file)
└── tests/
    ├── ui/
    │   ├── landing.spec.js    # E2E test for home page layout & shop redirection
    │   └── chat.spec.js       # E2E test for the customer chat widget interactions
    └── api/
        └── backend-api.spec.js # Direct REST API checks for backend responses & auth
```

---

## 🚀 Getting Started

### 1. Install Dependencies
Open your shell in the `automation-tests/` directory and install Playwright along with its browser binaries:

```bash
cd automation-tests
npm install
npx playwright install --with-deps
```

### 2. Running the Automation Tests

Execute these npm scripts from the `automation-tests/` directory:

| Command | Description |
| :--- | :--- |
| `npm run test` | Run all E2E & API tests across all configured browsers in **headless** mode. |
| `npm run test:ui` | Run tests in Chromium with a **graphical interactive UI** for step-by-step debugging. |
| `npm run test:api` | Run only the **Backend REST API** test suite. |
| `npm run test:headed` | Run tests showing the real browser window (**headed** mode). |
| `npm run report` | Open the automatically generated premium HTML report with full trace data. |

---

## 📊 Beautiful HTML Reports & Traces

After a test execution finishes, Playwright produces a professional HTML report:
* **Interactive Steps**: View exact execute times per action.
* **Visual Diff & Media**: If any test fails, it records the exact screenshot, full video recording, and DOM snapshot!
* **Web Trace**: Replay the browser state retroactively!

*To see the latest report:*
```bash
npm run report
```
