# Spinny Used Car UI Automation

Selenium 4 + Java 17 + TestNG framework for city selection and the used-car listing journey. The filter test exercises budget handles, brand/model selection, year selection, visible result cards, and navigation to a car detail page. Site selectors live in page objects so they can be updated in one place as Spinny changes its UI.

## Run

Prerequisites: Java 17+, Maven, and Chrome. Selenium Manager downloads/resolves the matching ChromeDriver.

Run the suite and refresh both reports. The script still generates the Allure HTML when tests fail, then returns the test failure status:

```bash
./run-tests.sh
```

Run only the budget, brand, model, and year cases:

```bash
./run-tests.sh -Dtest=UsedCarFiltersAndDetailsTest \
  -Dlisting.url=https://www.spinny.com/used-cars-in-delhi-ncr/s/ \
  -Dheadless=true
```

The filter test runs independent budget, make, model, and year cases. Each case prints matching car titles, opens a result, and prints its title, URL, and overview. Budget/year choices may be discrete in the live listing; the log reports the selected value. Browser defaults to Chrome; `-Dheadless=true` enables headless mode. Supply another `listing.url` for a different city's inventory.

## Reports

The workbook `test-results/Spinny_Execution_Results.xlsx` receives a refreshed `Execution Results` sheet after each suite run, while any other existing sheets are preserved. Rows include run date/time, selected city and observed listing data, filter applied, car details, pass/fail, error, and screenshot evidence reference. Override the workbook path with `-Dexcel.report=/path/to/report.xlsx`.

The static Allure report is generated at `target/site/allure-maven-plugin/index.html` after each run, including failed runs. To view it locally, use Allure's temporary HTTP server (opening `index.html` as a `file://` URL can leave the dashboard stuck on “Loading…”):

```bash
./mvnw allure:serve
```

Stop the local server with Ctrl+C. The report is generated from the latest `target/allure-results` files.

## GitHub Actions: daily run and Gmail reports

The `.github/workflows/daily-tests.yml` workflow runs the suite daily at **09:00 Asia/Kolkata** and can also be started manually from GitHub's **Actions** tab. It runs headless on a GitHub-hosted Linux runner, publishes the latest static Allure report to GitHub Pages, and emails a pass/fail summary with the live report link. The workbook and raw results remain downloadable as workflow artifacts for 7 days. The email contains no attachments because Gmail blocked the report files.

The GitHub repository shown during setup is **Public**, which means anyone can view the code after you push it. Standard GitHub-hosted Actions runners are free for public repositories. If you make the repository private, GitHub Free includes 2,000 Actions minutes per month; this Selenium suite's actual usage depends on how long each run takes. See [GitHub Actions billing](https://docs.github.com/en/billing/concepts/product-billing/github-actions) and [included usage](https://docs.github.com/en/billing/reference/product-usage-included).

### 1. Push this existing project to the new GitHub repository

Keep `origin` pointed at GitLab and add GitHub as a second remote:

```bash
cd ~/Desktop/SPINNY
git remote add github https://github.com/yashdhiman016-byte/spinny-ui-tests.git
git push -u github main
```

If Git says the `github` remote already exists, check it with `git remote -v`; if its URL is wrong, update it with `git remote set-url github https://github.com/yashdhiman016-byte/spinny-ui-tests.git` and retry the push. Do not add GitHub as `origin`, because `origin` is already your GitLab remote.

### 2. Add Gmail secrets on GitHub

Open the repository's **Settings → Secrets and variables → Actions → New repository secret** and add:

- `GMAIL_FROM`: the Gmail account used to send the email.
- `GMAIL_TO`: the recipient email address (commas are supported for multiple recipients).
- `GMAIL_APP_PASSWORD`: a Google app password, not your regular Gmail password.

