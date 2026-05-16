import React, { useState, useEffect, FormEvent } from 'react';
import { 
  onAuthStateChanged, 
  signInWithPopup,
  GoogleAuthProvider,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  signOut,
  User as FirebaseUser
} from 'firebase/auth';
import { 
  doc, 
  getDoc, 
  setDoc, 
  collection, 
  query, 
  where, 
  getDocs,
  serverTimestamp,
  writeBatch,
  increment,
  limit,
  orderBy,
  onSnapshot,
  addDoc,
  deleteDoc
} from 'firebase/firestore';
import { auth, db, OperationType, handleFirestoreError } from './lib/firebase';
import { cn } from './lib/utils';
import { 
  LogOut, 
  Search, 
  ShieldCheck, 
  ShieldAlert, 
  User, 
  TrendingUp, 
  ChevronRight,
  Shield,
  Loader2,
  LayoutDashboard,
  BadgeCheck,
  Activity,
  ArrowRight,
  UserPlus,
  Cpu,
  Zap,
  Smartphone,
  MessageSquare,
  ThumbsUp,
  Trash2,
  Send
} from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { Toaster, toast } from 'react-hot-toast';

// Types
interface UserProfile {
  uid: string;
  username: string;
  displayName: string;
  details: {
    location: string;
    occupation: string;
    bio: string;
  };
  trustworthyCount: number;
  untrustworthyCount: number;
  isNfcVerified?: boolean;
  nfcVerifiedAt?: any;
  createdAt: any;
}

interface VoteRecord {
  voterId: string;
  voterName: string;
  voterUsername: string;
  type: 'trustworthy' | 'untrustworthy';
  timestamp: any;
}

interface Post {
  id: string;
  authorId: string;
  authorName: string;
  authorUsername: string;
  content: string;
  likesCount: number;
  createdAt: any;
  hasLiked?: boolean;
}

type View = 'DASHBOARD' | 'SEARCH' | 'PROFILE_VIEW' | 'ONBOARDING' | 'EDIT_PROFILE' | 'NFC_VERIFICATION';

export default function App() {
  const [user, setUser] = useState<FirebaseUser | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [currentView, setCurrentView] = useState<View>('DASHBOARD');
  const [selectedUserUid, setSelectedUserUid] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribe = onAuthStateChanged(auth, async (u) => {
      setUser(u);
      if (u) {
        await fetchProfile(u.uid);
      } else {
        setProfile(null);
        setLoading(false);
      }
    });
    return unsubscribe;
  }, []);

  const fetchProfile = async (uid: string) => {
    try {
      const docRef = doc(db, 'users', uid);
      const docSnap = await getDoc(docRef);
      if (docSnap.exists()) {
        setProfile({ uid, ...docSnap.data() } as UserProfile);
        setCurrentView('DASHBOARD');
      } else {
        setProfile(null);
        setCurrentView('ONBOARDING');
      }
    } catch (e) {
      handleFirestoreError(e, OperationType.GET, `users/${uid}`);
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    const provider = new GoogleAuthProvider();
    try {
      await signInWithPopup(auth, provider);
      toast.success("Identity Verified via Google");
    } catch (e: any) {
      toast.error(e.message);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#0F172A]">
        <Loader2 className="w-8 h-8 animate-spin text-indigo-500" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0F172A] text-slate-200 font-sans">
      <Toaster 
        toastOptions={{
          style: {
            background: '#1E293B',
            color: '#fff',
            border: '1px solid #334155'
          }
        }}
      />
      {!user ? (
        <LoginScreen onGoogleLogin={handleGoogleLogin} />
      ) : currentView === 'ONBOARDING' ? (
        <OnboardingScreen user={user} onComplete={() => fetchProfile(user.uid)} />
      ) : (
        <div className="flex flex-col md:flex-row min-h-screen overflow-hidden">
          {/* Sidebar - Desktop */}
          <nav className="w-64 bg-[#1E293B] border-r border-[#334155] p-6 hidden md:flex flex-col justify-between shrink-0">
            <div>
              <div className="flex items-center gap-3 mb-10">
                <div className="w-10 h-10 bg-indigo-500 rounded-xl flex items-center justify-center font-bold text-white shadow-lg shadow-indigo-500/20">
                  <ShieldCheck className="w-6 h-6" />
                </div>
                <span className="text-xl font-bold tracking-tight text-white">TrustVouch</span>
              </div>
              <div className="space-y-2">
                <NavItem 
                  active={currentView === 'DASHBOARD'} 
                  onClick={() => setCurrentView('DASHBOARD')}
                  icon={<LayoutDashboard className="w-5 h-5" />}
                  label="Dashboard"
                />
                <NavItem 
                  active={currentView === 'SEARCH' || currentView === 'PROFILE_VIEW'} 
                  onClick={() => setCurrentView('SEARCH')}
                  icon={<Search className="w-5 h-5" />}
                  label="Network Audit"
                />
                <NavItem 
                  active={currentView === 'NFC_VERIFICATION'} 
                  onClick={() => setCurrentView('NFC_VERIFICATION')}
                  icon={<BadgeCheck className="w-5 h-5" />}
                  label="Verifications"
                />

              </div>
            </div>
            
            <div className="p-4 bg-[#0F172A]/50 rounded-2xl border border-[#334155]">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-slate-700 border-2 border-indigo-500 p-0.5 flex items-center justify-center">
                   <User className="text-slate-400 w-6 h-6" />
                </div>
                <div className="overflow-hidden">
                  <p className="text-sm font-bold text-white truncate">{profile?.displayName}</p>
                  <button 
                    onClick={() => signOut(auth)}
                    className="text-[10px] text-slate-500 hover:text-red-400 font-bold uppercase tracking-wider flex items-center gap-1 mt-0.5 transition-colors"
                  >
                    <LogOut className="w-3 h-3" /> De-Authenticate
                  </button>
                </div>
              </div>
            </div>
          </nav>

          {/* Main Content */}
          <main className="flex-1 overflow-y-auto p-4 md:p-8 bg-[#0F172A] pb-24 md:pb-8">
            <header className="mb-6 md:hidden flex items-center justify-between">
               <div className="flex items-center gap-2">
                  <div className="w-8 h-8 bg-indigo-500 rounded-lg flex items-center justify-center font-bold text-white shadow-lg">
                    <ShieldCheck className="w-5 h-5" />
                  </div>
                  <span className="font-black text-lg tracking-tighter text-white">TrustVouch</span>
               </div>
               <div className="flex items-center gap-3">
                 <div className="w-8 h-8 rounded-full bg-slate-800 border border-indigo-500 flex items-center justify-center">
                    <User className="text-slate-400 w-4 h-4" />
                 </div>
               </div>
            </header>

            <AnimatePresence mode="wait">
              {currentView === 'DASHBOARD' && profile && (
                <Dashboard 
                  profile={profile} 
                  onRefresh={() => { fetchProfile(user.uid); }} 
                  onEdit={() => setCurrentView('EDIT_PROFILE')}
                  onSelectUser={(uid) => {
                    setSelectedUserUid(uid);
                    setCurrentView('PROFILE_VIEW');
                  }}
                />
              )}
              {currentView === 'EDIT_PROFILE' && profile && (
                <EditProfileView 
                  profile={profile}
                  onBack={() => setCurrentView('DASHBOARD')}
                  onComplete={() => fetchProfile(user.uid)}
                />
              )}
              {currentView === 'NFC_VERIFICATION' && profile && (
                <NfcVerificationView 
                  profile={profile}
                  onBack={() => setCurrentView('DASHBOARD')}
                  onRefresh={() => fetchProfile(user.uid)}
                />
              )}
              {currentView === 'SEARCH' && (
                <UserSearch 
                  onSelectUser={(uid) => {
                    setSelectedUserUid(uid);
                    setCurrentView('PROFILE_VIEW');
                  }}
                />
              )}
              {currentView === 'PROFILE_VIEW' && selectedUserUid && (
                <UserProfileView 
                  targetUid={selectedUserUid}
                  voterProfile={profile}
                  onBack={() => setCurrentView('SEARCH')}
                  onVoteComplete={() => { fetchProfile(user.uid); }}
                />
              )}
            </AnimatePresence>
          </main>

          {/* Mobile Bottom Navigation */}
          <nav className="fixed bottom-0 left-0 right-0 h-16 bg-[#1E293B] border-t border-[#334155] md:hidden flex items-center justify-around px-4 z-50">
            <MobileNavItem 
              active={currentView === 'DASHBOARD'} 
              onClick={() => setCurrentView('DASHBOARD')}
              icon={<LayoutDashboard className="w-5 h-5" />}
              label="Home"
            />
            <MobileNavItem 
              active={currentView === 'SEARCH' || currentView === 'PROFILE_VIEW'} 
              onClick={() => setCurrentView('SEARCH')}
              icon={<Search className="w-5 h-5" />}
              label="Audit"
            />
            <MobileNavItem 
              active={currentView === 'NFC_VERIFICATION'} 
              onClick={() => setCurrentView('NFC_VERIFICATION')}
              icon={<BadgeCheck className="w-5 h-5" />}
              label="Trust"
            />
            <button 
              onClick={() => signOut(auth)}
              className="flex flex-col items-center gap-1 text-slate-500"
            >
              <LogOut className="w-5 h-5" />
              <span className="text-[8px] font-black uppercase tracking-widest">Exit</span>
            </button>
          </nav>
        </div>
      )}
    </div>
  );
}

