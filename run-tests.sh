#!/usr/bin/env bash

# Always build the Allure HTML, including when TestNG reports failures.
set -u

./mvnw clean test "$@"
test_status=$?

./mvnw allure:report
report_status=$?

mkdir -p test-results
{
  printf 'Test command exit code: %s\n' "$test_status"
  printf 'Allure report command exit code: %s\n' "$report_status"
} > test-results/run-status.txt

if [[ $test_status -ne 0 ]]; then
  exit "$test_status"
fi
exit "$report_status"