Use the same app password you created for the GitLab variables if you want. Never put these values in a file or commit them to Git. Google requires 2-Step Verification for app passwords; see [Google's instructions](https://support.google.com/mail/answer/185833?hl=en).

### 3. Run once, then let the schedule run

Before the first run, open **Settings → Pages** and select **GitHub Actions** as the publishing source. Pages is publicly accessible for this public repository; anyone with the link can view the Allure report. Each successful report deployment updates the same site URL. After pushing and saving the secrets, open **Actions → Daily Spinny UI tests → Run workflow** and start it on `main`. The workflow sends email even if tests fail, then marks the run failed so you can see the status. Open the run to inspect logs and download the `spinny-test-reports-*` artifact for the workbook and raw results.

GitHub runs scheduled workflows from the default branch and may delay them during busy periods. The schedule uses GitHub's IANA timezone support (`Asia/Kolkata`); see [workflow schedule events](https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows).

## GitLab CI: daily run and Gmail reports

This project includes `.gitlab-ci.yml`. It runs the tests in headless Chrome, generates Allure even if a test fails, and emails a pass/fail summary with a link to the CI run. Workbook and Allure files are kept as GitLab artifacts for 30 days. The email contains no attachments because Gmail blocked the report files; download the artifacts from the linked job instead. Anyone opening that link needs access to the GitLab project.

The project must be on GitLab (or imported into GitLab) for `.gitlab-ci.yml` to run. Pushing only to GitHub will not run GitLab CI; you can keep a GitHub mirror separately if needed.

### 1. Push the project to GitLab

Create a blank GitLab project, then run these commands in the project folder, replacing the URL with the one GitLab shows:

```bash
git init
git add .
git commit -m "Add Spinny UI automation and GitLab CI"
git branch -M main
git remote add origin https://gitlab.com/YOUR-NAME/YOUR-PROJECT.git
git push -u origin main
```

This pipeline is configured for a **self-managed macOS shell runner** tagged `macos-local`. Install Java 17, Maven 3.9+, Python 3, and Google Chrome on the Mac, then install and register GitLab Runner. Keep the Mac awake and connected while a scheduled run is due. Chrome must be installed at `/Applications/Google Chrome.app/Contents/MacOS/Google Chrome` (or update `CHROME_BIN` in `.gitlab-ci.yml`).

Runner setup:

1. In GitLab, open **Project → Settings → CI/CD → Runners**, create a project runner, and set its tag to `macos-local`. Copy the runner authentication token; do not share or commit it.
2. On the Mac, install the runner and register it. When asked for the executor, choose `shell`:

   ```bash
   brew install gitlab-runner maven
   gitlab-runner register --url https://gitlab.com --token 'PASTE-RUNNER-TOKEN-HERE'
   ```

   Start the registered runner with `brew services start gitlab-runner`. Confirm it appears **online** in the project's Runners settings. Keep the Mac awake; shell jobs run directly as the runner's user.

3. Verify the runner account can find `java`, `mvn`, `python3`, and Chrome. GitLab's macOS runner setup recommends Bash for shell executor jobs; use Bash for the runner shell.

`.gitignore` excludes the local Maven/JDK cache, build output, Allure cache, and generated workbooks. Those tool and report files are rebuilt by CI and retained as job artifacts.

### 2. Add Gmail credentials as GitLab variables

In GitLab, open **Settings → CI/CD → Variables** and add:

- `GMAIL_FROM`: the Gmail address that sends the message.
- `GMAIL_TO`: the address that receives it (the same address is okay).
- `GMAIL_APP_PASSWORD`: a Google app password. Set visibility to **Masked and hidden**; never put it in the repository. Only mark it **Protected** if `main` is a protected branch, otherwise scheduled/manual pipelines may not receive it.

Google requires 2-Step Verification before it allows an app password. Create one in your Google Account security settings and paste it into the GitLab variable. App passwords are documented in [Google Account Help](https://support.google.com/mail/answer/185833?hl=en).

### 3. Create the daily schedule

In GitLab, open **Build → Pipeline schedules → New schedule**. Select the `main` branch, set the interval to `0 9 * * *`, and set its timezone to `Asia/Kolkata`. Save and activate it. If the timezone selector is unavailable, use `30 3 * * *` with UTC. GitLab schedules and runner availability can delay the actual start.

To test it immediately after setting the variables and bringing the runner online, open **Build → Pipelines → New pipeline** and run the `main` branch. The same job is available to scheduled and manually started pipelines. Check its job page for logs/artifacts and the Gmail inbox for the email. A temporary schedule for five minutes from now is not guaranteed to start at that exact time; use the schedule's **Run** action for an immediate test, then edit the schedule to the daily time.

## Structure

- `src/test/java/com/spinny/framework`: browser lifecycle and reporting.
- `src/test/java/com/spinny/pages`: listing and car detail page operations.
- `src/test/java/com/spinny/tests`: independent journey tests.
- `test-results/Spinny_Execution_Results.xlsx`: refreshed execution results workbook.
- `.gitlab-ci.yml`: scheduled/manual CI job and report artifacts.
- `.github/workflows/daily-tests.yml`: scheduled/manual GitHub Actions job and report artifacts.
- `scripts/send_daily_report.py`: Gmail SMTP report delivery; credentials are read only from environment variables.