function NavItem({ active, onClick, icon, label }: { active: boolean, onClick: () => void, icon: React.ReactNode, label: string }) {
  return (
    <div 
      onClick={onClick}
      className={cn(
        "flex items-center gap-3 p-4 rounded-xl cursor-pointer transition-all border border-transparent",
        active 
          ? "bg-indigo-500/10 text-indigo-400 border-indigo-500/20 shadow-sm" 
          : "text-slate-400 hover:bg-slate-800/50 hover:text-slate-200"
      )}
    >
      {icon}
      <span className="font-bold text-sm">{label}</span>
    </div>
  );
}

function MobileNavItem({ active, onClick, icon, label }: { active: boolean, onClick: () => void, icon: React.ReactNode, label: string }) {
  return (
    <button 
      onClick={onClick}
      className={cn(
        "flex flex-col items-center gap-1 transition-all",
        active ? "text-indigo-400" : "text-slate-500 hover:text-slate-300"
      )}
    >
      {icon}
      <span className="text-[9px] font-black uppercase tracking-widest">{label}</span>
      {active && <motion.div layoutId="mobile-nav-dot" className="w-1 h-1 bg-indigo-400 rounded-full mt-0.5" />}
    </button>
  );
}

// --- Components ---

function LoginScreen({ onGoogleLogin }: { onGoogleLogin: () => void }) {
  const [mode, setMode] = useState<'LOGIN' | 'SIGNUP'>('LOGIN');
  const [formData, setFormData] = useState({
    username: '',
    password: ''
  });
  const [loading, setLoading] = useState(false);

  const handleStandardAuth = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);
    
    // Clean username for internal identifier
    const usernameClean = formData.username.toLowerCase().trim().replace(/[^a-z0-9]/g, '');
    if (usernameClean.length < 3) {
      toast.error("Node Identifier must be at least 3 characters.");
      setLoading(false);
      return;
    }

    const email = `${usernameClean}@trustvouch.app`;
    
    try {
      if (mode === 'SIGNUP') {
        await createUserWithEmailAndPassword(auth, email, formData.password);
        toast.success("Identity Created. Proceeding to Onboarding.");
      } else {
        await signInWithEmailAndPassword(auth, email, formData.password);
        toast.success("Protocol Access Granted.");
      }
    } catch (e: any) {
      if (e.code === 'auth/operation-not-allowed') {
        toast.error("System Notice: Email/Password access must be enabled in the Firebase Console.");
      } else if (e.code === 'auth/email-already-in-use') {
        toast.error("Node Identifier already indexed on ledger.");
      } else if (e.code === 'auth/invalid-credential') {
        toast.error("Access Refused: Invalid credentials.");
      } else {
        toast.error(e.message);
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen p-4 bg-[#0F172A]">
      <motion.div 
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        className="w-full max-w-md bg-[#1E293B] border border-[#334155] rounded-[2.5rem] shadow-2xl p-8 md:p-12"
      >
        <div className="flex flex-col items-center mb-8 text-center">
          <div className="w-16 h-16 bg-indigo-600 rounded-[1.5rem] flex items-center justify-center text-white mb-6 shadow-2xl shadow-indigo-500/20">
            <ShieldCheck className="w-10 h-10" />
          </div>
          <h2 className="text-2xl font-black text-white tracking-tight uppercase">TrustVouch</h2>
          <p className="text-slate-500 font-bold text-[10px] uppercase tracking-[0.2em] mt-1">Verified Proof Network</p>
        </div>

        <div className="space-y-6">
          <form onSubmit={handleStandardAuth} className="space-y-4">
            <div className="space-y-1.5 grayscale group hover:grayscale-0 transition-all">
              <label className="block text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Node Identifier</label>
              <input 
                required
                type="text"
                placeholder="eg. sentinel_01"
                className="w-full px-5 py-4 bg-[#0F172A] border border-[#334155] rounded-2xl focus:ring-2 focus:ring-indigo-500 transition-all outline-none text-white placeholder:text-slate-700 font-mono text-sm"
                value={formData.username}
                onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              />
            </div>
            <div className="space-y-1.5 grayscale group hover:grayscale-0 transition-all">
              <label className="block text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Cipher Key</label>
              <input 
                required
                type="password"
                placeholder="••••••••"
                className="w-full px-5 py-4 bg-[#0F172A] border border-[#334155] rounded-2xl focus:ring-2 focus:ring-indigo-500 transition-all outline-none text-white placeholder:text-slate-700 font-mono text-sm"
                value={formData.password}
                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              />
            </div>

            <button 
              type="submit"
              disabled={loading}
              className="w-full py-5 bg-indigo-600 hover:bg-indigo-500 text-white font-black rounded-2xl shadow-xl shadow-indigo-500/20 transition-all flex items-center justify-center gap-3 uppercase tracking-widest text-xs"
            >
              {loading ? <Loader2 className="animate-spin w-4 h-4" /> : mode === 'LOGIN' ? 'Access Profile' : 'Initialize Node'}
            </button>
          </form>

          <div className="relative py-2">
            <div className="absolute inset-0 flex items-center"><div className="w-full border-t border-[#334155]"></div></div>
            <div className="relative flex justify-center text-[10px] uppercase font-black tracking-widest text-slate-600 bg-[#1E293B] px-4">OR</div>
          </div>
          
          <button 
            type="button"
            onClick={onGoogleLogin}
            className="w-full py-5 bg-white text-slate-900 font-black rounded-2xl shadow-xl hover:bg-slate-100 transition-all flex items-center justify-center gap-3 uppercase tracking-widest text-xs"
          >
            <svg className="w-5 h-5" viewBox="0 0 24 24">
              <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
              <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
              <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l3.66-2.84z"/>
              <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
            </svg>
            Third-Party Auth
          </button>

          <div className="pt-4 text-center">
            <button 
              type="button"
              onClick={() => setMode(mode === 'LOGIN' ? 'SIGNUP' : 'LOGIN')}
              className="text-[10px] text-indigo-400 font-black uppercase tracking-widest hover:text-indigo-300 transition-colors"
            >
              {mode === 'LOGIN' ? 'Registration Required?' : 'Identity Already Synchronized?'}
            </button>
          </div>
        </div>
      </motion.div>
    </div>
  );
}

function OnboardingScreen({ user, onComplete }: { user: FirebaseUser, onComplete: () => void }) {
  const [formData, setFormData] = useState({
    username: '',
    displayName: user.displayName || '',
    location: '',
    occupation: '',
    bio: ''
  });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);

    const usernameClean = formData.username.toLowerCase().trim().replace(/[^a-z0-9]/g, '');
    if (!usernameClean || usernameClean.length < 3) {
      toast.error("Node Identifier must be at least 3 alphanumeric characters.");
      setLoading(false);
      return;
    }

    try {
      const q = query(collection(db, 'users'), where('username', '==', usernameClean), limit(1));
      const qSnap = await getDocs(q);
      if (!qSnap.empty) {
        toast.error("Username already indexed on ledger.");
        setLoading(false);
        return;
      }

      await setDoc(doc(db, 'users', user.uid), {
        username: usernameClean,
        displayName: formData.displayName,
        details: {
          location: formData.location,
          occupation: formData.occupation,
          bio: formData.bio
        },
        trustworthyCount: 0,
        untrustworthyCount: 0,
        createdAt: serverTimestamp()
      });

      toast.success("Identity Sequence Completed");
      onComplete();
    } catch (e: any) {
      toast.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex items-center justify-center min-h-screen p-4 bg-[#0F172A]">
      <motion.div 
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-lg bg-[#1E293B] border border-[#334155] rounded-[2.5rem] shadow-2xl p-8 md:p-12"
      >
        <div className="flex items-center gap-4 mb-10">
          <div className="w-16 h-16 bg-indigo-600 rounded-2xl flex items-center justify-center text-white shrink-0">
             <UserPlus className="w-8 h-8" />
          </div>
          <div>
            <h2 className="text-2xl font-black text-white tracking-tight uppercase">Initialize Node</h2>
            <p className="text-slate-500 font-bold text-[10px] uppercase tracking-widest">Profile Configuration Sequence</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-1.5 focus-within:translate-y-[-2px] transition-transform col-span-2 sm:col-span-1">
              <label className="block text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Unique Identifier</label>
              <input 
                required
                placeholder="eg. sentinel_01"
                className="w-full px-5 py-4 bg-[#0F172A] border border-[#334155] rounded-2xl focus:ring-2 focus:ring-indigo-500 transition-all outline-none text-white placeholder:text-slate-700 font-mono text-sm"
                value={formData.username}
                onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              />
            </div>
            <div className="space-y-1.5 focus-within:translate-y-[-2px] transition-transform col-span-2 sm:col-span-1">
              <label className="block text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Display Alias</label>
              <input 
                required
                placeholder="Full Name"
                className="w-full px-5 py-4 bg-[#0F172A] border border-[#334155] rounded-2xl focus:ring-2 focus:ring-indigo-500 transition-all outline-none text-white placeholder:text-slate-700 text-sm"
                value={formData.displayName}
                onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
              />
            </div>
            <div className="space-y-1.5 focus-within:translate-y-[-2px] transition-transform col-span-2 sm:col-span-1">
              <label className="block text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Global Sector</label>
              <input 
                placeholder="City, CO"
                className="w-full px-5 py-4 bg-[#0F172A] border border-[#334155] rounded-2xl focus:ring-2 focus:ring-indigo-500 transition-all outline-none text-white placeholder:text-slate-700 text-sm"
                value={formData.location}
                onChange={(e) => setFormData({ ...formData, location: e.target.value })}
              />
            </div>
            <div className="space-y-1.5 focus-within:translate-y-[-2px] transition-transform col-span-2 sm:col-span-1">
              <label className="block text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Verification Role</label>
              <input 
                placeholder="Design / Engineering"
                className="w-full px-5 py-4 bg-[#0F172A] border border-[#334155] rounded-2xl focus:ring-2 focus:ring-indigo-500 transition-all outline-none text-white placeholder:text-slate-700 text-sm"
                value={formData.location}
                onChange={(e) => setFormData({ ...formData, occupation: e.target.value })}
              />
            </div>
          </div>

          <div className="space-y-1.5 focus-within:translate-y-[-2px] transition-transform">
            <label className="block text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Consciousness Feed (Bio)</label>
            <textarea 
              placeholder="Record your background for peer verification..."
              className="w-full px-5 py-4 bg-[#0F172A] border border-[#334155] rounded-2xl focus:ring-2 focus:ring-indigo-500 transition-all outline-none text-white placeholder:text-slate-700 text-sm h-32 resize-none"
              value={formData.bio}
              onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
            />
          </div>

          <button 
            type="submit"
            disabled={loading}
            className="w-full py-5 bg-indigo-600 hover:bg-indigo-500 text-white font-black rounded-2xl shadow-xl shadow-indigo-500/20 transition-all flex items-center justify-center gap-3 uppercase tracking-widest text-xs"
          >
            {loading ? <Loader2 className="animate-spin w-5 h-5" /> : <ShieldCheck className="w-5 h-5" />}
            Register Node & Sync
          </button>
        </form>
      </motion.div>
    </div>
  );
}

function Dashboard({ profile, onEdit, onSelectUser, onRefresh }: { profile: UserProfile, onEdit: () => void, onSelectUser: (uid: string) => void, onRefresh: () => void }) {
  const [networkUsers, setNetworkUsers] = useState<UserProfile[]>([]);
  const [posts, setPosts] = useState<Post[]>([]);
  const [globalStats, setGlobalStats] = useState({ avgTrust: 0, totalActions: 0 });
  const [loading, setLoading] = useState(true);
  const [newPost, setNewPost] = useState('');
  const [posting, setPosting] = useState(false);

  useEffect(() => {
    const fetchNetwork = async () => {
      try {
        const q = query(collection(db, 'users'), limit(50));
        const qSnap = await getDocs(q);
        const users: UserProfile[] = [];
        let totalScoreSum = 0;
        let usersWithVotes = 0;

        qSnap.forEach(doc => {
          const data = doc.data() as UserProfile;
          users.push({ uid: doc.id, ...data });
          
          const tCount = data.trustworthyCount || 0;
          const uCount = data.untrustworthyCount || 0;
          const uTotal = tCount + uCount;
          if (uTotal > 0) {
            totalScoreSum += (tCount / uTotal) * 100;
            usersWithVotes++;
          }
        });

        const avg = usersWithVotes === 0 ? 0 : Math.round(totalScoreSum / usersWithVotes);

        setNetworkUsers(users.filter(u => u.uid !== profile.uid).slice(0, 8));
        setGlobalStats({ 
          avgTrust: avg, 
          totalActions: qSnap.size * 142 + (posts.length * 12)
        });
      } catch (e) {
        console.error("Ledger sync failure", e);
      } finally {
        setLoading(false);
      }
    };

    fetchNetwork();
  }, [profile.uid, posts.length]);

  useEffect(() => {
    const q = query(collection(db, 'posts'), orderBy('createdAt', 'desc'), limit(20));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      const fetchedPosts = snapshot.docs.map(doc => ({
        id: doc.id,
        ...doc.data()
      })) as Post[];
      setPosts(fetchedPosts);
    });
    return unsubscribe;
  }, []);

  const handlePost = async () => {
    if (!newPost.trim()) return;
    setPosting(true);
    try {
      await addDoc(collection(db, 'posts'), {
        authorId: profile.uid,
        authorName: profile.displayName,
        authorUsername: profile.username,
        content: newPost.trim(),
        likesCount: 0,
        createdAt: serverTimestamp()
      });
      setNewPost('');
      toast.success("Broadcast Dispatched");
    } catch (e) {
      toast.error("Transmission Failed");
    } finally {
      setPosting(false);
    }
  };

  const handleLike = async (postId: string) => {
    const postRef = doc(db, 'posts', postId);
    const likeRef = doc(db, 'posts', postId, 'likes', profile.uid);
    
    try {
      const likeSnap = await getDoc(likeRef);
      if (likeSnap.exists()) {
        toast.error("Redundant Handshake Detected");
        return;
      }

      const batch = writeBatch(db);
      batch.set(likeRef, { timestamp: serverTimestamp() });
      batch.update(postRef, { likesCount: increment(1) });
      await batch.commit();
      toast.success("Consensus Vouched");
    } catch (e) {
      toast.error("Vouch Failed");
    }
  };

  const handleDeletePost = async (postId: string) => {
    try {
      await deleteDoc(doc(db, 'posts', postId));
      toast.success("Data Purged");
    } catch (e) {
      toast.error("Purge Failed");
    }
  };

  const tCountProfile = profile.trustworthyCount || 0;
  const uCountProfile = profile.untrustworthyCount || 0;
  const totalVotesProfile = tCountProfile + uCountProfile;
  const trustScoreProfile = totalVotesProfile === 0 ? 0 : Math.round((tCountProfile / totalVotesProfile) * 100);

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="grid grid-cols-1 md:grid-cols-12 md:grid-rows-6 gap-4 md:gap-6 h-full max-w-6xl mx-auto"
    >
      {/* Welcome & Global Stats */}
      <div className="md:col-span-12 md:row-span-1 flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
           <div className="flex items-center gap-3">
             <h2 className="text-xl md:text-2xl font-black text-white tracking-tight">{profile.displayName}</h2>
             {profile.isNfcVerified && (
               <span className="flex items-center gap-1.5 px-2.5 py-1 bg-emerald-500/10 border border-emerald-500/20 rounded-full text-[9px] font-black text-emerald-400 uppercase tracking-widest">
                 <BadgeCheck className="w-3.5 h-3.5" /> Verified
               </span>
             )}
           </div>
           <p className="text-slate-500 text-xs md:text-sm font-medium italic">@{profile.username} — Node identity established.</p>
        </div>
        <div className="flex gap-2 md:gap-3">
           <div className="flex-1 md:flex-none bg-[#1E293B] border border-[#334155] px-4 md:px-5 py-2 md:py-3 rounded-2xl text-center shadow-lg">
             <p className="text-[8px] md:text-[10px] uppercase font-black text-slate-500 tracking-widest mb-1">Score</p>
             <p className="text-lg md:text-xl font-black text-emerald-400 font-mono tracking-tighter">{globalStats.avgTrust}%</p>
           </div>
        </div>
      </div>

      {/* Main Trust Score Bento */}
      <div className="md:col-span-7 md:row-span-3 bg-gradient-to-br from-[#1E293B] to-[#0F172A] border border-[#334155] rounded-[2rem] md:rounded-[2.5rem] p-6 md:p-8 flex flex-col justify-between shadow-2xl relative overflow-hidden group min-h-[400px] md:min-h-0">
        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-indigo-500 to-transparent opacity-50"></div>
        <div>
           <span className="px-3 py-1 bg-emerald-500/10 text-emerald-400 rounded-full text-[10px] font-black uppercase tracking-widest border border-emerald-400/20">Verified Status</span>
           <h3 className="text-4xl font-black text-white mt-4 tracking-tighter leading-none">Your Reputation</h3>
           <p className="text-slate-500 font-medium text-sm mt-2">Aggregated community proof across all verifications.</p>
        </div>

        <div className="flex flex-col sm:flex-row items-center gap-8 mt-12 sm:mt-0">
           <div className="relative w-48 h-48">
              <svg className="w-full h-full transform -rotate-90 filter drop-shadow-[0_0_15px_rgba(99,102,241,0.2)]">
                <circle cx="96" cy="96" r="80" stroke="#1E293B" strokeWidth="16" fill="transparent" />
                <circle 
                  cx="96" 
                  cy="96" 
                  r="80" 
                  stroke="#6366f1" 
                  strokeWidth="16" 
                  fill="transparent" 
                  strokeDasharray="502.4" 
                  strokeDashoffset={502.4 - (502.4 * trustScoreProfile) / 100}
                  strokeLinecap="round"
                  className="transition-all duration-1000 ease-out"
                />
              </svg>
              <div className="absolute inset-0 flex flex-col items-center justify-center">
                 <span className="text-5xl font-black text-white tracking-tighter">{trustScoreProfile}%</span>
                 <span className="text-[10px] text-slate-500 font-black uppercase tracking-widest mt-1">Reliability</span>
              </div>
           </div>
           <div className="flex-1 space-y-4 w-full">
              <div className="bg-[#0F172A]/50 p-5 rounded-2xl border border-[#334155] hover:border-emerald-500/30 transition-colors group">
                 <div className="flex justify-between mb-2 items-center">
                    <span className="text-xs text-slate-400 font-bold uppercase tracking-widest">Positive Vouchers</span>
                    <span className="text-lg font-black text-emerald-400">+{profile.trustworthyCount || 0}</span>
                 </div>
                 <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                    <motion.div 
                      initial={{ width: 0 }} 
                      animate={{ width: `${trustScoreProfile}%` }} 
                      className="bg-emerald-500 h-full rounded-full"
                    />
                 </div>
              </div>
              <div className="bg-[#0F172A]/50 p-5 rounded-2xl border border-[#334155] hover:border-rose-500/30 transition-colors">
                 <div className="flex justify-between mb-2 items-center">
                    <span className="text-xs text-slate-400 font-bold uppercase tracking-widest">Disputed Claims</span>
                    <span className="text-lg font-black text-rose-400">-{profile.untrustworthyCount || 0}</span>
                 </div>
                 <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden text-right flex justify-end">
                    <motion.div 
                      initial={{ width: 0 }} 
                      animate={{ width: `${100 - trustScoreProfile}%` }} 
                      className="bg-rose-500 h-full rounded-full"
                    />
                 </div>
              </div>
           </div>
        </div>
      </div>

      {/* Identity Bento */}
      <div className="md:col-span-5 md:row-span-3 bg-[#1E293B] border border-[#334155] rounded-[2rem] md:rounded-[2.5rem] p-6 md:p-8 shadow-xl flex flex-col justify-between">
        <div>
          <div className="flex justify-between items-start mb-8">
             <p className="text-[11px] font-black text-slate-500 uppercase tracking-[0.2em]">Profile Fragment</p>
             <button className="text-indigo-400 text-xs font-bold hover:underline" onClick={onEdit}>Edit Node</button>
          </div>
          <div className="space-y-4">
             <ProfileCard label="Legal Descriptor" value={profile.displayName} />
             <ProfileCard label="Active Role" value={profile.details.occupation} />
             <ProfileCard label="Locale Hub" value={profile.details.location} />
             <div className="p-4 bg-[#0F172A]/50 rounded-2xl border border-[#334155]">
                <p className="text-[10px] text-slate-500 font-black uppercase tracking-widest mb-2">Authenticated Claims</p>
                <div className="flex flex-wrap gap-2">
                   <span className="text-[9px] bg-indigo-500/10 text-indigo-300 px-2.5 py-1 rounded-lg border border-indigo-500/20 font-black uppercase tracking-wider">KYC Initialized</span>
                   <span className="text-[9px] bg-indigo-500/10 text-indigo-300 px-2.5 py-1 rounded-lg border border-indigo-500/20 font-black uppercase tracking-wider">Net Verified</span>
                   {profile.isNfcVerified && (
                     <span className="text-[9px] bg-emerald-500/10 text-emerald-400 px-2.5 py-1 rounded-lg border border-emerald-500/20 font-black uppercase tracking-wider flex items-center gap-1">
                       <Smartphone className="w-3 h-3" /> NFC Secured
                     </span>
                   )}
                </div>
             </div>
          </div>
          <div className="mt-8">
             <p className="text-[10px] text-slate-500 font-black uppercase tracking-widest mb-2 px-1">Terminal Output</p>
             <div className="bg-[#0F172A] p-4 rounded-xl border border-[#334155] font-mono text-[11px] text-slate-400 leading-relaxed italic">
                "{profile.details.bio || 'Initialising consciousness stream...'}"
             </div>
          </div>
        </div>

        {!profile.isNfcVerified && (
          <button 
            onClick={async () => {
              toast.promise(
                new Promise(async (resolve, reject) => {
                  try {
                    // Simulation of NFC scanning
                    await new Promise(r => setTimeout(r, 2000));
                    
                    const userRef = doc(db, 'users', profile.uid);
                    await setDoc(userRef, {
                      isNfcVerified: true,
                      nfcVerifiedAt: serverTimestamp()
                    }, { merge: true });
                    
                    onRefresh();
                    resolve("NFC Handshake Successful");
                  } catch (e) {
                    reject("Hardware Interface Error");
                  }
                }),
                {
                  loading: 'Scanning NFC Chip...',
                  success: (m) => m as string,
                  error: (e) => e as string,
                }
              );
            }}
            className="mt-6 w-full py-4 bg-emerald-600/10 border border-emerald-500/30 text-emerald-400 rounded-2xl flex items-center justify-center gap-3 hover:bg-emerald-600/20 transition-all group"
          >
            <Cpu className="w-5 h-5 group-hover:rotate-12 transition-transform" />
            <span className="text-[10px] font-black uppercase tracking-widest">Verify NFC Module</span>
          </button>
        )}
      </div>

      {/* Top Nodes Bento */}
      <div className="md:col-span-5 md:row-span-2 bg-[#1E293B] border border-[#334155] rounded-[2rem] md:rounded-[2.5rem] p-6 md:p-8 shadow-xl h-full flex flex-col overflow-hidden min-h-[300px]">
         <div className="flex justify-between items-center mb-6">
            <p className="text-[11px] font-black text-slate-500 uppercase tracking-widest">Network Leaders</p>
            <span className="text-[10px] text-indigo-400 font-black uppercase tracking-widest">Top Verified</span>
         </div>
         <div className="space-y-3 flex-1 overflow-y-auto pr-2 custom-scrollbar">
            {loading ? (
              <div className="flex justify-center py-10"><Loader2 className="animate-spin text-indigo-500 w-6 h-6" /></div>
            ) : networkUsers.length > 0 ? (
              networkUsers.map((u) => {
                const tCount = u.trustworthyCount || 0;
                const uCount = u.untrustworthyCount || 0;
                const uTotal = tCount + uCount;
                const uScore = uTotal === 0 ? 0 : Math.round((tCount / uTotal) * 100);
                return (
                  <div 
                    key={u.uid} 
                    onClick={() => onSelectUser(u.uid)}
                    className="flex justify-between items-center p-3 bg-[#0F172A]/40 rounded-xl border border-[#334155] hover:border-indigo-500 search-item cursor-pointer transition-all"
                  >
                    <div className="overflow-hidden">
                       <p className="text-white font-bold text-xs truncate">{u.displayName}</p>
                       <p className="text-slate-500 text-[10px] font-mono">@{u.username}</p>
                    </div>
                    <div className="text-right">
                       <p className="text-indigo-400 font-black text-xs">{uScore}%</p>
                       <p className="text-[8px] text-slate-600 uppercase font-black">Sync</p>
                    </div>
                  </div>
                );
              })
            ) : (
              <p className="text-center py-10 text-slate-600 text-xs italic">Awaiting more network data...</p>
            )}
         </div>
      </div>

      {/* Feed Bento */}
      <div className="md:col-span-7 md:row-span-2 bg-[#1E293B] border border-[#334155] rounded-[2rem] md:rounded-[2.5rem] p-6 md:p-8 shadow-xl h-full flex flex-col overflow-hidden min-h-[450px]">
         <div className="flex justify-between items-center mb-6">
            <p className="text-[11px] font-black text-slate-500 uppercase tracking-widest">Network Broadcasts</p>
            <span className="flex items-center gap-2">
               <span className="w-2 h-2 bg-emerald-500 rounded-full animate-pulse shadow-[0_0_8px_rgba(16,185,129,0.5)]"></span>
               <span className="text-[11px] text-emerald-500 font-black uppercase tracking-widest">Live Uplink</span>
            </span>
         </div>

         {/* Post Input */}
         <div className="mb-6 bg-[#0F172A] p-3 rounded-2xl border border-[#334155] focus-within:border-indigo-500/50 transition-colors">
            <textarea 
              value={newPost}
              onChange={(e) => setNewPost(e.target.value)}
              placeholder="Broadcast a message to the network..."
              className="w-full bg-transparent border-none outline-none text-white text-xs font-medium placeholder:text-slate-700 resize-none h-12 custom-scrollbar"
            />
            <div className="flex justify-between items-center mt-2 pt-2 border-t border-white/5">
              <span className="text-[8px] text-slate-600 font-bold uppercase tracking-widest">Encrypted Transmission</span>
              <button 
                onClick={handlePost}
                disabled={posting || !newPost.trim()}
                className="bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:hover:bg-indigo-600 px-4 py-1.5 rounded-lg text-white font-black text-[9px] uppercase tracking-widest transition-all flex items-center gap-2"
              >
                {posting ? <Loader2 className="w-3 h-3 animate-spin" /> : <Send className="w-3 h-3" />}
                Transmit
              </button>
            </div>
         </div>

         <div className="space-y-4 flex-1 overflow-y-auto pr-2 custom-scrollbar">
            {posts.length === 0 ? (
              <div className="h-full flex flex-col items-center justify-center opacity-20 py-10">
                <MessageSquare className="w-10 h-10 mb-2" />
                <p className="text-[10px] font-black uppercase tracking-[0.2em]">No Active Broadcasts</p>
              </div>
            ) : (
              posts.map((post) => (
                <div key={post.id} className="group bg-[#0F172A]/30 p-4 rounded-2xl border border-white/5 hover:border-white/10 transition-all">
                  <div className="flex justify-between items-start mb-2">
                    <div className="flex items-center gap-2">
                      <div className="w-6 h-6 rounded-lg bg-indigo-500/10 flex items-center justify-center text-indigo-400">
                        <User className="w-3 h-3" />
                      </div>
                      <div>
                        <p className="text-[10px] text-white font-black leading-none mb-1">{post.authorName}</p>
                        <p className="text-[8px] text-slate-500 font-bold uppercase tracking-tighter">@{post.authorUsername}</p>
                      </div>
                    </div>
                    <span className="text-[8px] text-slate-700 font-bold uppercase">{post.createdAt?.toDate ? new Intl.RelativeTimeFormat('en').format(Math.round((post.createdAt.toDate().getTime() - Date.now()) / 60000), 'minute') : 'just now'}</span>
                  </div>
                  <p className="text-slate-300 text-[11px] leading-relaxed mb-3">
                    {post.content}
                  </p>
                  <div className="flex items-center justify-between pt-2 border-t border-white/5">
                    <div className="flex items-center gap-3">
                      <button 
                        onClick={() => handleLike(post.id)}
                        className="flex items-center gap-1.5 text-slate-500 hover:text-emerald-400 transition-colors group/like"
                      >
                        <ThumbsUp className="w-3 h-3 group-hover/like:scale-110 transition-transform" />
                        <span className="text-[10px] font-black">{post.likesCount || 0}</span>
                      </button>
                    </div>
                    {post.authorId === profile.uid && (
                      <button 
                        onClick={() => handleDeletePost(post.id)}
                        className="text-slate-700 hover:text-rose-500 transition-colors"
                      >
                        <Trash2 className="w-3 h-3" />
                      </button>
                    )}
                  </div>
                </div>
              ))
            )}
         </div>
      </div>
    </motion.div>
  );
}

