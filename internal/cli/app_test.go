package cli

import (
	"bytes"
	"encoding/json"
	"path/filepath"
	"strings"
	"testing"

	"github.com/ghost-hid/ghid/internal/adb"
	"github.com/ghost-hid/ghid/internal/config"
	"github.com/ghost-hid/ghid/internal/gateway"
	"github.com/ghost-hid/ghid/internal/vault"
)

func setupTestApp(t *testing.T) (*App, *adb.FakeRunner, *vault.MemoryVault, *config.Store, *bytes.Buffer, *bytes.Buffer) {
	tmpDir := t.TempDir()
	configPath := filepath.Join(tmpDir, "config.json")
	store := config.NewStore(configPath)

	vlt := vault.NewMemoryVault()
	fakeRunner := adb.NewFakeRunner()

	stdout := &bytes.Buffer{}
	stderr := &bytes.Buffer{}

	app := &App{
		Version:     "1.0.0-test",
		ConfigStore: store,
		Vault:       vlt,
		GatewayFactory: func(serial string) (gateway.Controller, error) {
			mgr := adb.NewManagerWithRunner(fakeRunner, serial, "")
			return gateway.New(mgr), nil
		},
		Stdout: stdout,
		Stderr: stderr,
	}

	return app, fakeRunner, vlt, store, stdout, stderr
}

func TestAppVersion(t *testing.T) {
	app, _, _, _, stdout, _ := setupTestApp(t)
	code := app.Run([]string{"version"})
	if code != 0 {
		t.Errorf("expected exit code 0, got %d", code)
	}
	if !strings.Contains(stdout.String(), "1.0.0-test") {
		t.Errorf("stdout missing version: %q", stdout.String())
	}
}

func TestAppHelp(t *testing.T) {
	app, _, _, _, stdout, _ := setupTestApp(t)
	code := app.Run([]string{"--help"})
	if code != 0 {
		t.Errorf("expected exit code 0, got %d", code)
	}
	if !strings.Contains(stdout.String(), "Usage:") {
		t.Errorf("stdout missing usage help: %q", stdout.String())
	}
}

func TestAppStatus(t *testing.T) {
	app, fakeRunner, _, _, stdout, _ := setupTestApp(t)

	fakeRunner.Register("cat /sdcard/Android/data/com.sj010.ghid_headless/files/status.json", `{
  "registered": true,
  "connected": true,
  "state": "CONNECTED",
  "target": "ThinkPad",
  "address": "38:FC:98:E8:E3:87",
  "updated": "2026-10-23 00:00:00.000"
}`, nil)

	// JSON mode
	code := app.Run([]string{"status", "--json"})
	if code != 0 {
		t.Fatalf("expected exit code 0, got %d", code)
	}

	var status adb.GatewayStatus
	if err := json.Unmarshal(stdout.Bytes(), &status); err != nil {
		t.Fatalf("failed to parse json output: %v", err)
	}
	if !status.Connected || status.Target != "ThinkPad" {
		t.Errorf("status mismatch: %+v", status)
	}
}

func TestAppDoctorJSON(t *testing.T) {
	app, fakeRunner, vlt, _, stdout, _ := setupTestApp(t)

	fakeRunner.Register("devices -l", `List of devices attached
test-serial     device model:Pixel7 transport_id:1
`, nil)
	fakeRunner.Register("pm list packages", "package:com.sj010.ghid_headless\n", nil)
	fakeRunner.Register("settings get global bluetooth_on", "1\n", nil)
	fakeRunner.Register("cat /sdcard/Android/data/com.sj010.ghid_headless/files/status.json", `{
  "registered": true,
  "connected": true,
  "state": "CONNECTED",
  "target": "ThinkPad",
  "address": "38:FC:98:E8:E3:87",
  "updated": "2026-10-23 00:00:00.000"
}`, nil)

	_ = vlt.Set("Administrator", "Secret")

	code := app.Run([]string{"doctor", "--json"})
	if code != 0 {
		t.Fatalf("expected exit code 0, got %d", code)
	}

	var rep map[string]interface{}
	if err := json.Unmarshal(stdout.Bytes(), &rep); err != nil {
		t.Fatalf("failed to parse doctor json output: %v", err)
	}

	if rep["device_connected"] != true || rep["apk_installed"] != true {
		t.Errorf("doctor report mismatch: %+v", rep)
	}
}

