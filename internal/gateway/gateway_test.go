package gateway

import (
	"testing"

	"github.com/ghost-hid/ghid/internal/adb"
	"github.com/ghost-hid/ghid/internal/config"
	"github.com/ghost-hid/ghid/internal/vault"
)

func TestGatewayStatusAndDoctor(t *testing.T) {
	fake := adb.NewFakeRunner()
	fake.Register("devices -l", `List of devices attached
192.168.1.80:41839     device model:KB2001 transport_id:1
`, nil)
	fake.Register("pm list packages", "package:com.sj010.ghid_headless\n", nil)
	fake.Register("settings get global bluetooth_on", "1\n", nil)
	fake.Register("cat /sdcard/Android/data/com.sj010.ghid_headless/files/status.json", `{
  "registered": true,
  "connected": true,
  "state": "CONNECTED",
  "target": "Laptop",
  "address": "38:FC:98:E8:E3:87",
  "updated": "2026-10-23 00:00:00.000"
}`, nil)

	mgr := adb.NewManagerWithRunner(fake, "", "")
	gw := New(mgr)

	st, err := gw.Status()
	if err != nil {
		t.Fatalf("unexpected error getting status: %v", err)
	}
	if !st.Connected || !st.Registered {
		t.Errorf("status mismatch: %+v", st)
	}

	cfg := config.DefaultConfig()
	cfg.DefaultUser = "admin"
	vlt := vault.NewMemoryVault()
	_ = vlt.Set("admin", "pass123")

	report, err := gw.Doctor("1.0.0", cfg, vlt)
	if err != nil {
		t.Fatalf("unexpected error running doctor: %v", err)
	}

	if !report.ApkInstalled || !report.BluetoothOn || !report.GatewayConnected || !report.HasCachedPassword {
		t.Errorf("doctor report mismatch: %+v", report)
	}
	if len(report.Checks) != 9 {
		t.Errorf("expected 9 checks, got %d", len(report.Checks))
	}
}

func TestGatewayInputValidation(t *testing.T) {
	fake := adb.NewFakeRunner()
	mgr := adb.NewManagerWithRunner(fake, "test-dev", "")
	gw := New(mgr)

	// Combo validation
	if err := gw.Combo("invalidkeycombination"); err == nil {
		t.Error("expected error for invalid combo")
	}
	if err := gw.Combo("win+l"); err != nil {
		t.Errorf("unexpected error for valid combo: %v", err)
	}

	// Raw report validation
	if err := gw.Raw("invalid"); err == nil {
		t.Error("expected error for invalid raw report")
	}
	if err := gw.Raw("08000f0000000000"); err != nil {
		t.Errorf("unexpected error for valid raw report: %v", err)
	}

	// Login validation
	if err := gw.Login(LoginRequest{Username: "", Password: ""}, nil); err == nil {
		t.Error("expected error for empty login credentials")
	}
}