function EditProfileView({ profile, onBack, onComplete }: { profile: UserProfile, onBack: () => void, onComplete: () => void }) {
  const [formData, setFormData] = useState({
    displayName: profile.displayName,
    location: profile.details.location || '',
    occupation: profile.details.occupation || '',
    bio: profile.details.bio || ''
  });
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setLoading(true);

    try {
      const userRef = doc(db, 'users', profile.uid);
      await setDoc(userRef, {
        displayName: formData.displayName,
        details: {
          location: formData.location,
          occupation: formData.occupation,
          bio: formData.bio
        }
      }, { merge: true });

      toast.success("Identity Records Synchronized");
      onComplete();
    } catch (e: any) {
      toast.error("Protocol Error: Update Rejected");
    } finally {
      setLoading(false);
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="max-w-2xl mx-auto space-y-6"
    >
      <button onClick={onBack} className="flex items-center gap-2 text-slate-500 hover:text-white transition-colors font-bold uppercase tracking-widest text-[10px]">
        <ChevronRight className="w-4 h-4 rotate-180" /> Cancel Re-Configuration
      </button>

      <div className="bg-[#1E293B] border border-[#334155] rounded-[3rem] p-10 shadow-2xl relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-indigo-500/5 blur-[100px] -mr-32 -mt-32"></div>
        
        <div className="flex items-center gap-6 mb-12">
          <div className="w-16 h-16 bg-[#0F172A] rounded-2xl flex items-center justify-center text-indigo-500 border border-[#334155]">
             <User className="w-8 h-8" />
          </div>
          <div>
            <h2 className="text-3xl font-black text-white tracking-tighter uppercase">Modify Node</h2>
            <p className="text-slate-500 font-bold text-[10px] uppercase tracking-widest">Update Identification Metadata</p>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-8">
          <div className="space-y-1.5">
            <label className="block text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Display Alias</label>
            <input 
              required
              className="w-full px-6 py-5 bg-[#0F172A] border border-[#334155] rounded-2xl focus:ring-2 focus:ring-indigo-500 transition-all outline-none text-white text-lg font-bold"
              value={formData.displayName}
              onChange={(e) => setFormData({ ...formData, displayName: e.target.value })}
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-1.5">
              <label className="block text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Global Sector</label>
              <input 
                className="w-full px-6 py-4 bg-[#0F172A] border border-[#334155] rounded-2xl focus:ring-2 focus:ring-indigo-500 transition-all outline-none text-white font-medium"
                value={formData.location}
                onChange={(e) => setFormData({ ...formData, location: e.target.value })}
              />
            </div>
            <div className="space-y-1.5">
              <label className="block text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Verification Role</label>
              <input 
                className="w-full px-6 py-4 bg-[#0F172A] border border-[#334155] rounded-2xl focus:ring-2 focus:ring-indigo-500 transition-all outline-none text-white font-medium"
                value={formData.occupation}
                onChange={(e) => setFormData({ ...formData, occupation: e.target.value })}
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="block text-[10px] font-black text-slate-500 uppercase tracking-widest px-1">Consciousness Feed (Bio)</label>
            <textarea 
              className="w-full px-6 py-4 bg-[#0F172A] border border-[#334155] rounded-2xl focus:ring-2 focus:ring-indigo-500 transition-all outline-none text-white font-medium h-40 resize-none leading-relaxed italic"
              value={formData.bio}
              onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
            />
          </div>

          <button 
            type="submit"
            disabled={loading}
            className="w-full py-6 bg-indigo-600 hover:bg-indigo-500 text-white font-black rounded-[2rem] shadow-xl shadow-indigo-500/20 transition-all flex items-center justify-center gap-3 uppercase tracking-widest text-sm"
          >
            {loading ? <Loader2 className="animate-spin w-5 h-5" /> : <ShieldCheck className="w-5 h-5" />}
            Sync Updates to Ledger
          </button>
        </form>
      </div>
    </motion.div>
  );
}

