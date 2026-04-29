#!/bin/bash
# scae_test_all.sh — SCAE Backend ke sab endpoints test karo
# Usage: bash scae_test_all.sh [base_url]
# Default base URL: http://localhost:8000

BASE="${1:-http://localhost:8000}"
PASS=0
FAIL=0

ok()   { echo "  PASS  $1"; PASS=$((PASS+1)); }
fail() { echo "  FAIL  $1  (got: $2)"; FAIL=$((FAIL+1)); }

check() {
    local label="$1"
    local expected="$2"
    local actual="$3"
    if [ "$actual" = "$expected" ]; then ok "$label"
    else fail "$label" "$actual"; fi
}

echo "===================================================="
echo "  SCAE Backend — Full API Test"
echo "  Base: $BASE"
echo "===================================================="

# ── 1. Health ────────────────────────────────────────────
echo ""
echo "[ Health ]"
S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/health")
check "GET /api/health" "200" "$S"

# ── 2. Auth ──────────────────────────────────────────────
echo ""
echo "[ Auth ]"

ADMIN_RESP=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@scae.gov.in","password":"scae123"}')
ADMIN_TOKEN=$(echo "$ADMIN_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
if [ -n "$ADMIN_TOKEN" ]; then ok "POST /api/auth/login (admin)"
else fail "POST /api/auth/login (admin)" "no token"; fi

CITIZEN_RESP=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"citizen@scae.gov.in","password":"scae123"}')
CITIZEN_TOKEN=$(echo "$CITIZEN_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
if [ -n "$CITIZEN_TOKEN" ]; then ok "POST /api/auth/login (citizen)"
else fail "POST /api/auth/login (citizen)" "no token"; fi

DEPT_RESP=$(curl -s -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"dept@scae.gov.in","password":"scae123"}')
DEPT_TOKEN=$(echo "$DEPT_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
if [ -n "$DEPT_TOKEN" ]; then ok "POST /api/auth/login (dept)"
else fail "POST /api/auth/login (dept)" "no token"; fi

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/auth/me" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/auth/me" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@scae.gov.in","password":"wrongpass"}')
check "POST /api/auth/login (wrong password) → 401" "401" "$S"

# ── 3. Graph ─────────────────────────────────────────────
echo ""
echo "[ Graph ]"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/graph/nodes")
check "GET /api/graph/nodes" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/graph/edges")
check "GET /api/graph/edges" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE/api/graph/edges/1/close" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "PUT /api/graph/edges/1/close (admin)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE/api/graph/edges/1/open" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "PUT /api/graph/edges/1/open (admin)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/graph/nodes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CITIZEN_TOKEN" \
  -d '{"label":"Test Node","zone":"North"}')
check "POST /api/graph/nodes (citizen) → 403" "403" "$S"

# ── 4. Algorithms ────────────────────────────────────────
echo ""
echo "[ Algorithms ]"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/algo/dijkstra" \
  -H "Content-Type: application/json" \
  -d '{"source":0,"destination":9}')
check "POST /api/algo/dijkstra" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/algo/bellman-ford" \
  -H "Content-Type: application/json" \
  -d '{"source":0,"destination":9}')
check "POST /api/algo/bellman-ford" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/algo/bfs" \
  -H "Content-Type: application/json" \
  -d '{"source":0,"max_hops":5}')
check "POST /api/algo/bfs" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/algo/floyd-warshall" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/algo/floyd-warshall" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/algo/mst/kruskal" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/algo/mst/kruskal" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/algo/mst/prim" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/algo/mst/prim" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/algo/dfs/components" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"disabled_edges":[]}')
check "POST /api/algo/dfs/components" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/algo/knapsack" \
  -H "Content-Type: application/json" \
  -d '{"budget":300}')
check "POST /api/algo/knapsack" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/algo/huffman" \
  -H "Content-Type: application/json" \
  -d '{"text":"hello scae"}')
check "POST /api/algo/huffman" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/algo/kmp" \
  -H "Content-Type: application/json" \
  -d '{"text":"smart city algorithm engine","pattern":"city"}')
check "POST /api/algo/kmp" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/algo/job-scheduler" \
  -H "Content-Type: application/json" \
  -d '{}')
check "POST /api/algo/job-scheduler" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/algo/benchmark?n=1000" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/algo/benchmark" "200" "$S"

# ── 5. Complaints ────────────────────────────────────────
echo ""
echo "[ Complaints ]"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/complaints" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/complaints (admin)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/complaints" \
  -H "Authorization: Bearer $CITIZEN_TOKEN")
check "GET /api/complaints (citizen)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/complaints" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CITIZEN_TOKEN" \
  -d '{"category":"ROAD","description":"New pothole test","zone":"North","urgency":7}')
check "POST /api/complaints" "201" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/complaints/CMP-0001" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/complaints/CMP-0001" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE/api/complaints/CMP-0001/status" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"status":"In Progress","notes":"Team assigned"}')
check "PUT /api/complaints/CMP-0001/status" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/complaints/sorted" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/complaints/sorted" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/complaints/search?q=pothole" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/complaints/search?q=pothole" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/complaints" )
check "GET /api/complaints (no auth) → 401" "401" "$S"

# ── 6. Work Orders ───────────────────────────────────────
echo ""
echo "[ Work Orders ]"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/work-orders" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/work-orders (admin)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/work-orders" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"title":"Test WO","zone":"East","urgency":6,"deadline":"2026-05-10","crew":"Crew Echo","description":"Test order"}')
check "POST /api/work-orders" "201" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/work-orders/WO-001" \
  -H "Authorization: Bearer $DEPT_TOKEN")
