package adb

import (
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"path/filepath"
	"regexp"
	"strings"
	"time"

	"github.com/ghost-hid/ghid/internal/hid"
)

const (
	PackageName      = "com.sj010.ghid_headless"
	ServiceName      = "com.sj010.ghid_headless/.HidService"
	ReceiverName     = "com.sj010.ghid_headless/.HidCommandReceiver"
	ActionCmd        = "com.sj010.ghid_headless.ACTION_CMD"
	ActionLock       = "com.sj010.ghid_headless.ACTION_LOCK"
	StatusFileRemote = "/sdcard/Android/data/com.sj010.ghid_headless/files/status.json"
	LogFileRemote    = "/sdcard/Android/data/com.sj010.ghid_headless/files/gateway.log"
)

type Device struct {
	Serial string `json:"serial"`
	State  string `json:"state"`
	Model  string `json:"model,omitempty"`
}

type GatewayStatus struct {
	Registered bool   `json:"registered"`
	Connected  bool   `json:"connected"`
	State      string `json:"state"`
	Target     string `json:"target"`
	Address    string `json:"address"`
	Updated    string `json:"updated"`
}

// DirectiveHandler is invoked when a combo encounters a !DIRECTIVE step.
// If handled is true, the directive was processed.
type DirectiveHandler func(directive string) (bool, error)

type Manager struct {
	AdbPath          string
	Serial           string
	Root             string
	runner           Runner
	DirectiveHandler DirectiveHandler
}

// SetDirectiveHandler registers a custom handler for directives like !COMBO:SEND:<name>.
func (m *Manager) SetDirectiveHandler(h DirectiveHandler) {
	m.DirectiveHandler = h
}

func NewManager(serial string) (*Manager, error) {
	runner, err := NewExecRunner()
	if err != nil {
		return nil, err
	}

	root, _ := os.Getwd()
	return &Manager{
		AdbPath: runner.AdbPath(),
		Serial:  serial,
		Root:    root,
		runner:  runner,
	}, nil
}

func NewManagerWithRunner(runner Runner, serial, root string) *Manager {
	if root == "" {
		root, _ = os.Getwd()
	}
	return &Manager{
		AdbPath: runner.AdbPath(),
		Serial:  serial,
		Root:    root,
		runner:  runner,
	}
}

func (m *Manager) Run(args ...string) (string, error) {
	if m.runner == nil {
		r, err := NewExecRunner()
		if err != nil {
			return "", err
		}
		m.runner = r
		m.AdbPath = r.AdbPath()
	}
	return m.runner.Run(m.Serial, args...)
}

func (m *Manager) GetDevices() ([]Device, error) {
	if m.runner == nil {
		r, err := NewExecRunner()
		if err != nil {
			return nil, err
		}
		m.runner = r
		m.AdbPath = r.AdbPath()
	}

	out, err := m.runner.Run("", "devices", "-l")
	if err != nil {
		return nil, err
	}

	var devices []Device
	lines := strings.Split(out, "\n")
	if len(lines) <= 1 {
		return devices, nil
	}

	for _, line := range lines[1:] {
		line = strings.TrimSpace(line)
		if line == "" {
			continue
		}
		fields := strings.Fields(line)
		if len(fields) < 2 {
			continue
		}
		dev := Device{
			Serial: fields[0],
			State:  fields[1],
		}
		for _, f := range fields[2:] {
			if strings.HasPrefix(f, "model:") {
				dev.Model = strings.TrimPrefix(f, "model:")
			}
		}
		devices = append(devices, dev)
	}
	return devices, nil
}

func (m *Manager) SelectDevice() (*Device, error) {
	devices, err := m.GetDevices()
	if err != nil {
		return nil, err
	}
	if len(devices) == 0 {
		return nil, errors.New("no Android device detected via USB or Wireless ADB")
	}

	if m.Serial != "" {
		for _, d := range devices {
			if d.Serial == m.Serial {
				if d.State == "unauthorized" {
					return &d, errors.New("device is unauthorized. Please accept the USB debugging prompt on your phone screen")
				}
				if d.State == "offline" {
					return &d, errors.New("device is offline")
				}
				return &d, nil
			}
		}
		return nil, fmt.Errorf("device serial '%s' not found", m.Serial)
	}

	for _, d := range devices {
		if d.State == "device" {
			m.Serial = d.Serial
			return &d, nil
		}
	}

	first := devices[0]
	if first.State == "unauthorized" {
		return &first, errors.New("device is unauthorized. Please unlock phone and accept 'Allow USB debugging'")
	}
	return &first, fmt.Errorf("device in state '%s'", first.State)
}