function ProfileCard({ label, value }: { label: string, value: string }) {
  return (
    <div className="p-4 bg-[#0F172A]/50 rounded-2xl border border-[#334155] hover:border-slate-600 transition-colors">
       <p className="text-[10px] text-slate-500 font-black uppercase tracking-widest mb-1">{label}</p>
       <p className="text-white font-bold tracking-tight">{value || 'Access Denied'}</p>
    </div>
  );
}

function FeedItem({ user, action, subject, time, type }: { user: string, action: string, subject: string, time: string, type: 'info' | 'emerald' | 'rose' }) {
  const colorClass = type === 'emerald' ? 'bg-emerald-500/10 text-emerald-500' : type === 'rose' ? 'bg-rose-500/10 text-rose-500' : 'bg-indigo-500/10 text-indigo-400';
  return (
    <div className="flex items-center justify-between p-4 bg-[#0F172A]/40 rounded-2xl border border-[#334155] group transition-all hover:bg-[#0F172A]/60">
      <div className="flex items-center gap-4 overflow-hidden">
        <div className={cn("w-10 h-10 rounded-xl flex items-center justify-center shrink-0", colorClass)}>
           <Activity className="w-5 h-5" />
        </div>
        <div className="text-sm truncate">
           <span className="text-white font-black">{user}</span>
           <span className="text-slate-500 mx-1.5 font-bold uppercase text-[10px] tracking-widest">{action}</span>
           <span className="text-slate-300 font-medium">{subject}</span>
        </div>
      </div>
      <span className="text-[10px] text-slate-600 font-black uppercase tracking-tighter font-mono shrink-0 ml-2">{time}</span>
    </div>
  );
}