check "GET /api/work-orders/WO-001 (dept)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE/api/work-orders/WO-001/status" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $DEPT_TOKEN" \
  -d '{"status":"In Progress"}')
check "PUT /api/work-orders/WO-001/status" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/work-orders/sorted" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/work-orders/sorted" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/work-orders/assign-top" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/work-orders/assign-top" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/work-orders" \
  -H "Authorization: Bearer $CITIZEN_TOKEN")
check "GET /api/work-orders (citizen) → 403" "403" "$S"

# ── 7. Emergencies ───────────────────────────────────────
echo ""
echo "[ Emergencies ]"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/emergencies" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/emergencies" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/emergencies" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"type":"Gas Leak","zone":"South","urgency":8}')
check "POST /api/emergencies" "201" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/emergencies/1" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/emergencies/1" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/emergencies/sorted" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/emergencies/sorted" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/emergencies/dispatch-next" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/emergencies/dispatch-next" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/emergencies/generate" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "POST /api/emergencies/generate" "201" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE/api/emergencies/2/resolve" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "PUT /api/emergencies/2/resolve" "200" "$S"

# ── 8. Citizens ──────────────────────────────────────────
echo ""
echo "[ Citizens ]"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/citizens" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/citizens (admin)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/citizens/search?q=u002" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/citizens/search?q=u002" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/citizens/u002" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/citizens/u002" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/citizens" \
  -H "Authorization: Bearer $CITIZEN_TOKEN")
check "GET /api/citizens (citizen) → 403" "403" "$S"

# ── 9. Notices ───────────────────────────────────────────
echo ""
echo "[ Notices ]"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/notices")
check "GET /api/notices (public — no auth)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/notices" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"title":"Test Notice","priority":1}')
check "POST /api/notices (admin)" "201" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE/api/notices/n001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"title":"Updated Notice Title"}')
check "PUT /api/notices/n001 (admin)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/notices" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $CITIZEN_TOKEN" \
  -d '{"title":"Should fail"}')
check "POST /api/notices (citizen) → 403" "403" "$S"

# ── 10. Services ─────────────────────────────────────────
echo ""
echo "[ Services ]"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/services")
check "GET /api/services (public — no auth)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/services/search?q=hospital")
check "GET /api/services/search?q=hospital" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/services/zone/North")
check "GET /api/services/zone/North" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/services" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"name":"Test Clinic","zone":"East","type":"Hospital","node_id":10}')
check "POST /api/services (admin)" "201" "$S"

# ── 11. Projects ─────────────────────────────────────────
echo ""
echo "[ Projects ]"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/projects" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/projects (admin)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/api/projects" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"name":"Test Project","cost":90,"benefit":75,"category":"Test"}')
check "POST /api/projects (admin)" "201" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X PUT "$BASE/api/projects/p001" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"name":"Road Repair Phase 1 — Updated"}')
check "PUT /api/projects/p001 (admin)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE "$BASE/api/projects/p006" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "DELETE /api/projects/p006 (soft delete)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/projects" \
  -H "Authorization: Bearer $CITIZEN_TOKEN")
check "GET /api/projects (citizen) → 403" "403" "$S"

# ── 12. Stats ────────────────────────────────────────────
echo ""
echo "[ Stats ]"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/stats/summary")
check "GET /api/stats/summary (public)" "200" "$S"

SUMMARY=$(curl -s "$BASE/api/stats/summary")
if echo "$SUMMARY" | grep -q "complaints_resolved"; then ok "GET /api/stats/summary → correct fields"
else fail "GET /api/stats/summary → missing fields" "$SUMMARY"; fi

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/stats/complaints" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/stats/complaints (admin)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/stats/work-orders" \
  -H "Authorization: Bearer $ADMIN_TOKEN")
check "GET /api/stats/work-orders (admin)" "200" "$S"

S=$(curl -s -o /dev/null -w "%{http_code}" "$BASE/api/stats/complaints" \
  -H "Authorization: Bearer $CITIZEN_TOKEN")
check "GET /api/stats/complaints (citizen) → 403" "403" "$S"

# ── Summary ──────────────────────────────────────────────
echo ""
echo "===================================================="
printf "  Total : %d   PASS : %d   FAIL : %d\n" $((PASS+FAIL)) $PASS $FAIL
echo "===================================================="
if [ "$FAIL" -eq 0 ]; then
    echo "  All tests passed!"
else
    echo "  $FAIL test(s) failed — check output above"
fi
