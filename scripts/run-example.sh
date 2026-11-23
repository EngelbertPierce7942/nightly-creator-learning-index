#!/usr/bin/env sh
set -eu

: "${INFRAI_API_KEY:?Set INFRAI_API_KEY first}"
: "${PUBLIC_BASE_URL:?Set PUBLIC_BASE_URL to the reachable HTTPS origin of this service}"

mvn spring-boot:run
