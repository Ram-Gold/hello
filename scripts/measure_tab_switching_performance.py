#!/usr/bin/env python3
import subprocess
import time
import re
import sys
import json
import os

def get_connected_device():
    res = subprocess.run(["adb", "devices"], capture_output=True, text=True)
    for line in res.stdout.strip().splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            return parts[0]
    return "HYVOXWFQSOOFVW6L"

DEVICE_ID = get_connected_device()
PACKAGE_NAME = "com.example.hello"

# Target tab touch coordinates (screen: 720x1544, 320dpi)
# Dock height: 1400..1490 (center Y = 1445)
# Safe X coordinates based on active/inactive slot positions:
Y_COORD = 1445
X_HOME_SAFE = 120        # Safe X when navigating to Home from any state
X_TIMELINE_SAFE = 450    # Safe X when navigating to Timeline from any state
X_PEOPLE_FROM_HOME = 340 # Safe X when People is inactive (on the right of Home)
X_PEOPLE_FROM_TIMELINE = 220 # Safe X when People is inactive (on the left of Timeline)

def run_adb(cmd, device=DEVICE_ID):
    full_cmd = ["adb", "-s", device] + cmd
    res = subprocess.run(full_cmd, capture_output=True, text=True)
    if res.returncode != 0:
        raise RuntimeError(f"Command {' '.join(full_cmd)} failed: {res.stderr}")
    return res.stdout

def tap(x, y, delay=0.0):
    run_adb(["shell", "input", "tap", str(x), str(y)])
    if delay > 0:
        time.sleep(delay)

def reset_gfxinfo():
    run_adb(["shell", "dumpsys", "gfxinfo", PACKAGE_NAME, "reset"])

def get_gfxinfo():
    return run_adb(["shell", "dumpsys", "gfxinfo", PACKAGE_NAME])

def parse_gfxinfo(raw):
    metrics = {
        "total_frames": 0,
        "janky_frames": 0,
        "janky_pct": 0.0,
        "p50_ms": 0,
        "p90_ms": 0,
        "p95_ms": 0,
        "p99_ms": 0,
        "missed_vsync": 0,
        "slow_ui_thread": 0,
        "frame_deadline_missed": 0,
        "slow_draw_commands": 0
    }
    
    m = re.search(r"Total frames rendered:\s+(\d+)", raw)
    if m: metrics["total_frames"] = int(m.group(1))
    
    m = re.search(r"Janky frames:\s+(\d+)\s+\(([\d.]+)%\)", raw)
    if m:
        metrics["janky_frames"] = int(m.group(1))
        metrics["janky_pct"] = float(m.group(2))
        
    m = re.search(r"50th percentile:\s+(\d+)ms", raw)
    if m: metrics["p50_ms"] = int(m.group(1))
    
    m = re.search(r"90th percentile:\s+(\d+)ms", raw)
    if m: metrics["p90_ms"] = int(m.group(1))
    
    m = re.search(r"95th percentile:\s+(\d+)ms", raw)
    if m: metrics["p95_ms"] = int(m.group(1))
    
    m = re.search(r"99th percentile:\s+(\d+)ms", raw)
    if m: metrics["p99_ms"] = int(m.group(1))
    
    m = re.search(r"Number Missed Vsync:\s+(\d+)", raw)
    if m: metrics["missed_vsync"] = int(m.group(1))
    
    m = re.search(r"Number Slow UI thread:\s+(\d+)", raw)
    if m: metrics["slow_ui_thread"] = int(m.group(1))
    
    m = re.search(r"Number Frame deadline missed:\s+(\d+)", raw)
    if m: metrics["frame_deadline_missed"] = int(m.group(1))

    m = re.search(r"Number Slow issue draw commands:\s+(\d+)", raw)
    if m: metrics["slow_draw_commands"] = int(m.group(1))

    return metrics

def prepare_app():
    # Bring app to foreground and navigate to Home tab
    run_adb(["shell", "am", "start", "-n", f"{PACKAGE_NAME}/.MainActivity"])
    time.sleep(1.0)
    tap(X_HOME_SAFE, Y_COORD, delay=0.5)

def run_suite_home_people(iterations=10, interval=0.55):
    print(f"\n--- Running Suite 1: Home <-> People ({iterations} switches, {interval}s interval) ---")
    tap(X_HOME_SAFE, Y_COORD, delay=0.5)
    reset_gfxinfo()
    
    for i in range(iterations):
        tap(X_PEOPLE_FROM_HOME, Y_COORD, delay=interval)
        tap(X_HOME_SAFE, Y_COORD, delay=interval)
        
    time.sleep(0.5)
    raw = get_gfxinfo()
    metrics = parse_gfxinfo(raw)
    return metrics, raw

def run_suite_people_timeline(iterations=10, interval=0.55):
    print(f"\n--- Running Suite 2: People <-> Timeline ({iterations} switches, {interval}s interval) ---")
    tap(X_PEOPLE_FROM_HOME, Y_COORD, delay=0.5)
    reset_gfxinfo()
    
    for i in range(iterations):
        tap(X_TIMELINE_SAFE, Y_COORD, delay=interval)
        tap(X_PEOPLE_FROM_TIMELINE, Y_COORD, delay=interval)
        
    time.sleep(0.5)
    raw = get_gfxinfo()
    metrics = parse_gfxinfo(raw)
    return metrics, raw