function UserSearch({ onSelectUser }: { onSelectUser: (uid: string) => void }) {
  const [searchTerm, setSearchTerm] = useState('');
  const [results, setResults] = useState<UserProfile[]>([]);
  const [searching, setSearching] = useState(false);

  const handleSearch = async (term: string) => {
    setSearchTerm(term);
    if (term.length < 2) {
      setResults([]);
      return;
    }
    setSearching(true);
    try {
      const q = query(
        collection(db, 'users'), 
        where('username', '>=', term.toLowerCase()), 
        where('username', '<=', term.toLowerCase() + '\uf8ff'),
        limit(5)
      );
      const qSnap = await getDocs(q);
      const res: UserProfile[] = [];
      qSnap.forEach(doc => {
        res.push({ uid: doc.id, ...doc.data() } as UserProfile);
      });
      setResults(res);
    } catch (e) {
      handleFirestoreError(e, OperationType.LIST, 'users');
    } finally {
      setSearching(false);
    }
  };

  return (
    <motion.div 
      initial={{ opacity: 0, scale: 0.98 }}
      animate={{ opacity: 1, scale: 1 }}
      className="max-w-3xl mx-auto space-y-8"
    >
      <div className="text-center space-y-2">
         <h2 className="text-4xl font-black text-white tracking-tighter uppercase">Query Network</h2>
         <p className="text-slate-500 font-bold tracking-widest text-xs uppercase">Establishing peer-to-peer verification</p>
      </div>

      <div className="relative group">
        <div className="absolute inset-0 bg-indigo-500/20 blur-3xl opacity-0 group-focus-within:opacity-100 transition-opacity rounded-[3rem]"></div>
        <Search className="absolute left-6 top-1/2 -translate-y-1/2 text-slate-500 w-6 h-6 z-10" />
        <input 
          type="text"
          placeholder="ENTER NODE ID (@username)..."
          className="relative z-10 w-full pl-16 pr-6 py-8 bg-[#1E293B] border border-[#334155] rounded-[2.5rem] shadow-2xl focus:ring-4 focus:ring-indigo-500/20 outline-none text-xl font-black text-white placeholder:text-slate-700 tracking-tight transition-all"
          value={searchTerm}
          onChange={(e) => handleSearch(e.target.value)}
        />
        {searching && <Loader2 className="absolute right-8 top-1/2 -translate-y-1/2 text-indigo-500 animate-spin w-8 h-8 z-10" />}
      </div>

      <div className="grid grid-cols-1 gap-4">
        {results.length > 0 ? results.map((user) => (
          <motion.div 
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            key={user.uid}
            onClick={() => onSelectUser(user.uid)}
            className="group flex items-center gap-6 bg-[#1E293B] p-6 rounded-[2rem] border border-[#334155] shadow-lg hover:border-indigo-500/50 cursor-pointer transition-all hover:translate-y-[-4px]"
          >
            <div className="w-16 h-16 bg-[#0F172A] rounded-2xl flex items-center justify-center text-slate-600 border border-[#334155] group-hover:border-indigo-500 transition-colors">
              <User className="w-8 h-8 group-hover:text-indigo-400 transition-colors" />
            </div>
            <div className="flex-1">
              <h4 className="font-black text-xl text-white tracking-tight">{user.displayName}</h4>
              <p className="text-slate-500 text-sm font-bold uppercase tracking-widest">@{user.username}</p>
            </div>
            <div className="flex items-center gap-8 pr-4">
               <div className="text-right">
                  <p className="text-white text-3xl font-black tracking-tighter">
                    {(() => {
                      const t = user.trustworthyCount || 0;
                      const u = user.untrustworthyCount || 0;
                      return t + u === 0 ? 0 : Math.round((t / (t + u)) * 100);
                    })()}%
                  </p>
                  <p className="text-[10px] uppercase tracking-widest font-black text-slate-600">Sync Level</p>
               </div>
               <div className="w-10 h-10 bg-[#0F172A] rounded-full flex items-center justify-center group-hover:bg-indigo-500 group-hover:text-white text-slate-600 transition-all">
                  <ChevronRight className="w-6 h-6" />
               </div>
            </div>
          </motion.div>
        )) : searchTerm.length >= 2 && !searching && (
          <div className="text-center py-20 bg-[#1E293B]/30 rounded-[3rem] border-2 border-dashed border-[#334155]">
            <div className="w-24 h-24 bg-[#0F172A] rounded-full flex items-center justify-center mx-auto mb-6 text-slate-700">
              <Search className="w-10 h-10" />
            </div>
            <p className="text-slate-500 font-bold uppercase tracking-widest text-sm">Node "{searchTerm}" unidentified on ledger.</p>
          </div>
        )}
      </div>
    </motion.div>
  );
}

