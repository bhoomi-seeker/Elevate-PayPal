
import { useState } from "react";
import RouteIntelligence from "./pages/RouteIntelligence";
import { motion } from "motion/react";
import {
  ScanLine,
  ArrowLeftRight,
  Wallet,
  ShieldCheck,
  Globe2,
  Bell,
  Search,
  ChevronDown,
  ArrowUpRight,
  ArrowDownLeft,
  Sparkles,
  CircleHelp,
  CreditCard,
  Clock3,
  CheckCircle2,
  Menu,
  X,
} from "lucide-react";
import "./App.css";

const transactions = [
  {
    name: "Coffee House",
    detail: "Today, 10:42 AM",
    amount: "-₹240.00",
    type: "out",
    initials: "C",
    color: "peach",
  },
  {
    name: "Alex Morgan",
    detail: "Today, 09:18 AM",
    amount: "+₹2,500.00",
    type: "in",
    initials: "A",
    color: "lavender",
  },
  {
    name: "Global Store",
    detail: "Yesterday, 06:30 PM",
    amount: "-₹1,850.00",
    type: "out",
    initials: "G",
    color: "blue",
  },
];

const navItems = [
  { label: "QR Sense", icon: ScanLine },
  { label: "BridgeMatch", icon: ArrowLeftRight },
  { label: "PayPilot AI", icon: Sparkles },
  { label: "PayGuard", icon: ShieldCheck },
  { label: "RouteIQ", icon: Globe2 },
];

const features = [
  {
    icon: ScanLine,
    title: "QR Sense",
    description:
      "Decode the QR and identify its payment format and details.",
    className: "feature-lavender",
    action: "Decode the QR",
  },
  {
    icon: ArrowLeftRight,
    title: "BridgeMatch",
    description:
      "Check whether the scanned QR works with a supported payment method.",
    className: "feature-mint",
    action: "Check compatibility",
  },
  {
    icon: Sparkles,
    title: "PayPilot AI",
    description:
      "Understand your payment options with intelligent guidance.",
    className: "feature-peach",
    action: "Explain the options",
  },
  {
    icon: ShieldCheck,
    title: "PayGuard",
    description:
      "Review merchant and payment details before proceeding.",
    className: "feature-lavender",
    action: "Review payment details",
  },
  {
    icon: Globe2,
    title: "RouteIQ",
    description:
      "Identify a viable payment route based on supported networks.",
    className: "feature-mint",
    action: "Identify a viable route",
  },
];

function FeaturePage({ title, feature, onAction }) {
  const Icon = feature?.icon || Sparkles;

  return (
    <section className="scanner-page">
      <button
        className="back-link"
        onClick={() => onAction("Overview")}
      >
        ← Back to overview
      </button>

      <div className="scanner-heading">
        <div>
          <span className="eyebrow">
            <Sparkles size={14} /> PAYBRIDGE INTELLIGENCE
          </span>
          <h2>{title}</h2>
          <p>{feature?.description}</p>
        </div>
      </div>

      <div className="glass-card guidance-card">
        <span
          className={`feature-icon ${feature?.className || "feature-lavender"
            }`}
        >
          <Icon size={24} />
        </span>

        <h3>{feature?.action || "Explore this feature"}</h3>

        <p>
          {title === "BridgeMatch" &&
            "Explore how PayBridge could identify compatible payment methods for a QR code."}

          {title === "PayPilot AI" &&
            "Get clear explanations of payment options and guidance for unfamiliar payment flows."}

          {title === "PayGuard" &&
            "Review available merchant and payment details before deciding whether to proceed."}
        </p>

        <button className="primary-button" onClick={() => onAction(title)}>
          Explore demo <ArrowUpRight size={16} />
        </button>
      </div>
    </section>
  );
}