func TestAppCredsRemovedAndSnippetsUsed(t *testing.T) {
	app, _, _, _, stdout, stderr := setupTestApp(t)

	// Verify creds command is removed and returns code 1
	code := app.Run([]string{"creds"})
	if code != 1 {
		t.Fatalf("expected creds to return exit code 1 as removed command, got %d", code)
	}

	// Set username and secure password via snippets
	code = app.Run([]string{"snippets", "set", "username", "bob"})
	if code != 0 {
		t.Fatalf("snippets set username failed with code %d", code)
	}
	code = app.Run([]string{"snippets", "set", "password", "hunter2", "--secure"})
	if code != 0 {
		t.Fatalf("snippets set password failed with code %d", code)
	}

	// Verify snippets get
	stdout.Reset()
	code = app.Run([]string{"snippets", "get", "username", "--json"})
	if code != 0 {
		t.Fatalf("snippets get username failed with code %d: %s", code, stderr.String())
	}
	if !strings.Contains(stdout.String(), `"value": "bob"`) {
		t.Errorf("expected username bob in json, got %s", stdout.String())
	}
}

func TestAppDevices(t *testing.T) {
	app, _, _, store, stdout, _ := setupTestApp(t)

	// Add new device
	code := app.Run([]string{"devices", "add", "--name", "TestPC", "--addr", "11:22:33:44:55:66", "--os", "windows", "--select"})
	if code != 0 {
		t.Fatalf("devices add failed with code %d", code)
	}

	cfg := store.Load()
	sel := cfg.GetSelectedDevice()
	if sel == nil || sel.Name != "TestPC" || sel.Address != "11:22:33:44:55:66" {
		t.Errorf("selected device mismatch after add: %+v", sel)
	}

	// List devices
	stdout.Reset()
	code = app.Run([]string{"devices", "list", "--json"})
	if code != 0 {
		t.Fatalf("devices list failed with code %d", code)
	}

	var devices []config.TargetDevice
	if err := json.Unmarshal(stdout.Bytes(), &devices); err != nil {
		t.Fatalf("failed to parse devices list json: %v", err)
	}
	if len(devices) < 2 {
		t.Errorf("expected at least 2 devices, got %d", len(devices))
	}

	// Select previous device
	stdout.Reset()
	code = app.Run([]string{"devices", "select", "LP-TZD-0X114650"})
	if code != 0 {
		t.Fatalf("devices select failed with code %d", code)
	}
	cfg = store.Load()
	sel = cfg.GetSelectedDevice()
	if sel == nil || sel.Name != "LP-TZD-0X114650" {
		t.Errorf("selected device mismatch after select: %+v", sel)
	}
}

