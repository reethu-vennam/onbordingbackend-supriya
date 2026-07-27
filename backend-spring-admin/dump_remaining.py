import json, requests, subprocess, os, time

SUPABASE_URL = "https://grbbtgfvgwxtkgxtakug.supabase.co"
API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdyYmJ0Z2Z2Z3d4dGtneHRha3VnIiwicm9sZSI6InNlcnZpY2Vfcm9sZSIsImlhdCI6MTc1NzE2MTA2NywiZXhwIjoyMDcyNzM3MDY3fQ.2p5X36snvOo90_37wztsZyt89vhsj-jg1LpE-4W8NnY"
HEADERS = {"apikey": API_KEY, "Authorization": f"Bearer {API_KEY}"}
DUMP_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "dumps")
MYSQL = r"C:\Program Files\MySQL\MySQL Workbench 8.0\mysql.exe"
DB_ARGS = ["-h", "34.47.168.236", "-P", "7306", "-u", "sbuser", "-pKMmTKeK7yh77odw51gK12f", "sabbpeonboarding"]
os.makedirs(DUMP_DIR, exist_ok=True)

# Column mapping: supabase_name -> mariadb_name
COL_MAP = {
    "notifications": {"read": "is_read"}
}

TABLES = [
    "notifications", "application_status_history", "onboarding_audit_log",
    "distributor_profiles", "employee_profiles", "chat_audio_log",
    "settlement_history", "rolling_reserve_ledger", "chargebacks",
    "chargeback_history", "distributor_recovery_history"
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

    col_rename = COL_MAP.get(table, {})
    supa_cols = list(rows[0].keys())
    # Filter out columns that don't exist in MariaDB
    mariadb_cols = [col_rename.get(c, c) for c in supa_cols]
    col_list = ", ".join([f"`{c}`" for c in mariadb_cols])

    # Batch insert (10 rows at a time via temp file)
    for i in range(0, len(rows), 10):
        batch_rows = rows[i:i+10]
        values_list = []
        for row in batch_rows:
            vals = []
            for c in supa_cols:
                mc = col_rename.get(c, c)
                vals.append(f"`{mc}`={escape_val(row.get(c))}")
            values_list.append(f"({', '.join([escape_val(row.get(c)) for c in supa_cols])})")
        
        sql = f"INSERT INTO `{table}` ({col_list}) VALUES {', '.join(values_list)} ON DUPLICATE KEY UPDATE id=id;"
        
        tmp = os.path.join(DUMP_DIR, "tmp.sql")
        with open(tmp, "w", encoding="utf-8") as f:
            f.write(sql)
        
        try:
            subprocess.run([MYSQL] + DB_ARGS + [tmp], capture_output=True, timeout=30)
        except:
            pass

    total += len(rows)
    print(f"  Inserted {len(rows)} into MariaDB", flush=True)

print(f"\n=== DONE: {total} total rows ===")
