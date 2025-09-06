# Makefile for exercising the messaging API via curl
# Requirements: curl, jq
.ONESHELL:
SHELL := /bin/bash
.SHELLFLAGS := -eu -o pipefail -c

# Configurable vars
BASE_URL ?= http://localhost:8080
ACCEPT ?= application/hal+json
CONTENT_TYPE ?= application/json
ENV_FILE ?= .env

CURL := curl -sS
HDRS := -H "Accept: $(ACCEPT)" -H "Content-Type: $(CONTENT_TYPE)"

.PHONY: check-deps clean ensure-env setup demo \
        create-alice create-bob create-carol create-dave get-user \
        direct-thread post-direct-msg-alice post-direct-msg-bob list-direct-messages get-direct-message1 \
        group-create post-group-msg-bob post-group-msg-carol get-group-thread list-group-messages \
        threads-alice threads-bob threads-carol \
        err-direct-same-user err-group-too-small err-nonparticipant err-empty-content \
        health metrics

check-deps:
	@command -v jq >/dev/null || { echo "Error: jq is required (brew install jq | apt-get install jq)"; exit 1; }

clean:
	@rm -f $(ENV_FILE)
	@echo "Cleaned $(ENV_FILE)"

ensure-env:
	@printf "" > $(ENV_FILE)
	@echo "Initialized $(ENV_FILE)"

setup: check-deps clean ensure-env create-alice create-bob create-carol
	@echo "Setup complete."

demo: setup direct-thread post-direct-msg-alice post-direct-msg-bob list-direct-messages \
      group-create post-group-msg-bob post-group-msg-carol get-group-thread list-group-messages \
      threads-alice threads-bob threads-carol health
	@echo "Demo completed."

# --- Users ---

create-alice: check-deps
	@resp=$$($(CURL) $(HDRS) -X POST "$(BASE_URL)/users" --data '{"username":"alice"}'); \
	echo "$$resp" | jq .; \
	id=$$(echo "$$resp" | jq -r '.id'); \
	if [ -z "$$id" ] || [ "$$id" = "null" ] || ! echo "$$id" | grep -Eq '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$$'; then \
	  echo "Error: invalid ALICE_ID: '$$id' — not writing to $(ENV_FILE)"; exit 1; \
	fi; \
	tmp_file="$$(mktemp)"; \
	{ [ -f "$(ENV_FILE)" ] && grep -v '^ALICE_ID=' "$(ENV_FILE)" || true; echo "ALICE_ID=$$id"; } > "$$tmp_file"; \
	mv "$$tmp_file" "$(ENV_FILE)"; \
	echo "Saved ALICE_ID=$$id"

create-bob: check-deps
	@resp=$$($(CURL) $(HDRS) -X POST "$(BASE_URL)/users" --data '{"username":"bob"}'); \
	echo "$$resp" | jq .; \
	id=$$(echo "$$resp" | jq -r '.id'); \
	if [ -z "$$id" ] || [ "$$id" = "null" ] || ! echo "$$id" | grep -Eq '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$$'; then \
	  echo "Error: invalid BOB_ID: '$$id' — not writing to $(ENV_FILE)"; exit 1; \
	fi; \
	tmp_file="$$(mktemp)"; \
	{ [ -f "$(ENV_FILE)" ] && grep -v '^BOB_ID=' "$(ENV_FILE)" || true; echo "BOB_ID=$$id"; } > "$$tmp_file"; \
	mv "$$tmp_file" "$(ENV_FILE)"; \
	echo "Saved BOB_ID=$$id"

create-carol: check-deps
	@resp=$$($(CURL) $(HDRS) -X POST "$(BASE_URL)/users" --data '{"username":"carol"}'); \
	echo "$$resp" | jq .; \
	id=$$(echo "$$resp" | jq -r '.id'); \
	if [ -z "$$id" ] || [ "$$id" = "null" ] || ! echo "$$id" | grep -Eq '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$$'; then \
	  echo "Error: invalid CAROL_ID: '$$id' — not writing to $(ENV_FILE)"; exit 1; \
	fi; \
	tmp_file="$$(mktemp)"; \
	{ [ -f "$(ENV_FILE)" ] && grep -v '^CAROL_ID=' "$(ENV_FILE)" || true; echo "CAROL_ID=$$id"; } > "$$tmp_file"; \
	mv "$$tmp_file" "$(ENV_FILE)"; \
	echo "Saved CAROL_ID=$$id"

create-dave: check-deps
	@resp=$$($(CURL) $(HDRS) -X POST "$(BASE_URL)/users" --data '{"username":"dave"}'); \
	echo "$$resp" | jq .; \
	id=$$(echo "$$resp" | jq -r '.id'); \
	if [ -z "$$id" ] || [ "$$id" = "null" ] || ! echo "$$id" | grep -Eq '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$$'; then \
	  echo "Error: invalid DAVE_ID: '$$id' — not writing to $(ENV_FILE)"; exit 1; \
	fi; \
	tmp_file="$$(mktemp)"; \
	{ [ -f "$(ENV_FILE)" ] && grep -v '^DAVE_ID=' "$(ENV_FILE)" || true; echo "DAVE_ID=$$id"; } > "$$tmp_file"; \
	mv "$$tmp_file" "$(ENV_FILE)"; \
	echo "Saved DAVE_ID=$$id"

