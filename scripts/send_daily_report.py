#!/usr/bin/env python3
"""Email the latest Spinny test workbook and Allure report from CI."""

from datetime import datetime
from email.message import EmailMessage
from pathlib import Path
from zoneinfo import ZoneInfo
import os
import smtplib
import sys


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "target/site/allure-maven-plugin"
RESULTS_DIR = ROOT / "test-results"
WORKBOOK = RESULTS_DIR / "Spinny_Execution_Results.xlsx"


def main() -> int:
    sender = os.environ.get("GMAIL_FROM", "").strip()
    recipients = [address.strip() for address in os.environ.get("GMAIL_TO", "").split(",") if address.strip()]
    app_password = os.environ.get("GMAIL_APP_PASSWORD", "").replace(" ", "").strip()
    missing = [name for name, value in (
        ("GMAIL_FROM", sender), ("GMAIL_TO", recipients), ("GMAIL_APP_PASSWORD", app_password)
    ) if not value]
    if missing:
        print("Missing CI secrets/variables: " + ", ".join(missing), file=sys.stderr)
        return 2

    status_file = RESULTS_DIR / "run-status.txt"
    run_status = status_file.read_text(encoding="utf-8").strip() if status_file.exists() else "Run status was not recorded."
    failed = (
        "Test command exit code: 0" not in run_status
        or "Allure report command exit code: 0" not in run_status
    )
    report_exists = REPORT_DIR.is_dir() and any(REPORT_DIR.iterdir())

    message = EmailMessage()
    message["From"] = sender
    message["To"] = ", ".join(recipients)
    message["Date"] = datetime.now().astimezone().strftime("%a, %d %b %Y %H:%M:%S %z")
    message["Subject"] = f"{'FAILED' if failed else 'PASSED'}: Spinny UI test run — {datetime.now(ZoneInfo('Asia/Kolkata')).strftime('%Y-%m-%d %H:%M IST')}"

    body = [
        "Spinny used-car UI test run",
        "",
        run_status,
        "",
    ]
    if WORKBOOK.is_file():
        body.append("The Excel pass/fail workbook is available in the run artifacts.")
    else:
        body.append("The Excel workbook was not generated.")

    job_url = os.environ.get("CI_JOB_URL", "").strip()
    if not job_url:
        server_url = os.environ.get("GITHUB_SERVER_URL", "https://github.com").rstrip("/")
        repository = os.environ.get("GITHUB_REPOSITORY", "").strip()
        run_id = os.environ.get("GITHUB_RUN_ID", "").strip()
        if repository and run_id:
            job_url = f"{server_url}/{repository}/actions/runs/{run_id}"
    if report_exists:
        body.append(
            "The static Allure report and raw results are available in the run artifacts. "
            "Download the artifact, then open target/site/allure-maven-plugin/index.html."
        )
    else:
        body.append("The Allure HTML report was not generated; check the CI job log and artifacts.")

    if job_url:
        body.extend(["", "Open the CI run to view logs and download artifacts:", job_url])

    message.set_content("\n".join(body))

    host = os.environ.get("GMAIL_SMTP_HOST", "smtp.gmail.com")
    port = int(os.environ.get("GMAIL_SMTP_PORT", "587"))
    with smtplib.SMTP(host, port, timeout=30) as smtp:
        smtp.ehlo()
        smtp.starttls()
        smtp.ehlo()
        smtp.login(sender, app_password)
        smtp.send_message(message)

    print(f"Report email sent to {', '.join(recipients)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
