
import { BarChart3, ShieldCheck, Globe2, Sparkles } from "lucide-react";

function RouteIntelligence() {
    return (
        <div style={{ padding: "28px" }}>
            <h2>Route Intelligence</h2>
            <p>
                Understand QR compatibility before choosing a payment route.
            </p>

            <div
                style={{
                    display: "grid",
                    gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))",
                    gap: "16px",
                    marginTop: "24px",
                }}
            >
                <div className="glass-card" style={{ padding: "20px" }}>
                    <Globe2 size={24} />
                    <h3>QR Recognition</h3>
                    <p>Identify the QR format and payment ecosystem.</p>
                </div>

                <div className="glass-card" style={{ padding: "20px" }}>
                    <ShieldCheck size={24} />
                    <h3>Compatibility Check</h3>
                    <p>Review whether a payment route has been verified.</p>
                </div>

                <div className="glass-card" style={{ padding: "20px" }}>
                    <BarChart3 size={24} />
                    <h3>Route Analysis</h3>
                    <p>Compare available routes and their limitations.</p>
                </div>

                <div className="glass-card" style={{ padding: "20px" }}>
                    <Sparkles size={24} />
                    <h3>AI Guidance</h3>
                    <p>Get a clear explanation of the next possible step.</p>
                </div>
            </div>
        </div>
    );
}

export default RouteIntelligence;

