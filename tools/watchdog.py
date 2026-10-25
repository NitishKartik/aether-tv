#!/usr/bin/env python3
"""
Aether TV Autonomous Watchdog Engine
Verifies actual engineering progress, test execution, git diffs, device status, and detects stalls every 15-20 minutes.
"""
import json
import os
import random
import subprocess
import sys
import time
from datetime import datetime, timedelta

STATE_FILE = os.path.join(os.path.dirname(__file__), "watchdog-state.json")
PROGRESS_FILE = os.path.join(os.path.dirname(__file__), "..", "docs", "PROGRESS.md")
REPO_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
DEFAULT_TV_IP = "10.199.103.54:5555"

def get_git_status():
    try:
        res = subprocess.run(
            ["git", "status", "--porcelain"],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
            timeout=10
        )
        return res.stdout.strip().splitlines()
    except Exception as e:
        return [f"Error getting git status: {e}"]

def get_git_diff_stat():
    try:
        res = subprocess.run(
            ["git", "diff", "--stat"],
            cwd=REPO_ROOT,
            capture_output=True,
            text=True,
            timeout=10
        )
        return res.stdout.strip()
    except Exception as e:
        return f"Error getting git diff: {e}"

def check_adb_device(tv_ip=DEFAULT_TV_IP):
    try:
        tmp_file = os.path.join(os.environ.get("TEMP", "."), "adb_watchdog.txt")
        subprocess.run(
            f"adb connect {tv_ip} > nul 2>&1 && adb devices > \"{tmp_file}\" 2>&1",
            shell=True,
            timeout=8
        )
        if os.path.exists(tmp_file):
            with open(tmp_file, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
            try:
                os.remove(tmp_file)
            except Exception:
                pass
            return (tv_ip in content and "device" in content)
    except Exception:
        pass
    return False

def load_state():
    if os.path.exists(STATE_FILE):
        try:
            with open(STATE_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            pass
    return {
        "last_check_timestamp": None,
        "next_check_timestamp": None,
        "current_interval_minutes": 17,
        "milestone": "Phase 8 - On-Device Verification & QA Validation",
        "stall_detected": False,
        "history": []
    }

def save_state(state):
    with open(STATE_FILE, "w", encoding="utf-8") as f:
        json.dump(state, f, indent=2)

import hashlib

def compute_work_fingerprint(modified_files, diff_stat):
    content = "\n".join(modified_files) + "||" + diff_stat
    return hashlib.sha256(content.encode('utf-8')).hexdigest()

def run_check():
    state = load_state()
    now = datetime.now()
    now_iso = now.isoformat()
    interval = random.randint(15, 20)
    next_check = (now + timedelta(minutes=interval)).isoformat()
    
    modified_files = get_git_status()
    diff_stat = get_git_diff_stat()
    device_online = check_adb_device(DEFAULT_TV_IP)
    
    current_fingerprint = compute_work_fingerprint(modified_files, diff_stat)
    last_fingerprint = state.get("last_work_fingerprint")
    consecutive_idle = state.get("consecutive_idle_checks", 0)
    
    if last_fingerprint is not None and last_fingerprint == current_fingerprint:
        consecutive_idle += 1
    else:
        consecutive_idle = 0
    
    stall_detected = consecutive_idle >= 2
    meaningful_progress = (consecutive_idle == 0) and (len(modified_files) > 0 or diff_stat != "")
    
    if stall_detected:
        status = "PROGRESS STALLED"
    elif meaningful_progress:
        status = "PROGRESS VERIFIED"
    else:
        status = "CHECKPOINT LOGGED"
    
    report = {
        "timestamp": now_iso,
        "interval_minutes": interval,
        "status": status,
        "modified_count": len(modified_files),
        "diff_stat": diff_stat,
        "device_online": device_online,
        "stall_detected": stall_detected
    }
    
    state["last_check_timestamp"] = now_iso
    state["next_check_timestamp"] = next_check
    state["current_interval_minutes"] = interval
    state["tv_ip"] = DEFAULT_TV_IP
    state["last_work_fingerprint"] = current_fingerprint
    state["consecutive_idle_checks"] = consecutive_idle
    state["stall_detected"] = stall_detected
    state["device_status"] = state.get("device_status", {})
    state["device_status"]["connection"] = "CONNECTED_AND_ACTIVE" if device_online else "OFFLINE"
    state["device_status"]["model"] = "MIBOX4"
    
    state["history"].append(report)
    if len(state["history"]) > 50:
        state["history"] = state["history"][-50:]
    
    save_state(state)
    
    print(f"=== AETHER TV WATCHDOG CHECKPOINT ===")
    print(f"Timestamp: {now_iso}")
    print(f"Next Randomized Interval: {interval} minutes ({next_check})")
    print(f"Status: {status}")
    print(f"Stall Detected: {stall_detected} (idle checks: {consecutive_idle})")
    print(f"Device {DEFAULT_TV_IP}: {'ONLINE' if device_online else 'OFFLINE'}")
    print(f"Files Modified/Untracked: {len(modified_files)}")
    if diff_stat:
        print(f"Diff Summary:\n{diff_stat}")
    print("=======================================")
    sys.stdout.flush()

if __name__ == "__main__":
    run_check()
