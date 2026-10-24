package adb

import (
	"fmt"
	"strings"
	"testing"
)

func TestGetDevices(t *testing.T) {
	fake := NewFakeRunner()
	fake.Register("devices -l", `List of devices attached
988e1039484b39414e     device usb:337641472X product:OnePlus7Pro model:GM1917 device:OnePlus7Pro transport_id:1
192.168.1.100:5555     device product:Pixel6 model:Pixel_6 device:oriole transport_id:2
`, nil)

	mgr := NewManagerWithRunner(fake, "", "")
	devices, err := mgr.GetDevices()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}

	if len(devices) != 2 {
		t.Fatalf("expected 2 devices, got %d", len(devices))
	}

	if devices[0].Serial != "988e1039484b39414e" || devices[0].Model != "GM1917" {
		t.Errorf("device 0 mismatch: %+v", devices[0])
	}
	if devices[1].Serial != "192.168.1.100:5555" || devices[1].Model != "Pixel_6" {
		t.Errorf("device 1 mismatch: %+v", devices[1])
	}
}

func TestSelectDevice(t *testing.T) {
	fake := NewFakeRunner()
	fake.Register("devices -l", `List of devices attached
988e1039484b39414e     unauthorized usb:337641472X transport_id:1
`, nil)

	mgr := NewManagerWithRunner(fake, "", "")
	_, err := mgr.SelectDevice()
	if err == nil || !strings.Contains(err.Error(), "unauthorized") {
		t.Errorf("expected unauthorized error, got %v", err)
	}

	// Now with an authorized device
	fake.Register("devices -l", `List of devices attached
988e1039484b39414e     device usb:337641472X product:OnePlus7Pro model:GM1917 transport_id:1
`, nil)
	dev, err := mgr.SelectDevice()
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if dev.Serial != "988e1039484b39414e" {
		t.Errorf("selected device mismatch: %+v", dev)
	}
}

func TestGetStatus(t *testing.T) {
	fake := NewFakeRunner()
	fake.Register("cat /sdcard/Android/data/com.sj010.ghid_headless/files/status.json", `{
		"registered": true,
		"connected": true,
		"state": "CONNECTED",
		"target": "ThinkPad",
		"address": "38:FC:98:E8:E3:87",
		"updated": "2023-10-27T10:00:00Z"
	}`, nil)

	mgr := NewManagerWithRunner(fake, "test-serial", "")
	st := mgr.GetStatus()

	if !st.Registered {
		t.Error("expected registered=true")
	}
	if !st.Connected {
		t.Error("expected connected=true")
	}
	if st.State != "CONNECTED" || st.Target != "ThinkPad" || st.Address != "38:FC:98:E8:E3:87" {
		t.Errorf("status fields mismatch: %+v", st)
	}

	// Malformed JSON fallback
	fakeBroken := NewFakeRunner()
	fakeBroken.Register("cat /sdcard/Android/data/com.sj010.ghid_headless/files/status.json", "file not found", nil)
	mgrBroken := NewManagerWithRunner(fakeBroken, "test-serial", "")
	stBroken := mgrBroken.GetStatus()
	if stBroken.State != "OFFLINE" || stBroken.Connected != false {
		t.Errorf("expected fallback offline status, got %+v", stBroken)
	}
}

func TestSendRawValidation(t *testing.T) {
	fake := NewFakeRunner()
	mgr := NewManagerWithRunner(fake, "test-serial", "")

	// Invalid length
	if err := mgr.SendRaw("12345"); err == nil {
		t.Error("expected error for invalid hex length")
	}

	// Valid length
	if err := mgr.SendRaw("08000f0000000000"); err != nil {
		t.Errorf("unexpected error for valid raw report: %v", err)
	}
}

func TestExecuteLogin(t *testing.T) {
	fake := NewFakeRunner()
	mgr := NewManagerWithRunner(fake, "test-serial", "")

	delays := map[string]float64{
		"wake":    0.001,
		"overlay": 0.001,
		"user":    0.001,
		"tab":     0.001,
		"pass":    0.001,
		"submit":  0.001,
	}

	var steps []int
	err := mgr.ExecuteLogin("Administrator", "SecretPass123", delays, func(step int, desc string) {
		steps = append(steps, step)
	})

	if err != nil {
		t.Fatalf("unexpected error executing login: %v", err)
	}
	if len(steps) != 6 {
		t.Errorf("expected 6 steps called back, got %d (%v)", len(steps), steps)
	}
}