func (m *Manager) ConnectWireless(addr string) error {
	out, err := m.runner.Run("", "connect", addr)
	s := strings.ToLower(out)
	if strings.Contains(s, "connected to") {
		m.Serial = addr
		return nil
	}
	if err != nil {
		return fmt.Errorf("%s (%w)", strings.TrimSpace(out), err)
	}
	return fmt.Errorf("%s", strings.TrimSpace(out))
}

func (m *Manager) PairWireless(addr, code string) error {
	out, err := m.runner.Run("", "pair", addr, code)
	s := strings.ToLower(out)
	if strings.Contains(s, "successfully paired") {
		return nil
	}
	if err != nil {
		return fmt.Errorf("%s (%w)", strings.TrimSpace(out), err)
	}
	return fmt.Errorf("%s", strings.TrimSpace(out))
}

func (m *Manager) IsPackageInstalled() bool {
	out, err := m.Run("shell", "pm", "list", "packages", PackageName)
	if err != nil {
		return false
	}
	return strings.Contains(out, "package:"+PackageName)
}

func (m *Manager) InstallApk(customPath string) error {
	apkPath := customPath
	if apkPath == "" {
		candidates := []string{
			filepath.Join(m.Root, "build", "ghost-hid.apk"),
			"build/ghost-hid.apk",
		}
		for _, c := range candidates {
			if _, err := os.Stat(c); err == nil {
				apkPath = c
				break
			}
		}
	}
	if apkPath == "" {
		return errors.New("ghost-hid.apk not found. Build it with 'ghid build' first")
	}

	_, err := m.Run("install", "-r", apkPath)
	return err
}

func (m *Manager) BuildApk() error {
	buildScript := filepath.Join(m.Root, "build.sh")
	if _, err := os.Stat(buildScript); err != nil {
		buildScript = "build.sh"
	}

	if m.runner == nil {
		r, err := NewExecRunner()
		if err != nil {
			return err
		}
		m.runner = r
	}
	return m.runner.RunCommand(filepath.Dir(buildScript), "bash", buildScript)
}

func (m *Manager) GetBluetoothState() bool {
	out, err := m.Run("shell", "settings", "get", "global", "bluetooth_on")
	if err != nil {
		return false
	}
	return strings.TrimSpace(out) == "1"
}

func (m *Manager) EnableBluetooth() error {
	_, err := m.Run("shell", "cmd", "bluetooth_manager", "enable")
	time.Sleep(1 * time.Second)
	return err
}

func (m *Manager) GetDeviceModel() string {
	out, err := m.Run("shell", "getprop", "ro.product.model")
	if err != nil || strings.TrimSpace(out) == "" {
		return "Android Device"
	}
	return strings.TrimSpace(out)
}

func (m *Manager) StartGateway() error {
	_, _ = m.Run("shell", "dumpsys", "deviceidle", "whitelist", "+"+PackageName)
	_, err := m.Run("shell", "am", "start-foreground-service", "-n", ServiceName)
	return err
}

func (m *Manager) StopGateway() error {
	_, _ = m.Run("shell", "dumpsys", "deviceidle", "whitelist", "+"+PackageName)
	_, _ = m.Run("shell", "am", "broadcast", "-n", ReceiverName, "-a", ActionCmd, "--es", "cmd", "stop")
	_, err := m.Run("shell", "am", "stopservice", "-n", ServiceName)
	return err
}

func (m *Manager) GetStatus() GatewayStatus {
	fallback := GatewayStatus{
		Registered: false,
		Connected:  false,
		State:      "OFFLINE",
		Target:     "none",
		Address:    "none",
		Updated:    "none",
	}

	out, err := m.Run("shell", "cat", StatusFileRemote)
	if err != nil {
		return fallback
	}

	out = strings.TrimSpace(out)
	if strings.HasPrefix(out, "{") && strings.HasSuffix(out, "}") {
		var status GatewayStatus
		if err := json.Unmarshal([]byte(out), &status); err == nil {
			return status
		}
	}
	return fallback
}

func (m *Manager) ConnectTarget(address string) error {
	args := []string{"shell", "am", "broadcast", "-n", ReceiverName, "-a", ActionCmd, "--es", "cmd", "connect"}
	if address != "" {
		args = append(args, "--es", "address", address)
	}
	_, err := m.Run(args...)
	return err
}

