# Copyright 2023 Adobe Systems Incorporated
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Playwright image with browsers preinstalled; tests use the bundled Chromium (PW_CHANNEL="").
FROM mcr.microsoft.com/playwright:v1.65.1-noble

ENV APP_PATH=/usr/src/app
ENV PW_CHANNEL=""
ENV CI=true
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1

WORKDIR ${APP_PATH}
COPY ./test-module ./
RUN npm ci --no-audit --no-fund
ENTRYPOINT ["bash","run.sh"]