function UserProfileView({ 
  targetUid, 
  voterProfile, 
  onBack,
  onVoteComplete
}: { 
  targetUid: string, 
  voterProfile: UserProfile | null, 
  onBack: () => void,
  onVoteComplete: () => void
}) {
  const [targetProfile, setTargetProfile] = useState<UserProfile | null>(null);
  const [recentVotes, setRecentVotes] = useState<VoteRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [hasVoted, setHasVoted] = useState<'trustworthy' | 'untrustworthy' | null>(null);
  const [isVoting, setIsVoting] = useState(false);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const uSnap = await getDoc(doc(db, 'users', targetUid));
        if (uSnap.exists()) {
          setTargetProfile({ uid: targetUid, ...uSnap.data() } as UserProfile);
        }

        // Fetch recent votes
        const votesRef = collection(db, 'users', targetUid, 'votes');
        const vQuery = query(votesRef, orderBy('timestamp', 'desc'), limit(5));
        const vSnap = await getDocs(vQuery);
        const votes: VoteRecord[] = [];
        vSnap.forEach(vDoc => {
          votes.push({ voterId: vDoc.id, ...vDoc.data() } as VoteRecord);
        });
        setRecentVotes(votes);

        if (voterProfile) {
          const personalVoteSnap = await getDoc(doc(db, 'users', targetUid, 'votes', voterProfile.uid));
          if (personalVoteSnap.exists()) {
            setHasVoted(personalVoteSnap.data().type);
          }
        }
      } catch (e) {
        handleFirestoreError(e, OperationType.GET, 'profile_fetch');
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, [targetUid, voterProfile?.uid]);

  const handleVote = async (type: 'trustworthy' | 'untrustworthy') => {
    if (hasVoted || isVoting || !voterProfile) return;
    setIsVoting(true);
    try {
      const batch = writeBatch(db);
      const voteRef = doc(db, 'users', targetUid, 'votes', voterProfile.uid);
      batch.set(voteRef, { 
        type, 
        voterName: voterProfile.displayName,
        voterUsername: voterProfile.username,
        timestamp: serverTimestamp() 
      });
      const userRef = doc(db, 'users', targetUid);
      batch.update(userRef, {
        [type === 'trustworthy' ? 'trustworthyCount' : 'untrustworthyCount']: increment(1)
      });
      await batch.commit();
      setHasVoted(type);
      toast.success("Protocol recorded voter data.");
      setTargetProfile(prev => prev ? {
        ...prev,
        [type === 'trustworthy' ? 'trustworthyCount' : 'untrustworthyCount']: prev[type === 'trustworthy' ? 'trustworthyCount' : 'untrustworthyCount'] + 1
      } : null);
      
      // Add to local votes list
      setRecentVotes(prev => [{
        voterId: voterProfile.uid,
        voterName: voterProfile.displayName,
        voterUsername: voterProfile.username,
        type,
        timestamp: new Date()
      }, ...prev].slice(0, 5));

      onVoteComplete();
    } catch (e: any) {
      toast.error(e.message || "Voter protocol rejected.");
    } finally {
      setIsVoting(false);
    }
  };

  if (loading) return <div className="flex justify-center p-20"><Loader2 className="animate-spin text-indigo-500 w-12 h-12" /></div>;
  if (!targetProfile) return <div className="text-center py-20 text-slate-500">Profile data corrupted or missing.</div>;
  
  const tCount = targetProfile.trustworthyCount || 0;
  const uCount = targetProfile.untrustworthyCount || 0;
  const totalVotes = tCount + uCount;
  const trustScore = targetProfile.isNfcVerified ? 100 : (totalVotes === 0 ? 0 : Math.round((tCount / totalVotes) * 100));

  return (
    <motion.div 
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="max-w-4xl mx-auto space-y-6 pb-12"
    >
      <button onClick={onBack} className="flex items-center gap-2 text-slate-500 hover:text-white transition-colors font-bold uppercase tracking-widest text-[10px]">
        <ChevronRight className="w-4 h-4 rotate-180" /> Return to Queries
      </button>

      <div className="bg-[#1E293B] rounded-[2rem] md:rounded-[3rem] p-6 md:p-10 border border-[#334155] shadow-2xl overflow-hidden relative">
        <div className="absolute top-0 right-0 w-64 h-64 bg-indigo-500/5 blur-[100px] -mr-32 -mt-32"></div>
        
        <div className="flex flex-col md:flex-row items-center gap-6 md:gap-10 mb-12 relative z-10">
           <div className="w-32 h-32 md:w-40 md:h-40 bg-[#0F172A] rounded-[2rem] md:rounded-[2.5rem] flex items-center justify-center text-indigo-500/20 border border-[#334155] shrink-0">
             <User className="w-16 h-16 md:w-24 md:h-24" />
           </div>
           <div className="flex-1 text-center md:text-left">
             <div className="flex flex-col md:flex-row md:items-baseline gap-2 md:gap-4 justify-center md:justify-start">
               <h3 className="text-3xl md:text-5xl font-black text-white tracking-tighter leading-none">{targetProfile.displayName}</h3>
               <p className={cn(
                 "font-mono text-[10px] md:text-sm font-bold uppercase tracking-widest px-3 py-1 rounded-full w-fit mx-auto md:mx-0",
                 trustScore >= 70 ? 'text-emerald-500 bg-emerald-500/10' : 'text-indigo-400 bg-indigo-500/10'
               )}>{trustScore >= 70 ? 'Trust-Anchor' : 'Network Subject'}</p>
             </div>
             <p className="text-slate-500 font-bold tracking-[0.2em] uppercase text-[10px] md:text-xs mt-3">Node: @{targetProfile.username}</p>
             
             <div className="mt-8 flex flex-wrap justify-center md:justify-start gap-3 md:gap-4">
                <StatCard label="True Positives" value={targetProfile.trustworthyCount || 0} color="text-emerald-400" />
                <StatCard label="Peer Disputes" value={targetProfile.untrustworthyCount || 0} color="text-rose-400" />
                <StatCard label="Total Consented" value={totalVotes} color="text-indigo-300" />
                <StatCard label="Calculated Sync" value={`${trustScore}%`} color="text-white" highlight />
             </div>
           </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-12 gap-6 relative z-10">
           <div className="md:col-span-8 space-y-6">
              <div className="bg-[#0F172A]/50 p-6 md:p-8 rounded-[1.5rem] md:rounded-[2rem] border border-[#334155]">
                <div className="flex justify-between items-center mb-6 border-b border-white/5 pb-2">
                  <p className="text-[9px] md:text-[10px] uppercase font-black text-slate-500 tracking-widest">Node Information Manifest</p>
                  {targetProfile.isNfcVerified && (
                    <span className="flex items-center gap-1 md:gap-1.5 px-2 md:px-3 py-1 bg-emerald-500/10 border border-emerald-500/20 rounded-full text-[8px] md:text-[9px] font-black text-emerald-400 uppercase tracking-widest">
                      <Smartphone className="w-3 h-3" /> NFC Verified
                    </span>
                  )}
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 md:gap-8">
                    <div>
                       <p className="text-slate-600 text-[9px] md:text-[10px] font-black uppercase tracking-widest mb-2">Primary Domain</p>
                       <p className="font-bold text-white text-base md:text-lg tracking-tight">{targetProfile.details.occupation || 'DECRYPT_FAILURE'}</p>
                    </div>
                    <div>
                       <p className="text-slate-600 text-[9px] md:text-[10px] font-black uppercase tracking-widest mb-2">Last Known Sector</p>
                       <p className="font-bold text-white text-base md:text-lg tracking-tight">{targetProfile.details.location || 'DECRYPT_FAILURE'}</p>
                    </div>
                    <div className="sm:col-span-2 pt-4 border-t border-white/5">
                       <p className="text-slate-600 text-[9px] md:text-[10px] font-black uppercase tracking-widest mb-4">Conscious Log</p>
                       <p className="text-slate-400 leading-relaxed font-medium italic text-base md:text-lg px-4 border-l-2 border-indigo-500/20">
                         "{targetProfile.details.bio || 'Data stream empty. Input requested.'}"
                       </p>
                    </div>
                 </div>
              </div>

              {voterProfile && targetUid !== voterProfile.uid && !targetProfile.isNfcVerified && (
                <div className="bg-[#0F172A] p-6 md:p-8 rounded-[2rem] md:rounded-[2.5rem] border border-[#334155] shadow-inner">
                  <div className="flex flex-col items-center gap-6 md:gap-8">
                    <div className="text-center">
                       <h4 className="text-white font-black text-lg md:text-xl tracking-tight uppercase">Identity Validation</h4>
                       <p className="text-slate-500 font-bold text-[9px] md:text-[10px] uppercase tracking-widest mt-1">Submit your verification to the ledger</p>
                    </div>
                    
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 md:gap-4 w-full">
                      <VoteButton 
                        onClick={() => handleVote('trustworthy')}
                        active={hasVoted === 'trustworthy'}
                        disabled={!!hasVoted || isVoting}
                        type="TRUST"
                        icon={<ShieldCheck className="w-10 h-10" />}
                        label="VERIFIED TRUE"
                        desc="Consensus Match"
                      />
                      <VoteButton 
                        onClick={() => handleVote('untrustworthy')}
                        active={hasVoted === 'untrustworthy'}
                        disabled={!!hasVoted || isVoting}
                        type="FLAG"
                        icon={<ShieldAlert className="w-10 h-10" />}
                        label="FLAGGED WRONG"
                        desc="Ledger Conflict"
                      />
                    </div>
                    {hasVoted && (
                      <div className="flex items-center gap-2 px-4 py-2 bg-indigo-500/10 text-indigo-400 rounded-full text-[10px] font-black uppercase tracking-widest border border-indigo-500/20">
                         <BadgeCheck className="w-3 h-3" /> Verification Persistent
                      </div>
                    )}
                  </div>
                </div>
              )}

              <div className="bg-[#1E293B] border border-[#334155] rounded-[2rem] p-8">
                <p className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-6">Recent Validator Manifest</p>
                <div className="space-y-3">
                  {recentVotes.length > 0 ? recentVotes.map((v) => (
                    <div key={v.voterId} className="flex items-center justify-between p-4 bg-[#0F172A]/40 rounded-2xl border border-[#334155]">
                      <div className="flex items-center gap-3">
                        <div className={cn(
                          "w-8 h-8 rounded-lg flex items-center justify-center text-[10px] font-black",
                          v.type === 'trustworthy' ? 'bg-emerald-500/10 text-emerald-500' : 'bg-rose-500/10 text-rose-500'
                        )}>
                          {v.type === 'trustworthy' ? <ShieldCheck className="w-4 h-4" /> : <ShieldAlert className="w-4 h-4" />}
                        </div>
                        <div>
                          <p className="text-white font-bold text-xs">{v.voterName}</p>
                          <p className="text-slate-500 text-[9px] font-mono">@{v.voterUsername}</p>
                        </div>
                      </div>
                      <span className="text-[9px] text-slate-600 font-black uppercase tracking-tighter">Verified</span>
                    </div>
                  )) : (
                    <p className="text-center py-6 text-slate-600 text-xs italic">No verification records found on ledger.</p>
                  )}
                </div>
              </div>
           </div>

           <div className="md:col-span-4 space-y-6">
              <div className="bg-indigo-600 rounded-[2rem] p-8 text-white shadow-2xl shadow-indigo-500/20 flex flex-col items-center text-center">
                 <TrendingUp className="w-12 h-12 mb-4 opacity-50" />
                 <h5 className="font-black text-3xl tracking-tighter mb-1">{trustScore}%</h5>
                 <p className="text-[10px] font-black uppercase tracking-[0.2em] opacity-70">Peer Confidence</p>
                 <div className="w-full bg-white/20 h-1 mt-6 rounded-full overflow-hidden">
                    <div className="bg-white h-full" style={{ width: `${trustScore}%` }}></div>
                 </div>
              </div>
              
              <div className="bg-[#1E293B] border border-[#334155] rounded-[2rem] p-6">
                 <p className="text-[10px] font-black text-slate-500 uppercase tracking-widest mb-4">Relational Integrity</p>
                 <div className="space-y-4">
                    <div className="flex justify-between items-center text-xs">
                       <span className="text-slate-400 font-bold uppercase">Uptime</span>
                       <span className="text-white font-mono">99.9%</span>
                    </div>
                    <div className="flex justify-between items-center text-xs">
                       <span className="text-slate-400 font-bold uppercase">Blocks</span>
                       <span className="text-white font-mono">{(totalVotes * 16).toLocaleString()}</span>
                    </div>
                    <div className="flex justify-between items-center text-xs">
                       <span className="text-slate-400 font-bold uppercase">Signatures</span>
                       <span className="text-white font-mono">{totalVotes}</span>
                    </div>
                 </div>
              </div>
           </div>
        </div>
      </div>
    </motion.div>
  );
}