func (m *Manager) DisconnectTarget() error {
	_, err := m.Run("shell", "am", "broadcast", "-n", ReceiverName, "-a", ActionCmd, "--es", "cmd", "disconnect")
	return err
}

func (m *Manager) UnpairTarget() error {
	_, err := m.Run("shell", "am", "broadcast", "-n", ReceiverName, "-a", ActionCmd, "--es", "cmd", "unpair")
	return err
}

func (m *Manager) SendLock() error {
	_, err := m.Run("shell", "am", "broadcast", "-n", ReceiverName, "-a", ActionLock)
	return err
}

var varInTextRegex = regexp.MustCompile(`\$([a-zA-Z0-9_-]+)|\$\{([a-zA-Z0-9_-]+)\}`)

func expandTextVariables(text string, resolver func(string) (string, error)) string {
	return varInTextRegex.ReplaceAllStringFunc(text, func(match string) string {
		name := strings.TrimPrefix(match, "$")
		if strings.HasPrefix(name, "{") && strings.HasSuffix(name, "}") {
			name = name[1 : len(name)-1]
		}
		if val, err := resolver(name); err == nil && val != "" {
			return val
		}
		return match
	})
}

// SendCombo executes a sequence of key chords, mouse actions, and typed strings.
func (m *Manager) SendCombo(keys string) error {
	return m.SendComboWithResolver(keys, nil)
}

// SendComboWithResolver executes a combo sequence, resolving any $VARIABLE
// references using the supplied resolver function.
func (m *Manager) SendComboWithResolver(keys string, resolver func(string) (string, error)) error {
	steps, err := hid.ParseActionSequence(keys)
	if err != nil {
		return err
	}
	for _, step := range steps {
		if step.Directive != "" {
			if m.DirectiveHandler != nil {
				handled, err := m.DirectiveHandler(step.Directive)
				if handled {
					if err != nil {
						return err
					}
					if step.Delay > 0 {
						time.Sleep(step.Delay)
					}
					continue
				}
			}
			if err := m.ExecuteBuiltinDirective(step.Directive); err != nil {
				return err
			}
		} else if step.Variable != "" {
			if resolver == nil {
				return fmt.Errorf("snippet variable $%s requires a resolver", step.Variable)
			}
			val, err := resolver(step.Variable)
			if err != nil {
				return err
			}
			if strings.TrimSpace(val) == "" {
				return fmt.Errorf("snippet variable $%s is empty", step.Variable)
			}
			if err := m.TypeText(val); err != nil {
				return err
			}
		} else if step.Text != "" {
			text := step.Text
			if resolver != nil {
				text = expandTextVariables(text, resolver)
			}
			if err := m.TypeText(text); err != nil {
				return err
			}
		} else if step.Mouse != nil {
			if err := m.sendMouseAction(*step.Mouse); err != nil {
				return err
			}
		} else {
			clean := hid.NormalizeComboForAndroid(step.Combo)
			if _, err := m.Run("shell", "am", "broadcast", "-n", ReceiverName, "-a", ActionCmd, "--es", "cmd", "combo", "--es", "keys", clean); err != nil {
				return err
			}
		}
		if step.Delay > 0 {
			time.Sleep(step.Delay)
		}
	}
	return nil
}

// ExecuteBuiltinDirective executes standard lifecycle directives:
// !EXIT, !DISCONNECT, !CONNECT, !RECONNECT, !START, !STOP, !RESTART, !UNPAIR.
func (m *Manager) ExecuteBuiltinDirective(directive string) error {
	upper := strings.ToUpper(strings.TrimSpace(directive))
	switch {
	case upper == "!EXIT":
		return hid.ErrExitRequested
	case upper == "!DISCONNECT":
		return m.DisconnectTarget()
	case upper == "!CONNECT":
		return m.ConnectTarget("")
	case strings.HasPrefix(upper, "!CONNECT:"):
		target := strings.TrimPrefix(directive, "!CONNECT:")
		return m.ConnectTarget(strings.TrimSpace(target))
	case upper == "!RECONNECT":
		_ = m.DisconnectTarget()
		time.Sleep(500 * time.Millisecond)
		return m.ConnectTarget("")
	case upper == "!START":
		return m.StartGateway()
	case upper == "!STOP":
		return m.StopGateway()
	case upper == "!RESTART":
		_ = m.StopGateway()
		time.Sleep(500 * time.Millisecond)
		return m.StartGateway()
	case upper == "!UNPAIR":
		return m.UnpairTarget()
	case strings.HasPrefix(upper, "!COMBO:SEND:"):
		return fmt.Errorf("directive %s requires combo store context", directive)
	default:
		return fmt.Errorf("unsupported directive %q", directive)
	}
}

