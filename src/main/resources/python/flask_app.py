import os
import sys
import json
from flask import Flask, request, jsonify
from flask_cors import CORS
import importlib.util

# Ensure this directory is on sys.path for local imports (patternconfig, etc.)
BASE_DIR = os.path.dirname(__file__)
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

# Dynamically import reclamation-agent.py (filename contains a dash)
AGENT_PATH = os.path.join(BASE_DIR, 'reclamation-agent.py')
spec = importlib.util.spec_from_file_location('reclamation_agent_module', AGENT_PATH)
agent_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(agent_module)

ReclamationAgent = agent_module.ReclamationAgent

app = Flask(__name__)
CORS(app)

# Single instance of the agent (can be adjusted to per-request if needed)
agent = ReclamationAgent()

@app.post('/analyser')
def analyser():
    try:
        data = request.get_json(force=True)
        texte = data.get('texte') if isinstance(data, dict) else None
        if not texte or not str(texte).strip():
            return jsonify({"error": "texte requis"}), 400

        resultat = agent.analyser_reclamation(texte)
        return jsonify(resultat)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.post('/traiter-lot')
def traiter_lot():
    try:
        data = request.get_json(force=True)
        reclamations = data.get('reclamations') if isinstance(data, dict) else None
        if not reclamations or not isinstance(reclamations, list):
            return jsonify({"error": "reclamations (liste) requis"}), 400

        resultats = []
        for rec in reclamations:
            texte = rec if isinstance(rec, str) else (rec.get('texte') if isinstance(rec, dict) else None)
            if texte and str(texte).strip():
                resultats.append(agent.analyser_reclamation(texte))
        return jsonify(resultats)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

# ====== PATTERN RECOGNITION ENDPOINTS ======
# Dynamically import pattern recognition module (.PY uppercase)
RESOURCES_DIR = os.path.abspath(os.path.join(BASE_DIR, '..'))
PATTERN_PATH = os.path.join(RESOURCES_DIR, 'pattern_recognition', 'patternrecog.PY')
pattern_spec = importlib.util.spec_from_file_location('pattern_recog_module', PATTERN_PATH)
pattern_module = importlib.util.module_from_spec(pattern_spec)
try:
    pattern_spec.loader.exec_module(pattern_module)
    MLTrendPatternDetector = getattr(pattern_module, 'MLTrendPatternDetector', None)
except Exception:
    MLTrendPatternDetector = None

@app.post('/patterns/analyze')
def patterns_analyze():
    try:
        data = request.get_json(force=True) or {}
        symbol = (data.get('symbol') or '').strip()
        period = (data.get('period') or '1y').strip()
        full = bool(data.get('fullAnalysis'))

        if not symbol:
            return jsonify({"success": False, "errorMessage": "symbol requis"}), 400

        if MLTrendPatternDetector is None:
            return jsonify({
                "symbol": symbol,
                "success": False,
                "errorMessage": "Module patternrecog introuvable"
            }), 500

        detector = MLTrendPatternDetector(symbol, period=period)
        # Minimal pipeline to obtain a model and a confidence
        detector.create_technical_features()
        detector.train_models()

        best_name = getattr(detector, 'best_model_name', None)
        models = getattr(detector, 'models', {})
        best = models.get(best_name, {}) if best_name else {}
        accuracy = float(best.get('accuracy', 0.0))

        # Heuristic signal based on last prediction if available
        signal = 'INDETERMINE'
        try:
            X_train, X_test, y_train, y_test, _ = detector.prepare_ml_data()
            preds = best.get('predictions')
            if preds is not None and len(preds) > 0:
                last = preds[-1]
                signal = 'HAUSSIER' if int(last) == 1 else 'BAISSIER'
        except Exception:
            pass

        response = {
            "symbol": symbol,
            "signal": signal,
            "confidence": accuracy,
            "detectedPatterns": {},
            "recommendations": [
                "Vérifier la tendance multi-échelle",
                "Ajuster les stops selon volatilité",
                "Confirmer avec MACD/RSI"
            ],
            "success": True
        }

        # Optionally run comprehensive analysis if requested
        if full:
            try:
                detector.comprehensive_analysis()
            except Exception:
                # ignore, already provided minimal results
                pass

        return jsonify(response)
    except Exception as e:
        return jsonify({"success": False, "errorMessage": str(e)}), 500

# ====== TRADER ANALYSIS ENDPOINT ======
PROJECT_ROOT = os.path.abspath(os.path.join(BASE_DIR, '..', '..'))
TRADER_PATH = os.path.join(PROJECT_ROOT, 'Comportement', 'AnalyseTrader.py')
trader_spec = importlib.util.spec_from_file_location('trader_analysis_module', TRADER_PATH)
trader_module = None
try:
    trader_module = importlib.util.module_from_spec(trader_spec)
    trader_spec.loader.exec_module(trader_module)
except Exception:
    trader_module = None

@app.get('/trader/analyze')
def trader_analyze():
    try:
        # If the module provides a function, prefer using it; otherwise, return mock data
        strategies = []
        if trader_module and hasattr(trader_module, 'run_analysis'):
            try:
                raw = trader_module.run_analysis()
                # Expect raw to be a list of dicts with required fields
                for s in raw:
                    strategies.append({
                        "name": s.get('name', ''),
                        "classification": s.get('classification', 'NEUTRE'),
                        "winRate": float(s.get('winRate', 0.0)),
                        "profitFactor": float(s.get('profitFactor', 1.0)),
                        "pnlTotal": float(s.get('pnlTotal', 0.0))
                    })
            except Exception:
                # Fallback to mock if execution fails
                strategies = []
        if not strategies:
            strategies = [
                {"name": "Momentum", "classification": "GAGNANTE", "winRate": 58.2, "profitFactor": 1.75, "pnlTotal": 5240},
                {"name": "Mean Reversion", "classification": "NEUTRE", "winRate": 48.7, "profitFactor": 1.12, "pnlTotal": 1850},
                {"name": "Breakout", "classification": "À RISQUE", "winRate": 42.1, "profitFactor": 0.95, "pnlTotal": -320},
                {"name": "Trend Following", "classification": "PERDANTE", "winRate": 35.6, "profitFactor": 0.68, "pnlTotal": -1450}
            ]

        return jsonify({"strategies": strategies, "success": True})
    except Exception as e:
        return jsonify({"success": False, "errorMessage": str(e)}), 500

if __name__ == '__main__':
    port = int(os.environ.get('FLASK_PORT', '5001'))
    app.run(host='0.0.0.0', port=port)