func TestSendSleepSequence(t *testing.T) {
	fake := NewFakeRunner()
	mgr := NewManagerWithRunner(fake, "test-serial", "")

	err := mgr.SendSleepSequence()
	if err != nil {
		t.Fatalf("unexpected error during sleep sequence: %v", err)
	}

	// Verify the calls: esc -> esc -> win+x -> u -> s
	var comboKeys []string
	for _, call := range fake.Calls {
		for i, arg := range call {
			if arg == "combo" && i+2 < len(call) && call[i+1] == "--es" && call[i+2] == "keys" && i+3 < len(call) {
				comboKeys = append(comboKeys, call[i+3])
			}
		}
	}

	expected := []string{"esc", "esc", "win+x", "u", "s"}
	if len(comboKeys) != len(expected) {
		t.Fatalf("got %d combo keys sent (%v), want %d (%v)", len(comboKeys), comboKeys, len(expected), expected)
	}
	for i := range expected {
		if comboKeys[i] != expected[i] {
			t.Errorf("comboKey %d = %q, want %q", i, comboKeys[i], expected[i])
		}
	}
}

func TestSendComboSequence(t *testing.T) {
	fake := NewFakeRunner()
	mgr := NewManagerWithRunner(fake, "test-serial", "")

	// Sequence: esc ->{0.001} super+l
	err := mgr.SendCombo("esc ->{0.001} super+l")
	if err != nil {
		t.Fatalf("unexpected error during combo sequence: %v", err)
	}

	var comboKeys []string
	for _, call := range fake.Calls {
		for i, arg := range call {
			if arg == "combo" && i+2 < len(call) && call[i+1] == "--es" && call[i+2] == "keys" && i+3 < len(call) {
				comboKeys = append(comboKeys, call[i+3])
			}
		}
	}

	// super should normalize to win for Android broadcast
	expected := []string{"esc", "win+l"}
	if len(comboKeys) != len(expected) {
		t.Fatalf("got %d combo keys sent (%v), want %d (%v)", len(comboKeys), comboKeys, len(expected), expected)
	}
	for i := range expected {
		if comboKeys[i] != expected[i] {
			t.Errorf("comboKey %d = %q, want %q", i, comboKeys[i], expected[i])
		}
	}
}

func TestSendComboTypedText(t *testing.T) {
	fake := NewFakeRunner()
	mgr := NewManagerWithRunner(fake, "", "")

	if err := mgr.SendCombo("win+s ->{0.001} type:rustdesk"); err != nil {
		t.Fatalf("SendCombo failed: %v", err)
	}

	var typed string
	for _, call := range fake.Calls {
		for i := 0; i+3 < len(call); i++ {
			if call[i] == "type" && call[i+1] == "--es" && call[i+2] == "text" && call[i+3] == "rustdesk" {
				typed = call[i+3]
			}
		}
	}
	if typed != "rustdesk" {
		t.Fatalf("typed text broadcast not found in %v", fake.Calls)
	}
}

func TestSendComboWithResolver(t *testing.T) {
	fake := NewFakeRunner()
	mgr := NewManagerWithRunner(fake, "", "")

	resolver := func(name string) (string, error) {
		switch strings.ToLower(name) {
		case "username":
			return "alice", nil
		case "password":
			return "Secret99!", nil
		default:
			return "", fmt.Errorf("unknown variable %s", name)
		}
	}

	seq := "ctrl+alt+del ->{0.001} esc ->{0.001} $USERNAME ->{0.001} tab ->{0.001} $PASSWORD ->{0.001} enter"
	if err := mgr.SendComboWithResolver(seq, resolver); err != nil {
		t.Fatalf("SendComboWithResolver failed: %v", err)
	}

	var typed []string
	for _, call := range fake.Calls {
		for i := 0; i+3 < len(call); i++ {
			if call[i] == "type" && call[i+1] == "--es" && call[i+2] == "text" {
				typed = append(typed, call[i+3])
			}
		}
	}

	if len(typed) != 2 || typed[0] != "alice" || typed[1] != "Secret99!" {
		t.Errorf("unexpected typed values: %v", typed)
	}
}
