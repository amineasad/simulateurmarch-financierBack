Flask Microservice for Models

Overview
- This Flask app exposes endpoints for:
  - `POST /analyser` and `POST /traiter-lot` (reclamations)
  - `POST /patterns/analyze` (pattern recognition)
  - `GET /trader/analyze` (trader behavior analysis)

Prerequisites
- Python 3.9+
- Install dependencies: `pip install flask flask-cors`
- Optional model deps for patterns: `pip install numpy pandas yfinance scikit-learn xgboost tensorflow` (use only what you need)

Run
- From `src/main/resources/python`:
  - `python flask_app.py`
- The service listens on `http://localhost:5001` by default (set `FLASK_PORT` to change).

Endpoints
- Reclamations
  - `POST /analyser`
    - Body: `{ "texte": "..." }`
    - Response: analysis JSON
  - `POST /traiter-lot`
    - Body: `{ "reclamations": ["...", "..."] }`
    - Response: list of analyses

- Pattern Recognition
  - `POST /patterns/analyze`
    - Body: `{ "symbol": "AAPL", "period": "1y", "fullAnalysis": true }`
    - Response: `{ symbol, signal, confidence, detectedPatterns, recommendations, success }`

- Trader Behavior
  - `GET /trader/analyze`
    - Response: `{ strategies: [ { name, classification, winRate, profitFactor, pnlTotal }, ... ], success }`

Spring Boot Integration
- Properties in `application.properties`:
  - `reclamations.useFlask=true`
  - `reclamations.flask.baseUrl=http://localhost:5001`
  - `patterns.useFlask=true`
  - `patterns.flask.baseUrl=http://localhost:5001`
  - `trader.useFlask=true`
  - `trader.flask.baseUrl=http://localhost:5001`

Notes
- The patterns endpoint loads `pattern_recognition/patternrecog.PY` dynamically.
- The trader endpoint returns mock data unless `Comportement/AnalyseTrader.py` exposes `run_analysis()`.
