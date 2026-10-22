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

## GitLab CI: daily run and Gmail reports

This project now includes `.gitlab-ci.yml`. It runs the tests in headless Chromium, generates Allure even if a test fails, and emails the Excel results plus a ZIP of the Allure site. The job also keeps the report files as GitLab artifacts for 30 days. The ZIP is attached when it is at most 15 MiB; if it is larger, the email includes the GitLab job link instead. Anyone opening that fallback link needs access to the GitLab project.

The project must be on GitLab (or imported into GitLab) for `.gitlab-ci.yml` to run. Pushing only to GitHub will not run GitLab CI; you can keep a GitHub mirror separately if needed. This workspace is not initialized as a Git repository yet.

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

Use an enabled Linux Docker runner for the project; the job image installs Chromium with `apt`.

`.gitignore` excludes the local Maven/JDK cache, build output, Allure cache, and generated workbooks. Those tool and report files are rebuilt by CI and retained as job artifacts.

### 2. Add Gmail credentials as GitLab variables

In GitLab, open **Settings → CI/CD → Variables** and add:

- `GMAIL_FROM`: the Gmail address that sends the message.
- `GMAIL_TO`: the address that receives it (the same address is okay).
- `GMAIL_APP_PASSWORD`: a Google app password. Set visibility to **Masked and hidden**; never put it in the repository. Only mark it **Protected** if `main` is a protected branch, otherwise scheduled/manual pipelines may not receive it.

Google requires 2-Step Verification before it allows an app password. Create one in your Google Account security settings and paste it into the GitLab variable. App passwords are documented in [Google Account Help](https://support.google.com/mail/answer/185833?hl=en).

### 3. Create the daily schedule

In GitLab, open **Build → Pipeline schedules → New schedule**. Select the `main` branch, set the interval to `0 9 * * *`, and set its timezone to `Asia/Kolkata`. Save and activate it. If the timezone selector is unavailable, use `30 3 * * *` with UTC. GitLab schedules can start later than the requested minute when the runner is busy.

To test it immediately after setting the variables, open **Build → Pipelines → New pipeline** and run the `main` branch. The same job is available to scheduled and manually started pipelines. Check its job page for logs/artifacts and the Gmail inbox for the email.

## Structure

- `src/test/java/com/spinny/framework`: browser lifecycle and reporting.
- `src/test/java/com/spinny/pages`: listing and car detail page operations.
- `src/test/java/com/spinny/tests`: independent journey tests.
- `test-results/Spinny_Execution_Results.xlsx`: refreshed execution results workbook.
- `.gitlab-ci.yml`: scheduled/manual CI job and report artifacts.
- `scripts/send_daily_report.py`: Gmail SMTP report delivery; credentials are read only from environment variables.