function App() {
  const [activeNav, setActiveNav] = useState("Overview");
  const [scannerOpen, setScannerOpen] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const [notice, setNotice] = useState("");

  const openScanner = () => {
    setActiveNav("QR Sense");
    setScannerOpen(true);
    setNotice("");
  };

  const selectNav = (label) => {
    setActiveNav(label);
    setMenuOpen(false);
    setNotice("");
    setScannerOpen(label === "QR Sense");
  };

  const handleFeatureAction = (label) => {
    if (label === "Overview") {
      setActiveNav("Overview");
      setScannerOpen(false);
      setNotice("");
      return;
    }

    setNotice(
      `${label} is currently a demo. Its core functionality still needs to be connected.`
    );
  };

  return (
    <div className="app-shell">
      <aside className={`sidebar ${menuOpen ? "sidebar-open" : ""}`}>
        <a
          className="brand"
          href="#overview"
          onClick={() => {
            setActiveNav("Overview");
            setScannerOpen(false);
            setMenuOpen(false);
            setNotice("");
          }}
        >
          <span className="brand-mark">
            <ArrowLeftRight size={23} strokeWidth={2.5} />
          </span>

          <span>
            Pay<span className="brand-accent">Bridge</span>
            <small>SMARTER PAYMENTS</small>
          </span>

          <button
            type="button"
            className="icon-button close-menu"
            onClick={(event) => {
              event.preventDefault();
              setMenuOpen(false);
            }}
            aria-label="Close navigation"
          >
            <X size={20} />
          </button>
        </a>

        <div className="workspace-label">WORKSPACE</div>

        <nav className="side-nav">
          {navItems.map(({ label, icon: Icon }) => (
            <button
              key={label}
              className={`nav-item ${activeNav === label ? "nav-active" : ""
                }`}
              onClick={() => selectNav(label)}
            >
              <Icon size={19} />
              <span>{label}</span>

              {label === "QR Sense" && (
                <span className="nav-new">NEW</span>
              )}
            </button>
          ))}
        </nav>

        <div className="sidebar-divider" />

        <div className="workspace-label">YOUR ACCOUNT</div>

        <button
          className="nav-item"
          onClick={() =>
            setNotice("Payment methods preview coming soon.")
          }
        >
          <CreditCard size={19} />
          <span>Payment methods</span>
        </button>

        <button
          className="nav-item"
          onClick={() =>
            setNotice("Help center preview coming soon.")
          }
        >
          <CircleHelp size={19} />
          <span>Help &amp; support</span>
        </button>

        <div className="sidebar-bottom">
          <div className="help-card">
            <div className="help-icon">
              <Sparkles size={19} />
            </div>

            <strong>Need a little help?</strong>
            <p>Your AI payment companion is here for you.</p>

            <button
              className="help-link"
              onClick={() => selectNav("PayPilot AI")}
            >
              Explore AI guide <ArrowUpRight size={15} />
            </button>
          </div>

          <div className="profile-row">
            <div className="avatar">BT</div>

            <div className="profile-info">
              <strong>Bhoomi Tiwari</strong>
              <span>Personal account</span>
            </div>

            <ChevronDown size={17} className="muted-icon" />
          </div>
        </div>
      </aside>

      <main className="main-content" id="overview">
        <header className="topbar">
          <div className="topbar-left">
            <button
              className="icon-button mobile-menu"
              onClick={() => setMenuOpen(true)}
              aria-label="Open navigation"
            >
              <Menu size={21} />
            </button>

            <div>
              <div className="breadcrumb">
                Workspace / {activeNav}
              </div>
              <h1>{activeNav}</h1>
            </div>
          </div>

          <div className="topbar-actions">
            <label className="search-box">
              <Search size={17} />
              <input
                placeholder="Search anything..."
                aria-label="Search"
              />
              <kbd>/</kbd>
            </label>

            <button
              className="icon-button notification-button"
              aria-label="Notifications"
              onClick={() => setNotice("You're all caught up!")}
            >
              <Bell size={19} />
              <span className="notification-dot" />
            </button>

            <div className="top-avatar">BT</div>
          </div>
        </header>

        {notice && (
          <motion.div
            className="notice-banner"
            initial={{ opacity: 0, y: -8 }}
            animate={{ opacity: 1, y: 0 }}
          >
            <span>{notice}</span>
            <button
              onClick={() => setNotice("")}
              aria-label="Dismiss"
            >
              <X size={16} />
            </button>
          </motion.div>
        )}

        {scannerOpen ? (
          <section className="scanner-page">
            <button
              className="back-link"
              onClick={() => {
                setScannerOpen(false);
                setActiveNav("Overview");
                setNotice("");
              }}
            >
              ← Back to overview
            </button>

            <div className="scanner-heading">
              <div>
                <span className="eyebrow">
                  <Sparkles size={14} /> SMART QR ASSISTANT
                </span>

                <h2>Your next payment, made simpler.</h2>

                <p>
                  Explore how PayBridge can help you understand an
                  unfamiliar QR code before you pay.
                </p>
              </div>
            </div>

            <div className="scanner-layout">
              <motion.div
                className="scanner-card glass-card"
                whileHover={{ y: -4 }}
                transition={{
                  type: "spring",
                  stiffness: 240,
                  damping: 20,
                }}
              >
                <div className="scanner-card-header">
                  <div>
                    <span className="muted-label">
                      QR COMPATIBILITY
                    </span>
                    <h3>Scan a QR code</h3>
                  </div>

                  <span className="status-pill">
                    <span className="status-dot" /> Demo mode
                  </span>
                </div>

                <div className="scan-frame">
                  <div className="scan-corner corner-tl" />
                  <div className="scan-corner corner-tr" />
                  <div className="scan-corner corner-bl" />
                  <div className="scan-corner corner-br" />

                  <div className="mock-qr">
                    {Array.from({ length: 81 }, (_, i) => (
                      <span
                        key={i}
                        className={
                          (i * 7 + Math.floor(i / 3) * 11) % 5 < 2
                            ? "qr-dark"
                            : ""
                        }
                      />
                    ))}
                  </div>

                  <motion.div
                    className="scan-line"
                    animate={{ top: ["15%", "85%", "15%"] }}
                    transition={{
                      duration: 3,
                      repeat: Infinity,
                      ease: "easeInOut",
                    }}
                  />

                  <div className="scan-center-icon">
                    <ScanLine size={24} />
                  </div>
                </div>

                <p className="scanner-hint">
                  <ShieldCheck size={16} />
                  This preview does not access your camera or initiate
                  payments.
                </p>

                <button
                  className="primary-button scanner-action"
                  onClick={() =>
                    setNotice(
                      "Demo scan: real QR decoding and compatibility checks will be connected to your backend."
                    )
                  }
                >
                  <ScanLine size={18} /> Try demo scan
                </button>
              </motion.div>

              <div className="scanner-side">
                <div className="glass-card guidance-card">
                  <div className="feature-icon feature-lavender">
                    <Sparkles size={21} />
                  </div>

                  <h3>What PayBridge checks</h3>

                  <ul className="check-list">
                    <li>
                      <CheckCircle2 size={17} /> QR format recognition
                    </li>
                    <li>
                      <CheckCircle2 size={17} /> Payment provider details
                    </li>
                    <li>
                      <CheckCircle2 size={17} /> Available payment options
                    </li>
                    <li>
                      <CheckCircle2 size={17} /> Safe next-step guidance
                    </li>
                  </ul>
                </div>

                <div className="ai-note">
                  <Sparkles size={19} />

                  <div>
                    <strong>Made for unfamiliar payments</strong>
                    <p>
                      Get clear guidance when you encounter a payment
                      method you don't recognize.
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </section>
        ) : activeNav === "RouteIQ" ? (
          <RouteIntelligence />
        ) : activeNav !== "Overview" ? (
          <FeaturePage
            title={activeNav}
            feature={features.find(
              (item) => item.title === activeNav
            )}
            onAction={handleFeatureAction}
          />
        ) : (
          <>
            <section className="welcome-section">
              <motion.div
                className="welcome-copy"
                initial={{ opacity: 0, y: 16 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.55 }}
              >
                <span className="eyebrow">
                  <Sparkles size={14} /> YOUR MONEY, CONNECTED
                </span>

                <h2>
                  Payments without
                  <br />
                  <span>the boundaries.</span>
                </h2>

                <p>
                  A smarter way to navigate QR payments, discover
                  compatible payment options, and make every transaction
                  feel simpler.
                </p>

                <button
                  className="primary-button"
                  onClick={openScanner}
                >
                  <ScanLine size={18} /> Explore QR assistant
                  <ArrowUpRight size={16} />
                </button>

                <div className="trust-note">
                  <ShieldCheck size={15} />
                  Your payment. Your control.
                </div>
              </motion.div>

              <motion.div
                className="hero-visual"
                initial={{ opacity: 0, scale: 0.94, y: 14 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                transition={{ duration: 0.7, delay: 0.15 }}
              >
                <div className="orbit orbit-one" />
                <div className="orbit orbit-two" />

                <motion.div
                  className="floating-orb orb-one"
                  animate={{ y: [0, -10, 0], rotate: [0, 8, 0] }}
                  transition={{
                    duration: 5,
                    repeat: Infinity,
                    ease: "easeInOut",
                  }}
                />

                <motion.div
                  className="floating-orb orb-two"
                  animate={{ y: [0, 12, 0], x: [0, 5, 0] }}
                  transition={{
                    duration: 6,
                    repeat: Infinity,
                    ease: "easeInOut",
                  }}
                />

                <motion.div
                  className="payment-visual-card avatar-payment-card"
                  whileHover={{
                    rotateY: -3,
                    rotateX: 2,
                    scale: 1.02,
                  }}
                  animate={{ y: [0, -7, 0] }}
                  transition={{
                    y: {
                      duration: 5,
                      repeat: Infinity,
                      ease: "easeInOut",
                    },
                    rotateY: { duration: 0.25 },
                    rotateX: { duration: 0.25 },
                  }}
                >
                  <motion.div
                    className="avatar-welcome-bubble"
                    initial={{ opacity: 0, y: 10, scale: 0.9 }}
                    animate={{ opacity: 1, y: 0, scale: 1 }}
                    transition={{
                      duration: 0.5,
                      delay: 0.3,
                    }}
                  >
                    Welcome to PayBridge!
                  </motion.div>

                  <img
                    src="/avatar.jpg"
                    alt="Bhoomi Tiwari"
                    className="avatar-card-image"
                  />

                  <div className="avatar-card-holder">
                    <span>ACCOUNT HOLDER</span>
                    <strong>Bhoomi Tiwari</strong>
                  </div>
                </motion.div>

                <motion.div
                  className="floating-info floating-info-top"
                  animate={{ y: [0, -5, 0] }}
                  transition={{
                    duration: 4,
                    repeat: Infinity,
                  }}
                >
                  <span className="floating-icon icon-mint">
                    <CheckCircle2 size={18} />
                  </span>

                  <span>
                    <strong>Smart assistance</strong>
                    <small>Guidance at every step</small>
                  </span>
                </motion.div>

                <motion.div
                  className="floating-info floating-info-bottom"
                  animate={{ y: [0, 6, 0] }}
                  transition={{
                    duration: 4.5,
                    repeat: Infinity,
                  }}
                >
                  <span className="floating-icon icon-purple">
                    <Globe2 size={18} />
                  </span>

                  <span>
                    <strong>One connected world</strong>
                    <small>Payments made simpler</small>
                  </span>
                </motion.div>
              </motion.div>
            </section>

            <section className="stats-grid" aria-label="Account summary">
              <motion.div
                className="stat-card glass-card"
                whileHover={{ y: -5, rotateX: 2, rotateY: -2 }}
              >
                <div className="stat-top">
                  <span className="stat-icon lavender-icon">
                    <Wallet size={19} />
                  </span>
                  <span className="stat-caption">DEMO DATA</span>
                </div>

                <span className="stat-label">Total balance</span>
                <strong className="stat-value">₹24,680.50</strong>

                <span className="stat-foot">
                  <span className="positive-text">
                    <ArrowUpRight size={14} /> Available
                  </span>
                  <span>Sample account</span>
                </span>
              </motion.div>

              <motion.div
                className="stat-card glass-card"
                whileHover={{ y: -5, rotateX: 2, rotateY: 2 }}
              >
                <div className="stat-top">
                  <span className="stat-icon peach-icon">
                    <ArrowUpRight size={19} />
                  </span>
                  <span className="stat-caption">THIS MONTH</span>
                </div>

                <span className="stat-label">Money sent</span>
                <strong className="stat-value">₹8,420.00</strong>

                <span className="stat-foot">
                  <span className="neutral-text">
                    Demo transactions
                  </span>
                </span>
              </motion.div>

              <motion.div
                className="stat-card glass-card"
                whileHover={{ y: -5, rotateX: 2, rotateY: -2 }}
              >
                <div className="stat-top">
                  <span className="stat-icon blue-icon">
                    <ArrowDownLeft size={19} />
                  </span>
                  <span className="stat-caption">THIS MONTH</span>
                </div>

                <span className="stat-label">Money received</span>
                <strong className="stat-value">₹12,500.00</strong>

                <span className="stat-foot">
                  <span className="neutral-text">
                    Demo transactions
                  </span>
                </span>
              </motion.div>
            </section>

            <section className="dashboard-grid">
              <div className="transactions-panel glass-card">
                <div className="section-heading">
                  <div>
                    <h3>Recent transactions</h3>
                    <p>Your latest payment activity</p>
                  </div>

                  <button
                    className="text-button"
                    onClick={() =>
                      setNotice(
                        "Transactions preview: transaction history will be connected here."
                      )
                    }
                  >
                    View all <ArrowUpRight size={15} />
                  </button>
                </div>

                <div className="transaction-list">
                  {transactions.map((tx, index) => (
                    <motion.div
                      className="transaction-row"
                      key={tx.name}
                      initial={{ opacity: 0, x: -8 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: index * 0.1 }}
                    >
                      <div
                        className={`transaction-avatar ${tx.color}`}
                      >
                        {tx.initials}
                      </div>

                      <div className="transaction-details">
                        <strong>{tx.name}</strong>
                        <span>{tx.detail}</span>
                      </div>

                      <div className="transaction-amount">
                        <strong
                          className={
                            tx.type === "in" ? "positive-text" : ""
                          }
                        >
                          {tx.amount}
                        </strong>

                        <span>
                          <CheckCircle2 size={12} /> Sample
                        </span>
                      </div>
                    </motion.div>
                  ))}
                </div>
              </div>

              <div className="quick-panel">
                <div className="section-heading">
                  <div>
                    <h3>Quick actions</h3>
                    <p>What would you like to do?</p>
                  </div>
                </div>

                <motion.button
                  className="quick-action glass-card"
                  whileHover={{ y: -3, scale: 1.01 }}
                  onClick={openScanner}
                >
                  <span className="quick-icon lavender-icon">
                    <ScanLine size={21} />
                  </span>

                  <span>
                    <strong>Scan &amp; understand</strong>
                    <small>Explore QR compatibility</small>
                  </span>

                  <ArrowUpRight size={17} className="quick-arrow" />
                </motion.button>

                <motion.button
                  className="quick-action glass-card"
                  whileHover={{ y: -3, scale: 1.01 }}
                  onClick={() =>
                    setNotice(
                      "Payment methods demo. No payment can be initiated."
                    )
                  }
                >
                  <span className="quick-icon blue-icon">
                    <CreditCard size={21} />
                  </span>

                  <span>
                    <strong>Payment methods</strong>
                    <small>Explore available options</small>
                  </span>

                  <ArrowUpRight size={17} className="quick-arrow" />
                </motion.button>

                <motion.button
                  className="quick-action glass-card"
                  whileHover={{ y: -3, scale: 1.01 }}
                  onClick={() => selectNav("PayPilot AI")}
                >
                  <span className="quick-icon peach-icon">
                    <Sparkles size={21} />
                  </span>

                  <span>
                    <strong>Ask AI assistant</strong>
                    <small>Get payment guidance</small>
                  </span>

                  <ArrowUpRight size={17} className="quick-arrow" />
                </motion.button>
              </div>
            </section>

            <section className="features-section">
              <div className="section-heading">
                <div>
                  <span className="eyebrow">
                    BUILT FOR A CONNECTED WORLD
                  </span>
                  <h3>A little smarter at every step.</h3>
                </div>

                <Clock3 size={20} className="muted-icon" />
              </div>

              <div className="features-grid">
                {features.map(
                  (
                    {
                      icon: Icon,
                      title,
                      description,
                      className,
                      action,
                    },
                    i
                  ) => (
                    <motion.button
                      type="button"
                      className="feature-card glass-card"
                      key={title}
                      initial={{ opacity: 0, y: 14 }}
                      animate={{ opacity: 1, y: 0 }}
                      transition={{ delay: i * 0.1 }}
                      whileHover={{
                        y: -6,
                        rotateX: 2,
                        rotateY: -2,
                      }}
                      onClick={() => selectNav(title)}
                    >
                      <span className={`feature-icon ${className}`}>
                        <Icon size={21} />
                      </span>

                      <h4>{title}</h4>
                      <p>{description}</p>

                      <span className="feature-action">
                        {action}
                        <ArrowUpRight size={14} />
                      </span>
                    </motion.button>
                  )
                )}
              </div>
            </section>

            <footer className="footer">
              <span>© 2026 PayBridge · Hackathon prototype</span>
              <span>
                <ShieldCheck size={14} /> Secure-by-design vision
              </span>
            </footer>
          </>
        )}
      </main>
    </div>
  );
}

export default App;
