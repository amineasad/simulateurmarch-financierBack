import os
import sys
import json
from flask import Flask, request, jsonify
from flask_cors import CORS
import importlib.util

# === Réglage sys.path ===
BASE_DIR = os.path.dirname(__file__)
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

# === Chargement Agent de Réclamation ===
AGENT_PATH = os.path.join(BASE_DIR, 'reclamation-agent.py')
spec = importlib.util.spec_from_file_location('reclamation_agent_module', AGENT_PATH)
agent_module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(agent_module)

ReclamationAgent = agent_module.ReclamationAgent

app = Flask(__name__)
CORS(app)

agent = ReclamationAgent()

@app.post('/analyser')
def analyser():
    try:
        data = request.get_json(force=True)
        texte = data.get('texte')
        if not texte or not texte.strip():
            return jsonify({"error": "texte requis"}), 400

        resultat = agent.analyser_reclamation(texte)
        return jsonify(resultat)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.post('/traiter-lot')
def traiter_lot():
    try:
        data = request.get_json(force=True)
        reclamations = data.get('reclamations')
        if not reclamations or not isinstance(reclamations, list):
            return jsonify({"error": "reclamations (liste) requis"}), 400

        resultats = []
        for rec in reclamations:
            texte = rec if isinstance(rec, str) else rec.get('texte')
            if texte and texte.strip():
                resultats.append(agent.analyser_reclamation(texte))
        return jsonify(resultats)
    except Exception as e:
        return jsonify({"error": str(e)}), 500


# === Chargement Pattern Recognition ===
PATTERN_PATH = os.path.join(BASE_DIR, 'pattern_recognition', 'patternrecog.py')
pattern_spec = importlib.util.spec_from_file_location('pattern_recog_module', PATTERN_PATH)
pattern_module = importlib.util.module_from_spec(pattern_spec)

try:
    pattern_spec.loader.exec_module(pattern_module)
    MLTrendPatternDetector = getattr(pattern_module, 'MLTrendPatternDetector', None)
    print("📌 Module patternrecog importé avec succès")
except Exception as e:
    print("❌ ERREUR import patternrecog:", e)
    MLTrendPatternDetector = None


@app.post('/patterns/analyze')
def patterns_analyze():
    try:
        data = request.get_json(force=True)
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

        detector = MLTrendPatternDetector(symbol, period)
        detector.create_technical_features()
        detector.train_models()

        best_name = detector.best_model_name
        best = detector.models.get(best_name, {})
        accuracy = float(best.get('accuracy', 0.0))

        preds = best.get('predictions', [])
        signal = 'INDETERMINE'
        if len(preds) > 0:
            signal = 'HAUSSIER' if int(preds[-1]) == 1 else 'BAISSIER'

        response = {
            "symbol": symbol,
            "signal": signal,
            "confidence": accuracy,
            "detectedPatterns": {},
            "recommendations": [
                "Vérifier tendance multi-échelle",
                "Ajuster stops selon volatilité",
                "Confirmer avec MACD / RSI"
            ],
            "success": True
        }

        if full:
            detector.comprehensive_analysis()

        return jsonify(response)
    except Exception as e:
        return jsonify({"success": False, "errorMessage": str(e)}), 500


# === TRADER ANALYSIS ===
PROJECT_ROOT = os.path.abspath(os.path.join(BASE_DIR, '..', '..'))
TRADER_PATH = os.path.join(PROJECT_ROOT, 'Comportement', 'AnalyseTrader.py')

trader_spec = importlib.util.spec_from_file_location('trader_module', TRADER_PATH)
trader_module = None
try:
    trader_module = importlib.util.module_from_spec(trader_spec)
    trader_spec.loader.exec_module(trader_module)
except Exception:
    trader_module = None


@app.get('/trader/analyze')
def trader_analyze():
    try:
        strategies = []
        if trader_module and hasattr(trader_module, 'run_analysis'):
            raw = trader_module.run_analysis()
            for s in raw:
                strategies.append({
                    "name": s.get('name'),
                    "classification": s.get('classification', 'NEUTRE'),
                    "winRate": float(s.get('winRate', 0.0)),
                    "profitFactor": float(s.get('profitFactor', 1.0)),
                    "pnlTotal": float(s.get('pnlTotal', 0.0))
                })

        if not strategies:
            strategies = [
                {"name": "Momentum", "classification": "GAGNANTE", "winRate": 58.2, "profitFactor": 1.75, "pnlTotal": 5240},
                {"name": "Breakout", "classification": "À RISQUE", "winRate": 42.1, "profitFactor": 0.95, "pnlTotal": -320}
            ]

        return jsonify({"strategies": strategies, "success": True})

    except Exception as e:
        return jsonify({"success": False, "errorMessage": str(e)}), 500


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