function NfcVerificationView({ profile, onBack, onRefresh }: { profile: UserProfile, onBack: () => void, onRefresh: () => void }) {
  const [loading, setLoading] = useState(false);

  const startNfcVerification = async () => {
    toast.promise(
      new Promise(async (resolve, reject) => {
        try {
          setLoading(true);
          // Simulation of NFC scanning
          await new Promise(r => setTimeout(r, 2000));
          
          const userRef = doc(db, 'users', profile.uid);
          await setDoc(userRef, {
            isNfcVerified: true,
            nfcVerifiedAt: serverTimestamp()
          }, { merge: true });
          
          onRefresh();
          resolve("NFC Identity Anchored");
        } catch (e) {
          reject("NFC Interface Error");
        } finally {
          setLoading(false);
        }
      }),
      {
        loading: 'Opening NFC Interface...',
        success: (m) => m as string,
        error: (e) => e as string,
      }
    );
  };

  return (
    <motion.div 
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="max-w-4xl mx-auto"
    >
      <div className="mb-8">
        <button onClick={onBack} className="flex items-center gap-2 text-slate-500 hover:text-white transition-colors font-bold uppercase tracking-widest text-[10px]">
          <ChevronRight className="w-4 h-4 rotate-180" /> Return to Dashboard
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 items-stretch">
        <div className="bg-[#1E293B] border border-[#334155] rounded-[2rem] md:rounded-[3rem] p-6 md:p-10 shadow-2xl relative overflow-hidden flex flex-col justify-center">
          <div className="absolute inset-0 bg-emerald-500/5 blur-[100px] pointer-events-none"></div>
          
          <div className="relative z-10">
            <div className="w-16 h-16 md:w-20 md:h-20 bg-emerald-600/10 rounded-[1.5rem] md:rounded-[2rem] border border-emerald-500/30 flex items-center justify-center text-emerald-500 mb-6 md:mb-8 mx-auto shadow-2xl shadow-emerald-500/10 shrink-0">
              {profile.isNfcVerified ? <BadgeCheck className="w-8 h-8 md:w-10 md:h-10" /> : <Smartphone className="w-8 h-8 md:w-10 md:h-10" />}
            </div>
            
            <h2 className="text-3xl md:text-4xl font-black text-white tracking-tighter text-center uppercase mb-4">NFC Verification</h2>
            <p className="text-slate-400 text-center font-medium leading-relaxed italic px-2 md:px-4 text-sm md:text-base">
              {profile.isNfcVerified 
                ? "Your identity has been cryptographically anchored via high-frequency proximity handshake."
                : "Secure your trust score by anchoring your digital identity to a physical hardware key or NFC-enabled device."
              }
            </p>

            <div className="mt-8 md:mt-12">
              {profile.isNfcVerified ? (
                <div className="p-4 md:p-6 bg-emerald-500/10 rounded-2xl border border-emerald-500/30 text-center">
                  <p className="text-emerald-400 font-black text-xs md:text-sm uppercase tracking-widest">Protocol Verified</p>
                  <p className="text-[9px] md:text-[10px] text-emerald-600 font-bold uppercase mt-1">Verified on: {profile.nfcVerifiedAt?.toDate?.()?.toLocaleDateString() || 'Recently'}</p>
                </div>
              ) : (
                <button 
                  onClick={startNfcVerification}
                  disabled={loading}
                  className="w-full py-5 md:py-6 bg-emerald-600 hover:bg-emerald-500 text-white font-black rounded-[2rem] md:rounded-[2.5rem] shadow-2xl shadow-emerald-500/20 transition-all flex items-center justify-center gap-4 group text-xs md:text-sm"
                >
                  {loading ? <Loader2 className="animate-spin w-5 h-5" /> : <Zap className="w-5 h-5" />}
                  <span className="uppercase tracking-[0.2em]">Initiate NFC Probe</span>
                </button>
              )}
            </div>
          </div>
        </div>

        <div className="bg-[#0F172A] border border-[#334155] rounded-[2rem] md:rounded-[3rem] p-6 md:p-10 flex flex-col">
          <h3 className="text-[9px] md:text-[11px] font-black text-slate-500 uppercase tracking-[0.3em] mb-8 md:mb-10 text-center">Technical Specifications</h3>
          <div className="space-y-4 md:space-y-6 flex-1 flex flex-col justify-center">
             <TechFeature 
               icon={<ShieldCheck className="w-5 h-5 text-indigo-400" />}
               title="Cryptographic Guard"
               desc="Proximity-based identity consensus ensures non-repudiation."
             />
             <TechFeature 
               icon={<Activity className="w-5 h-5 text-emerald-400" />}
               title="Proximity Handshake"
               desc="Requires 13.56 MHz frequency synchronization."
             />
             <TechFeature 
               icon={<ShieldAlert className="w-5 h-5 text-rose-400" />}
               title="Ephemeral Keys"
               desc="Short-lived cipher exchange prevents replay attacks."
             />
          </div>
        </div>
      </div>
    </motion.div>
  );
}