def run_suite_rapid_switches(switches=20, interval=0.15):
    print(f"\n--- Running Suite 3: Repeated Rapid Switches ({switches} switches, {interval}s interval) ---")
    tap(X_HOME_SAFE, Y_COORD, delay=0.5)
    reset_gfxinfo()
    
    sequence = [
        (X_PEOPLE_FROM_HOME, Y_COORD),
        (X_TIMELINE_SAFE, Y_COORD),
        (X_PEOPLE_FROM_TIMELINE, Y_COORD),
        (X_HOME_SAFE, Y_COORD)
    ]
    
    for i in range(switches):
        target = sequence[i % len(sequence)]
        tap(target[0], target[1], delay=interval)
        
    time.sleep(0.5)
    raw = get_gfxinfo()
    metrics = parse_gfxinfo(raw)
    return metrics, raw

def run_suite_home_person_detail(iterations=10, interval=0.55):
    print(f"\n--- Running Suite 4: Home <-> Person Detail ({iterations} round-trips, {interval}s interval) ---")
    tap(X_HOME_SAFE, Y_COORD, delay=0.5)
    reset_gfxinfo()
    
    for i in range(iterations):
        tap(350, 1030, delay=interval)  # Enter Person Detail
        tap(56, 176, delay=interval)    # Back to Home
        
    time.sleep(0.5)
    raw = get_gfxinfo()
    metrics = parse_gfxinfo(raw)
    return metrics, raw

def run_suite_home_settings(iterations=10, interval=0.55):
    print(f"\n--- Running Suite 5: Home <-> Settings ({iterations} round-trips, {interval}s interval) ---")
    tap(X_HOME_SAFE, Y_COORD, delay=0.5)
    reset_gfxinfo()
    
    for i in range(iterations):
        tap(636, 152, delay=interval)  # Enter Settings
        tap(76, 180, delay=interval)   # Back to Home
        
    time.sleep(0.5)
    raw = get_gfxinfo()
    metrics = parse_gfxinfo(raw)
    return metrics, raw

def main():
    print("=" * 60)
    print("Hello Android Tab & Screen Switching Performance Benchmark")
    print(f"Device: {DEVICE_ID}")
    print(f"Package: {PACKAGE_NAME}")
    print("=" * 60)
    
    prepare_app()
    
    results = {}
    
    s1_metrics, s1_raw = run_suite_home_people(iterations=10, interval=0.55)
    results["home_people"] = s1_metrics
    print(f"Home <-> People: Total Frames={s1_metrics['total_frames']}, Janky={s1_metrics['janky_frames']} ({s1_metrics['janky_pct']}%), p50={s1_metrics['p50_ms']}ms, p90={s1_metrics['p90_ms']}ms, p95={s1_metrics['p95_ms']}ms, p99={s1_metrics['p99_ms']}ms, Missed Deadlines={s1_metrics['frame_deadline_missed']}")
    
    s2_metrics, s2_raw = run_suite_people_timeline(iterations=10, interval=0.55)
    results["people_timeline"] = s2_metrics
    print(f"People <-> Timeline: Total Frames={s2_metrics['total_frames']}, Janky={s2_metrics['janky_frames']} ({s2_metrics['janky_pct']}%), p50={s2_metrics['p50_ms']}ms, p90={s2_metrics['p90_ms']}ms, p95={s2_metrics['p95_ms']}ms, p99={s2_metrics['p99_ms']}ms, Missed Deadlines={s2_metrics['frame_deadline_missed']}")
    
    s3_metrics, s3_raw = run_suite_rapid_switches(switches=20, interval=0.15)
    results["rapid_switches"] = s3_metrics
    print(f"Rapid Switches: Total Frames={s3_metrics['total_frames']}, Janky={s3_metrics['janky_frames']} ({s3_metrics['janky_pct']}%), p50={s3_metrics['p50_ms']}ms, p90={s3_metrics['p90_ms']}ms, p95={s3_metrics['p95_ms']}ms, p99={s3_metrics['p99_ms']}ms, Missed Deadlines={s3_metrics['frame_deadline_missed']}")

    s4_metrics, s4_raw = run_suite_home_person_detail(iterations=10, interval=0.55)
    results["home_person_detail"] = s4_metrics
    print(f"Home <-> Person Detail: Total Frames={s4_metrics['total_frames']}, Janky={s4_metrics['janky_frames']} ({s4_metrics['janky_pct']}%), p50={s4_metrics['p50_ms']}ms, p90={s4_metrics['p90_ms']}ms, p95={s4_metrics['p95_ms']}ms, p99={s4_metrics['p99_ms']}ms, Missed Deadlines={s4_metrics['frame_deadline_missed']}")
    
    s5_metrics, s5_raw = run_suite_home_settings(iterations=10, interval=0.55)
    results["home_settings"] = s5_metrics
    print(f"Home <-> Settings: Total Frames={s5_metrics['total_frames']}, Janky={s5_metrics['janky_frames']} ({s5_metrics['janky_pct']}%), p50={s5_metrics['p50_ms']}ms, p90={s5_metrics['p90_ms']}ms, p95={s5_metrics['p95_ms']}ms, p99={s5_metrics['p99_ms']}ms, Missed Deadlines={s5_metrics['frame_deadline_missed']}")
    
    os.makedirs("build/benchmark-results", exist_ok=True)
    with open("build/benchmark-results/tab_switching_baseline.json", "w") as f:
        json.dump(results, f, indent=2)
        
    print("\nBenchmark completed. Summary saved to build/benchmark-results/tab_switching_baseline.json")

if __name__ == "__main__":
    main()