func TestAppCommands(t *testing.T) {
	app, _, _, _, stdout, stderr := setupTestApp(t)

	// Lock command
	stdout.Reset()
	code := app.Run([]string{"lock", "--json"})
	if code != 0 {
		t.Fatalf("lock failed with code %d", code)
	}
	var lockRes map[string]interface{}
	if err := json.Unmarshal(stdout.Bytes(), &lockRes); err != nil || lockRes["action"] != "lock" {
		t.Errorf("unexpected lock json: %s", stdout.String())
	}

	// Combo command (single combo with super alias)
	stdout.Reset()
	code = app.Run([]string{"combo", "super+l", "--json"})
	if code != 0 {
		t.Fatalf("combo failed with code %d", code)
	}
	var comboRes map[string]interface{}
	if err := json.Unmarshal(stdout.Bytes(), &comboRes); err != nil || comboRes["combo"] != "super+l" {
		t.Errorf("unexpected combo json: %s", stdout.String())
	}

	// Combo command (sequence with delay)
	stdout.Reset()
	code = app.Run([]string{"combo", "esc", "->{0.001}", "super+l", "--json"})
	if code != 0 {
		t.Fatalf("combo sequence failed with code %d", code)
	}
	var seqRes map[string]interface{}
	if err := json.Unmarshal(stdout.Bytes(), &seqRes); err != nil || seqRes["status"] != "success" {
		t.Errorf("unexpected combo sequence json: %s", stdout.String())
	}

	// Sleep command
	stdout.Reset()
	code = app.Run([]string{"sleep", "--countdown", "1", "--json"})
	if code != 0 {
		t.Fatalf("sleep failed with code %d", code)
	}
	var sleepRes map[string]interface{}
	if err := json.Unmarshal(stdout.Bytes(), &sleepRes); err != nil || sleepRes["action"] != "sleep" {
		t.Errorf("unexpected sleep json: %s", stdout.String())
	}

	// Quick send command (default type)
	stdout.Reset()
	code = app.Run([]string{"quick", "hello", "world", "--json"})
	if code != 0 {
		t.Fatalf("quick type failed with code %d: %s", code, stderr.String())
	}
	var quickTypeRes map[string]interface{}
	if err := json.Unmarshal(stdout.Bytes(), &quickTypeRes); err != nil || quickTypeRes["typed"] != "hello world" {
		t.Errorf("unexpected quick type json: %s", stdout.String())
	}

	// Quick send command (--raw flag)
	stdout.Reset()
	code = app.Run([]string{"quick", "--raw", "08000f0000000000", "--json"})
	if code != 0 {
		t.Fatalf("quick raw failed with code %d", code)
	}
	var rawRes map[string]interface{}
	if err := json.Unmarshal(stdout.Bytes(), &rawRes); err != nil || rawRes["raw"] != "08000f0000000000" {
		t.Errorf("unexpected raw json: %s", stdout.String())
	}

	// Quick send with raw: prefix
	stdout.Reset()
	code = app.Run([]string{"quick", "raw:08000f0000000000", "--json"})
	if code != 0 {
		t.Fatalf("quick raw: prefix failed with code %d", code)
	}

	// Root type and raw should now be unknown commands (no backward compatibility)
	stderr.Reset()
	if code = app.Run([]string{"type", "foo"}); code != 1 {
		t.Errorf("expected exit code 1 for removed type command, got %d", code)
	}
	stderr.Reset()
	if code = app.Run([]string{"raw", "08000f0000000000"}); code != 1 {
		t.Errorf("expected exit code 1 for removed raw command, got %d", code)
	}

	// Unknown command
	stderr.Reset()
	code = app.Run([]string{"unknown-command"})
	if code != 1 {
		t.Errorf("expected exit code 1 for unknown command, got %d", code)
	}
}

func TestComboMetadataAndDirectives(t *testing.T) {
	app, _, _, store, stdout, stderr := setupTestApp(t)

	// Save combo with --emoji and --desc
	stdout.Reset()
	code := app.Run([]string{"combo", "save", "MyLock", "!COMBO:SEND:lock", "--emoji", "🔒", "--desc", "Chained lock combo", "--json"})
	if code != 0 {
		t.Fatalf("combo save failed: code %d, stderr: %s", code, stderr.String())
	}
	var saveRes map[string]interface{}
	if err := json.Unmarshal(stdout.Bytes(), &saveRes); err != nil {
		t.Fatalf("parse save response: %v", err)
	}
	comboObj := saveRes["combo"].(map[string]interface{})
	if comboObj["emoji"] != "🔒" || comboObj["description"] != "Chained lock combo" {
		t.Errorf("unexpected combo metadata: %+v", comboObj)
	}

	// Dispatch combo with directive chaining !COMBO:SEND:lock
	stdout.Reset()
	code = app.Run([]string{"combo", "send", "MyLock", "--json"})
	if code != 0 {
		t.Fatalf("combo send failed: code %d, stdout: %s, stderr: %s", code, stdout.String(), stderr.String())
	}

	// Execute combo with !EXIT directive
	stdout.Reset()
	code = app.Run([]string{"combo", "!EXIT", "--json"})
	if code != 0 {
		t.Fatalf("combo !EXIT failed: code %d, stderr: %s", code, stderr.String())
	}
	var exitRes map[string]interface{}
	if err := json.Unmarshal(stdout.Bytes(), &exitRes); err != nil || exitRes["exit"] != true {
		t.Errorf("unexpected exit response: %s", stdout.String())
	}

	_ = store
}