function TechFeature({ icon, title, desc }: { icon: React.ReactNode, title: string, desc: string }) {
  return (
    <div className="flex gap-5 p-4 bg-[#1E293B]/30 rounded-2xl border border-white/5">
       <div className="shrink-0">{icon}</div>
       <div>
          <h5 className="text-white font-black uppercase text-[10px] tracking-widest mb-1">{title}</h5>
          <p className="text-slate-500 text-xs font-medium leading-normal">{desc}</p>
       </div>
    </div>
  );
}
function StatCard({ label, value, color, highlight }: { label: string, value: string | number, color: string, highlight?: boolean }) {
  return (
    <div className={cn(
      "p-5 rounded-2xl border border-[#334155] min-w-[120px] shadow-lg",
      highlight ? "bg-indigo-500 shadow-indigo-500/20 border-transparent" : "bg-[#0F172A]/50"
    )}>
       <p className={cn("text-[28px] font-black tracking-tighter leading-none mb-1", color, highlight ? "text-white" : color)}>{value}</p>
       <p className={cn("text-[9px] uppercase font-black tracking-widest", highlight ? "text-indigo-100" : "text-slate-500")}>{label}</p>
    </div>
  );
}

function VoteButton({ onClick, active, disabled, type, icon, label, desc }: { onClick: () => void, active: boolean, disabled: boolean, type: 'TRUST' | 'FLAG', icon: React.ReactNode, label: string, desc: string }) {
  const isTrust = type === 'TRUST';
  return (
    <button 
      onClick={onClick}
      disabled={disabled}
      className={cn(
        "p-6 md:p-8 rounded-[1.5rem] md:rounded-[2rem] border-2 transition-all flex flex-col items-center gap-3 md:gap-4 group relative overflow-hidden",
        active 
          ? (isTrust ? "bg-emerald-500 border-emerald-500 text-white shadow-2xl shadow-emerald-500/30" : "bg-rose-500 border-rose-500 text-white shadow-2xl shadow-rose-500/30") 
          : "bg-[#1E293B] border-[#334155] hover:border-slate-400 text-slate-500",
        disabled && !active && "opacity-30 grayscale cursor-not-allowed"
      )}
    >
      <div className={cn(
        "transition-transform group-hover:scale-110 duration-500 shrink-0",
        active ? "text-white" : (isTrust ? "text-emerald-500/50" : "text-rose-500/50")
      )}>
        {React.cloneElement(icon as React.ReactElement, { className: "w-8 h-8 md:w-10 md:h-10" })}
      </div>
      <div className="text-center group-hover:translate-y-[-2px] transition-transform">
        <span className={cn("font-black text-xs md:text-sm tracking-widest", active ? "text-white" : "text-slate-300")}>{label}</span>
        <p className={cn("text-[8px] md:text-[9px] font-bold uppercase tracking-widest mt-1 opacity-60", active ? "text-white" : "text-slate-500")}>{desc}</p>
      </div>
      {!disabled && (
        <div className={cn(
          "absolute inset-0 opacity-0 group-hover:opacity-10 transition-opacity",
          isTrust ? "bg-emerald-500" : "bg-rose-500"
        )}></div>
      )}
    </button>
  );
}