# Pass USER_ID=<uuid>, defaults to ALICE_ID from .env
get-user: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	: $${USER_ID:=$${ALICE_ID}}; \
	$(CURL) -H "Accept: $(ACCEPT)" "$(BASE_URL)/users/$$USER_ID" | jq .

# --- Direct thread and messages ---

direct-thread: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	: $${U1:=$${ALICE_ID}}; : $${U2:=$${BOB_ID}}; \
	resp=$$($(CURL) $(HDRS) -X PUT "$(BASE_URL)/threads/direct" --data '{"user1Id":"'"$$U1"'","user2Id":"'"$$U2"'"}'); \
	echo "$$resp" | jq .; \
	id=$$(echo "$$resp" | jq -r '.id'); \
	if [ -z "$$id" ] || [ "$$id" = "null" ] || ! echo "$$id" | grep -Eq '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$$'; then \
	  echo "Error: invalid DIRECT_THREAD_ID: '$$id' — not writing to $(ENV_FILE)"; exit 1; \
	fi; \
	tmp_file="$$(mktemp)"; \
	{ [ -f "$(ENV_FILE)" ] && grep -v '^DIRECT_THREAD_ID=' "$(ENV_FILE)" || true; echo "DIRECT_THREAD_ID=$$id"; } > "$$tmp_file"; \
	mv "$$tmp_file" "$(ENV_FILE)"; \
	echo "Saved DIRECT_THREAD_ID=$$id"

# CONTENT can be overridden: make post-direct-msg-alice CONTENT="your text"
post-direct-msg-alice: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	: $${CONTENT:=Hi Bob!}; \
	resp=$$($(CURL) $(HDRS) -X POST "$(BASE_URL)/threads/$${DIRECT_THREAD_ID}/messages" --data '{"senderId":"'"$${ALICE_ID}"'","content":"'"$${CONTENT//\"/\\\"}"'"}'); \
	echo "$$resp" | jq .; \
	id=$$(echo "$$resp" | jq -r '.id'); \
	if [ -z "$$id" ] || [ "$$id" = "null" ] || ! echo "$$id" | grep -Eq '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$$'; then \
	  echo "Error: invalid DIRECT_MSG1_ID: '$$id' — not writing to $(ENV_FILE)"; exit 1; \
	fi; \
	tmp_file="$$(mktemp)"; \
	{ [ -f "$(ENV_FILE)" ] && grep -v '^DIRECT_MSG1_ID=' "$(ENV_FILE)" || true; echo "DIRECT_MSG1_ID=$$id"; } > "$$tmp_file"; \
	mv "$$tmp_file" "$(ENV_FILE)"; \
	echo "Saved DIRECT_MSG1_ID=$$id"

post-direct-msg-bob: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	: $${CONTENT:=Hi Alice!}; \
	resp=$$($(CURL) $(HDRS) -X POST "$(BASE_URL)/threads/$${DIRECT_THREAD_ID}/messages" --data '{"senderId":"'"$${BOB_ID}"'","content":"'"$${CONTENT//\"/\\\"}"'"}'); \
	echo "$$resp" | jq .; \
	id=$$(echo "$$resp" | jq -r '.id'); \
	if [ -z "$$id" ] || [ "$$id" = "null" ] || ! echo "$$id" | grep -Eq '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$$'; then \
	  echo "Error: invalid DIRECT_MSG2_ID: '$$id' — not writing to $(ENV_FILE)"; exit 1; \
	fi; \
	tmp_file="$$(mktemp)"; \
	{ [ -f "$(ENV_FILE)" ] && grep -v '^DIRECT_MSG2_ID=' "$(ENV_FILE)" || true; echo "DIRECT_MSG2_ID=$$id"; } > "$$tmp_file"; \
	mv "$$tmp_file" "$(ENV_FILE)"; \
	echo "Saved DIRECT_MSG2_ID=$$id"

# PAGE and SIZE can be overridden
list-direct-messages: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	: $${PAGE:=0}; : $${SIZE:=10}; \
	$(CURL) -H "Accept: $(ACCEPT)" "$(BASE_URL)/threads/$${DIRECT_THREAD_ID}/messages?page=$${PAGE}&size=$${SIZE}" | jq .

get-direct-message1: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	$(CURL) -H "Accept: $(ACCEPT)" "$(BASE_URL)/threads/$${DIRECT_THREAD_ID}/messages/$${DIRECT_MSG1_ID}" | jq .

# --- Group thread and messages ---

