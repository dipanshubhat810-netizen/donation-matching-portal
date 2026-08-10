# Demo Walkthrough — Donation Matching Portal

A 5-minute live demo script for the hackathon. It exercises the full MVP
flow: donor → donation → receiver → requirement → admin approval → matching →
match approval → transaction → completion.

## Before you start

- The backend is deployed and **Live** on Render (cold start can take ~30s on
  the free plan — warm it up before demoing).
- `ADMIN_EMAIL` / `ADMIN_PASSWORD` were set when creating the Blueprint. You
  will sign in as that admin.
- `jq` is installed (`brew install jq` if not).
- A terminal with `curl`.

Set two shell variables for the rest of this script:

```bash
BASE=https://<your-service>.onrender.com
ADMIN_EMAIL=admin@sevasahayog.org
ADMIN_PASSWORD=<the password you set in Render>
```

---

## Step 0 — One-time setup: seed a RECEIVER account

Registration always creates a `DONOR`, and there is no API to create a
`RECEIVER` (by design — roles are provisioned administratively). So seed one
receiver directly in the database. The password for this demo account is
`receiver123` (demo-only; the hash below is its BCrypt encoding).

In the Render dashboard open the **Database** → **Connect** → copy the **PSQL
Command**, then run:

```bash
psql "<connection-string>" -c "
INSERT INTO users (name, email, password, role, active, created_at, updated_at)
VALUES ('Asha Sharma', 'asha.receiver@demo.com',
        '\$2y\$10\$dlftb2NE5HIioWa.Rw2UdOXxCj8eG/WKD1jX7GYm29bUXAGZG.FCG',
        'RECEIVER', true, now(), now())
ON CONFLICT (email) DO NOTHING;"
```

> The `ON CONFLICT` makes this idempotent — safe to run before every demo.

---

## Step 1 — Health check (public)

```bash
curl -s $BASE/api/health
# {"status":"UP","database":"UP"}
```

---

## Step 2 — Donor signs up and creates a donation

```bash
# Register (public; always creates a DONOR)
DONOR=$(curl -s -X POST $BASE/api/auth/register -H 'Content-Type: application/json' \
  -d '{"name":"Ravi Kumar","email":"ravi.donor@demo.com","password":"donorPass123"}')
DONOR_TOKEN=$(echo "$DONOR" | jq -r .accessToken)
echo "$DONOR" | jq '{email, role}'
# -> email "ravi.donor@demo.com", role "DONOR"

# Create a donation -> status SUBMITTED
DONATION=$(curl -s -X POST $BASE/api/donations -H "Authorization: Bearer $DONOR_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"5kg of rice","description":"Fresh rice packets for a family","category":"FOOD","quantity":10,"quantityUnit":"KG","condition":"NEW","city":"Pune","locality":"Kothrud","pincode":"411038"}')
DONATION_ID=$(echo "$DONATION" | jq -r .id)
echo "$DONATION" | jq '{id, title, status}'
# -> id, title "5kg of rice", status "SUBMITTED"
```

Talk about: donors submit donations; nothing is public until an admin approves it.

---

## Step 3 — Receiver logs in and posts a requirement

```bash
# Login as the seeded receiver
RECEIVER=$(curl -s -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"asha.receiver@demo.com","password":"receiver123"}')
RECEIVER_TOKEN=$(echo "$RECEIVER" | jq -r .accessToken)
echo "$RECEIVER" | jq '{email, role}'
# -> email "asha.receiver@demo.com", role "RECEIVER"

# Post a requirement -> status SUBMITTED
REQ=$(curl -s -X POST $BASE/api/requirements -H "Authorization: Bearer $RECEIVER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"title":"Rice needed for family of 4","description":"Need rice for one month","category":"FOOD","quantity":5,"quantityUnit":"KG","city":"Pune","locality":"Kothrud","urgency":"HIGH"}')
REQ_ID=$(echo "$REQ" | jq -r .id)
echo "$REQ" | jq '{id, title, status, urgency}'
# -> id, status "SUBMITTED", urgency "HIGH"
```

Talk about: receivers describe what they need; requirements also need approval
before they can be matched.

---

## Step 4 — Admin approves both, then generates match suggestions

```bash
# Login as admin
ADMIN=$(curl -s -X POST $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}")
ADMIN_TOKEN=$(echo "$ADMIN" | jq -r .accessToken)
echo "$ADMIN" | jq '{email, role}'
# -> email <your admin>, role "ADMIN"

# Approve the donation
curl -s -X PATCH $BASE/api/donations/$DONATION_ID/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"status":"APPROVED"}' | jq '{id, status}'
# -> status "APPROVED"

# Approve the requirement
curl -s -X PATCH $BASE/api/requirements/$REQ_ID/status \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"status":"APPROVED"}' | jq '{id, status}'
# -> status "APPROVED"

# Generate suggestions (for one requirement, or omit ?requirementId= for all)
SUGGESTIONS=$(curl -s -X POST "$BASE/api/admin/matches/suggest?requirementId=$REQ_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
echo "$SUGGESTIONS" | jq '.[] | {id, score, status, breakdown}'
# -> a match with score 100.0 and an explainable breakdown
```

Talk about: matching is explainable, not a black box — category 30 + quantity 30
+ location 20 + urgency 20 = 100/100, and anything under 70 is not suggested.

---

## Step 5 — Admin approves the match (creates a transaction)

```bash
MATCH_ID=$(echo "$SUGGESTIONS" | jq -r '.[0].id')

curl -s -X POST $BASE/api/admin/matches/$MATCH_ID/approve \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '{id, status}'
# -> status "APPROVED" (the donation automatically becomes MATCHED
#    and a PENDING transaction is created)
```

Talk about: the algorithm only *suggests*; the admin makes the final call —
full administrative control.

---

## Step 6 — Admin runs the transaction to completion

```bash
# List transactions
TXNS=$(curl -s $BASE/api/admin/transactions -H "Authorization: Bearer $ADMIN_TOKEN")
TXN_ID=$(echo "$TXNS" | jq -r '.content[0].id')
echo "$TXNS" | jq '.content[0] | {id, matchId, status, donor, receiver}'
# -> status "PENDING"

# Start fulfilment
curl -s -X POST $BASE/api/admin/transactions/$TXN_ID/start \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '{id, status}'
# -> status "IN_PROGRESS"

# Complete it
curl -s -X POST $BASE/api/admin/transactions/$TXN_ID/complete \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '{id, status, completedAt}'
# -> status "COMPLETED"
```

Talk about: the full loop — donated item is now fulfilled and the match is
complete, end to end with no spreadsheets.

---

## Optional callouts

- **Discovery:** as the receiver, `GET "$BASE/api/donations?category=FOOD&city=Pune"` shows only approved donations.
- **Interactive docs:** open `$BASE/swagger-ui/index.html`, click **Authorize**, paste a token, and run any call from the browser.
- **Live spec:** `$BASE/v3/api-docs`.
- **Cold start:** the free Render plan sleeps; the first call after idle takes longer. Send a ping (`curl -s $BASE/api/health`) before walking on stage.

## If something fails

- `401` on an admin call → wrong `ADMIN_PASSWORD`/email case; re-login.
- `403` → you're logged in with the wrong role (e.g. DONOR hitting a receiver/admin endpoint) — login as the right account.
- `409` at registration → that demo email already exists from a previous run; change the address or delete the row.
- Match list empty at Step 5 → re-run Step 4's suggest call; it needs both an APPROVED donation and an APPROVED requirement with the same category, unit and city, and donation quantity ≥ requirement quantity.
