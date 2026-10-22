#!/usr/bin/env python3
"""Email the latest Spinny test workbook and Allure report from CI."""

from datetime import datetime
from email.message import EmailMessage
from pathlib import Path
from zoneinfo import ZoneInfo
import mimetypes
import os
import smtplib
import sys
import zipfile


ROOT = Path(__file__).resolve().parents[1]
REPORT_DIR = ROOT / "target/site/allure-maven-plugin"
RESULTS_DIR = ROOT / "test-results"
WORKBOOK = RESULTS_DIR / "Spinny_Execution_Results.xlsx"
REPORT_ZIP = RESULTS_DIR / "Allure-Report.zip"
MAX_REPORT_ATTACHMENT_BYTES = 15 * 1024 * 1024


def build_report_zip() -> bool:
    if not REPORT_DIR.is_dir() or not any(REPORT_DIR.iterdir()):
        return False

    RESULTS_DIR.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(REPORT_ZIP, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for path in REPORT_DIR.rglob("*"):
            if path.is_file():
                archive.write(path, Path("Allure-Report") / path.relative_to(REPORT_DIR))
    return True


def attach_file(message: EmailMessage, path: Path) -> None:
    content_type, _ = mimetypes.guess_type(path.name)
    if content_type is None:
        content_type = "application/octet-stream"
    main_type, sub_type = content_type.split("/", 1)
    message.add_attachment(path.read_bytes(), maintype=main_type, subtype=sub_type, filename=path.name)


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
    report_exists = build_report_zip()

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
    attachments = []
    if WORKBOOK.is_file():
        body.append("The latest Excel pass/fail workbook is attached.")
        attachments.append(WORKBOOK)
    else:
        body.append("The Excel workbook was not generated.")

    job_url = os.environ.get("CI_JOB_URL", "").strip()
    if not job_url:
        server_url = os.environ.get("GITHUB_SERVER_URL", "https://github.com").rstrip("/")
        repository = os.environ.get("GITHUB_REPOSITORY", "").strip()
        run_id = os.environ.get("GITHUB_RUN_ID", "").strip()
        if repository and run_id:
            job_url = f"{server_url}/{repository}/actions/runs/{run_id}"
    if report_exists and REPORT_ZIP.stat().st_size <= MAX_REPORT_ATTACHMENT_BYTES:
        body.append("The complete static Allure report is attached as Allure-Report.zip.")
        attachments.append(REPORT_ZIP)
    elif report_exists:
        body.append(
            "The Allure report is too large for a Gmail attachment. "
            "It is available in the CI job artifacts instead."
        )
        if job_url:
            body.extend(["", "Open the CI run and download its artifacts:", job_url])
    else:
        body.append("The Allure HTML report was not generated; check the CI job log and artifacts.")
        if job_url:
            body.extend(["", "CI run:", job_url])

    message.set_content("\n".join(body))
    for attachment in attachments:
        attach_file(message, attachment)

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
