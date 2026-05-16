import "react-native-get-random-values";
import { useCallback, useState } from "react";
import {
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from "react-native";
import { StatusBar } from "expo-status-bar";
import { CameraView, useCameraPermissions } from "expo-camera";
import { ethers } from "ethers";
import {
  DEFAULT_API_BASE,
  DEMO_CREDENTIAL,
  DEMO_QR_PAYLOAD,
  DEMO_SIGNER_PRIVATE_KEY,
} from "./src/demoData";

export default function App() {
  const [screen, setScreen] = useState("home");
  const [apiBase, setApiBase] = useState(DEFAULT_API_BASE);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const [sessionId, setSessionId] = useState(null);
  const [challengeNonce, setChallengeNonce] = useState(null);
  const [did, setDid] = useState(null);

  const [verifyStatus, setVerifyStatus] = useState(null);
  const [reasons, setReasons] = useState([]);

  const [pasteQr, setPasteQr] = useState("");
  const [adminSecret, setAdminSecret] = useState("demo-secret-change-me");

  const [permission, requestPermission] = useCameraPermissions();
  const [scanning, setScanning] = useState(true);

  const resetFlow = useCallback(() => {
    setSessionId(null);
    setChallengeNonce(null);
    setDid(null);
    setVerifyStatus(null);
    setReasons([]);
    setError(null);
    setScanning(true);
    setScreen("home");
  }, []);

  const parseQrPayload = (raw) => {
    let payload = raw.trim();
    if (!payload) throw new Error("Empty QR payload");
    const parsed = JSON.parse(payload);
    if (!parsed.did) throw new Error("QR JSON missing did");
    return parsed;
  };

  const postVerifyQr = async (payloadObj) => {
    const res = await fetch(`${apiBase.replace(/\/$/, "")}/verify-qr`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ qrPayload: payloadObj }),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok) throw new Error(data.error || data.detail || `HTTP ${res.status}`);
    return data;
  };

  const postVerifySignature = async (sigBody) => {
    const res = await fetch(`${apiBase.replace(/\/$/, "")}/verify-nfc-signature`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(sigBody),
    });
    const data = await res.json().catch(() => ({}));
    if (!res.ok && !data.status) throw new Error(data.error || `HTTP ${res.status}`);
    return data;
  };

  const handleQrDecoded = async (raw) => {
    setError(null);
    setLoading(true);
    try {
      const payloadObj = typeof raw === "string" ? parseQrPayload(raw) : raw;
      const data = await postVerifyQr(payloadObj);
      setSessionId(data.sessionId);
      setChallengeNonce(data.challengeNonce);
      setDid(data.did);
      setScreen("challenge");
    } catch (e) {
      setError(String(e.message || e));
    } finally {
      setLoading(false);
      setScanning(false);
    }
  };

  const simulateNfcSign = async () => {
    setError(null);
    setLoading(true);
    try {
      const wallet = new ethers.Wallet(DEMO_SIGNER_PRIVATE_KEY);
      const message = `${sessionId}:${challengeNonce}:${did}`;
      const signature = await wallet.signMessage(message);
      const result = await postVerifySignature({
        credential: DEMO_CREDENTIAL,
        sessionId,
        signature,
      });
      setVerifyStatus(result.status || "REJECTED");
      setReasons(Array.isArray(result.reasons) ? result.reasons : []);
      setScreen("result");
    } catch (e) {
      setError(String(e.message || e));
    } finally {
      setLoading(false);
    }
  };

  const simulateWrongKeySign = async () => {
    setError(null);
    setLoading(true);
    try {
      const rogue = ethers.Wallet.createRandom();
      const message = `${sessionId}:${challengeNonce}:${did}`;
      const signature = await rogue.signMessage(message);
      const result = await postVerifySignature({
        credential: DEMO_CREDENTIAL,
        sessionId,
        signature,
      });
      setVerifyStatus(result.status || "REJECTED");
      setReasons(Array.isArray(result.reasons) ? result.reasons : []);
      setScreen("result");
    } catch (e) {
      setError(String(e.message || e));
    } finally {
      setLoading(false);
    }
  };

  const demoRevoke = async () => {
    setError(null);
    setLoading(true);
    try {
      const res = await fetch(`${apiBase.replace(/\/$/, "")}/admin/revoke`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Demo-Admin-Secret": adminSecret,
        },
        body: JSON.stringify({ did: DEMO_CREDENTIAL.did }),
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.error || `HTTP ${res.status}`);
      setError(null);
      Alert.alert("Revoked", `On-chain revoke submitted.\nTx: ${data.txHash || "ok"}`);
    } catch (e) {
      setError(String(e.message || e));
    } finally {
      setLoading(false);
    }
  };

  const renderHome = () => (
    <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
      <Text style={styles.title}>VERID</Text>
      <Text style={styles.sub}>
        Verifier app · QR → blockchain check · simulated NFC signature
      </Text>

      <Text style={styles.label}>Backend URL</Text>
      <TextInput
        style={styles.input}
        value={apiBase}
        onChangeText={setApiBase}
        placeholder="http://YOUR_IP:8787"
        placeholderTextColor="#64748b"
        autoCapitalize="none"
        autoCorrect={false}
      />
      <Text style={styles.hint}>
        Simulator: localhost works (iOS). Android emulator: http://10.0.2.2:8787 · Physical
        device: your Mac/PC LAN IP.
      </Text>

      <Pressable style={styles.primaryBtn} onPress={() => setScreen("scan")}>
        <Text style={styles.primaryBtnText}>Scan QR</Text>
      </Pressable>

      <Text style={styles.label}>Paste QR JSON (demo)</Text>
      <TextInput
        style={[styles.input, styles.mono]}
        value={pasteQr}
        onChangeText={setPasteQr}
        placeholder={DEMO_QR_PAYLOAD}
        placeholderTextColor="#475569"
        multiline
      />
      <Pressable
        style={styles.secondaryBtn}
        onPress={() => {
          const raw = pasteQr.trim() || DEMO_QR_PAYLOAD;
          handleQrDecoded(raw);
        }}
      >
        <Text style={styles.secondaryBtnText}>Use pasted / demo QR</Text>
      </Pressable>

      <Pressable style={styles.linkBtn} onPress={() => setScreen("lab")}>
        <Text style={styles.linkText}>Issuer lab (revoke demo)</Text>
      </Pressable>
    </ScrollView>
  );

  const renderScan = () => {
    if (!permission) {
      return (
        <View style={styles.centered}>
          <Text style={styles.body}>Checking camera permission…</Text>
        </View>
      );
    }
    if (!permission.granted) {
      return (
        <View style={styles.centered}>
          <Text style={styles.body}>Camera access is needed to scan passport QR codes.</Text>
          <Pressable style={styles.primaryBtn} onPress={requestPermission}>
            <Text style={styles.primaryBtnText}>Allow camera</Text>
          </Pressable>
          <Pressable style={styles.linkBtn} onPress={() => setScreen("home")}>
            <Text style={styles.linkText}>Back</Text>
          </Pressable>
        </View>
      );
    }

    return (
      <View style={styles.flex}>
        <CameraView
          style={styles.camera}
          facing="back"
          barcodeScannerSettings={{ barcodeTypes: ["qr"] }}
          onBarcodeScanned={
            scanning && !loading
              ? ({ data }) => {
                  setScanning(false);
                  handleQrDecoded(data);
                }
              : undefined
          }
        />
        <View style={styles.scanOverlay}>
          <Text style={styles.overlayTitle}>Align QR in frame</Text>
          {loading && <ActivityIndicator color="#38bdf8" size="large" />}
          {error ? <Text style={styles.err}>{error}</Text> : null}
          <Pressable
            style={styles.secondaryBtn}
            onPress={() => {
              setScanning(true);
              setError(null);
            }}
          >
            <Text style={styles.secondaryBtnText}>Scan again</Text>
          </Pressable>
          <Pressable style={styles.linkBtn} onPress={() => setScreen("home")}>
            <Text style={styles.linkText}>Cancel</Text>
          </Pressable>
        </View>
      </View>
    );
  };

  const renderChallenge = () => (
    <ScrollView contentContainerStyle={styles.scroll}>
      <Text style={styles.title}>Challenge</Text>
      <Text style={styles.body}>Session bound to DID. Simulated NFC signs the challenge.</Text>
      <View style={styles.card}>
        <Text style={styles.monoSmall}>{did}</Text>
        <Text style={styles.monoSmall} numberOfLines={1}>
          session: {sessionId}
        </Text>
      </View>
      {error ? <Text style={styles.err}>{error}</Text> : null}
      {loading ? <ActivityIndicator color="#38bdf8" style={{ marginVertical: 16 }} /> : null}
      <Pressable style={styles.primaryBtn} onPress={simulateNfcSign} disabled={loading}>
        <Text style={styles.primaryBtnText}>Simulate NFC tap (correct key)</Text>
      </Pressable>
      <Pressable style={styles.dangerBtn} onPress={simulateWrongKeySign} disabled={loading}>
        <Text style={styles.dangerBtnText}>Simulate wrong key (reject demo)</Text>
      </Pressable>
      <Pressable style={styles.linkBtn} onPress={resetFlow}>
        <Text style={styles.linkText}>Start over</Text>
      </Pressable>
    </ScrollView>
  );

  const renderResult = () => {
    const ok = verifyStatus === "VERIFIED";
    return (
      <ScrollView contentContainerStyle={styles.scroll}>
        <View style={[styles.badge, ok ? styles.badgeOk : styles.badgeBad]}>
          <Text style={styles.badgeText}>{verifyStatus}</Text>
        </View>
        <Text style={styles.section}>Reasons</Text>
        {reasons.map((r, i) => (
          <Text key={i} style={styles.reason}>
            · {r}
          </Text>
        ))}
        <Pressable style={styles.primaryBtn} onPress={resetFlow}>
          <Text style={styles.primaryBtnText}>New verification</Text>
        </Pressable>
      </ScrollView>
    );
  };

  const renderLab = () => (
    <ScrollView contentContainerStyle={styles.scroll}>
      <Text style={styles.title}>Issuer lab</Text>
      <Text style={styles.body}>
        Sends an on-chain revoke for the demo DID (issuer key must be configured on the backend).
      </Text>
      <Text style={styles.label}>Demo admin secret</Text>
      <TextInput
        style={styles.input}
        value={adminSecret}
        onChangeText={setAdminSecret}
        placeholderTextColor="#64748b"
        autoCapitalize="none"
      />
      {error ? <Text style={styles.err}>{error}</Text> : null}
      <Pressable style={styles.dangerBtn} onPress={demoRevoke} disabled={loading}>
        <Text style={styles.dangerBtnText}>Revoke demo passport</Text>
      </Pressable>
      <Pressable style={styles.linkBtn} onPress={() => setScreen("home")}>
        <Text style={styles.linkText}>Back home</Text>
      </Pressable>
    </ScrollView>
  );

  return (
    <KeyboardAvoidingView
      style={styles.root}
      behavior={Platform.OS === "ios" ? "padding" : undefined}
    >
      <StatusBar style="light" />
      {screen === "home" && renderHome()}
      {screen === "scan" && renderScan()}
      {screen === "challenge" && renderChallenge()}
      {screen === "result" && renderResult()}
      {screen === "lab" && renderLab()}
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: "#0f172a",
    paddingTop: Platform.OS === "android" ? 40 : 56,
    paddingHorizontal: 20,
    paddingBottom: 24,
  },
  flex: { flex: 1 },
  scroll: { paddingBottom: 48 },
  centered: { flex: 1, justifyContent: "center", gap: 16 },
  title: {
    fontSize: 28,
    fontWeight: "700",
    color: "#f8fafc",
    marginBottom: 8,
  },
  sub: { color: "#94a3b8", marginBottom: 24, fontSize: 15, lineHeight: 22 },
  section: {
    color: "#cbd5e1",
    fontWeight: "600",
    marginTop: 16,
    marginBottom: 8,
    fontSize: 16,
  },
  body: { color: "#cbd5e1", fontSize: 15, lineHeight: 22, marginBottom: 12 },
  label: { color: "#94a3b8", fontSize: 13, marginBottom: 6, marginTop: 12 },
  hint: { color: "#64748b", fontSize: 12, marginBottom: 8, lineHeight: 18 },
  input: {
    backgroundColor: "#1e293b",
    borderRadius: 12,
    padding: 14,
    color: "#f8fafc",
    fontSize: 15,
    borderWidth: 1,
    borderColor: "#334155",
  },
  mono: { fontFamily: Platform.OS === "ios" ? "Menlo" : "monospace", fontSize: 13 },
  monoSmall: {
    fontFamily: Platform.OS === "ios" ? "Menlo" : "monospace",
    fontSize: 12,
    color: "#cbd5e1",
    marginBottom: 6,
  },
  primaryBtn: {
    backgroundColor: "#0ea5e9",
    paddingVertical: 16,
    borderRadius: 14,
    alignItems: "center",
    marginTop: 16,
  },
  primaryBtnText: { color: "#0f172a", fontWeight: "700", fontSize: 16 },
  secondaryBtn: {
    borderWidth: 1,
    borderColor: "#475569",
    paddingVertical: 14,
    borderRadius: 14,
    alignItems: "center",
    marginTop: 12,
  },
  secondaryBtnText: { color: "#e2e8f0", fontWeight: "600", fontSize: 15 },
  dangerBtn: {
    backgroundColor: "#7f1d1d",
    paddingVertical: 14,
    borderRadius: 14,
    alignItems: "center",
    marginTop: 12,
  },
  dangerBtnText: { color: "#fecaca", fontWeight: "700", fontSize: 15 },
  linkBtn: { marginTop: 16, alignSelf: "flex-start" },
  linkText: { color: "#38bdf8", fontSize: 15, fontWeight: "600" },
  card: {
    backgroundColor: "#1e293b",
    padding: 16,
    borderRadius: 12,
    marginTop: 16,
    borderWidth: 1,
    borderColor: "#334155",
  },
  camera: { flex: 1, borderRadius: 16, overflow: "hidden" },
  scanOverlay: {
    paddingVertical: 16,
    gap: 8,
  },
  overlayTitle: {
    color: "#f8fafc",
    fontWeight: "600",
    fontSize: 16,
    textAlign: "center",
    marginBottom: 8,
  },
  err: { color: "#fca5a5", marginTop: 8, fontSize: 14 },
  badge: {
    alignSelf: "flex-start",
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 12,
    marginBottom: 20,
  },
  badgeOk: { backgroundColor: "#14532d" },
  badgeBad: { backgroundColor: "#7f1d1d" },
  badgeText: { color: "#f8fafc", fontWeight: "800", fontSize: 22 },
  reason: { color: "#cbd5e1", fontSize: 15, marginBottom: 6, paddingRight: 8 },
});