// SendMouse dispatches a single Windows mouse gesture through the phone.
func (m *Manager) SendMouse(action string) error {
	parsed, ok, err := hid.ParseMouseAction(action)
	if err != nil {
		return err
	}
	if !ok {
		return fmt.Errorf("unknown mouse action %q (use %s)", action, strings.Join(hid.MouseActions(), ", "))
	}
	return m.sendMouseAction(parsed)
}

func (m *Manager) sendMouseAction(action hid.MouseAction) error {
	_, err := m.Run("shell", "am", "broadcast", "-n", ReceiverName, "-a", ActionCmd, "--es", "cmd", "mouse", "--es", "action", action.Name)
	return err
}

func (m *Manager) TypeText(text string) error {
	_, err := m.Run("shell", "am", "broadcast", "-n", ReceiverName, "-a", ActionCmd, "--es", "cmd", "type", "--es", "text", text)
	return err
}

func (m *Manager) SendRaw(hexReport string) error {
	if len(hexReport) != 16 {
		return fmt.Errorf("raw report must be exactly 16 hex characters (8 bytes), got %d", len(hexReport))
	}
	_, err := m.Run("shell", "am", "broadcast", "-n", ReceiverName, "-a", ActionCmd, "--es", "cmd", "raw", "--es", "hex", hexReport)
	return err
}

func (m *Manager) SendSleepSequence() error {
	_ = m.SendCombo("esc")
	time.Sleep(300 * time.Millisecond)
	_ = m.SendCombo("esc")
	time.Sleep(400 * time.Millisecond)
	_ = m.SendCombo("win+x")
	time.Sleep(600 * time.Millisecond)
	_ = m.SendCombo("u")
	time.Sleep(400 * time.Millisecond)
	return m.SendCombo("s")
}

func (m *Manager) GetLogs(lines int) (string, error) {
	out, err := m.Run("logcat", "-d")
	if err != nil {
		return "", err
	}
	allLines := strings.Split(out, "\n")
	var matched []string
	for _, l := range allLines {
		if strings.Contains(strings.ToUpper(l), "GHOST_HID") {
			matched = append(matched, l)
		}
	}
	if len(matched) > lines {
		matched = matched[len(matched)-lines:]
	}
	return strings.Join(matched, "\n"), nil
}

func (m *Manager) GetFileLog() (string, error) {
	out, err := m.Run("shell", "cat", LogFileRemote)
	if err != nil || strings.TrimSpace(out) == "" {
		return "Remote gateway.log empty or missing.", nil
	}
	return strings.TrimSpace(out), nil
}

func (m *Manager) ExecuteLogin(user, pass string, delays map[string]float64, stepCb func(int, string)) error {
	getDelay := func(key string, def float64) time.Duration {
		if v, ok := delays[key]; ok && v > 0 {
			return time.Duration(v * float64(time.Second))
		}
		return time.Duration(def * float64(time.Second))
	}

	if stepCb != nil {
		stepCb(1, "Waking lock screen (Ctrl + Alt + Delete)...")
	}
	_ = m.SendCombo("ctrl+alt+del")
	time.Sleep(getDelay("wake", 1.0))

	if stepCb != nil {
		stepCb(2, "Dismissing lock overlay (Escape)...")
	}
	_ = m.SendCombo("esc")
	time.Sleep(getDelay("overlay", 1.0))

	if stepCb != nil {
		stepCb(3, fmt.Sprintf("Typing username (%d chars)...", len(user)))
	}
	_ = m.TypeText(user)
	time.Sleep(getDelay("user", 1.2))

	if stepCb != nil {
		stepCb(4, "Navigating to password field (Tab)...")
	}
	_ = m.SendCombo("tab")
	time.Sleep(getDelay("tab", 0.8))

	if stepCb != nil {
		stepCb(5, fmt.Sprintf("Injecting password (%d chars)...", len(pass)))
	}
	_ = m.TypeText(pass)
	time.Sleep(getDelay("pass", 1.2))

	if stepCb != nil {
		stepCb(6, "Submitting authentication credentials (Enter)...")
	}
	_ = m.SendCombo("enter")
	time.Sleep(getDelay("submit", 1.0))

	return nil
}
