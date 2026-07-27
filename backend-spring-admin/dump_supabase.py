import json, requests, subprocess, os, sys, time

SUPABASE_URL = "https://grbbtgfvgwxtkgxtakug.supabase.co"
API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdyYmJ0Z2Z2Z3d4dGtneHRha3VnIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc1NzE2MTA2NywiZXhwIjoyMDcyNzM3MDY3fQ.2p5X36snvOo90_37wztsZyt89vhsj-jg1LpE-4W8NnY"
HEADERS = {"apikey": API_KEY, "Authorization": f"Bearer {API_KEY}"}
DUMP_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "dumps")
MYSQL = r"C:\Program Files\MySQL\MySQL Workbench 8.0\mysql.exe"
DB_ARGS = ["-h", "34.47.168.236", "-P", "7306", "-u", "sbuser", "-pKMmTKeK7yh77odw51gK12f", "sabbpeonboarding"]
os.makedirs(DUMP_DIR, exist_ok=True)

TABLES = [
    "merchant_profiles", "merchant_documents", "merchant_kyc",
    "merchant_bank_details", "merchant_persons", "tickets", "ticket_messages",
    "merchant_invitations", "merchant_agreements", "merchant_sub_products",
    "product_catalog", "product_sub_catalog", "transactions", "notifications",
    "application_status_history", "onboarding_audit_log", "distributor_profiles",
    "employee_profiles", "chat_audio_log", "settlement_history",
    "rolling_reserve_ledger", "chargebacks", "chargeback_history",
    "distributor_recovery_history"
]

def fetch_table(table):
    all_rows = []
    offset = 0
    batch = 100
    while True:
        url = f"{SUPABASE_URL}/rest/v1/{table}?select=*&limit={batch}&offset={offset}"
        try:
            r = requests.get(url, headers=HEADERS, timeout=30)
            if r.status_code != 200:
                break
            data = r.json()
            if not data:
                break
            all_rows.extend(data)
            if len(data) < batch:
                break
            offset += batch
        except:
            break
    return all_rows

def escape_val(v):
    if v is None:
        return "NULL"
    if isinstance(v, bool):
        return "TRUE" if v else "FALSE"
    if isinstance(v, (int, float)):
        return str(v)
    if isinstance(v, (dict, list)):
        s = json.dumps(v, default=str).replace("\\", "\\\\").replace("'", "\\'")
        return f"'{s}'"
    s = str(v).replace("\\", "\\\\").replace("'", "\\'")
    return f"'{s}'"

total = 0
for table in TABLES:
    print(f"Fetching {table}...", flush=True)
    rows = fetch_table(table)
    print(f"  Got {len(rows)} rows", flush=True)
    if not rows:
        continue

    with open(os.path.join(DUMP_DIR, f"{table}.json"), "w", encoding="utf-8") as f:
        json.dump(rows, f, default=str, ensure_ascii=False)

    cols = list(rows[0].keys())
    col_list = ", ".join([f"`{c}`" for c in cols])

    inserted = 0
    for row in rows:
        vals = ", ".join([escape_val(row.get(c)) for c in cols])
        sql = f"INSERT INTO `{table}` ({col_list}) VALUES ({vals}) ON DUPLICATE KEY UPDATE id=id;"
        try:
            subprocess.run([MYSQL] + DB_ARGS + ["-e", sql], capture_output=True, timeout=10)
            inserted += 1
        except Exception as e:
            print(f"  Error: {e}", flush=True)

    total += inserted
    print(f"  Inserted {inserted}/{len(rows)} into MariaDB", flush=True)

print(f"\n=== DONE: {total} total rows ===")