group-create: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	resp=$$($(CURL) $(HDRS) -X POST "$(BASE_URL)/threads/group" --data '{"participantIds":["'"$${ALICE_ID}"'","'"$${BOB_ID}"'","'"$${CAROL_ID}"'"],"name":"Team Chat","senderId":"'"$${ALICE_ID}"'","initialMessage":"Welcome team!"}'); \
	echo "$$resp" | jq .; \
	id=$$(echo "$$resp" | jq -r '.id'); \
	if [ -z "$$id" ] || [ "$$id" = "null" ] || ! echo "$$id" | grep -Eq '^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$$'; then \
	  echo "Error: invalid GROUP_THREAD_ID: '$$id' — not writing to $(ENV_FILE)"; exit 1; \
	fi; \
	tmp_file="$$(mktemp)"; \
	{ [ -f "$(ENV_FILE)" ] && grep -v '^GROUP_THREAD_ID=' "$(ENV_FILE)" || true; echo "GROUP_THREAD_ID=$$id"; } > "$$tmp_file"; \
	mv "$$tmp_file" "$(ENV_FILE)"; \
	echo "Saved GROUP_THREAD_ID=$$id"

post-group-msg-bob: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	: $${CONTENT:=Hello all!}; \
	$(CURL) $(HDRS) -X POST "$(BASE_URL)/threads/$${GROUP_THREAD_ID}/messages" --data '{"senderId":"'"$${BOB_ID}"'","content":"'"$${CONTENT//\"/\\\"}"'"}' | jq .

post-group-msg-carol: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	: $${CONTENT:=Hi!}; \
	$(CURL) $(HDRS) -X POST "$(BASE_URL)/threads/$${GROUP_THREAD_ID}/messages" --data '{"senderId":"'"$${CAROL_ID}"'","content":"'"$${CONTENT//\"/\\\"}"'"}' | jq .

get-group-thread: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	$(CURL) -H "Accept: $(ACCEPT)" "$(BASE_URL)/threads/$${GROUP_THREAD_ID}" | jq .

list-group-messages: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	: $${PAGE:=0}; : $${SIZE:=20}; \
	$(CURL) -H "Accept: $(ACCEPT)" "$(BASE_URL)/threads/$${GROUP_THREAD_ID}/messages?page=$${PAGE}&size=$${SIZE}" | jq .

# --- Threads visible to each user ---

threads-alice: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	$(CURL) -H "Accept: $(ACCEPT)" "$(BASE_URL)/users/$${ALICE_ID}/threads?page=0&size=10" | jq .

threads-bob: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	$(CURL) -H "Accept: $(ACCEPT)" "$(BASE_URL)/users/$${BOB_ID}/threads?page=0&size=10" | jq .

threads-carol: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	$(CURL) -H "Accept: $(ACCEPT)" "$(BASE_URL)/users/$${CAROL_ID}/threads?page=0&size=10" | jq .

# --- Error scenarios ---

err-direct-same-user: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	out=$$($(CURL) -w "\nHTTP_STATUS:%{http_code}\n" $(HDRS) -X PUT "$(BASE_URL)/threads/direct" --data '{"user1Id":"'"$${ALICE_ID}"'","user2Id":"'"$${ALICE_ID}"'"}'); \
	body="$${out%HTTP_STATUS:*}"; code="$${out##*HTTP_STATUS:}"; \
	echo "$$body" | jq . || echo "$$body"; \
	echo "HTTP $$code (expected 400)"

err-group-too-small: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	out=$$($(CURL) -w "\nHTTP_STATUS:%{http_code}\n" $(HDRS) -X POST "$(BASE_URL)/threads/group" --data '{"participantIds":["'"$${ALICE_ID}"'","'"$${BOB_ID}"'"],"name":"Too Small Group"}'); \
	body="$${out%HTTP_STATUS:*}"; code="$${out##*HTTP_STATUS:}"; \
	echo "$$body" | jq . || echo "$$body"; \
	echo "HTTP $$code (expected 400)"

err-nonparticipant: check-deps create-dave
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	out=$$($(CURL) -w "\nHTTP_STATUS:%{http_code}\n" $(HDRS) -X POST "$(BASE_URL)/threads/$${GROUP_THREAD_ID}/messages" --data '{"senderId":"'"$${DAVE_ID}"'","content":"Let me in!"}'); \
	body="$${out%HTTP_STATUS:*}"; code="$${out##*HTTP_STATUS:}"; \
	echo "$$body" | jq . || echo "$$body"; \
	echo "HTTP $$code (expected 400)"

err-empty-content: check-deps
	@set -a; [ -f "$(ENV_FILE)" ] && . "$(ENV_FILE)"; \
	out=$$($(CURL) -w "\nHTTP_STATUS:%{http_code}\n" $(HDRS) -X POST "$(BASE_URL)/threads/$${DIRECT_THREAD_ID}/messages" --data '{"senderId":"'"$${ALICE_ID}"'","content":""}'); \
	body="$${out%HTTP_STATUS:*}"; code="$${out##*HTTP_STATUS:}"; \
	echo "$$body" | jq . || echo "$$body"; \
	echo "HTTP $$code (expected 400)"

# --- Actuator ---

health:
	@$(CURL) -H "Accept: application/json" "$(BASE_URL)/actuator/health" | jq .

metrics:
	@$(CURL) -H "Accept: application/json" "$(BASE_URL)/actuator/metrics" | jq .
